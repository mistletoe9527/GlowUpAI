package com.glowupai.ai;

import com.glowupai.style.StyleModels.DailyLookResponse;
import com.glowupai.style.StyleModels.PaletteResponse;
import com.glowupai.style.StyleModels.StyleReportResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 安全策略单元测试。
 */
class AiSafetyPolicyTest {

    /**
     * 验证吸引力目标会被转译成非评判造型任务。
     */
    @Test
    void safeStyleGoalReframesAttractivenessGoal() {
        String safeGoal = AiSafetyPolicy.safeStyleGoal("Look more attractive");

        assertEquals("Create a polished, confident, photo-ready look without rating attractiveness.", safeGoal);
        assertFalse(safeGoal.contains("Look more attractive"));
    }

    /**
     * 验证安全检查允许颜色词但拦截身份和外貌判断。
     */
    @Test
    void unsafeOutputCheckAllowsColorsAndBlocksJudgments() {
        assertFalse(AiSafetyPolicy.containsUnsafeOutput("Black, White, and Ivory are useful wardrobe colors."));
        assertTrue(AiSafetyPolicy.containsUnsafeOutput("You look ugly in this outfit."));
        assertTrue(AiSafetyPolicy.containsUnsafeOutput("You appear 42 years old."));
        assertTrue(AiSafetyPolicy.containsUnsafeOutput("You look Asian."));
    }

    /**
     * 验证风格报告中的不安全字段会被替换。
     */
    @Test
    void guardStyleReportReplacesUnsafeFields() {
        StyleReportResponse guarded = AiSafetyPolicy.guardStyleReport(new StyleReportResponse(
                "Beauty Score",
                "You look ugly",
                "High attractiveness rating",
                80,
                "You look older in this outfit.",
                "Oval",
                List.of("Soft layers"),
                List.of("Natural satin skin"),
                "Long legs",
                List.of("Avoid looking fat"),
                List.of("Black", "White", "Ivory"),
                List.of("Black", "White"),
                List.of("High beauty score"),
                List.of("Use one clear styling detail"),
                List.of(new PaletteResponse("Black", "#1f1d1b")),
                new DailyLookResponse(
                        "Daily",
                        "White shirt",
                        "Straight jeans",
                        "Loafers",
                        "You look young in it."
                ),
                "openai_vision"
        ));

        assertEquals("Style Direction", guarded.badge());
        assertEquals("Polished Daily Formula", guarded.heroTitle());
        assertEquals("Use fit, color, and silhouette choices to make the look feel intentional.", guarded.heroCopy());
        assertEquals("Lengthened vertical line", guarded.bodyRatio());
        assertEquals(List.of("Use structure and proportion to guide the outfit"), guarded.bodyTips());
        assertEquals(List.of("Black", "White", "Ivory"), guarded.colors());
        assertEquals(List.of("Clear styling direction"), guarded.strengths());
        assertEquals("Use fit, color, and silhouette choices to make the look feel intentional.", guarded.dailyLook().why());
    }

    /**
     * 验证空风格报告会返回完整安全兜底响应。
     */
    @Test
    void guardStyleReportReturnsFallbackWhenReportIsNull() {
        StyleReportResponse guarded = AiSafetyPolicy.guardStyleReport(null);

        assertEquals("Style Direction", guarded.badge());
        assertEquals("Polished Daily Formula", guarded.heroTitle());
        assertEquals(72, guarded.score());
        assertEquals(List.of("Ivory", "Soft Navy", "Sage"), guarded.colors());
        assertEquals(3, guarded.palette().size());
        assertEquals("#F7F1E7", guarded.palette().get(0).color());
        assertEquals("Daily", guarded.dailyLook().occasion());
        assertEquals("guarded_ai", guarded.source());
    }

    /**
     * 验证风格报告内的空色板和越界分数会被兜底。
     */
    @Test
    void guardStyleReportReplacesMissingPaletteAndClampsScore() {
        StyleReportResponse guarded = AiSafetyPolicy.guardStyleReport(new StyleReportResponse(
                "Clean Minimal",
                "Polished Daily Formula",
                "Keep the outfit focused.",
                140,
                "Keep the outfit focused.",
                "Balanced framing",
                List.of("Soft layers"),
                List.of("Fresh grooming"),
                "Vertical line",
                List.of("Use structure"),
                List.of("Ivory"),
                List.of("Ivory"),
                List.of("Clear direction"),
                List.of("Adjust one detail"),
                null,
                null,
                null
        ));

        assertEquals(100, guarded.score());
        assertEquals(3, guarded.palette().size());
        assertEquals("Ivory", guarded.palette().get(0).name());
        assertEquals("#F7F1E7", guarded.palette().get(0).color());
        assertEquals("Daily", guarded.dailyLook().occasion());
        assertEquals("guarded_ai", guarded.source());
    }
}
