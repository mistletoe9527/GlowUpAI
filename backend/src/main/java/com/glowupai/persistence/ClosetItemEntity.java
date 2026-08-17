package com.glowupai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * AI 衣橱单品持久化实体。
 */
@Entity
@Table(name = "closet_items")
public class ClosetItemEntity {

    /**
     * 衣橱单品 ID。
     */
    @Id
    @Column(name = "item_id", length = 64)
    String itemId;

    /**
     * 用户 ID。
     */
    @Column(name = "user_id", length = 64, nullable = false)
    String userId;

    /**
     * 关联照片 ID。
     */
    @Column(name = "photo_id", length = 64, nullable = false)
    String photoId;

    /**
     * 原始文件名。
     */
    @Column(name = "original_name", length = 255)
    String originalName;

    /**
     * 单品品类。
     */
    @Column(name = "category", length = 40, nullable = false)
    String category;

    /**
     * 单品颜色。
     */
    @Column(name = "color", length = 40, nullable = false)
    String color;

    /**
     * 识别品牌。
     */
    @Column(name = "brand", length = 80, nullable = false)
    String brand;

    /**
     * 适用季节。
     */
    @Column(name = "season", length = 40, nullable = false)
    String season;

    /**
     * 风格标签。
     */
    @Column(name = "style", length = 60, nullable = false)
    String style;

    /**
     * 识别来源。
     */
    @Column(name = "source", length = 60, nullable = false)
    String source;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /**
     * JPA 使用的无参构造方法。
     */
    protected ClosetItemEntity() {
    }

    /**
     * 创建衣橱单品实体。
     *
     * @param itemId 单品 ID
     * @param userId 用户 ID
     * @param photoId 照片 ID
     * @param originalName 原始文件名
     * @param category 单品品类
     * @param color 单品颜色
     * @param brand 识别品牌
     * @param season 适用季节
     * @param style 风格标签
     * @param source 识别来源
     */
    ClosetItemEntity(
            String itemId,
            String userId,
            String photoId,
            String originalName,
            String category,
            String color,
            String brand,
            String season,
            String style,
            String source
    ) {
        this.itemId = itemId;
        this.userId = userId;
        this.photoId = photoId;
        this.originalName = originalName;
        this.category = category;
        this.color = color;
        this.brand = brand;
        this.season = season;
        this.style = style;
        this.source = source;
        this.createdAt = Instant.now();
    }

    /**
     * 转换为不可变快照。
     *
     * @return 衣橱单品快照
     */
    ClosetItemSnapshot toSnapshot() {
        return new ClosetItemSnapshot(
                itemId,
                userId,
                photoId,
                originalName,
                category,
                color,
                brand,
                season,
                style,
                source,
                createdAt
        );
    }

    /**
     * 衣橱单品快照。
     *
     * @param itemId 单品 ID
     * @param userId 用户 ID
     * @param photoId 照片 ID
     * @param originalName 原始文件名
     * @param category 单品品类
     * @param color 单品颜色
     * @param brand 识别品牌
     * @param season 适用季节
     * @param style 风格标签
     * @param source 识别来源
     * @param createdAt 创建时间
     */
    public record ClosetItemSnapshot(
            String itemId,
            String userId,
            String photoId,
            String originalName,
            String category,
            String color,
            String brand,
            String season,
            String style,
            String source,
            Instant createdAt
    ) {
    }
}
