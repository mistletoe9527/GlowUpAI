package com.glowupai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 上传照片元数据持久化实体。
 */
@Entity
@Table(name = "photos")
public class PhotoEntity {

    /**
     * 照片 ID。
     */
    @Id
    @Column(name = "photo_id", length = 64)
    String photoId;

    /**
     * 用户 ID。
     */
    @Column(name = "user_id", length = 64)
    String userId;

    /**
     * 照片槽位。
     */
    @Column(name = "slot", length = 24, nullable = false)
    String slot;

    /**
     * 原始文件名。
     */
    @Column(name = "original_name", length = 255)
    String originalName;

    /**
     * 文件 MIME 类型。
     */
    @Column(name = "content_type", length = 80)
    String contentType;

    /**
     * 文件大小。
     */
    @Column(name = "file_size", nullable = false)
    long fileSize;

    /**
     * 存储模式。
     */
    @Column(name = "storage_mode", length = 40, nullable = false)
    String storageMode;

    /**
     * 存储路径。
     */
    @Column(name = "storage_path", length = 500)
    String storagePath;

    /**
     * 是否已删除。
     */
    @Column(name = "deleted", nullable = false)
    boolean deleted;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /**
     * 删除时间。
     */
    @Column(name = "deleted_at")
    Instant deletedAt;

    /**
     * JPA 使用的无参构造方法。
     */
    protected PhotoEntity() {
    }

    /**
     * 创建照片元数据实体。
     *
     * @param photoId 照片 ID
     * @param userId 用户 ID
     * @param slot 照片槽位
     * @param originalName 原始文件名
     * @param contentType 文件 MIME 类型
     * @param fileSize 文件大小
     * @param storageMode 存储模式
     * @param storagePath 存储路径
     */
    PhotoEntity(String photoId, String userId, String slot, String originalName, String contentType, long fileSize, String storageMode, String storagePath) {
        this.photoId = photoId;
        this.userId = userId;
        this.slot = slot;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageMode = storageMode;
        this.storagePath = storagePath;
        this.deleted = false;
        this.createdAt = Instant.now();
    }

    /**
     * 标记照片已删除。
     */
    void markDeleted() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }

    /**
     * 获取照片 ID。
     *
     * @return 照片 ID
     */
    String photoId() {
        return photoId;
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    String userId() {
        return userId;
    }

    /**
     * 获取照片槽位。
     *
     * @return 照片槽位
     */
    String slot() {
        return slot;
    }

    /**
     * 获取原始文件名。
     *
     * @return 原始文件名
     */
    String originalName() {
        return originalName;
    }

    /**
     * 获取文件 MIME 类型。
     *
     * @return 文件 MIME 类型
     */
    String contentType() {
        return contentType;
    }

    /**
     * 获取文件大小。
     *
     * @return 文件大小
     */
    long fileSize() {
        return fileSize;
    }

    /**
     * 获取存储模式。
     *
     * @return 存储模式
     */
    String storageMode() {
        return storageMode;
    }

    /**
     * 获取存储路径。
     *
     * @return 存储路径
     */
    String storagePath() {
        return storagePath;
    }

    /**
     * 判断照片是否已删除。
     *
     * @return 是否已删除
     */
    boolean deleted() {
        return deleted;
    }
}
