package com.glowupai.ai;

import com.glowupai.persistence.PersistenceService;
import com.glowupai.style.StyleModels.ChatMessageResponse;
import com.glowupai.style.StyleModels.StyleAnalyzeRequest;
import com.glowupai.style.StyleModels.StyleReportResponse;
import com.glowupai.style.StyleModels.UploadSummaryRequest;
import com.glowupai.style.StyleModels.UserProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OpenAI 风格分析器单元测试。
 */
class OpenAiStyleAnalyzerTest {

    /**
     * 验证 OpenAI 返回空报告对象时会走安全兜底。
     *
     * @throws Exception 测试异常
     */
    @Test
    void analyzeReturnsFallbackWhenModelReturnsNullReport() throws Exception {
        AtomicReference<String> capturedRequestBody = new AtomicReference<>("");
        HttpServer server = startFakeOpenAiServer(capturedRequestBody, "null");
        try {
            AiProperties aiProperties = new AiProperties();
            aiProperties.setProvider("openai");
            aiProperties.setOpenaiApiKey("test-key");
            aiProperties.setOpenaiBaseUrl("http://127.0.0.1:%d/v1/responses".formatted(server.getAddress().getPort()));
            PersistenceService persistenceService = mock(PersistenceService.class);
            when(persistenceService.loadStoredPhotos(any(StyleAnalyzeRequest.class))).thenReturn(List.of(
                    new PersistenceService.StoredPhotoData("face-id", "face", "image/png", new byte[]{1, 2, 3})
            ));
            OpenAiStyleAnalyzer analyzer = new OpenAiStyleAnalyzer(
                    aiProperties,
                    persistenceService,
                    new ObjectMapper(),
                    new RestTemplateBuilder()
            );

            StyleReportResponse response = analyzer.analyze(new StyleAnalyzeRequest(
                    testProfile(),
                    List.of(
                            new UploadSummaryRequest("face-id", "face", "face.png", "image/png", 3),
                            new UploadSummaryRequest("body-id", "body", "body.png", "image/png", 3)
                    )
            ));

            assertEquals("Style Direction", response.badge());
            assertEquals(72, response.score());
            assertEquals(3, response.palette().size());
            assertEquals("guarded_ai", response.source());
            assertTrue(capturedRequestBody.get().contains("\"type\":\"input_image\""));
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证聊天请求会携带照片并解析 OpenAI 输出文本。
     *
     * @throws Exception 测试异常
     */
    @Test
    void chatSendsImageAndParsesOutputText() throws Exception {
        AtomicReference<String> capturedRequestBody = new AtomicReference<>("");
        HttpServer server = startFakeOpenAiServer(
                capturedRequestBody,
                "Yes, keep the silhouette clean and repeat one strong color."
        );
        try {
            AiProperties aiProperties = new AiProperties();
            aiProperties.setProvider("openai");
            aiProperties.setOpenaiApiKey("test-key");
            aiProperties.setOpenaiBaseUrl("http://127.0.0.1:%d/v1/responses".formatted(server.getAddress().getPort()));
            PersistenceService persistenceService = mock(PersistenceService.class);
            when(persistenceService.loadStoredPhotos(anyList())).thenReturn(List.of(
                    new PersistenceService.StoredPhotoData("outfit-id", "outfit", "image/png", new byte[]{1, 2, 3})
            ));
            OpenAiStyleAnalyzer analyzer = new OpenAiStyleAnalyzer(
                    aiProperties,
                    persistenceService,
                    new ObjectMapper(),
                    new RestTemplateBuilder()
            );

            ChatMessageResponse response = analyzer.chat(
                    "Can I wear this?",
                    testProfile(),
                    List.of(new UploadSummaryRequest("outfit-id", "outfit", "outfit.png", "image/png", 3))
            );

            assertEquals("Yes, keep the silhouette clean and repeat one strong color.", response.reply());
            assertTrue(capturedRequestBody.get().contains("\"type\":\"input_image\""));
            assertTrue(capturedRequestBody.get().contains("data:image/png;base64,AQID"));
            assertTrue(capturedRequestBody.get().contains("Can I wear this?"));
            assertTrue(capturedRequestBody.get().contains("Do not rate, rank, or judge attractiveness"));
            assertTrue(capturedRequestBody.get().contains("Create a polished, confident, photo-ready look without rating attractiveness."));
            assertFalse(capturedRequestBody.get().contains("style goal: Look more attractive"));
        } finally {
            server.stop(0);
        }
    }

    /**
     * 验证不安全的 OpenAI 聊天输出不会原样返回。
     *
     * @throws Exception 测试异常
     */
    @Test
    void chatReplacesUnsafeModelReply() throws Exception {
        AtomicReference<String> capturedRequestBody = new AtomicReference<>("");
        HttpServer server = startFakeOpenAiServer(capturedRequestBody, "You look ugly and your body rating is low.");
        try {
            AiProperties aiProperties = new AiProperties();
            aiProperties.setProvider("openai");
            aiProperties.setOpenaiApiKey("test-key");
            aiProperties.setOpenaiBaseUrl("http://127.0.0.1:%d/v1/responses".formatted(server.getAddress().getPort()));
            PersistenceService persistenceService = mock(PersistenceService.class);
            when(persistenceService.loadStoredPhotos(anyList())).thenReturn(List.of());
            OpenAiStyleAnalyzer analyzer = new OpenAiStyleAnalyzer(
                    aiProperties,
                    persistenceService,
                    new ObjectMapper(),
                    new RestTemplateBuilder()
            );

            ChatMessageResponse response = analyzer.chat(
                    "Do I look ugly?",
                    new UserProfileRequest(
                            "user-1",
                            "Emma",
                            "Apple",
                            "emma@example.com",
                            "Find my style",
                            "Female",
                            "1998-01-15",
                            "5'6\"",
                            "125 lb",
                            "New York, NY"
                    ),
                    List.of()
            );

            assertEquals("I cannot judge attractiveness, body value, race, ethnicity, or exact age. I can help with styling choices: keep the silhouette intentional, repeat one strong color, and adjust one fit detail at a time.", response.reply());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 构造测试用户资料。
     *
     * @return 测试用户资料
     */
    private UserProfileRequest testProfile() {
        return new UserProfileRequest(
                "user-1",
                "Emma",
                "Apple",
                "emma@example.com",
                "Look more attractive",
                "Female",
                "1998-01-15",
                "5'6\"",
                "125 lb",
                "New York, NY"
        );
    }

    /**
     * 启动本地 OpenAI 假服务。
     *
     * @param capturedRequestBody 捕获的请求体
     * @param outputText 输出文本
     * @return HTTP 服务
     * @throws IOException 启动异常
     */
    private HttpServer startFakeOpenAiServer(AtomicReference<String> capturedRequestBody, String outputText) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            capturedRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = """
                    {
                      "output": [
                        {
                          "content": [
                            {
                              "type": "output_text",
                              "text": "%s"
                            }
                          ]
                        }
                      ]
                    }
                    """.formatted(outputText).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }
}
