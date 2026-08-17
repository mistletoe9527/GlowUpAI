package com.glowupai.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 照片对象存储服务。
 */
@Service
public class PhotoObjectStorageService {

    /**
     * 上传根目录。
     */
    private final Path uploadRoot;

    /**
     * 存储提供方。
     */
    private final PhotoStorageProvider provider;

    /**
     * S3 bucket。
     */
    private final String s3Bucket;

    /**
     * S3 region。
     */
    private final String s3Region;

    /**
     * S3 key 前缀。
     */
    private final String s3Prefix;

    /**
     * 延迟创建的 S3 客户端。
     */
    private S3Client s3Client;

    /**
     * 创建照片对象存储服务。
     *
     * @param uploadDir 本地上传目录
     * @param provider 存储提供方配置
     * @param s3Bucket S3 bucket
     * @param s3Region S3 region
     * @param s3Prefix S3 key 前缀
     */
    public PhotoObjectStorageService(
            @Value("${glowup.upload-dir:uploads}") String uploadDir,
            @Value("${glowup.storage.provider:local}") String provider,
            @Value("${glowup.storage.s3.bucket:}") String s3Bucket,
            @Value("${glowup.storage.s3.region:us-east-1}") String s3Region,
            @Value("${glowup.storage.s3.prefix:photos}") String s3Prefix
    ) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.provider = PhotoStorageProvider.fromValue(provider);
        this.s3Bucket = s3Bucket;
        this.s3Region = s3Region;
        this.s3Prefix = normalizePrefix(s3Prefix);
    }

    /**
     * 存储加密照片对象。
     *
     * @param photoId 照片 ID
     * @param extension 文件扩展名
     * @param encryptedBytes 加密后的照片字节
     * @return 已存储对象
     */
    public StoredPhotoObject store(String photoId, String extension, byte[] encryptedBytes) {
        if (provider == PhotoStorageProvider.S3) {
            return storeToS3(photoId, extension, encryptedBytes);
        }
        return storeToLocalFile(photoId, extension, encryptedBytes);
    }

    /**
     * 读取已存储对象字节。
     *
     * @param storagePath 存储路径
     * @return 对象字节
     */
    public Optional<byte[]> read(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return Optional.empty();
        }
        if (storagePath.startsWith("s3://")) {
            return readFromS3(storagePath);
        }
        return readFromLocalFile(storagePath);
    }

    /**
     * 删除已存储对象。
     *
     * @param storagePath 存储路径
     * @return 是否删除成功
     */
    public boolean delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        if (storagePath.startsWith("s3://")) {
            return deleteFromS3(storagePath);
        }
        return deleteFromLocalFile(storagePath);
    }

    /**
     * 本地写入加密照片。
     *
     * @param photoId 照片 ID
     * @param extension 文件扩展名
     * @param encryptedBytes 加密后的照片字节
     * @return 已存储对象
     */
    private StoredPhotoObject storeToLocalFile(String photoId, String extension, byte[] encryptedBytes) {
        Path target = uploadRoot.resolve(photoId + extension + ".enc").normalize();
        try {
            Files.createDirectories(uploadRoot);
            Files.write(target, encryptedBytes);
            return new StoredPhotoObject(target.toString(), PhotoEncryptionService.ENCRYPTED_LOCAL_STORAGE_MODE);
        } catch (IOException exception) {
            throw new IllegalStateException("Photo local upload failed", exception);
        }
    }

    /**
     * S3 写入加密照片。
     *
     * @param photoId 照片 ID
     * @param extension 文件扩展名
     * @param encryptedBytes 加密后的照片字节
     * @return 已存储对象
     */
    private StoredPhotoObject storeToS3(String photoId, String extension, byte[] encryptedBytes) {
        validateS3Configuration();
        String key = s3Key(photoId, extension);
        s3Client().putObject(
                PutObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(key)
                        .contentType("application/octet-stream")
                        .build(),
                RequestBody.fromBytes(encryptedBytes)
        );
        return new StoredPhotoObject("s3://%s/%s".formatted(s3Bucket, key), PhotoEncryptionService.ENCRYPTED_S3_STORAGE_MODE);
    }

    /**
     * 从本地文件读取对象。
     *
     * @param storagePath 存储路径
     * @return 对象字节
     */
    private Optional<byte[]> readFromLocalFile(String storagePath) {
        try {
            Path path = Path.of(storagePath);
            if (!Files.exists(path)) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    /**
     * 从 S3 读取对象。
     *
     * @param storagePath 存储路径
     * @return 对象字节
     */
    private Optional<byte[]> readFromS3(String storagePath) {
        try {
            S3ObjectAddress address = parseS3Address(storagePath);
            return Optional.of(s3Client().getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(address.bucket())
                            .key(address.key())
                            .build())
                    .asByteArray());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * 删除本地文件对象。
     *
     * @param storagePath 存储路径
     * @return 是否删除成功
     */
    private boolean deleteFromLocalFile(String storagePath) {
        try {
            return Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Photo local delete failed", exception);
        }
    }

    /**
     * 删除 S3 对象。
     *
     * @param storagePath 存储路径
     * @return 是否删除成功
     */
    private boolean deleteFromS3(String storagePath) {
        S3ObjectAddress address = parseS3Address(storagePath);
        s3Client().deleteObject(DeleteObjectRequest.builder()
                .bucket(address.bucket())
                .key(address.key())
                .build());
        return true;
    }

    /**
     * 获取 S3 客户端。
     *
     * @return S3 客户端
     */
    private S3Client s3Client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .region(Region.of(s3Region))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build();
        }
        return s3Client;
    }

    /**
     * 校验 S3 配置。
     */
    private void validateS3Configuration() {
        if (s3Bucket == null || s3Bucket.isBlank()) {
            throw new IllegalStateException("GLOWUP_STORAGE_S3_BUCKET is not configured");
        }
    }

    /**
     * 生成 S3 key。
     *
     * @param photoId 照片 ID
     * @param extension 文件扩展名
     * @return S3 key
     */
    private String s3Key(String photoId, String extension) {
        String fileName = photoId + extension + ".enc";
        return s3Prefix.isBlank() ? fileName : s3Prefix + "/" + fileName;
    }

    /**
     * 解析 S3 地址。
     *
     * @param storagePath 存储路径
     * @return S3 地址
     */
    private S3ObjectAddress parseS3Address(String storagePath) {
        URI uri = URI.create(storagePath);
        String bucket = uri.getHost();
        String key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (bucket == null || bucket.isBlank() || key.isBlank()) {
            throw new IllegalArgumentException("Invalid S3 storage path");
        }
        return new S3ObjectAddress(bucket, key);
    }

    /**
     * 标准化 S3 key 前缀。
     *
     * @param prefix 原始前缀
     * @return 标准化前缀
     */
    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    /**
     * 已存储照片对象。
     *
     * @param storagePath 存储路径
     * @param storageMode 存储模式
     */
    public record StoredPhotoObject(String storagePath, String storageMode) {
    }

    /**
     * S3 对象地址。
     *
     * @param bucket bucket 名称
     * @param key 对象 key
     */
    private record S3ObjectAddress(String bucket, String key) {
    }
}
