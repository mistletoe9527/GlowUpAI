package com.glowupai.style;

import com.glowupai.ai.AiProperties;
import com.glowupai.ai.AiSafetyPolicy;
import com.glowupai.ai.OpenAiStyleAnalyzer;
import com.glowupai.style.StyleModels.ChatMessageResponse;
import com.glowupai.style.StyleModels.ClosetCategory;
import com.glowupai.style.StyleModels.ClosetItemResponse;
import com.glowupai.style.StyleModels.ClosetOutfitResponse;
import com.glowupai.style.StyleModels.ClosetSeason;
import com.glowupai.style.StyleModels.ClosetStyle;
import com.glowupai.style.StyleModels.DailyLookResponse;
import com.glowupai.style.StyleModels.Occasion;
import com.glowupai.style.StyleModels.OutfitResponse;
import com.glowupai.style.StyleModels.PaletteResponse;
import com.glowupai.style.StyleModels.PhotoUploadResponse;
import com.glowupai.style.StyleModels.ProductResponse;
import com.glowupai.style.StyleModels.StyleAnalyzeRequest;
import com.glowupai.style.StyleModels.StyleGoal;
import com.glowupai.style.StyleModels.StyleReportResponse;
import com.glowupai.style.StyleModels.SubscriptionPlan;
import com.glowupai.style.StyleModels.SubscriptionStartResponse;
import com.glowupai.style.StyleModels.UploadSummaryRequest;
import com.glowupai.style.StyleModels.UploadSlot;
import com.glowupai.style.StyleModels.UserProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 风格推荐服务。
 */
@Service
public class StyleRecommendationService {

    /**
     * 日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(StyleRecommendationService.class);

    /**
     * 欢迎页主视觉图片。
     */
    private static final String WELCOME_IMAGE = "https://lh3.googleusercontent.com/aida-public/AB6AXuBvjcpesNZWd0YW-qMprou8lKI9a7Feuwvta0iGNa46U7sNjmQ3C-NPtckayrl9JCJAyfYq7uY2mbKdWNek2x6k8Pt6J59NYjfKUHtRGwj2Rk4_wP9W_Jr46qMPAFT9tlGgzw-8GoWYYxjNWV1wtwAtA8JNA4whyWnznyUFLJRJGk-pYRI0I5Fr776z6mivtuqwony7FIWTlBl386sRwzFDeijRjK0uLrT0Pb4yadrO3fRP5b682uawNA";

    /**
     * 首页主视觉图片。
     */
    private static final String HOME_IMAGE = "https://lh3.googleusercontent.com/aida-public/AB6AXuCXnHZsF1Az80uj6AIhRV1kNvpzzTqHwWLeOnx5_kyM698v39bfiVLV3_kwyZLPth4KVZks2bcX_ydZLR8KP3xCysxUlfe49TiUKx4s3YHesbfzQjLfX58V0p2rsaMsXU-OS7zayyCoVsY440tvF8qSSVEPNoYWpQ8QFgrwTUi7Ilz_x7dmxUXO6vrMN91Df4jCkhiIH2B3SyHK5GQ-CEuEhUfQcmc0r066nEPAAEl5LHkEbR1pRTBE7Q";

    /**
     * 西装商品图片。
     */
    private static final String BLAZER_IMAGE = "https://lh3.googleusercontent.com/aida-public/AB6AXuA5rB0-6CtiKZAwlBMD6HdcEQYMjOZKxbVAOdd1bsI8pg7NTdec3LDUvgXOKcNgAoqzPrrQBAfh8-4Q7S1QflgOB1DcBM7ux8fwfMKfV6zF1HGhpjg_rhuNEP5WWba8hcezqYT_NIAx2uspcapyxKgRfWSDLyFNJKWPUJvtIAAVdKDLpWOqcxwNvK2LOfeNq-I-2NrU70p9jHsljNEPFW20Cx00idCdPJ_jR58PmVpARK9E6cPZ5fh5Ww";

    /**
     * 丝巾商品图片。
     */
    private static final String SCARF_IMAGE = "https://lh3.googleusercontent.com/aida-public/AB6AXuACd5c8lR501ars--TyXpPvWF5f0kYXbdqPdaRzLZFkfHrNFlkgFRQSO_Y0-mMbJmNgcIPaoTSF24fFL0oA7xy3e0H30gPamrkCQVQb6LS9ZC7xckxg1epLBbECyFQCKCX5cCKrxI8jdoyv6vsCyNJhQTP1XRg7LNg9tFCyjMjR6p1_G2atbDkjJ9tFiBNbBnI5-s01AqoL5qivxCTDPN7I2sPdJcC5ZRJBNWesSZb26brF427kzXNmNA";

    /**
     * 鞋履商品图片。
     */
    private static final String SHOE_IMAGE = "https://lh3.googleusercontent.com/aida-public/AB6AXuCV3RlKFP5EExsOdlkyUZ8W2vpW8lWeoEOyv-stvGQFicwYNSs1TcfxOT4ZtRYsNZK-ePlLU1SV8P_ksbFn3muX4WwuxkTz-dt4F1yLOK5_lFMBOHFuAUcRdGeJKKfpxJwcao4UF0vbiYtZp-SKxCzIKJ5T3770smsfD6kwlpOOVMG-9jmNy7CNSCuYvFTvsits4jAU8NYlwH2pj8DlmiyKVT7RzyHdGM2tWlJkew-Xt20Qd843DJ2egQ";

    /**
     * 风格目标预设。
     */
    private final Map<StyleGoal, StylePreset> stylePresets = createStylePresets();

    /**
     * 场景穿搭预设。
     */
    private final Map<Occasion, List<LookPreset>> occasionLooks = createOccasionLooks();

    /**
     * AI 配置。
     */
    private final AiProperties aiProperties;

    /**
     * OpenAI 风格分析器。
     */
    private final OpenAiStyleAnalyzer openAiStyleAnalyzer;

    /**
     * Affiliate 商品 feed 服务。
     */
    private final AffiliateFeedService affiliateFeedService;

    /**
     * 创建风格推荐服务。
     *
     * @param aiProperties AI 配置
     * @param openAiStyleAnalyzer OpenAI 风格分析器
     * @param affiliateFeedService Affiliate 商品 feed 服务
     */
    public StyleRecommendationService(
            AiProperties aiProperties,
            OpenAiStyleAnalyzer openAiStyleAnalyzer,
            AffiliateFeedService affiliateFeedService
    ) {
        this.aiProperties = aiProperties;
        this.openAiStyleAnalyzer = openAiStyleAnalyzer;
        this.affiliateFeedService = affiliateFeedService;
    }

    /**
     * 生成风格报告。
     *
     * @param request 风格分析请求
     * @return 风格报告
     */
    public StyleReportResponse analyze(StyleAnalyzeRequest request) {
        if (aiProperties.openaiEnabled()) {
            try {
                return openAiStyleAnalyzer.analyze(request);
            } catch (RuntimeException exception) {
                log.error("openai style analyze failed, falling back to mock: {}", exception.getMessage());
            }
        }
        return buildMockStyleReport(request);
    }

    /**
     * 生成本地模拟风格报告。
     *
     * @param request 风格分析请求
     * @return 风格报告
     */
    private StyleReportResponse buildMockStyleReport(StyleAnalyzeRequest request) {
        StyleGoal styleGoal = StyleGoal.fromLabel(request.profile().styleGoal());
        StylePreset preset = stylePresets.get(styleGoal);
        int uploadBonus = calculateUploadBonus(request);
        int profileBonus = calculateProfileBonus(request.profile());
        int score = clamp(preset.score() + Math.min(6, uploadBonus + profileBonus), 72, 96);
        DailyLookResponse dailyLook = new DailyLookResponse(
                Occasion.DAILY.label(),
                "White shirt",
                "Straight jeans",
                "Loafers",
                "Creates a sharp, easy base for everyday dressing."
        );
        return AiSafetyPolicy.guardStyleReport(new StyleReportResponse(
                preset.badge(),
                preset.heroTitle(),
                preset.heroCopy(),
                score,
                preset.description(),
                preset.faceShape(),
                preset.hair(),
                preset.makeup(),
                preset.bodyRatio(),
                preset.bodyTips(),
                preset.colors(),
                preset.colors(),
                preset.strengths(),
                preset.improvements(),
                preset.palette(),
                dailyLook,
                "backend_mock"
        ));
    }

    /**
     * 生成指定场景的三套穿搭。
     *
     * @param occasionLabel 场景标签
     * @param profile 用户资料
     * @return 穿搭列表
     */
    public List<OutfitResponse> generateOutfits(String occasionLabel, UserProfileRequest profile) {
        Occasion occasion = Occasion.fromLabel(occasionLabel);
        StylePreset preset = stylePresets.get(StyleGoal.fromLabel(profile.styleGoal()));
        List<LookPreset> looks = occasionLooks.getOrDefault(occasion, occasionLooks.get(Occasion.DAILY));
        List<OutfitResponse> responses = new ArrayList<>();
        for (int index = 0; index < looks.size(); index++) {
            LookPreset look = looks.get(index);
            responses.add(new OutfitResponse(
                    index + 1,
                    occasion.label(),
                    look.style(),
                    look.top(),
                    look.bottom(),
                    look.shoes(),
                    look.why() + " Tuned for " + preset.badge().toLowerCase() + "."
            ));
        }
        return responses;
    }

    /**
     * 生成商品推荐。
     *
     * @param occasionLabel 场景标签
     * @return 商品推荐列表
     */
    public List<ProductResponse> recommendProducts(String occasionLabel) {
        Occasion occasion = Occasion.fromLabel(occasionLabel);
        List<ProductResponse> feedProducts = affiliateFeedService.productsFor(occasion);
        if (!feedProducts.isEmpty()) {
            return feedProducts;
        }
        return switch (occasion) {
            case WORK -> List.of(
                    product("Studio Nicholson", "Linen Tailored Blazer", "Work", "$475", "Adds structure without breaking your soft neutral palette.", "linen+tailored+blazer", BLAZER_IMAGE),
                    product("Theory", "Wide-Leg Trouser", "Work", "$195", "Gives your work outfits a longer, cleaner line.", "wide+leg+tailored+trouser+women", HOME_IMAGE),
                    product("Aeyde", "Sculpted Leather Heel", "Polished", "$295", "Sharpens wide-leg trousers and midi silhouettes.", "sculpted+leather+heel", SHOE_IMAGE)
            );
            case DATE -> List.of(
                    product("Reformation", "Draped Satin Top", "Date", "$148", "Soft shine keeps the outfit romantic without feeling loud.", "draped+satin+top+women", WELCOME_IMAGE),
                    product("Hermès", "Geometric Silk Scarf", "Style Match", "$245", "A polished accent for cleaner minimalist outfits.", "geometric+silk+scarf", SCARF_IMAGE),
                    product("Aeyde", "Sculpted Leather Heel", "New", "$295", "Sharpens wide-leg trousers and midi silhouettes.", "sculpted+leather+heel", SHOE_IMAGE)
            );
            default -> List.of(
                    product("GlowUp Select", "Neutral Layering Set", "Saved", "$128", "A repeatable base for travel, work, and weekend looks.", "neutral+layering+set+women", HOME_IMAGE),
                    product("Hermès", "Geometric Silk Scarf", "Style Match", "$245", "A polished accent for cleaner minimalist outfits.", "geometric+silk+scarf", SCARF_IMAGE),
                    product("Aeyde", "Sculpted Leather Heel", "New", "$295", "Sharpens wide-leg trousers and midi silhouettes.", "sculpted+leather+heel", SHOE_IMAGE)
            );
        };
    }

    /**
     * 识别衣橱单品。
     *
     * @param photo 上传照片响应
     * @return 衣橱单品识别结果
     */
    public ClosetItemResponse recognizeClosetItem(PhotoUploadResponse photo) {
        String fileName = photo.name() == null ? "" : photo.name();
        String normalized = fileName.toLowerCase(Locale.US);
        ClosetCategory category = inferCategory(normalized);
        String color = inferColor(normalized);
        ClosetSeason season = inferSeason(normalized, category);
        ClosetStyle style = inferStyle(normalized, category);
        String itemName = "%s %s".formatted(color, category.label()).trim();
        return new ClosetItemResponse(
                null,
                photo.photoId(),
                itemName,
                category.label(),
                color,
                "Unknown",
                season.label(),
                style.label(),
                "local_rule_mvp"
        );
    }

    /**
     * 基于用户衣橱生成今日穿搭。
     *
     * @param items 用户衣橱单品
     * @param occasionLabel 场景标签
     * @param weather 天气描述
     * @return 衣橱穿搭推荐
     */
    public ClosetOutfitResponse generateClosetOutfit(List<ClosetItemResponse> items, String occasionLabel, String weather) {
        Occasion occasion = Occasion.fromLabel(occasionLabel);
        String weatherText = hasText(weather) ? weather.trim() : "Mild weather";
        String preferredStyle = preferredClosetStyle(occasion);
        ClosetItemResponse top = pickClosetItem(items, "Top", preferredStyle);
        ClosetItemResponse bottom = pickClosetItem(items, "Bottom", preferredStyle);
        ClosetItemResponse dress = pickClosetItem(items, "Dress", preferredStyle);
        ClosetItemResponse shoes = pickClosetItem(items, "Shoes", preferredStyle);
        ClosetItemResponse layer = needsLayer(weatherText) ? pickClosetItem(items, "Outerwear", preferredStyle) : null;
        ClosetItemResponse accessory = pickClosetItem(items, "Accessory", preferredStyle);
        String topText = displayItemOrFallback(top, dress, "Add a clean top or dress");
        String bottomText = dress != null ? "Not needed with dress" : displayItemOrFallback(bottom, null, "Add a versatile bottom");
        String shoesText = displayItemOrFallback(shoes, null, "Add a polished shoe");
        String layerText = layer == null ? (needsLayer(weatherText) ? "Add a light layer" : "Optional") : layer.name();
        String accessoryText = accessory == null ? "Optional simple accessory" : accessory.name();
        String missingItem = missingClosetItem(top, bottom, dress, shoes, layer, weatherText);
        return new ClosetOutfitResponse(
                occasion.label(),
                weatherText,
                preferredStyle,
                topText,
                bottomText,
                shoesText,
                layerText,
                accessoryText,
                "Built from your saved closet pieces for a practical " + occasion.label().toLowerCase(Locale.US) + " outfit.",
                missingItem
        );
    }

    /**
     * 生成聊天回复。
     *
     * @param message 用户消息
     * @param profile 用户资料
     * @param uploads 聊天附带照片
     * @return 聊天回复
     */
    public ChatMessageResponse chat(String message, UserProfileRequest profile, List<UploadSummaryRequest> uploads) {
        if (aiProperties.openaiEnabled()) {
            try {
                return openAiStyleAnalyzer.chat(message, profile, uploads);
            } catch (RuntimeException exception) {
                log.error("openai chat failed, falling back to mock: {}", exception.getMessage());
            }
        }
        StylePreset preset = stylePresets.get(StyleGoal.fromLabel(profile.styleGoal()));
        String normalized = message == null ? "" : message.toLowerCase(Locale.US);
        String colors = String.join(", ", preset.colors());
        String reply;
        if (hasOutfitPhoto(uploads) && containsAny(normalized, "wear", "outfit", "look", "this", "穿", "搭", "可以")) {
            reply = "I can use the outfit photo as context. Yes, it can work if you keep the silhouette intentional: "
                    + "repeat one of your best colors (" + colors + "), check that the hem and shoe line feel clean, "
                    + "and remove one extra detail if the look feels busy.";
        } else if (hasOutfitPhoto(uploads)) {
            reply = "I can use the outfit photo as context. Keep the strongest piece visible, repeat "
                    + preset.badge().toLowerCase(Locale.US) + " colors (" + colors + "), and make one fit adjustment at a time.";
        } else if (normalized.contains("date")) {
            reply = "For a date outfit, keep the base " + preset.badge().toLowerCase()
                    + " and add one softer detail. Your strongest colors here are " + colors + ".";
        } else if (normalized.contains("work") || normalized.contains("interview")) {
            reply = "Yes, this can work if the proportions stay clean. Choose one structured layer, keep shoes polished, and use "
                    + colors + " so the outfit feels intentional without being distracting.";
        } else if (normalized.contains("add") || normalized.contains("accessor")) {
            reply = "Add one deliberate accessory instead of several small ones: a silk scarf, clean earrings, or a structured bag.";
        } else {
            reply = "Keep this positive and practical: lean into " + preset.badge().toLowerCase()
                    + ", repeat your best colors (" + colors + "), and make one clear improvement at a time.";
        }
        return new ChatMessageResponse(AiSafetyPolicy.guardChatReply(reply));
    }

    /**
     * 判断聊天上下文是否包含穿搭照片。
     *
     * @param uploads 聊天附带照片
     * @return 是否包含穿搭照片
     */
    private boolean hasOutfitPhoto(List<UploadSummaryRequest> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            return false;
        }
        return uploads.stream()
                .map(upload -> UploadSlot.requireStyleAssessmentKey(upload.slot()))
                .anyMatch(UploadSlot.OUTFIT::equals);
    }

    /**
     * 生成订阅开始响应。
     *
     * @param planLabel 套餐标签
     * @return 订阅开始响应
     */
    public SubscriptionStartResponse startSubscription(String planLabel) {
        SubscriptionPlan plan = SubscriptionPlan.fromLabel(planLabel);
        Instant startedAt = Instant.now();
        return new SubscriptionStartResponse("Plus", plan.label(), plan.price(), "active", plan.expiresAt(startedAt).toString());
    }

    /**
     * 创建商品响应。
     *
     * @param brand 品牌
     * @param name 商品名
     * @param tag 标签
     * @param price 价格
     * @param reason 推荐理由
     * @param query 搜索关键词
     * @param image 图片链接
     * @return 商品响应
     */
    private ProductResponse product(String brand, String name, String tag, String price, String reason, String query, String image) {
        return new ProductResponse(
                brand,
                name,
                tag,
                price,
                reason,
                "https://www.amazon.com/s?k=" + query,
                image
        );
    }

    /**
     * 获取场景偏好的衣橱风格。
     *
     * @param occasion 场景
     * @return 衣橱风格标签
     */
    private String preferredClosetStyle(Occasion occasion) {
        return switch (occasion) {
            case WORK, INTERVIEW -> ClosetStyle.PROFESSIONAL.label();
            case DATE, WEDDING -> ClosetStyle.ROMANTIC.label();
            case GYM -> ClosetStyle.ATHLEISURE.label();
            case PARTY -> ClosetStyle.EVENING.label();
            default -> ClosetStyle.MINIMAL.label();
        };
    }

    /**
     * 选择最合适的衣橱单品。
     *
     * @param items 衣橱单品
     * @param category 品类
     * @param preferredStyle 偏好风格
     * @return 匹配单品
     */
    private ClosetItemResponse pickClosetItem(List<ClosetItemResponse> items, String category, String preferredStyle) {
        return items.stream()
                .filter(item -> category.equals(item.category()))
                .filter(item -> preferredStyle.equals(item.style()))
                .findFirst()
                .orElseGet(() -> items.stream()
                        .filter(item -> category.equals(item.category()))
                        .findFirst()
                        .orElse(null));
    }

    /**
     * 展示衣橱单品或缺省建议。
     *
     * @param item 首选单品
     * @param alternative 替代单品
     * @param fallback 缺省建议
     * @return 展示文案
     */
    private String displayItemOrFallback(ClosetItemResponse item, ClosetItemResponse alternative, String fallback) {
        if (item != null) {
            return item.name();
        }
        if (alternative != null) {
            return alternative.name();
        }
        return fallback;
    }

    /**
     * 判断天气是否需要外套。
     *
     * @param weather 天气描述
     * @return 是否需要外套
     */
    private boolean needsLayer(String weather) {
        String normalized = weather.toLowerCase(Locale.US);
        return containsAny(normalized, "cold", "cool", "rain", "wind", "snow", "fall", "winter");
    }

    /**
     * 生成衣橱缺口建议。
     *
     * @param top 上装
     * @param bottom 下装
     * @param dress 连衣裙
     * @param shoes 鞋履
     * @param layer 外套
     * @param weather 天气描述
     * @return 缺口建议
     */
    private String missingClosetItem(
            ClosetItemResponse top,
            ClosetItemResponse bottom,
            ClosetItemResponse dress,
            ClosetItemResponse shoes,
            ClosetItemResponse layer,
            String weather
    ) {
        if (top == null && dress == null) {
            return "Add one neutral top or dress to complete more outfits.";
        }
        if (bottom == null && dress == null) {
            return "Add one clean trouser, jean, or skirt for better outfit range.";
        }
        if (shoes == null) {
            return "Add one polished shoe to finish daily looks.";
        }
        if (needsLayer(weather) && layer == null) {
            return "Add a light jacket or blazer for cooler weather.";
        }
        return "Your closet has enough core pieces for this look.";
    }

    /**
     * 推断衣橱单品品类。
     *
     * @param normalizedName 标准化文件名
     * @return 单品品类
     */
    private ClosetCategory inferCategory(String normalizedName) {
        if (containsAny(normalizedName, "shoe", "heel", "sneaker", "loafer", "boot", "sandal")) {
            return ClosetCategory.SHOES;
        }
        if (containsAny(normalizedName, "dress", "gown")) {
            return ClosetCategory.DRESS;
        }
        if (containsAny(normalizedName, "blazer", "jacket", "coat", "trench", "cardigan")) {
            return ClosetCategory.OUTERWEAR;
        }
        if (containsAny(normalizedName, "pant", "trouser", "jean", "skirt", "short")) {
            return ClosetCategory.BOTTOM;
        }
        if (containsAny(normalizedName, "bag", "scarf", "belt", "earring", "necklace", "hat")) {
            return ClosetCategory.ACCESSORY;
        }
        if (containsAny(normalizedName, "legging", "sports", "workout", "gym", "training")) {
            return ClosetCategory.ACTIVEWEAR;
        }
        return ClosetCategory.TOP;
    }

    /**
     * 推断衣橱单品颜色。
     *
     * @param normalizedName 标准化文件名
     * @return 单品颜色
     */
    private String inferColor(String normalizedName) {
        if (containsAny(normalizedName, "black")) {
            return "Black";
        }
        if (containsAny(normalizedName, "white", "ivory", "cream")) {
            return "Ivory";
        }
        if (containsAny(normalizedName, "beige", "sand", "oat", "camel")) {
            return "Beige";
        }
        if (containsAny(normalizedName, "navy")) {
            return "Navy";
        }
        if (containsAny(normalizedName, "denim", "blue")) {
            return "Blue";
        }
        if (containsAny(normalizedName, "rose", "pink", "blush")) {
            return "Rose";
        }
        if (containsAny(normalizedName, "olive", "green")) {
            return "Olive";
        }
        if (containsAny(normalizedName, "charcoal", "gray", "grey")) {
            return "Charcoal";
        }
        if (containsAny(normalizedName, "brown", "espresso", "chocolate")) {
            return "Brown";
        }
        return "Neutral";
    }

    /**
     * 推断衣橱单品季节。
     *
     * @param normalizedName 标准化文件名
     * @param category 单品品类
     * @return 单品季节
     */
    private ClosetSeason inferSeason(String normalizedName, ClosetCategory category) {
        if (containsAny(normalizedName, "linen", "short", "sandal", "tank")) {
            return ClosetSeason.SUMMER;
        }
        if (containsAny(normalizedName, "coat", "wool", "sweater", "boot")) {
            return ClosetSeason.WINTER;
        }
        if (containsAny(normalizedName, "trench", "cardigan")) {
            return ClosetSeason.FALL;
        }
        if (category == ClosetCategory.ACCESSORY || category == ClosetCategory.SHOES) {
            return ClosetSeason.ALL_SEASON;
        }
        return ClosetSeason.SPRING;
    }

    /**
     * 推断衣橱单品风格。
     *
     * @param normalizedName 标准化文件名
     * @param category 单品品类
     * @return 单品风格
     */
    private ClosetStyle inferStyle(String normalizedName, ClosetCategory category) {
        if (containsAny(normalizedName, "work", "interview", "blazer", "trouser")) {
            return ClosetStyle.PROFESSIONAL;
        }
        if (containsAny(normalizedName, "date", "satin", "silk", "rose")) {
            return ClosetStyle.ROMANTIC;
        }
        if (containsAny(normalizedName, "gym", "legging", "sneaker", "sports", "training")) {
            return ClosetStyle.ATHLEISURE;
        }
        if (containsAny(normalizedName, "party", "evening", "metallic", "gown")) {
            return ClosetStyle.EVENING;
        }
        if (category == ClosetCategory.TOP || category == ClosetCategory.BOTTOM || category == ClosetCategory.OUTERWEAR) {
            return ClosetStyle.MINIMAL;
        }
        return ClosetStyle.CASUAL;
    }

    /**
     * 判断文本是否包含任一关键字。
     *
     * @param value 待匹配文本
     * @param keywords 关键字列表
     * @return 是否命中
     */
    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算上传照片带来的置信加分。
     *
     * @param request 风格分析请求
     * @return 上传照片加分
     */
    private int calculateUploadBonus(StyleAnalyzeRequest request) {
        return (int) request.uploads().stream()
                .map(upload -> UploadSlot.requireStyleAssessmentKey(upload.slot()))
                .distinct()
                .count();
    }

    /**
     * 计算资料完整度加分。
     *
     * @param profile 用户资料
     * @return 资料完整度加分
     */
    private int calculateProfileBonus(UserProfileRequest profile) {
        int bonus = 0;
        bonus += hasText(profile.birthday()) ? 1 : 0;
        bonus += hasText(profile.height()) ? 1 : 0;
        bonus += hasText(profile.weight()) ? 1 : 0;
        bonus += hasText(profile.location()) ? 1 : 0;
        return bonus;
    }

    /**
     * 判断字符串是否有内容。
     *
     * @param value 字符串
     * @return 是否有内容
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 限制数值区间。
     *
     * @param value 原始数值
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的数值
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 创建风格目标预设。
     *
     * @return 风格目标预设
     */
    private Map<StyleGoal, StylePreset> createStylePresets() {
        Map<StyleGoal, StylePreset> presets = new EnumMap<>(StyleGoal.class);
        presets.put(StyleGoal.LOOK_MORE_ATTRACTIVE, new StylePreset(
                "Modern Minimalist",
                "Effortless Friday",
                "Tailored comfort meets modern polish.",
                84,
                "Clean lines, quiet structure, and sharp proportions make the look feel polished without trying too hard.",
                "Oval",
                List.of("Shoulder-length layers", "Soft bangs"),
                List.of("Soft matte base", "Champagne highlight"),
                "Lengthened vertical line",
                List.of("High-waist pants", "Short jackets"),
                List.of("Black", "White", "Beige"),
                List.of("Good color matching", "Clean silhouette"),
                List.of("Add accessories", "Try more layering"),
                List.of(new PaletteResponse("Black", "#1f1d1b"), new PaletteResponse("White", "#f7f4ef"), new PaletteResponse("Beige", "#d6c1b0"))
        ));
        presets.put(StyleGoal.PROFESSIONAL, new StylePreset(
                "Sharp Professional",
                "Interview-Ready Polish",
                "Structured pieces that make your presence feel clear and prepared.",
                87,
                "A professional wardrobe works best when the silhouette is clean, the palette is calm, and one piece adds authority.",
                "Oval",
                List.of("Smooth blowout", "Clean low bun"),
                List.of("Natural satin skin", "Defined brow"),
                "Balanced frame",
                List.of("Single-button blazers", "Long straight trousers"),
                List.of("Charcoal", "Ivory", "Navy"),
                List.of("Strong first impression", "Clean proportions"),
                List.of("Add one quality belt", "Keep shoes sharply polished"),
                List.of(new PaletteResponse("Charcoal", "#303030"), new PaletteResponse("Ivory", "#f7f3ed"), new PaletteResponse("Navy", "#1f2b3f"))
        ));
        presets.put(StyleGoal.UPGRADE_MY_WARDROBE, new StylePreset(
                "Elevated Essentials",
                "Curated Basics",
                "A refined capsule that works across your week.",
                86,
                "A tighter edit of timeless pieces gives your wardrobe more range with fewer decisions.",
                "Soft square",
                List.of("Collarbone cut", "Side part"),
                List.of("Warm neutral eye", "Soft rose lip"),
                "Balanced frame",
                List.of("Monochrome layers", "Structured trousers"),
                List.of("Oat", "Chocolate", "Stone"),
                List.of("Smart layering", "Reliable staples"),
                List.of("Introduce texture", "Refresh footwear"),
                List.of(new PaletteResponse("Oat", "#d9c7b2"), new PaletteResponse("Chocolate", "#5a4236"), new PaletteResponse("Stone", "#e7ddd2"))
        ));
        presets.put(StyleGoal.FIND_MY_STYLE, new StylePreset(
                "Soft Signature",
                "Your Style, Refined",
                "Small details that make the whole look feel like you.",
                88,
                "Your strongest looks feel effortless, balanced, and slightly editorial with a softened finish.",
                "Oval",
                List.of("Soft layers", "Glossy waves"),
                List.of("Cream blush", "Brown liner"),
                "Natural waist",
                List.of("Cropped knits", "Fluid midi skirts"),
                List.of("Cream", "Sand", "Black"),
                List.of("Balanced proportions", "Natural polish"),
                List.of("Try stronger accessories", "Add contrast"),
                List.of(new PaletteResponse("Cream", "#f4ede2"), new PaletteResponse("Sand", "#d8c5b0"), new PaletteResponse("Black", "#1e1b19"))
        ));
        presets.put(StyleGoal.FIND_MY_PERSONAL_STYLE, new StylePreset(
                "Soft Signature",
                "Your Style, Refined",
                "Small details that make the whole look feel like you.",
                88,
                "Your strongest looks feel effortless, balanced, and slightly editorial with a softened finish.",
                "Oval",
                List.of("Soft layers", "Glossy waves"),
                List.of("Cream blush", "Brown liner"),
                "Natural waist",
                List.of("Cropped knits", "Fluid midi skirts"),
                List.of("Cream", "Sand", "Black"),
                List.of("Balanced proportions", "Natural polish"),
                List.of("Try stronger accessories", "Add contrast"),
                List.of(new PaletteResponse("Cream", "#f4ede2"), new PaletteResponse("Sand", "#d8c5b0"), new PaletteResponse("Black", "#1e1b19"))
        ));
        presets.put(StyleGoal.DATING_CONFIDENCE, new StylePreset(
                "Romantic Polish",
                "Soft Confidence",
                "Romantic texture with a clean, modern edge.",
                85,
                "Date-night style should feel warm and intentional without making you feel like you are wearing a costume.",
                "Heart",
                List.of("Face-framing waves", "Soft side part"),
                List.of("Rosy cheek tint", "Glossy soft lip"),
                "Defined waist",
                List.of("Draped tops", "Midi skirts"),
                List.of("Rose", "Espresso", "Cream"),
                List.of("Warm texture", "Soft shape"),
                List.of("Add a sharper shoe", "Use one subtle highlight detail"),
                List.of(new PaletteResponse("Rose", "#c7a59b"), new PaletteResponse("Espresso", "#47342c"), new PaletteResponse("Cream", "#f4ede2"))
        ));
        presets.put(StyleGoal.EVERYDAY_OUTFIT, new StylePreset(
                "Daily Ease",
                "Simple Morning Formula",
                "Reliable outfits that still feel considered.",
                83,
                "The best everyday wardrobe gives you repeatable formulas: a clean base, one layer, and one finishing accessory.",
                "Soft oval",
                List.of("Low-maintenance layers", "Soft volume"),
                List.of("Tinted moisturizer", "Soft mascara"),
                "Balanced frame",
                List.of("Straight denim", "Relaxed blazers"),
                List.of("Stone", "White", "Olive"),
                List.of("Easy outfit formulas", "Comfortable polish"),
                List.of("Rotate accessories", "Use more texture"),
                List.of(new PaletteResponse("Stone", "#e7ddd2"), new PaletteResponse("White", "#f7f4ef"), new PaletteResponse("Olive", "#6d705e"))
        ));
        presets.put(StyleGoal.IMPROVE_CONFIDENCE, new StylePreset(
                "Polished Ease",
                "Built for Confidence",
                "Clean choices that make getting dressed feel easier.",
                82,
                "Your best looks feel calm, intentional, and easy to repeat without looking predictable.",
                "Heart",
                List.of("Framing layers", "Soft volume"),
                List.of("Bright inner corner", "Rose nude lip"),
                "Defined waist",
                List.of("Wrap silhouettes", "A-line hems"),
                List.of("Ivory", "Rose taupe", "Espresso"),
                List.of("Easy to repeat", "Soft balance"),
                List.of("Add structure at the shoulders", "Use more contrast"),
                List.of(new PaletteResponse("Ivory", "#f7f3ed"), new PaletteResponse("Rose Taupe", "#c7a59b"), new PaletteResponse("Espresso", "#47342c"))
        ));
        return presets;
    }

    /**
     * 创建场景穿搭预设。
     *
     * @return 场景穿搭预设
     */
    private Map<Occasion, List<LookPreset>> createOccasionLooks() {
        Map<Occasion, List<LookPreset>> looks = new EnumMap<>(Occasion.class);
        looks.put(Occasion.DAILY, List.of(
                new LookPreset("City Ease", "Ivory silk shirt", "Straight denim", "Leather loafers", "Keeps the look polished while staying easy to wear."),
                new LookPreset("Soft Layers", "Fine rib tank", "Tailored trouser", "Minimal sneakers", "Balances structure and comfort for a full day out."),
                new LookPreset("Weekend Edit", "Cream knit tee", "Bias midi skirt", "Pointed flats", "Adds quiet shape without losing ease.")
        ));
        looks.put(Occasion.WORK, List.of(
                new LookPreset("Desk Ready", "Tailored blouse", "Wide-leg trousers", "Low heel mules", "Feels authoritative without looking stiff."),
                new LookPreset("Meeting Sharp", "Fine knit top", "Cropped blazer", "Sleek loafers", "Adds structure with a modern, calm finish."),
                new LookPreset("After Hours", "Silk shell", "Column skirt", "Block-heel sandals", "Moves from desk to dinner with almost no change.")
        ));
        looks.put(Occasion.DATE, List.of(
                new LookPreset("Soft Focus", "Draped satin top", "Slim black skirt", "Ankle-strap heels", "Highlights the waist and keeps the mood romantic."),
                new LookPreset("Quiet Glow", "Off-shoulder knit", "Fluid trousers", "Heeled sandals", "A little more skin, but still very polished."),
                new LookPreset("Modern Romance", "Wrap blouse", "Midi slip skirt", "Pointed mules", "Combines softness and shape in one look.")
        ));
        looks.put(Occasion.PARTY, List.of(
                new LookPreset("Statement Night", "Sculpted corset top", "Sharp tailored pants", "Heeled slingbacks", "Keeps the silhouette strong and photo-ready."),
                new LookPreset("City Lights", "Metallic knit", "Wide-leg satin trouser", "Strappy heels", "Adds shimmer without feeling overworked."),
                new LookPreset("Modern Drama", "Sheer blouse", "Column skirt", "Pumps", "Creates clean lines with a richer finish.")
        ));
        looks.put(Occasion.TRAVEL, List.of(
                new LookPreset("Airport Ease", "Soft sweater", "Relaxed jogger", "Slip-on sneakers", "Comfort first, but still pulled together."),
                new LookPreset("Long Haul", "Layered tee set", "Stretch trouser", "Flat loafers", "Easy to move in and simple to style in transit."),
                new LookPreset("Arrival Ready", "Lightweight trench", "Straight pant", "Low-heeled boots", "Looks fresh when you step off the plane.")
        ));
        looks.put(Occasion.GYM, List.of(
                new LookPreset("Studio Clean", "Supportive tank", "High-rise legging", "Training sneaker", "Simple, secure, and easy to move in."),
                new LookPreset("Match Set", "Longline sports bra", "Matching short", "Running shoe", "A coordinated set gives the look more structure."),
                new LookPreset("After Class", "Boxy sweatshirt", "Bike short", "Retro sneaker", "Transitions cleanly into errands or coffee.")
        ));
        looks.put(Occasion.WEDDING, List.of(
                new LookPreset("Guest Polished", "Silk blouse", "Flowing midi skirt", "Dress heels", "Elegant enough for photos and easy to move in."),
                new LookPreset("Evening Formal", "Draped bodice", "Tailored wide leg", "Satin pumps", "Feels dressed up without competing with the dress code."),
                new LookPreset("Modern Guest", "Soft blazer", "Bias dress", "Heel sandals", "Adds refinement with a slightly more current edge.")
        ));
        looks.put(Occasion.INTERVIEW, List.of(
                new LookPreset("Confident Entry", "Structured shirt", "Straight trousers", "Low block heels", "Looks prepared, calm, and credible."),
                new LookPreset("Smart Minimal", "Fine knit top", "Single-button blazer", "Loafers", "Keeps the attention on you, not the outfit."),
                new LookPreset("Polished Clear", "Crisp blouse", "Column pant", "Pointed flats", "Works well when you want a strong but low-noise impression.")
        ));
        return looks;
    }

    /**
     * 风格目标预设。
     *
     * @param badge 风格类型
     * @param heroTitle 首页标题
     * @param heroCopy 首页说明
     * @param score 基础分数
     * @param description 风格说明
     * @param faceShape 脸型维度
     * @param hair 发型建议
     * @param makeup 妆容建议
     * @param bodyRatio 身形比例维度
     * @param bodyTips 身形建议
     * @param colors 推荐颜色
     * @param strengths 优势
     * @param improvements 改进项
     * @param palette 色板
     */
    private record StylePreset(
            String badge,
            String heroTitle,
            String heroCopy,
            int score,
            String description,
            String faceShape,
            List<String> hair,
            List<String> makeup,
            String bodyRatio,
            List<String> bodyTips,
            List<String> colors,
            List<String> strengths,
            List<String> improvements,
            List<PaletteResponse> palette
    ) {
    }

    /**
     * 穿搭预设。
     *
     * @param style 风格名
     * @param top 上装
     * @param bottom 下装
     * @param shoes 鞋履
     * @param why 推荐理由
     */
    private record LookPreset(
            String style,
            String top,
            String bottom,
            String shoes,
            String why
    ) {
    }
}
