package com.glowupai.photo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 照片对象存储服务测试。
 */
class PhotoObjectStorageServiceTest {

    /**
     * 临时上传目录。
     */
    @TempDir
    Path uploadRoot;

    /**
     * 验证本地对象存储可写入、读取和删除。
     *
     * @throws Exception 测试异常
     */
    @Test
    void localStorageWritesReadsAndDeletesObject() throws Exception {
        PhotoObjectStorageService service = new PhotoObjectStorageService(
                uploadRoot.toString(),
                "local",
                "",
                "us-east-1",
                "photos"
        );
        byte[] encryptedBytes = new byte[]{8, 6, 7, 5};

        PhotoObjectStorageService.StoredPhotoObject storedObject = service.store(
                "00000000-0000-0000-0000-000000000001",
                ".png",
                encryptedBytes
        );

        assertEquals(PhotoEncryptionService.ENCRYPTED_LOCAL_STORAGE_MODE, storedObject.storageMode());
        assertTrue(Files.exists(Path.of(storedObject.storagePath())));
        assertArrayEquals(encryptedBytes, service.read(storedObject.storagePath()).orElseThrow());
        assertTrue(service.delete(storedObject.storagePath()));
        assertFalse(service.read(storedObject.storagePath()).isPresent());
    }

    /**
     * 验证 S3 模式缺少 bucket 时会拒绝写入。
     */
    @Test
    void s3StorageRequiresBucketForWrites() {
        PhotoObjectStorageService service = new PhotoObjectStorageService(
                uploadRoot.toString(),
                "s3",
                "",
                "us-east-1",
                "photos"
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                service.store("00000000-0000-0000-0000-000000000002", ".jpg", new byte[]{1})
        );
        assertEquals("GLOWUP_STORAGE_S3_BUCKET is not configured", exception.getMessage());
    }

    /**
     * 验证未知存储提供方会快速失败，避免误写入本地。
     */
    @Test
    void invalidStorageProviderFailsFast() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new PhotoObjectStorageService(
                        uploadRoot.toString(),
                        "invalid",
                        "",
                        "us-east-1",
                        "photos"
                )
        );
        assertEquals("Unsupported photo storage provider: invalid", exception.getMessage());
    }
}
