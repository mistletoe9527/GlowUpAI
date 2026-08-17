package com.glowupai.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 照片 AES-GCM 加密服务。
 */
@Service
public class PhotoEncryptionService {

    /**
     * 加密文件版本号。
     */
    private static final byte FORMAT_VERSION = 1;

    /**
     * GCM 随机 nonce 字节数。
     */
    private static final int NONCE_BYTES = 12;

    /**
     * GCM 认证标签位数。
     */
    private static final int TAG_BITS = 128;

    /**
     * AES 密钥字节数。
     */
    private static final int AES_KEY_BYTES = 32;

    /**
     * AES-GCM 转换名称。
     */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * 本地加密文件存储模式。
     */
    public static final String ENCRYPTED_LOCAL_STORAGE_MODE = "encrypted_local_file";

    /**
     * S3 加密对象存储模式。
     */
    public static final String ENCRYPTED_S3_STORAGE_MODE = "encrypted_s3_object";

    /**
     * 兼容旧代码的加密存储模式。
     */
    public static final String ENCRYPTED_STORAGE_MODE = ENCRYPTED_LOCAL_STORAGE_MODE;

    /**
     * 兼容早期未加密存储模式。
     */
    public static final String LEGACY_STORAGE_MODE = "local_file";

    /**
     * 安全随机数生成器。
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * AES 密钥。
     */
    private final SecretKeySpec secretKeySpec;

    /**
     * 创建照片加密服务。
     *
     * @param configuredKey 配置中的照片加密密钥
     */
    public PhotoEncryptionService(@Value("${glowup.photo.encryption-key:}") String configuredKey) {
        this.secretKeySpec = new SecretKeySpec(resolveKey(configuredKey), "AES");
    }

    /**
     * 加密照片字节。
     *
     * @param plainBytes 明文字节
     * @return 加密后的文件字节
     */
    public byte[] encrypt(byte[] plainBytes) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] cipherBytes = cipher.doFinal(plainBytes);
            return ByteBuffer.allocate(1 + nonce.length + cipherBytes.length)
                    .put(FORMAT_VERSION)
                    .put(nonce)
                    .put(cipherBytes)
                    .array();
        } catch (Exception exception) {
            throw new IllegalStateException("Photo encryption failed", exception);
        }
    }

    /**
     * 解密照片字节。
     *
     * @param storedBytes 存储字节
     * @param storageMode 存储模式
     * @return 明文字节
     */
    public byte[] decrypt(byte[] storedBytes, String storageMode) {
        if (!isEncryptedStorageMode(storageMode)) {
            return storedBytes;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(storedBytes);
            byte version = buffer.get();
            if (version != FORMAT_VERSION) {
                throw new IllegalArgumentException("Unsupported encrypted photo format");
            }
            byte[] nonce = new byte[NONCE_BYTES];
            buffer.get(nonce);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(cipherBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Photo decryption failed", exception);
        }
    }

    /**
     * 判断是否为加密存储模式。
     *
     * @param storageMode 存储模式
     * @return 是否为加密存储模式
     */
    private boolean isEncryptedStorageMode(String storageMode) {
        return ENCRYPTED_LOCAL_STORAGE_MODE.equals(storageMode)
                || ENCRYPTED_S3_STORAGE_MODE.equals(storageMode);
    }

    /**
     * 解析 AES 密钥。
     *
     * @param configuredKey 配置密钥
     * @return 32 字节 AES 密钥
     */
    private byte[] resolveKey(String configuredKey) {
        if (configuredKey != null && !configuredKey.isBlank()) {
            byte[] decoded = decodeConfiguredKey(configuredKey);
            return Arrays.copyOf(decoded, AES_KEY_BYTES);
        }
        return hashKey("glowup-ai-local-development-photo-key");
    }

    /**
     * 解码配置密钥。
     *
     * @param configuredKey 配置密钥
     * @return 解码后的密钥字节
     */
    private byte[] decodeConfiguredKey(String configuredKey) {
        try {
            return Base64.getDecoder().decode(configuredKey);
        } catch (IllegalArgumentException exception) {
            return hashKey(configuredKey);
        }
    }

    /**
     * 通过 SHA-256 派生 32 字节密钥。
     *
     * @param value 原始密钥文本
     * @return 派生密钥
     */
    private byte[] hashKey(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Photo encryption key derivation failed", exception);
        }
    }
}
