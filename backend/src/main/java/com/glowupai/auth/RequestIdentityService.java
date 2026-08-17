package com.glowupai.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * 请求身份校验服务。
 */
@Service
public class RequestIdentityService {

    /**
     * iOS 客户端传入当前用户 ID 的请求头。
     */
    public static final String USER_ID_HEADER = "X-GlowUp-User-Id";

    /**
     * Authorization 请求头。
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer token 前缀。
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 是否强制要求身份。
     */
    private final boolean authRequired;

    /**
     * 是否启用 Firebase Auth。
     */
    private final boolean firebaseAuthEnabled;

    /**
     * 身份 token 校验器。
     */
    private final IdentityTokenVerifier identityTokenVerifier;

    /**
     * 创建请求身份校验服务。
     *
     * @param authRequired 是否强制要求身份
     * @param firebaseAuthEnabled 是否启用 Firebase Auth
     * @param identityTokenVerifier 身份 token 校验器
     */
    public RequestIdentityService(
            @Value("${glowup.auth.required:false}") boolean authRequired,
            @Value("${glowup.auth.firebase.enabled:false}") boolean firebaseAuthEnabled,
            IdentityTokenVerifier identityTokenVerifier
    ) {
        this.authRequired = authRequired;
        this.firebaseAuthEnabled = firebaseAuthEnabled;
        this.identityTokenVerifier = identityTokenVerifier;
    }

    /**
     * 读取当前请求身份。
     *
     * @param headerUserId 身份请求头用户 ID
     * @return 当前请求用户 ID
     */
    public Optional<String> currentUserId(String headerUserId) {
        String bearerToken = currentBearerToken();
        if (bearerToken != null) {
            if (!firebaseAuthEnabled) {
                throw new IllegalArgumentException("Firebase Auth is not enabled");
            }
            return Optional.of(identityTokenVerifier.verifyIdToken(bearerToken));
        }
        if (firebaseAuthEnabled && authRequired) {
            throw new IllegalArgumentException("Firebase ID token is required");
        }
        String normalizedHeader = normalize(headerUserId);
        if (normalizedHeader != null) {
            return Optional.of(normalizedHeader);
        }
        if (authRequired) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return Optional.empty();
    }

    /**
     * 校验当前身份可以访问目标用户数据。
     *
     * @param requestedUserId 业务请求中的用户 ID
     * @param headerUserId 身份请求头用户 ID
     * @return 可信用户 ID
     */
    public String requireUserAccess(String requestedUserId, String headerUserId) {
        String normalizedRequest = normalize(requestedUserId);
        Optional<String> currentUserId = currentUserId(headerUserId);
        if (currentUserId.isEmpty()) {
            return normalizedRequest == null ? "" : normalizedRequest;
        }
        if (normalizedRequest != null && !currentUserId.get().equals(normalizedRequest)) {
            throw new IllegalArgumentException("Authenticated user does not match requested user");
        }
        return currentUserId.get();
    }

    /**
     * 判断身份校验是否启用。
     *
     * @return 是否强制身份校验
     */
    public boolean authRequired() {
        return authRequired;
    }

    /**
     * 判断 Firebase Auth 是否启用。
     *
     * @return Firebase Auth 是否启用
     */
    public boolean firebaseAuthEnabled() {
        return firebaseAuthEnabled;
    }

    /**
     * 读取当前请求 Bearer token。
     *
     * @return Bearer token
     */
    private String currentBearerToken() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String authorization = attributes.getRequest().getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return normalize(authorization.substring(BEARER_PREFIX.length()));
    }

    /**
     * 标准化用户 ID。
     *
     * @param value 原始用户 ID
     * @return 标准化用户 ID
     */
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
