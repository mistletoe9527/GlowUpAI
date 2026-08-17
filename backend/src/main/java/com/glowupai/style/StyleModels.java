package com.glowupai.style;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 风格推荐领域模型集合。
 */
public final class StyleModels {

    /**
     * 私有构造方法，避免工具类被实例化。
     */
    private StyleModels() {
    }

    /**
     * 用户风格目标枚举。
     */
    public enum StyleGoal {
        /**
         * 提升吸引力目标。
         */
        LOOK_MORE_ATTRACTIVE("Look more attractive"),

        /**
         * 职场专业形象目标。
         */
        PROFESSIONAL("Professional"),

        /**
         * 升级衣橱目标。
         */
        UPGRADE_MY_WARDROBE("Upgrade my wardrobe"),

        /**
         * 找到个人风格目标，来自 iOS PRD 文案。
         */
        FIND_MY_STYLE("Find my style"),

        /**
         * 找到个人风格目标，兼容早期 Web 原型文案。
         */
        FIND_MY_PERSONAL_STYLE("Find my personal style"),

        /**
         * 约会自信目标。
         */
        DATING_CONFIDENCE("Dating confidence"),

        /**
         * 日常穿搭目标。
         */
        EVERYDAY_OUTFIT("Everyday outfit"),

        /**
         * 提升自信目标。
         */
        IMPROVE_CONFIDENCE("Improve confidence");

        /**
         * 面向前端展示的标签。
         */
        private final String label;

        /**
         * 创建风格目标枚举。
         *
         * @param label 展示标签
         */
        StyleGoal(String label) {
            this.label = label;
        }

        /**
         * 获取展示标签。
         *
         * @return 展示标签
         */
        public String label() {
            return label;
        }

        /**
         * 按前端标签解析风格目标。
         *
         * @param value 前端传入值
         * @return 风格目标
         */
        public static StyleGoal fromLabel(String value) {
            return Optional.ofNullable(value)
                    .flatMap(text -> List.of(values()).stream()
                            .filter(goal -> goal.label.equalsIgnoreCase(text.trim()))
                            .findFirst())
                    .orElse(FIND_MY_PERSONAL_STYLE);
        }

        /**
         * 严格解析风格目标，拒绝未知标签。
         *
         * @param value 前端传入值
         * @return 风格目标
         */
        public static StyleGoal requireLabel(String value) {
            return Optional.ofNullable(value)
                    .flatMap(text -> List.of(values()).stream()
                            .filter(goal -> goal.label.equalsIgnoreCase(text.trim()))
                            .findFirst())
                    .orElseThrow(() -> new IllegalArgumentException("Style goal must be a supported onboarding option"));
        }
    }

    /**
     * 穿搭场景枚举。
     */
    public enum Occasion {
        /**
         * 日常场景。
         */
        DAILY("Daily"),

        /**
         * 工作场景。
         */
        WORK("Work"),

        /**
         * 约会场景。
         */
        DATE("Date"),

        /**
         * 派对场景。
         */
        PARTY("Party"),

        /**
         * 旅行场景。
         */
        TRAVEL("Travel"),

        /**
         * 健身场景。
         */
        GYM("Gym"),

        /**
         * 婚礼场景。
         */
        WEDDING("Wedding"),

        /**
         * 面试场景。
         */
        INTERVIEW("Interview");

        /**
         * 面向前端展示的标签。
         */
        private final String label;

        /**
         * 创建场景枚举。
         *
         * @param label 展示标签
         */
        Occasion(String label) {
            this.label = label;
        }

        /**
         * 获取展示标签。
         *
         * @return 展示标签
         */
        public String label() {
            return label;
        }

        /**
         * 按前端标签解析场景。
         *
         * @param value 前端传入值
         * @return 场景枚举
         */
        public static Occasion fromLabel(String value) {
            return Optional.ofNullable(value)
                    .flatMap(text -> List.of(values()).stream()
                            .filter(occasion -> occasion.label.equalsIgnoreCase(text.trim()))
                            .findFirst())
                    .orElse(DAILY);
        }

        /**
         * 严格解析穿搭场景，拒绝未知标签。
         *
         * @param value 前端传入值
         * @return 穿搭场景
         */
        public static Occasion requireLabel(String value) {
            return Optional.ofNullable(value)
                    .flatMap(text -> List.of(values()).stream()
                            .filter(occasion -> occasion.label.equalsIgnoreCase(text.trim()))
                            .findFirst())
                    .orElseThrow(() -> new IllegalArgumentException("Occasion must be Daily, Work, Date, Party, Travel, Gym, Wedding, or Interview"));
        }
    }

    /**
     * 上传照片槽位枚举。
     */
    public enum UploadSlot {
        /**
         * 面部照片。
         */
        FACE,

        /**
         * 全身照片。
         */
        BODY,

        /**
         * 当前穿搭照片。
         */
        OUTFIT,

        /**
         * 衣橱单品照片。
         */
        CLOSET;

        /**
         * 风格评估必填照片槽位。
         *
         * @return 必填槽位集合
         */
        public static Set<UploadSlot> requiredForStyleAssessment() {
            return EnumSet.of(FACE, BODY);
        }

        /**
         * 风格评估可见照片槽位。
         *
         * @return 风格评估照片槽位集合
         */
        public static Set<UploadSlot> styleAssessmentSlots() {
            return EnumSet.of(FACE, BODY, OUTFIT);
        }

        /**
         * 获取与前端传输一致的小写槽位 key。
         *
         * @return 小写槽位 key
         */
        public String key() {
            return name().toLowerCase(Locale.US);
        }

        /**
         * 严格解析仅允许用于风格评估的照片槽位。
         *
         * @param value 前端传入槽位
         * @return 风格评估照片槽位
         */
        public static UploadSlot requireStyleAssessmentKey(String value) {
            UploadSlot slot = requireKey(value);
            if (!styleAssessmentSlots().contains(slot)) {
                throw new IllegalArgumentException("Photo slot must be face, body, or outfit");
            }
            return slot;
        }

        /**
         * 按前端 key 解析照片槽位。
         *
         * @param value 前端传入 key
         * @return 上传照片槽位
         */
        public static UploadSlot fromKey(String value) {
            return Optional.ofNullable(value)
                    .map(text -> text.trim().toUpperCase(Locale.US))
                    .flatMap(text -> List.of(values()).stream()
                            .filter(slot -> slot.name().equals(text))
                            .findFirst())
                    .orElse(OUTFIT);
        }

        /**
         * 严格解析照片槽位，拒绝未知值。
         *
         * @param value 前端传入槽位
         * @return 照片槽位
         */
        public static UploadSlot requireKey(String value) {
            String normalized = Optional.ofNullable(value)
                    .map(text -> text.trim().toUpperCase(Locale.US))
                    .orElse("");
            return List.of(values()).stream()
                    .filter(slot -> slot.name().equals(normalized))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Photo slot must be face, body, or outfit"));
        }
    }

    /**
     * 衣橱单品品类枚举。
     */
    public enum ClosetCategory {
        /**
         * 上装。
         */
        TOP("Top"),

        /**
         * 下装。
         */
        BOTTOM("Bottom"),

        /**
         * 鞋履。
         */
        SHOES("Shoes"),

        /**
         * 外套。
         */
        OUTERWEAR("Outerwear"),

        /**
         * 连衣裙。
         */
        DRESS("Dress"),

        /**
         * 配饰。
         */
        ACCESSORY("Accessory"),

        /**
         * 运动单品。
         */
        ACTIVEWEAR("Activewear");

        /**
         * 展示标签。
         */
        private final String label;

        /**
         * 创建衣橱品类。
         *
         * @param label 展示标签
         */
        ClosetCategory(String label) {
            this.label = label;
        }

        /**
         * 获取展示标签。
         *
         * @return 展示标签
         */
        public String label() {
            return label;
        }
    }

    /**
     * 衣橱单品季节枚举。
     */
    public enum ClosetSeason {
        /**
         * 春季。
         */
        SPRING("Spring"),

        /**
         * 夏季。
         */
        SUMMER("Summer"),

        /**
         * 秋季。
         */
        FALL("Fall"),

        /**
         * 冬季。
         */
        WINTER("Winter"),

        /**
         * 四季适用。
         */
        ALL_SEASON("All season");

        /**
         * 展示标签。
         */
        private final String label;

        /**
         * 创建衣橱季节。
         *
         * @param label 展示标签
         */
        ClosetSeason(String label) {
            this.label = label;
        }

        /**
         * 获取展示标签。
         *
         * @return 展示标签
         */
        public String label() {
            return label;
        }
    }

    /**
     * 衣橱单品风格枚举。
     */
    public enum ClosetStyle {
        /**
         * 极简风格。
         */
        MINIMAL("Minimal"),

        /**
         * 职场风格。
         */
        PROFESSIONAL("Professional"),

        /**
         * 浪漫风格。
         */
        ROMANTIC("Romantic"),

        /**
         * 运动休闲风格。
         */
        ATHLEISURE("Athleisure"),

        /**
         * 日常休闲风格。
         */
        CASUAL("Casual"),

        /**
         * 晚装风格。
         */
        EVENING("Evening");

        /**
         * 展示标签。
         */
        private final String label;

        /**
         * 创建衣橱风格。
         *
         * @param label 展示标签
         */
        ClosetStyle(String label) {
            this.label = label;
        }

        /**
         * 获取展示标签。
         *
         * @return 展示标签
         */
        public String label() {
            return label;
        }
    }

    /**
     * 订阅套餐枚举。
     */
    public enum SubscriptionPlan {
        /**
         * 周付套餐。
         */
        WEEKLY("Weekly", "$4.99", 7),

        /**
         * 月付套餐。
         */
        MONTHLY("Monthly", "$14.99", 30),

        /**
         * 年付套餐。
         */
        YEARLY("Yearly", "$79.99", 365);

        /**
         * 面向前端展示的标签。
         */
        private final String label;

        /**
         * 面向前端展示的价格。
         */
        private final String price;

        /**
         * MVP 有效天数。
         */
        private final long durationDays;

        /**
         * 创建订阅套餐枚举。
         *
         * @param label 展示标签
         * @param price 展示价格
         * @param durationDays MVP 有效天数
         */
        SubscriptionPlan(String label, String price, long durationDays) {
            this.label = label;
            this.price = price;
            this.durationDays = durationDays;
        }

        /**
         * 获取展示标签。
         *
         * @return 展示标签
         */
        public String label() {
            return label;
        }

        /**
         * 获取展示价格。
         *
         * @return 展示价格
         */
        public String price() {
            return price;
        }

        /**
         * 基于开始时间计算 MVP 订阅到期时间。
         *
         * @param startedAt 开始时间
         * @return 到期时间
         */
        public Instant expiresAt(Instant startedAt) {
            return startedAt.plus(durationDays, ChronoUnit.DAYS);
        }

        /**
         * 按前端标签解析订阅套餐。
         *
         * @param value 前端传入值
         * @return 订阅套餐
         */
        public static SubscriptionPlan fromLabel(String value) {
            return Optional.ofNullable(value)
                    .flatMap(text -> List.of(values()).stream()
                            .filter(plan -> plan.label.equalsIgnoreCase(text.trim()))
                            .findFirst())
                    .orElse(MONTHLY);
        }

        /**
         * 严格解析订阅套餐，拒绝未知标签。
         *
         * @param value 前端传入值
         * @return 订阅套餐
         */
        public static SubscriptionPlan requireLabel(String value) {
            return Optional.ofNullable(value)
                    .flatMap(text -> List.of(values()).stream()
                            .filter(plan -> plan.label.equalsIgnoreCase(text.trim()))
                            .findFirst())
                    .orElseThrow(() -> new IllegalArgumentException("Subscription plan must be Weekly, Monthly, or Yearly"));
        }
    }

    /**
     * 埋点事件枚举。
     */
    public enum AnalyticsEventName {
        /**
         * 注册完成事件。
         */
        SIGNUP_COMPLETE("signup_complete"),

        /**
         * 照片上传事件。
         */
        PHOTO_UPLOADED("photo_uploaded"),

        /**
         * 风格报告生成事件。
         */
        STYLE_REPORT_GENERATED("style_report_generated"),

        /**
         * 订阅开始事件。
         */
        SUBSCRIPTION_STARTED("subscription_started"),

        /**
         * 分享点击事件。
         */
        SHARE_CLICKED("share_clicked"),

        /**
         * 购物点击事件。
         */
        AFFILIATE_PRODUCT_CLICKED("affiliate_product_clicked"),

        /**
         * 衣橱单品上传事件。
         */
        CLOSET_ITEM_UPLOADED("closet_item_uploaded"),

        /**
         * 衣橱穿搭生成事件。
         */
        CLOSET_OUTFIT_GENERATED("closet_outfit_generated"),

        /**
         * 聊天消息事件。
         */
        AI_CHAT_MESSAGE_SENT("ai_chat_message_sent"),

        /**
         * 聊天照片事件。
         */
        AI_CHAT_PHOTO_SENT("ai_chat_photo_sent"),

        /**
         * 照片删除事件。
         */
        PHOTO_DELETED("photo_deleted"),

        /**
         * 所有照片删除事件。
         */
        PHOTOS_DELETED("photos_deleted"),

        /**
         * 用户数据删除事件。
         */
        USER_DATA_DELETED("user_data_deleted");

        /**
         * 埋点事件名。
         */
        private final String eventName;

        /**
         * 创建埋点事件枚举。
         *
         * @param eventName 埋点事件名
         */
        AnalyticsEventName(String eventName) {
            this.eventName = eventName;
        }

        /**
         * 获取埋点事件名。
         *
         * @return 埋点事件名
         */
        public String eventName() {
            return eventName;
        }

        /**
         * 按事件名解析埋点枚举。
         *
         * @param value 前端传入事件名
         * @return 埋点事件枚举
         */
        public static Optional<AnalyticsEventName> fromEventName(String value) {
            return Optional.ofNullable(value)
                    .flatMap(text -> List.of(values()).stream()
                            .filter(event -> event.eventName.equalsIgnoreCase(text.trim()))
                            .findFirst());
        }
    }

    /**
     * 用户基础资料请求。
     *
     * @param userId 用户 ID
     * @param name 用户昵称
     * @param authMethod 登录方式
     * @param email 登录邮箱
     * @param styleGoal 风格目标标签
     * @param gender 性别标签
     * @param birthday 生日
     * @param height 身高
     * @param weight 体重
     * @param location 所在地区国家码
     */
    public record UserProfileRequest(
            @NotBlank String userId,
            @NotBlank String name,
            @NotBlank String authMethod,
            String email,
            @NotBlank String styleGoal,
            @NotBlank String gender,
            @NotBlank
            @Pattern(
                    regexp = "\\d{4}-\\d{2}-\\d{2}",
                    message = "must use yyyy-MM-dd format"
            )
            String birthday,
            String height,
            String weight,
            @NotBlank String location
    ) {
    }

    /**
     * 用户资料保存响应。
     *
     * @param userId 用户 ID
     * @param status 保存状态
     */
    public record UserProfileResponse(
            String userId,
            String status
    ) {
    }

    /**
     * 用户数据删除响应。
     *
     * @param userId 用户 ID
     * @param profileDeleted 用户资料是否删除
     * @param photoMetadataDeleted 照片元数据删除数量
     * @param photoObjectsDeleted 照片对象删除数量
     * @param closetItemsDeleted 衣橱单品删除数量
     * @param styleReportsDeleted 风格报告删除数量
     * @param subscriptionsDeleted 订阅记录删除数量
     * @param analyticsEventsDeleted 埋点事件删除数量
     */
    public record UserDataDeletionResponse(
            String userId,
            boolean profileDeleted,
            long photoMetadataDeleted,
            long photoObjectsDeleted,
            long closetItemsDeleted,
            long styleReportsDeleted,
            long subscriptionsDeleted,
            long analyticsEventsDeleted
    ) {
    }

    /**
     * 照片删除响应。
     *
     * @param userId 用户 ID
     * @param photoMetadataDeleted 标记删除的照片元数据数量
     * @param photoObjectsDeleted 删除的照片对象数量
     */
    public record PhotoDeletionResponse(
            String userId,
            long photoMetadataDeleted,
            long photoObjectsDeleted
    ) {
    }

    /**
     * 上传照片摘要请求。
     *
     * @param slot 照片槽位
     * @param name 文件名
     * @param type 文件类型
     * @param size 文件大小
     */
    public record UploadSummaryRequest(
            String photoId,
            @NotBlank String slot,
            String name,
            String type,
            long size
    ) {
    }

    /**
     * 风格分析请求。
     *
     * @param profile 用户资料
     * @param uploads 上传照片摘要
     */
    public record StyleAnalyzeRequest(
            @Valid @NotNull UserProfileRequest profile,
            @Valid @NotEmpty List<UploadSummaryRequest> uploads
    ) {
        /**
         * 校验风格分析必填照片槽位。
         */
        public void validateRequiredUploads() {
            Set<UploadSlot> presentSlots = EnumSet.noneOf(UploadSlot.class);
            if (uploads != null) {
                uploads.stream()
                        .filter(upload -> upload != null && upload.slot() != null)
                        .map(upload -> UploadSlot.requireStyleAssessmentKey(upload.slot()))
                        .forEach(presentSlots::add);
            }
            if (!presentSlots.containsAll(UploadSlot.requiredForStyleAssessment())) {
                throw new IllegalArgumentException("Face photo and full body photo are required");
            }
        }
    }

    /**
     * 穿搭生成请求。
     *
     * @param profile 用户资料
     * @param occasion 穿搭场景
     */
    public record OutfitGenerateRequest(
            @Valid @NotNull UserProfileRequest profile,
            @NotBlank String occasion
    ) {
    }

    /**
     * 聊天消息请求。
     *
     * @param profile 用户资料
     * @param message 用户消息
     * @param uploads 聊天附带照片
     */
    public record ChatMessageRequest(
            @Valid @NotNull UserProfileRequest profile,
            @NotBlank String message,
            List<UploadSummaryRequest> uploads
    ) {
    }

    /**
     * 埋点事件请求。
     *
     * @param name 事件名
     * @param payload 事件参数
     */
    public record AnalyticsEventRequest(
            @NotBlank String name,
            Map<String, Object> payload
    ) {
    }

    /**
     * 订阅开始请求。
     *
     * @param userId 用户 ID
     * @param plan 套餐标签
     */
    public record SubscriptionStartRequest(
            String userId,
            @NotBlank String plan
    ) {
    }

    /**
     * 颜色色板响应。
     *
     * @param name 颜色名
     * @param color 色值
     */
    public record PaletteResponse(
            String name,
            String color
    ) {
    }

    /**
     * 单套穿搭响应。
     *
     * @param order 排序
     * @param occasion 场景
     * @param style 风格名
     * @param top 上装
     * @param bottom 下装
     * @param shoes 鞋履
     * @param why 推荐理由
     */
    public record OutfitResponse(
            int order,
            String occasion,
            String style,
            String top,
            String bottom,
            String shoes,
            String why
    ) {
    }

    /**
     * 日常推荐穿搭响应。
     *
     * @param occasion 场景
     * @param top 上装
     * @param bottom 下装
     * @param shoes 鞋履
     * @param why 推荐理由
     */
    public record DailyLookResponse(
            String occasion,
            String top,
            String bottom,
            String shoes,
            String why
    ) {
    }

    /**
     * 风格报告响应。
     *
     * @param badge 风格类型
     * @param heroTitle 首页标题
     * @param heroCopy 首页说明
     * @param score 风格分数
     * @param description 风格说明
     * @param faceShape 脸型建议维度
     * @param hair 发型建议
     * @param makeup 妆容建议
     * @param bodyRatio 身形比例维度
     * @param bodyTips 身形穿搭建议
     * @param colors 推荐颜色名
     * @param bestColors 最佳颜色名
     * @param strengths 优势列表
     * @param improvements 改进列表
     * @param palette 色板
     * @param dailyLook 日常穿搭
     * @param source 数据来源
     */
    public record StyleReportResponse(
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
            List<String> bestColors,
            List<String> strengths,
            List<String> improvements,
            List<PaletteResponse> palette,
            DailyLookResponse dailyLook,
            String source
    ) {
    }

    /**
     * 商品推荐响应。
     *
     * @param brand 品牌
     * @param name 商品名
     * @param tag 标签
     * @param price 价格
     * @param reason 推荐理由
     * @param buyUrl 购买链接
     * @param image 图片链接
     */
    public record ProductResponse(
            String brand,
            String name,
            String tag,
            String price,
            String reason,
            String buyUrl,
            String image
    ) {
    }

    /**
     * 聊天消息响应。
     *
     * @param reply AI 回复
     */
    public record ChatMessageResponse(
            String reply
    ) {
    }

    /**
     * 照片上传响应。
     *
     * @param photoId 照片 ID
     * @param slot 照片槽位
     * @param name 原始文件名
     * @param type 文件类型
     * @param size 文件大小
     * @param storageMode 存储模式
     */
    public record PhotoUploadResponse(
            String photoId,
            String slot,
            String name,
            String type,
            long size,
            String storageMode
    ) {
    }

    /**
     * 衣橱单品响应。
     *
     * @param itemId 单品 ID
     * @param photoId 关联照片 ID
     * @param name 单品名称
     * @param category 单品品类
     * @param color 单品颜色
     * @param brand 识别品牌
     * @param season 适用季节
     * @param style 风格标签
     * @param source 识别来源
     */
    public record ClosetItemResponse(
            String itemId,
            String photoId,
            String name,
            String category,
            String color,
            String brand,
            String season,
            String style,
            String source
    ) {
    }

    /**
     * 衣橱穿搭推荐请求。
     *
     * @param userId 用户 ID
     * @param occasion 场景标签
     * @param weather 天气描述
     */
    public record ClosetOutfitRequest(
            String userId,
            String occasion,
            String weather
    ) {
    }

    /**
     * 衣橱穿搭推荐响应。
     *
     * @param occasion 场景标签
     * @param weather 天气描述
     * @param style 风格标签
     * @param top 上装
     * @param bottom 下装
     * @param shoes 鞋履
     * @param layer 外套
     * @param accessory 配饰
     * @param why 推荐理由
     * @param missingItem 衣橱缺口建议
     */
    public record ClosetOutfitResponse(
            String occasion,
            String weather,
            String style,
            String top,
            String bottom,
            String shoes,
            String layer,
            String accessory,
            String why,
            String missingItem
    ) {
    }

    /**
     * 订阅开始响应。
     *
     * @param tier 订阅层级
     * @param plan 套餐标签
     * @param price 套餐价格
     * @param status 订阅状态
     * @param expiresAt 到期时间
     */
    public record SubscriptionStartResponse(
            String tier,
            String plan,
            String price,
            String status,
            String expiresAt
    ) {
    }

    /**
     * 订阅状态响应。
     *
     * @param active 是否拥有有效订阅
     * @param tier 订阅层级
     * @param plan 套餐标签
     * @param price 套餐价格
     * @param status 订阅状态
     * @param expiresAt 到期时间
     */
    public record SubscriptionStatusResponse(
            boolean active,
            String tier,
            String plan,
            String price,
            String status,
            String expiresAt
    ) {
    }
}
