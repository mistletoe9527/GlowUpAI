package com.glowupai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 埋点事件仓储。
 */
interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, Long> {

    /**
     * 删除指定用户的埋点事件。
     *
     * @param userId 用户 ID
     * @return 删除数量
     */
    long deleteByUserId(String userId);
}
