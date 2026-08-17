package com.glowupai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 风格报告仓储。
 */
interface StyleReportRepository extends JpaRepository<StyleReportEntity, Long> {

    /**
     * 查询用户最近一次风格报告。
     *
     * @param userId 用户 ID
     * @return 最近一次风格报告
     */
    Optional<StyleReportEntity> findTopByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 删除指定用户的风格报告。
     *
     * @param userId 用户 ID
     * @return 删除数量
     */
    long deleteByUserId(String userId);
}
