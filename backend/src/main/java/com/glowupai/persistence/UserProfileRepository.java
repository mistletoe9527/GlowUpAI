package com.glowupai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户资料仓储。
 */
interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {
}
