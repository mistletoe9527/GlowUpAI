package com.glowupai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AI 衣橱单品仓储。
 */
interface ClosetItemRepository extends JpaRepository<ClosetItemEntity, String> {

    /**
     * 查询指定用户的衣橱单品。
     *
     * @param userId 用户 ID
     * @return 衣橱单品列表
     */
    List<ClosetItemEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 删除指定用户的衣橱单品。
     *
     * @param userId 用户 ID
     * @return 删除数量
     */
    long deleteByUserId(String userId);
}
