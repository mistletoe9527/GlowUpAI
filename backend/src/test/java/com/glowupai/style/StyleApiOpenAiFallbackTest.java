package com.glowupai.style;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OpenAI provider 回退行为集成测试。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:glowup-openai-fallback-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "glowup.upload-dir=target/test-uploads-openai-fallback",
        "glowup.ai.provider=openai",
        "glowup.ai.openai-api-key="
})
@AutoConfigureMockMvc
class StyleApiOpenAiFallbackTest {

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
     * 验证 OpenAI 未配置密钥时自动回退到 mock 报告。
     *
     * @throws Exception 请求异常
     */
    @Test
    void analyzeFallsBackToMockWhenOpenAiKeyMissing() throws Exception {
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture("fallback-user-1", "face", "face.png");
        StyleModels.PhotoUploadResponse bodyPhoto = uploadPhotoFixture("fallback-user-1", "body", "body.png");

        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "fallback-user-1",
                                    "name": "Emma",
                                    "authMethod": "Apple",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my style",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "5'6\\"",
                                    "location": "New York, NY"
                                  },
                                  "uploads": [
                                    {"photoId": "%s", "slot": "face", "name": "face.png", "type": "image/png", "size": 4},
                                    {"photoId": "%s", "slot": "body", "name": "body.png", "type": "image/png", "size": 4}
                                  ]
                                }
                                """.formatted(facePhoto.photoId(), bodyPhoto.photoId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.badge", is("Soft Signature")))
                .andExpect(jsonPath("$.data.makeup", hasSize(2)))
                .andExpect(jsonPath("$.data.source", is("backend_mock")));
    }

    /**
     * 验证 OpenAI 未配置密钥时聊天自动回退到本地规则。
     *
     * @throws Exception 请求异常
     */
    @Test
    void chatFallsBackToMockWhenOpenAiKeyMissing() throws Exception {
        startSubscriptionFixture("fallback-chat-user-1", "Monthly");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "fallback-chat-user-1",
                                    "name": "Emma",
                                    "authMethod": "Apple",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my style",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "5'6\\"",
                                    "location": "New York, NY"
                                  },
                                  "message": "Can I wear this to work?",
                                  "uploads": [
                                    {"photoId": "outfit-id", "slot": "outfit", "name": "outfit.png", "type": "image/png", "size": 1200}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.reply", containsString("outfit photo")));
    }

    /**
     * 创建测试订阅记录。
     *
     * @param userId 用户 ID
     * @param plan 套餐标签
     * @throws Exception 请求异常
     */
    private void startSubscriptionFixture(String userId, String plan) throws Exception {
        mockMvc.perform(post("/api/subscriptions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "plan": "%s"
                                }
                                """.formatted(userId, plan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
    }

    /**
     * 上传测试照片并返回上传响应。
     *
     * @param userId 用户 ID
     * @param slot 照片槽位
     * @param fileName 文件名
     * @return 照片上传响应
     * @throws Exception 请求异常
     */
    private StyleModels.PhotoUploadResponse uploadPhotoFixture(String userId, String slot, String fileName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "image/png",
                new byte[]{1, 2, 3, 4}
        );
        String responseBody = mockMvc.perform(multipart("/api/photos")
                        .file(file)
                        .param("userId", userId)
                        .param("slot", slot))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        return objectMapper.treeToValue(data, StyleModels.PhotoUploadResponse.class);
    }
}
