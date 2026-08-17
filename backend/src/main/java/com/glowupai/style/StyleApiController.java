package com.glowupai.style;

import com.glowupai.common.ApiResult;
import com.glowupai.auth.RequestIdentityService;
import com.glowupai.photo.PhotoStorageService;
import com.glowupai.persistence.PersistenceService;
import com.glowupai.style.StyleModels.AnalyticsEventName;
import com.glowupai.style.StyleModels.AnalyticsEventRequest;
import com.glowupai.style.StyleModels.ChatMessageRequest;
import com.glowupai.style.StyleModels.ChatMessageResponse;
import com.glowupai.style.StyleModels.ClosetItemResponse;
import com.glowupai.style.StyleModels.ClosetOutfitRequest;
import com.glowupai.style.StyleModels.ClosetOutfitResponse;
import com.glowupai.style.StyleModels.OutfitGenerateRequest;
import com.glowupai.style.StyleModels.OutfitResponse;
import com.glowupai.style.StyleModels.PhotoDeletionResponse;
import com.glowupai.style.StyleModels.PhotoUploadResponse;
import com.glowupai.style.StyleModels.ProductResponse;
import com.glowupai.style.StyleModels.StyleAnalyzeRequest;
import com.glowupai.style.StyleModels.StyleReportResponse;
import com.glowupai.style.StyleModels.Occasion;
import com.glowupai.style.StyleModels.SubscriptionStartRequest;
import com.glowupai.style.StyleModels.SubscriptionStartResponse;
import com.glowupai.style.StyleModels.SubscriptionStatusResponse;
import com.glowupai.style.StyleModels.SubscriptionPlan;
import com.glowupai.style.StyleModels.StyleGoal;
import com.glowupai.style.StyleModels.UploadSlot;
import com.glowupai.style.StyleModels.UserDataDeletionResponse;
import com.glowupai.style.StyleModels.UserProfileRequest;
import com.glowupai.style.StyleModels.UserProfileResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * GlowUp AI MVP API。
 */
@RestController
@RequestMapping("/api")
public class StyleApiController {

    /**
     * 日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(StyleApiController.class);

    /**
     * 风格推荐服务。
     */
    private final StyleRecommendationService styleRecommendationService;

    /**
     * 照片存储服务。
     */
    private final PhotoStorageService photoStorageService;

    /**
     * 持久化服务。
     */
    private final PersistenceService persistenceService;

    /**
     * 请求身份校验服务。
     */
    private final RequestIdentityService requestIdentityService;

    /**
     * 创建 API Controller。
     *
     * @param styleRecommendationService 风格推荐服务
     * @param photoStorageService 照片存储服务
     * @param persistenceService 持久化服务
     * @param requestIdentityService 请求身份校验服务
     */
    public StyleApiController(
            StyleRecommendationService styleRecommendationService,
            PhotoStorageService photoStorageService,
            PersistenceService persistenceService,
            RequestIdentityService requestIdentityService
    ) {
        this.styleRecommendationService = styleRecommendationService;
        this.photoStorageService = photoStorageService;
        this.persistenceService = persistenceService;
        this.requestIdentityService = requestIdentityService;
    }

    /**
     * 健康检查接口。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        return ApiResult.success(Map.of("status", "ok", "service", "glowup-ai-backend"));
    }

    /**
     * 保存用户资料。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param request 用户资料请求
     * @return 用户资料保存响应
     */
    @PostMapping("/users/profile")
    public ApiResult<UserProfileResponse> saveUserProfile(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @Valid @RequestBody UserProfileRequest request
    ) {
        String userId = requestIdentityService.requireUserAccess(request.userId(), headerUserId);
        UserProfileRequest securedRequest = profileForUser(request, userId);
        validateProfileEnums(securedRequest);
        String savedUserId = persistenceService.saveUserProfile(securedRequest);
        return ApiResult.success(new UserProfileResponse(savedUserId, "saved"));
    }

    /**
     * 查询用户资料。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @return 用户资料
     */
    @GetMapping("/users/profile")
    public ApiResult<UserProfileRequest> getUserProfile(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam(required = false) String userId
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        return ApiResult.success(persistenceService.findUserProfile(securedUserId));
    }

    /**
     * 删除指定用户的账户数据。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @return 用户数据删除响应
     */
    @DeleteMapping("/users/{userId}")
    public ApiResult<UserDataDeletionResponse> deleteUserData(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @PathVariable String userId
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        UserDataDeletionResponse response = persistenceService.deleteUserData(securedUserId);
        log.info(
                "user data deleted userId={} photos={} closetItems={} reports={} subscriptions={} analytics={}",
                response.userId(),
                response.photoMetadataDeleted(),
                response.closetItemsDeleted(),
                response.styleReportsDeleted(),
                response.subscriptionsDeleted(),
                response.analyticsEventsDeleted()
        );
        return ApiResult.success(response);
    }

    /**
     * 查询用户最近一次风格报告。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @return 最近一次风格报告
     */
    @GetMapping("/style/report")
    public ApiResult<StyleReportResponse> latestStyleReport(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam(required = false) String userId
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        return ApiResult.success(persistenceService.findLatestStyleReport(securedUserId));
    }

    /**
     * 上传照片。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @param slot 照片槽位
     * @param file 上传文件
     * @return 照片上传响应
     */
    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<PhotoUploadResponse> uploadPhoto(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam(required = false) String userId,
            @RequestParam String slot,
            @RequestParam MultipartFile file
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        UploadSlot.requireStyleAssessmentKey(slot);
        PhotoUploadResponse response = photoStorageService.store(securedUserId, slot, file);
        log.info("photo uploaded slot={} size={}", response.slot(), response.size());
        return ApiResult.success(response);
    }

    /**
     * 查询指定用户的风格评估照片列表。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @return 风格评估照片元数据列表
     */
    @GetMapping("/photos")
    public ApiResult<List<PhotoUploadResponse>> listPhotos(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam String userId
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        List<PhotoUploadResponse> response = persistenceService.listPhotos(securedUserId);
        log.info("photo list requested userId={} photos={}", securedUserId, response.size());
        return ApiResult.success(response);
    }

    /**
     * 下载指定照片的原始内容。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param photoId 照片 ID
     * @return 照片原始字节
     */
    @GetMapping("/photos/{photoId}/content")
    public ResponseEntity<byte[]> downloadPhoto(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @PathVariable String photoId
    ) {
        requestIdentityService.currentUserId(headerUserId)
                .ifPresent(userId -> persistenceService.validatePhotoBelongsToUser(photoId, userId));
        PersistenceService.StoredPhotoData storedPhoto = persistenceService.findStoredPhotoData(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        return ResponseEntity.ok()
                .contentType(resolveMediaType(storedPhoto.contentType()))
                .body(storedPhoto.bytes());
    }

    /**
     * 删除照片。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param photoId 照片 ID
     * @return 删除状态
     */
    @DeleteMapping("/photos/{photoId}")
    public ApiResult<Map<String, Object>> deletePhoto(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @PathVariable String photoId
    ) {
        requestIdentityService.currentUserId(headerUserId)
                .ifPresent(userId -> persistenceService.validatePhotoBelongsToUser(photoId, userId));
        boolean deleted = photoStorageService.delete(photoId);
        log.info("photo delete requested photoId={} deleted={}", photoId, deleted);
        return ApiResult.success(Map.of("deleted", deleted));
    }

    /**
     * 删除指定用户的所有风格评估照片。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @return 照片删除响应
     */
    @DeleteMapping("/photos")
    public ApiResult<PhotoDeletionResponse> deleteUserPhotos(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam String userId
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        PhotoDeletionResponse response = photoStorageService.deleteAllForUser(securedUserId);
        log.info(
                "user photos delete requested userId={} photos={} objects={}",
                response.userId(),
                response.photoMetadataDeleted(),
                response.photoObjectsDeleted()
        );
        return ApiResult.success(response);
    }

    /**
     * 生成风格分析报告。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param request 风格分析请求
     * @return 风格报告
     */
    @PostMapping("/style/analyze")
    public ApiResult<StyleReportResponse> analyze(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @Valid @RequestBody StyleAnalyzeRequest request
    ) {
        String userId = requestIdentityService.requireUserAccess(request.profile().userId(), headerUserId);
        StyleAnalyzeRequest securedRequest = analyzeRequestForUser(request, userId);
        validateProfileEnums(securedRequest.profile());
        securedRequest.validateRequiredUploads();
        persistenceService.validateStyleAnalysisPhotos(securedRequest);
        StyleReportResponse response = styleRecommendationService.analyze(securedRequest);
        persistenceService.saveStyleReport(securedRequest.profile(), response);
        log.info("style report generated for goal={}", securedRequest.profile().styleGoal());
        return ApiResult.success(response);
    }

    /**
     * 生成指定场景穿搭。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param request 穿搭生成请求
     * @return 穿搭列表
     */
    @PostMapping("/outfits/generate")
    public ApiResult<List<OutfitResponse>> generateOutfits(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @Valid @RequestBody OutfitGenerateRequest request
    ) {
        String userId = requestIdentityService.requireUserAccess(request.profile().userId(), headerUserId);
        UserProfileRequest securedProfile = profileForUser(request.profile(), userId);
        validateProfileEnums(securedProfile);
        Occasion occasion = Occasion.requireLabel(request.occasion());
        List<OutfitResponse> response = styleRecommendationService.generateOutfits(occasion.label(), securedProfile);
        return ApiResult.success(response);
    }

    /**
     * 查询购物推荐。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @param occasion 场景标签
     * @return 商品推荐列表
     */
    @GetMapping("/shopping/recommendations")
    public ApiResult<List<ProductResponse>> recommendProducts(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "Daily") String occasion
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        requirePlus(securedUserId, PremiumFeature.SHOPPING_RECOMMENDATIONS);
        Occasion selectedOccasion = Occasion.requireLabel(occasion);
        List<ProductResponse> response = styleRecommendationService.recommendProducts(selectedOccasion.label());
        return ApiResult.success(response);
    }

    /**
     * 上传并识别衣橱单品。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @param file 上传文件
     * @return 衣橱单品响应
     */
    @PostMapping(value = "/closet/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<ClosetItemResponse> uploadClosetItem(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam(required = false) String userId,
            @RequestParam MultipartFile file
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        requirePlus(securedUserId, PremiumFeature.AI_CLOSET);
        PhotoUploadResponse photo = photoStorageService.store(securedUserId, UploadSlot.CLOSET.key(), file);
        ClosetItemResponse recognition = styleRecommendationService.recognizeClosetItem(photo);
        ClosetItemResponse response = persistenceService.saveClosetItem(securedUserId, photo, recognition);
        log.info("closet item recognized category={} style={}", response.category(), response.style());
        return ApiResult.success(response);
    }

    /**
     * 查询用户衣橱单品。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @return 衣橱单品列表
     */
    @GetMapping("/closet/items")
    public ApiResult<List<ClosetItemResponse>> listClosetItems(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam(required = false) String userId
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        requirePlus(securedUserId, PremiumFeature.AI_CLOSET);
        List<ClosetItemResponse> response = persistenceService.listClosetItems(securedUserId);
        return ApiResult.success(response);
    }

    /**
     * 基于用户衣橱生成今日穿搭。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param request 衣橱穿搭请求
     * @return 衣橱穿搭推荐
     */
    @PostMapping("/closet/outfit")
    public ApiResult<ClosetOutfitResponse> generateClosetOutfit(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @Valid @RequestBody ClosetOutfitRequest request
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(request.userId(), headerUserId);
        requirePlus(securedUserId, PremiumFeature.AI_CLOSET);
        List<ClosetItemResponse> items = persistenceService.listClosetItems(securedUserId);
        Occasion occasion = Occasion.requireLabel(request.occasion());
        ClosetOutfitResponse response = styleRecommendationService.generateClosetOutfit(items, occasion.label(), request.weather());
        log.info("closet outfit generated occasion={} style={}", response.occasion(), response.style());
        return ApiResult.success(response);
    }

    /**
     * 生成 AI Stylist 聊天回复。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param request 聊天消息请求
     * @return 聊天回复
     */
    @PostMapping("/chat/message")
    public ApiResult<ChatMessageResponse> chat(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        String userId = requestIdentityService.requireUserAccess(request.profile().userId(), headerUserId);
        UserProfileRequest securedProfile = profileForUser(request.profile(), userId);
        validateProfileEnums(securedProfile);
        requirePlus(userId, PremiumFeature.AI_STYLIST_CHAT);
        if (requestIdentityService.currentUserId(headerUserId).isPresent()) {
            persistenceService.validateUploadsBelongToUser(userId, request.uploads());
        }
        ChatMessageResponse response = styleRecommendationService.chat(request.message(), securedProfile, request.uploads());
        return ApiResult.success(response);
    }

    /**
     * 接收前端埋点事件。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param request 埋点事件请求
     * @return 接收状态
     */
    @PostMapping("/analytics/events")
    public ApiResult<Map<String, String>> trackEvent(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @Valid @RequestBody AnalyticsEventRequest request
    ) {
        AnalyticsEventName eventName = requireKnownAnalyticsEvent(request.name());
        AnalyticsEventRequest normalizedRequest = new AnalyticsEventRequest(eventName.eventName(), request.payload());
        String payloadUserId = userIdFromPayload(request);
        String securedUserId = requestIdentityService.requireUserAccess(payloadUserId, headerUserId);
        persistenceService.saveAnalyticsEvent(normalizedRequest, securedUserId);
        log.info("analytics event received name={}", eventName.eventName());
        return ApiResult.success(Map.of("status", "accepted"));
    }

    /**
     * 校验埋点事件名是否属于 MVP 事件枚举。
     *
     * @param eventName 前端传入事件名
     * @return 已识别的事件枚举
     */
    private AnalyticsEventName requireKnownAnalyticsEvent(String eventName) {
        return AnalyticsEventName.fromEventName(eventName)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported analytics event: " + eventName));
    }

    /**
     * 开始订阅。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param request 订阅开始请求
     * @return 订阅状态
     */
    @PostMapping("/subscriptions/start")
    public ApiResult<SubscriptionStartResponse> startSubscription(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @Valid @RequestBody SubscriptionStartRequest request
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(request.userId(), headerUserId);
        SubscriptionPlan plan = SubscriptionPlan.requireLabel(request.plan());
        SubscriptionStartResponse response = styleRecommendationService.startSubscription(plan.label());
        persistenceService.saveSubscription(securedUserId, response);
        log.info("subscription started plan={}", response.plan());
        return ApiResult.success(response);
    }

    /**
     * 查询用户订阅状态。
     *
     * @param headerUserId 身份请求头用户 ID
     * @param userId 用户 ID
     * @return 订阅状态
     */
    @GetMapping("/subscriptions/status")
    public ApiResult<SubscriptionStatusResponse> subscriptionStatus(
            @RequestHeader(value = RequestIdentityService.USER_ID_HEADER, required = false) String headerUserId,
            @RequestParam(required = false) String userId
    ) {
        String securedUserId = requestIdentityService.requireUserAccess(userId, headerUserId);
        SubscriptionStatusResponse response = persistenceService.findSubscriptionStatus(securedUserId);
        return ApiResult.success(response);
    }

    /**
     * 将风格分析请求绑定到可信用户 ID。
     *
     * @param request 原始风格分析请求
     * @param userId 可信用户 ID
     * @return 已绑定用户 ID 的风格分析请求
     */
    private StyleAnalyzeRequest analyzeRequestForUser(StyleAnalyzeRequest request, String userId) {
        return new StyleAnalyzeRequest(profileForUser(request.profile(), userId), request.uploads());
    }

    /**
     * 将用户资料绑定到可信用户 ID。
     *
     * @param profile 原始用户资料
     * @param userId 可信用户 ID
     * @return 已绑定用户 ID 的用户资料
     */
    private UserProfileRequest profileForUser(UserProfileRequest profile, String userId) {
        if (userId == null || userId.isBlank()) {
            return profile;
        }
        return new UserProfileRequest(
                userId,
                profile.name(),
                profile.authMethod(),
                profile.email(),
                profile.styleGoal(),
                profile.gender(),
                profile.birthday(),
                profile.height(),
                profile.weight(),
                profile.location()
        );
    }

    /**
     * 解析照片内容类型。
     *
     * @param contentType 原始内容类型
     * @return Spring 媒体类型
     */
    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 校验用户资料中的业务枚举标签。
     *
     * @param profile 用户资料
     */
    private void validateProfileEnums(UserProfileRequest profile) {
        StyleGoal.requireLabel(profile.styleGoal());
    }

    /**
     * 从埋点 payload 中读取用户 ID。
     *
     * @param request 埋点事件请求
     * @return 用户 ID
     */
    private String userIdFromPayload(AnalyticsEventRequest request) {
        if (request.payload() == null || !request.payload().containsKey("userId")) {
            return null;
        }
        Object userId = request.payload().get("userId");
        return userId == null ? null : String.valueOf(userId);
    }

    /**
     * 校验用户是否拥有 Plus 权益。
     *
     * @param userId 用户 ID
     * @param feature 需要访问的付费功能
     */
    private void requirePlus(String userId, PremiumFeature feature) {
        if (!persistenceService.hasActiveSubscription(userId)) {
            throw new IllegalArgumentException("GlowUp Plus required for " + feature.label());
        }
    }

    /**
     * 付费权益功能。
     */
    private enum PremiumFeature {
        /**
         * AI Stylist 聊天。
         */
        AI_STYLIST_CHAT("AI Stylist Chat"),

        /**
         * AI 衣橱。
         */
        AI_CLOSET("AI Closet"),

        /**
         * 购物推荐。
         */
        SHOPPING_RECOMMENDATIONS("Shopping Recommendations");

        /**
         * 面向接口错误的功能标签。
         */
        private final String label;

        /**
         * 创建付费权益枚举。
         *
         * @param label 功能标签
         */
        PremiumFeature(String label) {
            this.label = label;
        }

        /**
         * 获取功能标签。
         *
         * @return 功能标签
         */
        String label() {
            return label;
        }
    }
}
