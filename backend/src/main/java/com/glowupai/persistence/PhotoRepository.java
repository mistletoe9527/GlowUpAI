package com.glowupai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * 照片元数据仓储。
 */
interface PhotoRepository extends JpaRepository<PhotoEntity, String> {

    /**
     * 查询指定用户的照片元数据。
     *
     * @param userId 用户 ID
     * @return 照片元数据列表
     */
    List<PhotoEntity> findByUserId(String userId);

    /**
     * 查询指定用户尚未删除的照片元数据。
     *
     * @param userId 用户 ID
     * @return 照片元数据列表
     */
    List<PhotoEntity> findByUserIdAndDeletedFalse(String userId);

    /**
     * 按创建时间倒序查询指定用户尚未删除的照片元数据。
     *
     * @param userId 用户 ID
     * @return 照片元数据列表
     */
    List<PhotoEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);

    /**
     * 按创建时间倒序查询指定用户、指定槽位且未删除的照片元数据。
     *
     * @param userId 用户 ID
     * @param slots 照片槽位集合
     * @return 照片元数据列表
     */
    List<PhotoEntity> findByUserIdAndDeletedFalseAndSlotInOrderByCreatedAtDesc(String userId, Collection<String> slots);

    /**
     * 删除指定用户的照片元数据。
     *
     * @param userId 用户 ID
     * @return 删除数量
     */
    long deleteByUserId(String userId);
}
