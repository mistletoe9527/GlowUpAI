package com.glowupai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 订阅记录持久化实体。
 */
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

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
     * 订阅层级。
     */
    @Column(name = "tier", length = 40, nullable = false)
    String tier;

    /**
     * 套餐标签。
     */
    @Column(name = "plan", length = 40, nullable = false)
    String plan;

    /**
     * 展示价格。
     */
    @Column(name = "price", length = 40)
    String price;

    /**
     * 订阅状态。
     */
    @Column(name = "status", length = 60, nullable = false)
    String status;

    /**
     * 到期时间。
     */
    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /**
     * JPA 使用的无参构造方法。
     */
    protected SubscriptionEntity() {
    }

    /**
     * 创建订阅记录实体。
     *
     * @param userId 用户 ID
     * @param tier 订阅层级
     * @param plan 套餐标签
     * @param price 展示价格
     * @param status 订阅状态
     * @param expiresAt 到期时间
     */
    SubscriptionEntity(String userId, String tier, String plan, String price, String status, Instant expiresAt) {
        this.userId = userId;
        this.tier = tier;
        this.plan = plan;
        this.price = price;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    /**
     * 获取订阅层级。
     *
     * @return 订阅层级
     */
    String tier() {
        return tier;
    }

    /**
     * 获取套餐标签。
     *
     * @return 套餐标签
     */
    String plan() {
        return plan;
    }

    /**
     * 获取展示价格。
     *
     * @return 展示价格
     */
    String price() {
        return price;
    }

    /**
     * 获取订阅状态。
     *
     * @return 订阅状态
     */
    String status() {
        return status;
    }

    /**
     * 获取到期时间。
     *
     * @return 到期时间
     */
    Instant expiresAt() {
        return expiresAt;
    }
}
