package com.glowupai.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 用户资料持久化实体。
 */
@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

    /**
     * 用户 ID，当前 MVP 由前端生成，生产版本可替换为 Firebase UID。
     */
    @Id
    @Column(name = "user_id", length = 64)
    String userId;

    /**
     * 用户昵称。
     */
    @Column(name = "name", length = 120)
    String name;

    /**
     * 登录方式。
     */
    @Column(name = "auth_method", length = 40)
    String authMethod;

    /**
     * 登录邮箱。
     */
    @Column(name = "email", length = 160)
    String email;

    /**
     * 风格目标。
     */
    @Column(name = "style_goal", length = 80)
    String styleGoal;

    /**
     * 性别标签。
     */
    @Column(name = "gender", length = 40)
    String gender;

    /**
     * 生日字符串。
     */
    @Column(name = "birthday", length = 20)
    String birthday;

    /**
     * 身高字符串。
     */
    @Column(name = "height", length = 40)
    String height;

    /**
     * 体重字符串。
     */
    @Column(name = "weight", length = 40)
    String weight;

    /**
     * 所在地区国家码。
     */
    @Column(name = "location", length = 120)
    String location;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /**
     * 更新时间。
     */
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    /**
     * JPA 使用的无参构造方法。
     */
    protected UserProfileEntity() {
    }

    /**
     * 创建用户资料实体。
     *
     * @param userId 用户 ID
     */
    UserProfileEntity(String userId) {
        Instant now = Instant.now();
        this.userId = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 更新保存时间。
     */
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    /**
     * 更新用户资料字段。
     *
     * @param name 用户昵称
     * @param authMethod 登录方式
     * @param email 登录邮箱
     * @param styleGoal 风格目标
     * @param gender 性别标签
     * @param birthday 生日
     * @param height 身高
     * @param weight 体重
     * @param location 所在地区国家码
     */
    void updateProfile(String name, String authMethod, String email, String styleGoal, String gender, String birthday, String height, String weight, String location) {
        this.name = name;
        this.authMethod = authMethod;
        this.email = email;
        this.styleGoal = styleGoal;
        this.gender = gender;
        this.birthday = birthday;
        this.height = height;
        this.weight = weight;
        this.location = location;
        this.updatedAt = Instant.now();
    }

    /**
     * 转换为用户资料请求模型。
     *
     * @return 用户资料请求模型
     */
    com.glowupai.style.StyleModels.UserProfileRequest toUserProfileRequest() {
        return new com.glowupai.style.StyleModels.UserProfileRequest(
                userId,
                name,
                authMethod,
                email,
                styleGoal,
                gender,
                birthday,
                height,
                weight,
                location
        );
    }
}
