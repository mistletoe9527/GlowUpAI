package com.glowupai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowupai.persistence.PersistenceService;
import com.glowupai.persistence.PersistenceService.StoredPhotoData;
import com.glowupai.style.StyleModels.ChatMessageResponse;
import com.glowupai.style.StyleModels.DailyLookResponse;
import com.glowupai.style.StyleModels.PaletteResponse;
import com.glowupai.style.StyleModels.StyleAnalyzeRequest;
import com.glowupai.style.StyleModels.StyleReportResponse;
import com.glowupai.style.StyleModels.UploadSummaryRequest;
import com.glowupai.style.StyleModels.UserProfileRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Responses API 风格分析器。
 */
@Service
public class OpenAiStyleAnalyzer {

    /**
     * AI 配置。
     */
    private final AiProperties aiProperties;

    /**
     * 持久化服务。
     */
    private final PersistenceService persistenceService;

    /**
     * JSON 处理器。
     */
    private final ObjectMapper objectMapper;

    /**
     * HTTP 客户端构建器。
     */
    private final RestTemplateBuilder restTemplateBuilder;

    /**
     * 创建 OpenAI 风格分析器。
     *
     * @param aiProperties AI 配置
     * @param persistenceService 持久化服务
     * @param objectMapper JSON 处理器
     * @param restTemplateBuilder HTTP 客户端构建器
     */
    public OpenAiStyleAnalyzer(
            AiProperties aiProperties,
            PersistenceService persistenceService,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.aiProperties = aiProperties;
        this.persistenceService = persistenceService;
        this.objectMapper = objectMapper;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    /**
     * 使用 OpenAI 生成风格报告。
     *
     * @param request 风格分析请求
     * @return 风格报告
     */
    public StyleReportResponse analyze(StyleAnalyzeRequest request) {
        validateConfiguration();
        List<StoredPhotoData> photos = persistenceService.loadStoredPhotos(request);
        if (photos.isEmpty()) {
            throw new IllegalStateException("No stored photos are available for OpenAI analysis");
        }
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(aiProperties.getOpenaiApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                aiProperties.getOpenaiBaseUrl(),
                new HttpEntity<>(requestBody(request, photos), headers),
                JsonNode.class
        );
        JsonNode body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("OpenAI response body is empty");
        }
        return parseStyleReport(body);
    }

    /**
     * 使用 OpenAI 生成聊天回复。
     *
     * @param message 用户消息
     * @param profile 用户资料
     * @param uploads 聊天附带照片
     * @return 聊天回复
     */
    public ChatMessageResponse chat(String message, UserProfileRequest profile, List<UploadSummaryRequest> uploads) {
        validateConfiguration();
        List<StoredPhotoData> photos = persistenceService.loadStoredPhotos(uploads);
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(aiProperties.getOpenaiApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                aiProperties.getOpenaiBaseUrl(),
                new HttpEntity<>(chatRequestBody(message, profile, uploads, photos), headers),
                JsonNode.class
        );
        JsonNode body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("OpenAI response body is empty");
        }
        String reply = findOutputText(body).trim();
        if (reply.isBlank()) {
            throw new IllegalStateException("OpenAI chat response does not contain output text");
        }
        return new ChatMessageResponse(AiSafetyPolicy.guardChatReply(reply));
    }

    /**
     * 校验 OpenAI 配置。
     */
    private void validateConfiguration() {
        if (aiProperties.getOpenaiApiKey() == null || aiProperties.getOpenaiApiKey().isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
    }

    /**
     * 构造 Responses API 请求体。
     *
     * @param request 风格分析请求
     * @param photos 已存储照片
     * @return 请求体
     */
    private Map<String, Object> requestBody(StyleAnalyzeRequest request, List<StoredPhotoData> photos) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiProperties.getOpenaiModel());
        body.put("input", List.of(messageContent(request, photos)));
        body.put("text", Map.of("format", responseFormat()));
        return body;
    }

    /**
     * 构造聊天 Responses API 请求体。
     *
     * @param message 用户消息
     * @param profile 用户资料
     * @param uploads 聊天附带照片摘要
     * @param photos 已存储照片
     * @return 请求体
     */
    private Map<String, Object> chatRequestBody(
            String message,
            UserProfileRequest profile,
            List<UploadSummaryRequest> uploads,
            List<StoredPhotoData> photos
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiProperties.getOpenaiModel());
        body.put("input", List.of(chatMessageContent(message, profile, uploads, photos)));
        return body;
    }

    /**
     * 构造用户消息内容。
     *
     * @param request 风格分析请求
     * @param photos 已存储照片
     * @return 用户消息
     */
    private Map<String, Object> messageContent(StyleAnalyzeRequest request, List<StoredPhotoData> photos) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "input_text",
                "text", prompt(request)
        ));
        photos.stream()
                .filter(photo -> supportedContentType(photo.contentType()))
                .forEach(photo -> content.add(Map.of(
                        "type", "input_image",
                        "image_url", dataUrl(photo)
                )));
        if (content.size() == 1) {
            throw new IllegalStateException("No OpenAI-supported image type is available");
        }
        return Map.of(
                "role", "user",
                "content", content
        );
    }

    /**
     * 构造聊天用户消息内容。
     *
     * @param message 用户消息
     * @param profile 用户资料
     * @param uploads 聊天附带照片摘要
     * @param photos 已存储照片
     * @return 用户消息
     */
    private Map<String, Object> chatMessageContent(
            String message,
            UserProfileRequest profile,
            List<UploadSummaryRequest> uploads,
            List<StoredPhotoData> photos
    ) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "input_text",
                "text", chatPrompt(message, profile, uploads, photos)
        ));
        photos.stream()
                .filter(photo -> supportedContentType(photo.contentType()))
                .forEach(photo -> content.add(Map.of(
                        "type", "input_image",
                        "image_url", dataUrl(photo)
                )));
        return Map.of(
                "role", "user",
                "content", content
        );
    }

    /**
     * 构造分析提示词。
     *
     * @param request 风格分析请求
     * @return 提示词
     */
    private String prompt(StyleAnalyzeRequest request) {
        return """
                You are an expert personal stylist in the United States.
                Analyze the user's photos and profile. Be positive, practical, and non-judgmental.
                %s
                Focus on confidence, fit, silhouette, colors, hair direction, makeup direction, and outfit formulas.
                User profile:
                - style goal: %s
                - gender: %s
                - birthday: %s
                - height: %s
                - weight: %s
                - location: %s
                Return only the requested JSON object.
                """.formatted(
                AiSafetyPolicy.instruction(),
                AiSafetyPolicy.safeStyleGoal(request.profile().styleGoal()),
                safe(request.profile().gender()),
                safe(request.profile().birthday()),
                safe(request.profile().height()),
                safe(request.profile().weight()),
                safe(request.profile().location())
        );
    }

    /**
     * 构造聊天提示词。
     *
     * @param message 用户消息
     * @param profile 用户资料
     * @param uploads 聊天附带照片摘要
     * @param photos 已存储照片
     * @return 聊天提示词
     */
    private String chatPrompt(
            String message,
            UserProfileRequest profile,
            List<UploadSummaryRequest> uploads,
            List<StoredPhotoData> photos
    ) {
        return """
                You are an expert personal stylist in the United States.
                Answer the user's style question in 2 to 4 concise sentences.
                Be positive, practical, and non-judgmental.
                %s
                Focus on confidence, fit, silhouette, color, occasion, and one or two actionable improvements.
                User profile:
                - style goal: %s
                - gender: %s
                - birthday: %s
                - height: %s
                - weight: %s
                - location: %s
                Attached upload slots: %s
                Provider-readable images: %d
                User question: %s
                """.formatted(
                AiSafetyPolicy.instruction(),
                AiSafetyPolicy.safeStyleGoal(profile == null ? null : profile.styleGoal()),
                safe(profile == null ? null : profile.gender()),
                safe(profile == null ? null : profile.birthday()),
                safe(profile == null ? null : profile.height()),
                safe(profile == null ? null : profile.weight()),
                safe(profile == null ? null : profile.location()),
                uploadSlots(uploads),
                photos.stream().filter(photo -> supportedContentType(photo.contentType())).count(),
                safe(message)
        );
    }

    /**
     * 构造结构化输出配置。
     *
     * @return 结构化输出配置
     */
    private Map<String, Object> responseFormat() {
        return Map.of(
                "type", "json_schema",
                "name", "glowup_style_report",
                "strict", true,
                "schema", styleReportSchema()
        );
    }

    /**
     * 构造风格报告 JSON Schema。
     *
     * @return JSON Schema
     */
    private Map<String, Object> styleReportSchema() {
        Map<String, Object> stringArray = Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );
        Map<String, Object> paletteItem = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "color", Map.of("type", "string")
                ),
                "required", List.of("name", "color")
        );
        Map<String, Object> dailyLook = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "occasion", Map.of("type", "string"),
                        "top", Map.of("type", "string"),
                        "bottom", Map.of("type", "string"),
                        "shoes", Map.of("type", "string"),
                        "why", Map.of("type", "string")
                ),
                "required", List.of("occasion", "top", "bottom", "shoes", "why")
        );
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.ofEntries(
                        Map.entry("badge", Map.of("type", "string")),
                        Map.entry("heroTitle", Map.of("type", "string")),
                        Map.entry("heroCopy", Map.of("type", "string")),
                        Map.entry("score", Map.of("type", "integer", "minimum", 0, "maximum", 100)),
                        Map.entry("description", Map.of("type", "string")),
                        Map.entry("faceShape", Map.of("type", "string")),
                        Map.entry("hair", stringArray),
                        Map.entry("makeup", stringArray),
                        Map.entry("bodyRatio", Map.of("type", "string")),
                        Map.entry("bodyTips", stringArray),
                        Map.entry("colors", stringArray),
                        Map.entry("bestColors", stringArray),
                        Map.entry("strengths", stringArray),
                        Map.entry("improvements", stringArray),
                        Map.entry("palette", Map.of("type", "array", "items", paletteItem)),
                        Map.entry("dailyLook", dailyLook),
                        Map.entry("source", Map.of("type", "string"))
                ),
                "required", List.of(
                        "badge",
                        "heroTitle",
                        "heroCopy",
                        "score",
                        "description",
                        "faceShape",
                        "hair",
                        "makeup",
                        "bodyRatio",
                        "bodyTips",
                        "colors",
                        "bestColors",
                        "strengths",
                        "improvements",
                        "palette",
                        "dailyLook",
                        "source"
                )
        );
    }

    /**
     * 解析 OpenAI 响应中的风格报告。
     *
     * @param body OpenAI 响应体
     * @return 风格报告
     */
    private StyleReportResponse parseStyleReport(JsonNode body) {
        String outputText = findOutputText(body);
        if (outputText.isBlank()) {
            throw new IllegalStateException("OpenAI response does not contain output text");
        }
        try {
            StyleReportResponse response = objectMapper.readValue(outputText, StyleReportResponse.class);
            if (response == null) {
                return AiSafetyPolicy.guardStyleReport(null);
            }
            return AiSafetyPolicy.guardStyleReport(new StyleReportResponse(
                    response.badge(),
                    response.heroTitle(),
                    response.heroCopy(),
                    response.score(),
                    response.description(),
                    response.faceShape(),
                    response.hair(),
                    response.makeup(),
                    response.bodyRatio(),
                    response.bodyTips(),
                    response.colors(),
                    response.bestColors(),
                    response.strengths(),
                    response.improvements(),
                    response.palette(),
                    response.dailyLook(),
                    "openai_vision"
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("OpenAI style report JSON could not be parsed", exception);
        }
    }

    /**
     * 递归查找 Responses API 输出文本。
     *
     * @param node JSON 节点
     * @return 输出文本
     */
    private String findOutputText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isObject()) {
            JsonNode type = node.get("type");
            JsonNode text = node.get("text");
            if (type != null && "output_text".equals(type.asText()) && text != null) {
                return text.asText();
            }
            for (JsonNode child : node) {
                String value = findOutputText(child);
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String value = findOutputText(child);
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    /**
     * 判断文件类型是否可发送给 OpenAI 图片输入。
     *
     * @param contentType 文件 MIME 类型
     * @return 是否支持
     */
    private boolean supportedContentType(String contentType) {
        return "image/jpeg".equalsIgnoreCase(contentType)
                || "image/png".equalsIgnoreCase(contentType)
                || "image/webp".equalsIgnoreCase(contentType)
                || "image/gif".equalsIgnoreCase(contentType);
    }

    /**
     * 构造图片 data URL。
     *
     * @param photo 照片数据
     * @return data URL
     */
    private String dataUrl(StoredPhotoData photo) {
        return "data:%s;base64,%s".formatted(
                photo.contentType(),
                Base64.getEncoder().encodeToString(photo.bytes())
        );
    }

    /**
     * 生成上传槽位摘要。
     *
     * @param uploads 上传照片摘要
     * @return 槽位摘要
     */
    private String uploadSlots(List<UploadSummaryRequest> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            return "None";
        }
        return uploads.stream()
                .map(UploadSummaryRequest::slot)
                .filter(slot -> slot != null && !slot.isBlank())
                .distinct()
                .toList()
                .toString();
    }

    /**
     * 安全字符串。
     *
     * @param value 原始值
     * @return 安全文本
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
