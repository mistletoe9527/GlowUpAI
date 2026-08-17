package com.glowupai.photo;

import com.glowupai.style.StyleModels.PhotoUploadResponse;
import com.glowupai.style.StyleModels.PhotoDeletionResponse;
import com.glowupai.style.StyleModels.UploadSlot;
import com.glowupai.persistence.PersistenceService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 照片上传存储服务。
 */
@Service
public class PhotoStorageService {

    /**
     * 单张照片最大字节数。
     */
    private static final long MAX_PHOTO_SIZE = 10L * 1024L * 1024L;

    /**
     * 允许上传的图片 MIME 类型。
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/heic",
            "image/heif"
    );

    /**
     * 照片对象存储服务。
     */
    private final PhotoObjectStorageService photoObjectStorageService;

    /**
     * 持久化服务。
     */
    private final PersistenceService persistenceService;

    /**
     * 照片加密服务。
     */
    private final PhotoEncryptionService photoEncryptionService;

    /**
     * 创建照片本地存储服务。
     *
     * @param photoObjectStorageService 照片对象存储服务
     * @param persistenceService 持久化服务
     * @param photoEncryptionService 照片加密服务
     */
    public PhotoStorageService(
            PhotoObjectStorageService photoObjectStorageService,
            PersistenceService persistenceService,
            PhotoEncryptionService photoEncryptionService
    ) {
        this.photoObjectStorageService = photoObjectStorageService;
        this.persistenceService = persistenceService;
        this.photoEncryptionService = photoEncryptionService;
    }

    /**
     * 存储上传照片。
     *
     * @param userId 用户 ID
     * @param slotKey 照片槽位 key
     * @param file 上传文件
     * @return 照片上传响应
     */
    public PhotoUploadResponse store(String userId, String slotKey, MultipartFile file) {
        UploadSlot slot = UploadSlot.requireKey(slotKey);
        validateFile(file);
        String photoId = UUID.randomUUID().toString();
        String extension = resolveExtension(file);
        try {
            byte[] encryptedBytes = photoEncryptionService.encrypt(file.getBytes());
            PhotoObjectStorageService.StoredPhotoObject storedObject = photoObjectStorageService.store(photoId, extension, encryptedBytes);
            PhotoUploadResponse response = new PhotoUploadResponse(
                    photoId,
                    slot.key(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    storedObject.storageMode()
            );
            persistenceService.savePhoto(userId, response, storedObject.storagePath());
            return response;
        } catch (Exception exception) {
            throw new IllegalStateException("Photo upload failed", exception);
        }
    }

    /**
     * 删除已上传照片。
     *
     * @param photoId 照片 ID
     * @return 是否删除成功
     */
    public boolean delete(String photoId) {
        validatePhotoId(photoId);
        return persistenceService.findPhotoStoragePath(photoId)
                .map(storagePath -> deleteStoredObject(photoId, storagePath))
                .orElse(false);
    }

    /**
     * 删除指定用户的所有风格评估照片。
     *
     * @param userId 用户 ID
     * @return 照片删除响应
     */
    public PhotoDeletionResponse deleteAllForUser(String userId) {
        List<PersistenceService.StoredPhotoReference> references = persistenceService.findUndeletedStylePhotoReferencesForUser(userId);
        long objectsDeleted = references.stream()
                .filter(reference -> deleteStoredObjectIfPresent(reference.storagePath()))
                .count();
        references.forEach(reference -> persistenceService.markPhotoDeleted(reference.photoId()));
        return new PhotoDeletionResponse(userId, references.size(), objectsDeleted);
    }

    /**
     * 删除已存储对象并标记元数据。
     *
     * @param photoId 照片 ID
     * @param storagePath 存储路径
     * @return 是否删除成功
     */
    private boolean deleteStoredObject(String photoId, String storagePath) {
        boolean deleted = deleteStoredObjectIfPresent(storagePath);
        if (deleted) {
            persistenceService.markPhotoDeleted(photoId);
        }
        return deleted;
    }

    /**
     * 删除存在的照片对象。
     *
     * @param storagePath 存储路径
     * @return 是否删除了实际对象
     */
    private boolean deleteStoredObjectIfPresent(String storagePath) {
        return photoObjectStorageService.delete(storagePath);
    }

    /**
     * 校验上传文件。
     *
     * @param file 上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file is required");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("Each photo must be 10MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.US))) {
            throw new IllegalArgumentException("Only jpg, png, heic, or heif photos are supported");
        }
    }

    /**
     * 校验照片 ID。
     *
     * @param photoId 照片 ID
     */
    private void validatePhotoId(String photoId) {
        if (photoId == null || !photoId.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid photo id");
        }
    }

    /**
     * 解析安全文件扩展名。
     *
     * @param file 上传文件
     * @return 文件扩展名
     */
    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/heic".equalsIgnoreCase(contentType)) {
            return ".heic";
        }
        if ("image/heif".equalsIgnoreCase(contentType)) {
            return ".heif";
        }
        return ".jpg";
    }
}
