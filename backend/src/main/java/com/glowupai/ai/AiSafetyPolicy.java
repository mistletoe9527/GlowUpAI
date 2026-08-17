package com.glowupai.ai;

import com.glowupai.style.StyleModels.DailyLookResponse;
import com.glowupai.style.StyleModels.PaletteResponse;
import com.glowupai.style.StyleModels.StyleGoal;
import com.glowupai.style.StyleModels.StyleReportResponse;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * AI 造型建议安全策略。
 */
public final class AiSafetyPolicy {

    /**
     * 安全策略提示词。
     */
    private static final String SAFETY_INSTRUCTION = """
            Safety policy:
            - Give style guidance only for clothing fit, silhouette, color, grooming direction, occasion, and confidence.
            - Do not rate, rank, or judge attractiveness, beauty, body value, race, ethnicity, or exact age.
            - Do not shame body shape, facial features, skin, weight, or any protected attribute.
            - If the user asks for those judgments, briefly refuse that judgment and redirect to styling choices.
            """;

    /**
     * 聊天安全兜底回复。
     */
    private static final String SAFE_CHAT_FALLBACK = "I cannot judge attractiveness, body value, race, ethnicity, or exact age. I can help with styling choices: keep the silhouette intentional, repeat one strong color, and adjust one fit detail at a time.";

    /**
     * 报告字段安全兜底文案。
     */
    private static final String SAFE_REPORT_FALLBACK = "Use fit, color, and silhouette choices to make the look feel intentional.";

    /**
     * 身形维度安全兜底文案。
     */
    private static final String SAFE_BODY_RATIO_FALLBACK = "Lengthened vertical line";

    /**
     * 脸型维度安全兜底文案。
     */
    private static final String SAFE_FACE_SHAPE_FALLBACK = "Face-framing direction";

    /**
     * 报告色板安全兜底。
     */
    private static final List<PaletteResponse> SAFE_PALETTE_FALLBACK = List.of(
            new PaletteResponse("Ivory", "#F7F1E7"),
            new PaletteResponse("Soft Navy", "#26334D"),
            new PaletteResponse("Sage", "#8FA58E")
    );

    /**
     * 不允许出现在 AI 输出中的模式。
     */
    private static final List<Pattern> UNSAFE_OUTPUT_PATTERNS = List.of(
            Pattern.compile("\\b(ugly|unattractive|hideous|hot|sexy)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(attractiveness|beauty|body)\\s+(score|rating|rank|value)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(rate|rank|judge)\\s+(your|the)\\s+(look|face|body|attractiveness|beauty)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(fat|obese|overweight|underweight|skinny|flabby|chubby)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(race|ethnicity|ethnic\\s+background)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(look|looks|appear|appears)\\s+(asian|black|white|latina|latino|hispanic|middle\\s+eastern|indian|native)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(look|looks|appear|appears)\\s+(young|old|younger|older|\\d{1,3})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(exact\\s+age|age\\s+is|years\\s+old)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\blong\\s+legs\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 私有构造方法，避免策略类被实例化。
     */
    private AiSafetyPolicy() {
    }

    /**
     * 获取可复用安全提示词。
     *
     * @return 安全提示词
     */
    public static String instruction() {
        return SAFETY_INSTRUCTION;
    }

    /**
     * 将用户目标转译成安全的造型任务。
     *
     * @param styleGoal 用户风格目标
     * @return 安全风格目标
     */
    public static String safeStyleGoal(String styleGoal) {
        StyleGoal goal = StyleGoal.fromLabel(styleGoal);
        if (goal == StyleGoal.LOOK_MORE_ATTRACTIVE) {
            return "Create a polished, confident, photo-ready look without rating attractiveness.";
        }
        return safeText(styleGoal, "Find practical styling improvements.");
    }

    /**
     * 守护聊天回复。
     *
     * @param reply 原始回复
     * @return 安全回复
     */
    public static String guardChatReply(String reply) {
        if (!hasText(reply)) {
            return SAFE_CHAT_FALLBACK;
        }
        if (containsUnsafeOutput(reply)) {
            return SAFE_CHAT_FALLBACK;
        }
        return reply.trim();
    }

    /**
     * 守护风格报告响应。
     *
     * @param response 原始报告
     * @return 安全报告
     */
    public static StyleReportResponse guardStyleReport(StyleReportResponse response) {
        if (response == null) {
            return fallbackStyleReport();
        }
        return new StyleReportResponse(
                safeText(response.badge(), "Style Direction"),
                safeText(response.heroTitle(), "Polished Daily Formula"),
                safeText(response.heroCopy(), SAFE_REPORT_FALLBACK),
                safeScore(response.score()),
                safeText(response.description(), SAFE_REPORT_FALLBACK),
                safeText(response.faceShape(), SAFE_FACE_SHAPE_FALLBACK),
                safeList(response.hair(), "Soft face-framing layers"),
                safeList(response.makeup(), "Fresh, balanced grooming"),
                safeText(response.bodyRatio(), SAFE_BODY_RATIO_FALLBACK),
                safeList(response.bodyTips(), "Use structure and proportion to guide the outfit"),
                safeList(response.colors(), "Ivory"),
                safeList(response.bestColors(), "Ivory"),
                safeList(response.strengths(), "Clear styling direction"),
                safeList(response.improvements(), "Adjust one styling detail at a time"),
                safePalette(response.palette()),
                safeDailyLook(response.dailyLook()),
                safeText(response.source(), "guarded_ai")
        );
    }

    /**
     * 判断文本是否包含不安全输出。
     *
     * @param value 待检查文本
     * @return 是否不安全
     */
    public static boolean containsUnsafeOutput(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.US);
        return UNSAFE_OUTPUT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(normalized).find());
    }

    /**
     * 守护日常穿搭响应。
     *
     * @param dailyLook 原始日常穿搭
     * @return 安全日常穿搭
     */
    private static DailyLookResponse safeDailyLook(DailyLookResponse dailyLook) {
        if (dailyLook == null) {
            return new DailyLookResponse(
                    "Daily",
                    "Clean top",
                    "Straight bottom",
                    "Polished shoe",
                    SAFE_REPORT_FALLBACK
            );
        }
        return new DailyLookResponse(
                safeText(dailyLook.occasion(), "Daily"),
                safeText(dailyLook.top(), "Clean top"),
                safeText(dailyLook.bottom(), "Straight bottom"),
                safeText(dailyLook.shoes(), "Polished shoe"),
                safeText(dailyLook.why(), SAFE_REPORT_FALLBACK)
        );
    }

    /**
     * 生成完整风格报告兜底响应。
     *
     * @return 安全风格报告
     */
    private static StyleReportResponse fallbackStyleReport() {
        return new StyleReportResponse(
                "Style Direction",
                "Polished Daily Formula",
                SAFE_REPORT_FALLBACK,
                72,
                SAFE_REPORT_FALLBACK,
                SAFE_FACE_SHAPE_FALLBACK,
                List.of("Soft face-framing layers"),
                List.of("Fresh, balanced grooming"),
                SAFE_BODY_RATIO_FALLBACK,
                List.of("Use structure and proportion to guide the outfit"),
                List.of("Ivory", "Soft Navy", "Sage"),
                List.of("Ivory", "Soft Navy"),
                List.of("Clear styling direction"),
                List.of("Adjust one styling detail at a time"),
                SAFE_PALETTE_FALLBACK,
                safeDailyLook(null),
                "guarded_ai"
        );
    }

    /**
     * 守护风格分数。
     *
     * @param score 原始分数
     * @return 0 到 100 之间的安全分数
     */
    private static int safeScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 守护色板列表。
     *
     * @param values 原始色板
     * @return 安全色板
     */
    private static List<PaletteResponse> safePalette(List<PaletteResponse> values) {
        if (values == null || values.isEmpty()) {
            return SAFE_PALETTE_FALLBACK;
        }
        List<PaletteResponse> safeValues = values.stream()
                .filter(value -> value != null)
                .map(value -> new PaletteResponse(
                        safeText(value.name(), "Ivory"),
                        safeColor(value.color(), "#F7F1E7")
                ))
                .distinct()
                .toList();
        return safeValues.isEmpty() ? SAFE_PALETTE_FALLBACK : safeValues;
    }

    /**
     * 守护色值文本。
     *
     * @param color 原始色值
     * @param fallback 兜底色值
     * @return 安全色值
     */
    private static String safeColor(String color, String fallback) {
        if (!hasText(color)) {
            return fallback;
        }
        String trimmed = color.trim();
        if (!trimmed.matches("^#?[0-9a-fA-F]{6}$")) {
            return fallback;
        }
        return trimmed.startsWith("#") ? trimmed : "#" + trimmed;
    }

    /**
     * 守护字符串列表。
     *
     * @param values 原始列表
     * @param fallback 兜底值
     * @return 安全列表
     */
    private static List<String> safeList(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return List.of(fallback);
        }
        List<String> safeValues = values.stream()
                .map(value -> safeText(value, fallback))
                .distinct()
                .toList();
        return safeValues.isEmpty() ? List.of(fallback) : safeValues;
    }

    /**
     * 守护单个字符串。
     *
     * @param value 原始文本
     * @param fallback 兜底文本
     * @return 安全文本
     */
    private static String safeText(String value, String fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        if (containsUnsafeOutput(value)) {
            return fallback;
        }
        return value.trim();
    }

    /**
     * 判断字符串是否有内容。
     *
     * @param value 字符串
     * @return 是否有内容
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
