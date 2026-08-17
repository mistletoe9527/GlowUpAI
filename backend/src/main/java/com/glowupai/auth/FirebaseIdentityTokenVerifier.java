package com.glowupai.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Firebase ID token 校验器。
 */
@Service
public class FirebaseIdentityTokenVerifier implements IdentityTokenVerifier {

    /**
     * Firebase SecureToken issuer 前缀。
     */
    private static final String FIREBASE_ISSUER_PREFIX = "https://securetoken.google.com/";

    /**
     * Firebase 项目 ID。
     */
    private final String projectId;

    /**
     * Firebase 公钥证书地址。
     */
    private final String publicCertsUrl;

    /**
     * Google ID token 校验器。
     */
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    /**
     * 创建 Firebase ID token 校验器。
     *
     * @param projectId Firebase 项目 ID
     * @param publicCertsUrl Firebase 公钥证书地址
     */
    public FirebaseIdentityTokenVerifier(
            @Value("${glowup.auth.firebase.project-id:}") String projectId,
            @Value("${glowup.auth.firebase.public-certs-url:https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com}") String publicCertsUrl
    ) {
        this.projectId = projectId;
        this.publicCertsUrl = publicCertsUrl;
    }

    /**
     * 校验 Firebase ID token 并返回 Firebase uid。
     *
     * @param idToken Firebase ID token
     * @return Firebase uid
     */
    @Override
    public String verifyIdToken(String idToken) {
        try {
            GoogleIdToken token = googleIdTokenVerifier().verify(idToken);
            if (token == null || token.getPayload() == null || token.getPayload().getSubject() == null) {
                throw new IllegalArgumentException("Invalid Firebase ID token");
            }
            return token.getPayload().getSubject();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Firebase ID token");
        }
    }

    /**
     * 获取 Google ID token 校验器。
     *
     * @return Google ID token 校验器
     */
    private synchronized GoogleIdTokenVerifier googleIdTokenVerifier() {
        if (googleIdTokenVerifier != null) {
            return googleIdTokenVerifier;
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("Firebase project ID is not configured");
        }
        GooglePublicKeysManager publicKeysManager = new GooglePublicKeysManager.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setPublicCertsEncodedUrl(publicCertsUrl)
                .build();
        googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(publicKeysManager)
                .setAudience(List.of(projectId.trim()))
                .setIssuer(FIREBASE_ISSUER_PREFIX + projectId.trim())
                .build();
        return googleIdTokenVerifier;
    }
}
