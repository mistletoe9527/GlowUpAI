package com.glowupai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 风格报告持久化实体。
 */
@Entity
@Table(name = "style_reports")
public class StyleReportEntity {

    /**
     * 数据库主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * 用户 ID。
     */
    @Column(name = "user_id", length = 64)
    String userId;

    /**
     * 风格类型。
     */
    @Column(name = "style_type", length = 120)
    String styleType;

    /**
     * 风格分数。
     */
    @Column(name = "style_score")
    Integer styleScore;

    /**
     * 数据来源。
     */
    @Column(name = "source", length = 80)
    String source;

    /**
     * 报告完整 JSON。
     */
    @Lob
    @Column(name = "report_json")
    String reportJson;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /**
     * JPA 使用的无参构造方法。
     */
    protected StyleReportEntity() {
    }

    /**
     * 创建风格报告实体。
     *
     * @param userId 用户 ID
     * @param styleType 风格类型
     * @param styleScore 风格分数
     * @param source 数据来源
     * @param reportJson 报告完整 JSON
     */
    StyleReportEntity(String userId, String styleType, Integer styleScore, String source, String reportJson) {
        this.userId = userId;
        this.styleType = styleType;
        this.styleScore = styleScore;
        this.source = source;
        this.reportJson = reportJson;
        this.createdAt = Instant.now();
    }

    /**
     * 获取报告完整 JSON。
     *
     * @return 报告完整 JSON
     */
    String reportJson() {
        return reportJson;
    }
}
