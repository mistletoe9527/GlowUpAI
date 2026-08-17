package com.glowupai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 订阅记录仓储。
 */
interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    /**
     * 查询用户最近一次订阅记录。
     *
     * @param userId 用户 ID
     * @return 最近一次订阅记录
     */
    Optional<SubscriptionEntity> findTopByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 删除指定用户的订阅记录。
     *
     * @param userId 用户 ID
     * @return 删除数量
     */
    long deleteByUserId(String userId);
}
