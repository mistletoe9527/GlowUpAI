package com.glowupai.auth;

/**
 * 第三方身份 token 校验器。
 */
public interface IdentityTokenVerifier {

    /**
     * 校验 ID token 并返回用户 ID。
     *
     * @param idToken ID token
     * @return 用户 ID
     */
    String verifyIdToken(String idToken);
}
