package com.glowupai.style;

import com.glowupai.auth.IdentityTokenVerifier;
import com.glowupai.auth.RequestIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Firebase Auth 身份边界集成测试。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:glowup-firebase-auth-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "glowup.upload-dir=target/test-uploads-firebase-auth",
        "glowup.auth.required=true",
        "glowup.auth.firebase.enabled=true"
})
@AutoConfigureMockMvc
class StyleApiFirebaseAuthTest {

    /**
     * Mock MVC 客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证 Firebase 模式下不接受本地身份头替代 ID token。
     *
     * @throws Exception 请求异常
     */
    @Test
    void firebaseModeRequiresBearerTokenInsteadOfUserIdHeader() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .header(RequestIdentityService.USER_ID_HEADER, "firebase-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("firebase-user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Firebase ID token is required")));
    }

    /**
     * 验证 Firebase token 用户和业务用户一致时允许保存资料。
     *
     * @throws Exception 请求异常
     */
    @Test
    void firebaseBearerTokenAllowsMatchingUserRequest() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .header(RequestIdentityService.AUTHORIZATION_HEADER, "Bearer valid-firebase-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("firebase-user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.userId", is("firebase-user-1")))
                .andExpect(jsonPath("$.data.status", is("saved")));
    }

    /**
     * 验证 Firebase token 用户不能访问其他用户数据。
     *
     * @throws Exception 请求异常
     */
    @Test
    void firebaseBearerTokenRejectsMismatchedUserRequest() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .header(RequestIdentityService.AUTHORIZATION_HEADER, "Bearer valid-firebase-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("other-user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Authenticated user does not match requested user")));
    }

    /**
     * 生成资料 JSON。
     *
     * @param userId 用户 ID
     * @return 资料 JSON
     */
    private String profileJson(String userId) {
        return """
                {
                  "userId": "%s",
                  "name": "Firebase User",
                  "authMethod": "Firebase",
                  "email": "firebase@example.com",
                  "styleGoal": "Find my style",
                  "gender": "Female",
                  "birthday": "1992-01-01",
                  "height": "5'6\\"",
                  "location": "Seattle, WA"
                }
                """.formatted(userId);
    }

    /**
     * Firebase Auth 测试配置。
     */
    @TestConfiguration
    static class FirebaseAuthTestConfiguration {

        /**
         * 创建测试用 ID token 校验器。
         *
         * @return 测试用 ID token 校验器
         */
        @Bean
        @Primary
        IdentityTokenVerifier identityTokenVerifier() {
            return idToken -> {
                if ("valid-firebase-token".equals(idToken)) {
                    return "firebase-user-1";
                }
                throw new IllegalArgumentException("Invalid Firebase ID token");
            };
        }
    }
}
