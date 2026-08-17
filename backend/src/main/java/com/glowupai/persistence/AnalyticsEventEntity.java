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
 * 埋点事件持久化实体。
 */
@Entity
@Table(name = "analytics_events")
public class AnalyticsEventEntity {

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
     * 事件名。
     */
    @Column(name = "event_name", length = 100, nullable = false)
    String eventName;

    /**
     * 事件参数 JSON。
     */
    @Lob
    @Column(name = "payload_json")
    String payloadJson;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /**
     * JPA 使用的无参构造方法。
     */
    protected AnalyticsEventEntity() {
    }

    /**
     * 创建埋点事件实体。
     *
     * @param userId 用户 ID
     * @param eventName 事件名
     * @param payloadJson 事件参数 JSON
     */
    AnalyticsEventEntity(String userId, String eventName, String payloadJson) {
        this.userId = userId;
        this.eventName = eventName;
        this.payloadJson = payloadJson;
        this.createdAt = Instant.now();
    }
}
