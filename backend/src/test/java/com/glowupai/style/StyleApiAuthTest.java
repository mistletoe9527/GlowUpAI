package com.glowupai.style;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowupai.auth.RequestIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 身份边界集成测试。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:glowup-auth-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "glowup.upload-dir=target/test-uploads-auth",
        "glowup.auth.required=true"
})
@AutoConfigureMockMvc
class StyleApiAuthTest {

    /**
     * Mock MVC 客户端。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * JSON 解析器。
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 验证强身份模式会拒绝缺失身份头的用户请求。
     *
     * @throws Exception 请求异常
     */
    @Test
    void protectedUserRequestRequiresIdentityHeader() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "auth-user-1",
                                  "name": "Auth User",
                                  "authMethod": "Email",
                                  "email": "auth@example.com",
                                  "styleGoal": "Find my style",
                                  "gender": "Female",
                                  "birthday": "1992-01-01",
                                  "height": "5'6\\"",
                                  "location": "Seattle, WA"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Authenticated user is required")));
    }

    /**
     * 验证身份头和业务用户 ID 不一致时会被拒绝。
     *
     * @throws Exception 请求异常
     */
    @Test
    void identityHeaderMustMatchRequestedUser() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .header(RequestIdentityService.USER_ID_HEADER, "auth-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "auth-user-2",
                                  "name": "Auth User",
                                  "authMethod": "Email",
                                  "email": "auth@example.com",
                                  "styleGoal": "Find my style",
                                  "gender": "Female",
                                  "birthday": "1992-01-01",
                                  "height": "5'6\\"",
                                  "location": "Seattle, WA"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Authenticated user does not match requested user")));
    }

    /**
     * 验证用户资料读取不能跨用户访问。
     *
     * @throws Exception 请求异常
     */
    @Test
    void userProfileReadRequiresOwnerIdentity() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .header(RequestIdentityService.USER_ID_HEADER, "profile-owner-auth-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "profile-owner-auth-1",
                                  "name": "Auth User",
                                  "authMethod": "Email",
                                  "email": "auth@example.com",
                                  "styleGoal": "Find my style",
                                  "gender": "Female",
                                  "birthday": "1992-01-01",
                                  "height": "5'6\\"",
                                  "location": "Seattle, WA"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        mockMvc.perform(get("/api/users/profile")
                        .header(RequestIdentityService.USER_ID_HEADER, "profile-attacker-auth-1")
                        .param("userId", "profile-owner-auth-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Authenticated user does not match requested user")));

        mockMvc.perform(get("/api/users/profile")
                        .header(RequestIdentityService.USER_ID_HEADER, "profile-owner-auth-1")
                        .param("userId", "profile-owner-auth-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.userId", is("profile-owner-auth-1")));
    }

    /**
     * 验证照片删除必须由照片归属用户发起。
     *
     * @throws Exception 请求异常
     */
    @Test
    void photoDeletionRequiresPhotoOwnerIdentity() throws Exception {
        String photoId = uploadPhotoFixture("photo-owner-auth-1");

        mockMvc.perform(delete("/api/photos/{photoId}", photoId)
                        .header(RequestIdentityService.USER_ID_HEADER, "photo-attacker-auth-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Photo does not belong to authenticated user")));

        mockMvc.perform(delete("/api/photos/{photoId}", photoId)
                        .header(RequestIdentityService.USER_ID_HEADER, "photo-owner-auth-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.deleted", is(true)));
    }

    /**
     * 上传测试照片并返回照片 ID。
     *
     * @param userId 用户 ID
     * @return 照片 ID
     * @throws Exception 请求异常
     */
    private String uploadPhotoFixture(String userId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "auth-face.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );
        String responseBody = mockMvc.perform(multipart("/api/photos")
                        .file(file)
                        .header(RequestIdentityService.USER_ID_HEADER, userId)
                        .param("userId", userId)
                        .param("slot", "face"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        return data.path("photoId").asText();
    }
}
