package com.glowupai.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowupai.photo.PhotoEncryptionService;
import com.glowupai.photo.PhotoObjectStorageService;
import com.glowupai.style.StyleModels.StyleAnalyzeRequest;
import com.glowupai.style.StyleModels.UploadSummaryRequest;
import com.glowupai.style.StyleModels.AnalyticsEventRequest;
import com.glowupai.style.StyleModels.ClosetItemResponse;
import com.glowupai.style.StyleModels.PhotoUploadResponse;
import com.glowupai.style.StyleModels.StyleReportResponse;
import com.glowupai.style.StyleModels.SubscriptionStartResponse;
import com.glowupai.style.StyleModels.SubscriptionStatusResponse;
import com.glowupai.style.StyleModels.UploadSlot;
import com.glowupai.style.StyleModels.UserDataDeletionResponse;
import com.glowupai.style.StyleModels.UserProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * MVP 数据持久化服务。
 */
@Service
public class PersistenceService {

    /**
     * 默认演示用户 ID。
     */
    private static final String DEMO_USER_ID = "demo-user";

    /**
     * 用户资料仓储。
     */
    private final UserProfileRepository userProfileRepository;

    /**
     * 照片元数据仓储。
     */
    private final PhotoRepository photoRepository;

    /**
     * 风格报告仓储。
     */
    private final StyleReportRepository styleReportRepository;

    /**
     * 埋点事件仓储。
     */
    private final AnalyticsEventRepository analyticsEventRepository;

    /**
     * 订阅记录仓储。
     */
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 衣橱单品仓储。
     */
    private final ClosetItemRepository closetItemRepository;

    /**
     * 照片加密服务。
     */
    private final PhotoEncryptionService photoEncryptionService;

    /**
     * 照片对象存储服务。
     */
    private final PhotoObjectStorageService photoObjectStorageService;

    /**
     * JSON 处理器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建持久化服务。
     *
     * @param userProfileRepository 用户资料仓储
     * @param photoRepository 照片元数据仓储
     * @param styleReportRepository 风格报告仓储
     * @param analyticsEventRepository 埋点事件仓储
     * @param subscriptionRepository 订阅记录仓储
     * @param closetItemRepository 衣橱单品仓储
     * @param photoEncryptionService 照片加密服务
     * @param photoObjectStorageService 照片对象存储服务
     * @param objectMapper JSON 处理器
     */
    public PersistenceService(
            UserProfileRepository userProfileRepository,
            PhotoRepository photoRepository,
            StyleReportRepository styleReportRepository,
            AnalyticsEventRepository analyticsEventRepository,
            SubscriptionRepository subscriptionRepository,
            ClosetItemRepository closetItemRepository,
            PhotoEncryptionService photoEncryptionService,
            PhotoObjectStorageService photoObjectStorageService,
            ObjectMapper objectMapper
    ) {
        this.userProfileRepository = userProfileRepository;
        this.photoRepository = photoRepository;
        this.styleReportRepository = styleReportRepository;
        this.analyticsEventRepository = analyticsEventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.closetItemRepository = closetItemRepository;
        this.photoEncryptionService = photoEncryptionService;
        this.photoObjectStorageService = photoObjectStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存或更新用户资料。
     *
     * @param profile 用户资料请求
     * @return 用户 ID
     */
    @Transactional
    public String saveUserProfile(UserProfileRequest profile) {
        String userId = resolveUserId(profile);
        UserProfileEntity entity = userProfileRepository.findById(userId)
                .orElseGet(() -> new UserProfileEntity(userId));
        if (profile != null) {
            entity.updateProfile(
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
        userProfileRepository.save(entity);
        return userId;
    }

    /**
     * 查询用户资料。
     *
     * @param userId 用户 ID
     * @return 用户资料
     */
    @Transactional(readOnly = true)
    public UserProfileRequest findUserProfile(String userId) {
        String resolvedUserId = requireUserId(userId);
        return userProfileRepository.findById(resolvedUserId)
                .map(UserProfileEntity::toUserProfileRequest)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
    }

    /**
     * 保存照片元数据。
     *
     * @param userId 用户 ID
     * @param response 照片上传响应
     * @param storagePath 存储路径
     */
    @Transactional
    public void savePhoto(String userId, PhotoUploadResponse response, String storagePath) {
        PhotoEntity entity = new PhotoEntity(
                response.photoId(),
                defaultIfBlank(userId, DEMO_USER_ID),
                response.slot(),
                response.name(),
                response.type(),
                response.size(),
                response.storageMode(),
                storagePath
        );
        photoRepository.save(entity);
    }

    /**
     * 标记照片已删除。
     *
     * @param photoId 照片 ID
     */
    @Transactional
    public void markPhotoDeleted(String photoId) {
        photoRepository.findById(photoId).ifPresent(entity -> {
            entity.markDeleted();
            photoRepository.save(entity);
        });
    }

    /**
     * 查询照片存储路径。
     *
     * @param photoId 照片 ID
     * @return 存储路径
     */
    @Transactional(readOnly = true)
    public Optional<String> findPhotoStoragePath(String photoId) {
        return photoRepository.findById(photoId)
                .filter(entity -> !entity.deleted())
                .map(PhotoEntity::storagePath);
    }

    /**
     * 查询指定用户尚未删除的照片对象引用。
     *
     * @param userId 用户 ID
     * @return 照片对象引用列表
     */
    @Transactional(readOnly = true)
    public List<StoredPhotoReference> findUndeletedPhotoReferencesForUser(String userId) {
        String resolvedUserId = requireUserId(userId);
        return photoRepository.findByUserIdAndDeletedFalse(resolvedUserId).stream()
                .map(entity -> new StoredPhotoReference(entity.photoId(), entity.storagePath()))
                .toList();
    }

    /**
     * 查询指定用户的未删除风格评估照片元数据。
     *
     * @param userId 用户 ID
     * @return 照片元数据响应列表
     */
    @Transactional(readOnly = true)
    public List<PhotoUploadResponse> listPhotos(String userId) {
        String resolvedUserId = requireUserId(userId);
        return findUndeletedStylePhotos(resolvedUserId).stream()
                .map(entity -> new PhotoUploadResponse(
                        entity.photoId(),
                        entity.slot(),
                        entity.originalName(),
                        entity.contentType(),
                        entity.fileSize(),
                        entity.storageMode()
                ))
                .toList();
    }

    /**
     * 查询指定用户尚未删除的风格评估照片对象引用。
     *
     * @param userId 用户 ID
     * @return 风格评估照片对象引用列表
     */
    @Transactional(readOnly = true)
    public List<StoredPhotoReference> findUndeletedStylePhotoReferencesForUser(String userId) {
        String resolvedUserId = requireUserId(userId);
        return findUndeletedStylePhotos(resolvedUserId).stream()
                .map(entity -> new StoredPhotoReference(entity.photoId(), entity.storagePath()))
                .toList();
    }

    /**
     * 保存风格报告。
     *
     * @param profile 用户资料
     * @param response 风格报告响应
     */
    @Transactional
    public void saveStyleReport(UserProfileRequest profile, StyleReportResponse response) {
        String userId = saveUserProfile(profile);
        StyleReportEntity entity = new StyleReportEntity(
                userId,
                response.badge(),
                response.score(),
                response.source(),
                toJson(response)
        );
        styleReportRepository.save(entity);
    }

    /**
     * 查询用户最近一次风格报告。
     *
     * @param userId 用户 ID
     * @return 最近一次风格报告
     */
    @Transactional(readOnly = true)
    public StyleReportResponse findLatestStyleReport(String userId) {
        String resolvedUserId = requireUserId(userId);
        return styleReportRepository.findTopByUserIdOrderByCreatedAtDesc(resolvedUserId)
                .map(this::toStyleReportResponse)
                .orElseThrow(() -> new IllegalArgumentException("Style report not found"));
    }

    /**
     * 保存埋点事件。
     *
     * @param request 埋点事件请求
     */
    @Transactional
    public void saveAnalyticsEvent(AnalyticsEventRequest request) {
        saveAnalyticsEvent(request, null);
    }

    /**
     * 保存埋点事件。
     *
     * @param request 埋点事件请求
     * @param authenticatedUserId 已认证用户 ID
     */
    @Transactional
    public void saveAnalyticsEvent(AnalyticsEventRequest request, String authenticatedUserId) {
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        String userId = Optional.ofNullable(payload.get("userId"))
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .orElse(defaultIfBlank(authenticatedUserId, DEMO_USER_ID));
        analyticsEventRepository.save(new AnalyticsEventEntity(userId, request.name(), toJson(payload)));
    }

    /**
     * 保存订阅记录。
     *
     * @param userId 用户 ID
     * @param response 订阅开始响应
     */
    @Transactional
    public void saveSubscription(String userId, SubscriptionStartResponse response) {
        subscriptionRepository.save(new SubscriptionEntity(
                defaultIfBlank(userId, DEMO_USER_ID),
                response.tier(),
                response.plan(),
                response.price(),
                response.status(),
                Instant.parse(response.expiresAt())
        ));
    }

    /**
     * 查询用户订阅状态。
     *
     * @param userId 用户 ID
     * @return 订阅状态响应
     */
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse findSubscriptionStatus(String userId) {
        String resolvedUserId = defaultIfBlank(userId, DEMO_USER_ID);
        return subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(resolvedUserId)
                .map(this::toSubscriptionStatusResponse)
                .orElseGet(() -> new SubscriptionStatusResponse(false, "Free", "None", "$0", "inactive", ""));
    }

    /**
     * 判断用户是否拥有有效订阅。
     *
     * @param userId 用户 ID
     * @return 是否拥有有效订阅
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(String userId) {
        return findSubscriptionStatus(userId).active();
    }

    /**
     * 删除指定用户的所有本地数据和照片对象。
     *
     * @param userId 用户 ID
     * @return 用户数据删除响应
     */
    @Transactional
    public UserDataDeletionResponse deleteUserData(String userId) {
        String resolvedUserId = requireUserId(userId);
        List<PhotoEntity> photos = photoRepository.findByUserId(resolvedUserId);
        long photoObjectsDeleted = photos.stream()
                .map(PhotoEntity::storagePath)
                .filter(path -> path != null && !path.isBlank())
                .filter(photoObjectStorageService::delete)
                .count();
        long closetItemsDeleted = closetItemRepository.deleteByUserId(resolvedUserId);
        long styleReportsDeleted = styleReportRepository.deleteByUserId(resolvedUserId);
        long subscriptionsDeleted = subscriptionRepository.deleteByUserId(resolvedUserId);
        long analyticsEventsDeleted = analyticsEventRepository.deleteByUserId(resolvedUserId);
        long photoMetadataDeleted = photoRepository.deleteByUserId(resolvedUserId);
        boolean profileDeleted = userProfileRepository.existsById(resolvedUserId);
        if (profileDeleted) {
            userProfileRepository.deleteById(resolvedUserId);
        }
        return new UserDataDeletionResponse(
                resolvedUserId,
                profileDeleted,
                photoMetadataDeleted,
                photoObjectsDeleted,
                closetItemsDeleted,
                styleReportsDeleted,
                subscriptionsDeleted,
                analyticsEventsDeleted
        );
    }

    /**
     * 保存衣橱单品。
     *
     * @param userId 用户 ID
     * @param photo 照片上传响应
     * @param recognition 识别结果
     * @return 已保存的衣橱单品响应
     */
    @Transactional
    public ClosetItemResponse saveClosetItem(String userId, PhotoUploadResponse photo, ClosetItemResponse recognition) {
        String itemId = UUID.randomUUID().toString();
        String resolvedUserId = defaultIfBlank(userId, DEMO_USER_ID);
        ClosetItemEntity entity = new ClosetItemEntity(
                itemId,
                resolvedUserId,
                photo.photoId(),
                photo.name(),
                recognition.category(),
                recognition.color(),
                recognition.brand(),
                recognition.season(),
                recognition.style(),
                recognition.source()
        );
        closetItemRepository.save(entity);
        return toClosetItemResponse(entity.toSnapshot());
    }

    /**
     * 查询用户衣橱单品。
     *
     * @param userId 用户 ID
     * @return 衣橱单品响应列表
     */
    @Transactional(readOnly = true)
    public List<ClosetItemResponse> listClosetItems(String userId) {
        String resolvedUserId = defaultIfBlank(userId, DEMO_USER_ID);
        return closetItemRepository.findByUserIdOrderByCreatedAtDesc(resolvedUserId).stream()
                .map(ClosetItemEntity::toSnapshot)
                .map(this::toClosetItemResponse)
                .toList();
    }

    /**
     * 解析用户 ID。
     *
     * @param profile 用户资料
     * @return 用户 ID
     */
    public String resolveUserId(UserProfileRequest profile) {
        if (profile == null || profile.userId() == null || profile.userId().isBlank()) {
            return DEMO_USER_ID;
        }
        return profile.userId();
    }

    /**
     * 读取风格分析请求关联的未删除照片。
     *
     * @param request 风格分析请求
     * @return 已存储照片数据
     */
    @Transactional(readOnly = true)
    public List<StoredPhotoData> loadStoredPhotos(StyleAnalyzeRequest request) {
        if (request == null || request.uploads() == null) {
            return List.of();
        }
        return loadStoredPhotos(request.uploads());
    }

    /**
     * 校验风格分析所需照片已经真实上传。
     *
     * @param request 风格分析请求
     */
    @Transactional(readOnly = true)
    public void validateStyleAnalysisPhotos(StyleAnalyzeRequest request) {
        String userId = resolveUserId(request.profile());
        Set<UploadSlot> validSlots = EnumSet.noneOf(UploadSlot.class);
        request.uploads().stream()
                .filter(upload -> upload.photoId() != null && !upload.photoId().isBlank())
                .forEach(upload -> collectValidUploadedSlot(userId, upload, validSlots));
        if (!validSlots.containsAll(UploadSlot.requiredForStyleAssessment())) {
            throw new IllegalArgumentException("Uploaded face photo and full body photo must exist before analysis");
        }
    }

    /**
     * 校验上传照片是否归属指定用户。
     *
     * @param userId 用户 ID
     * @param uploads 上传照片摘要
     */
    @Transactional(readOnly = true)
    public void validateUploadsBelongToUser(String userId, List<UploadSummaryRequest> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            return;
        }
        String resolvedUserId = defaultIfBlank(userId, DEMO_USER_ID);
        uploads.stream()
                .filter(upload -> upload != null && upload.photoId() != null && !upload.photoId().isBlank())
                .forEach(upload -> validatePhotoBelongsToUser(upload.photoId(), resolvedUserId));
    }

    /**
     * 校验照片是否归属指定用户。
     *
     * @param photoId 照片 ID
     * @param userId 用户 ID
     */
    @Transactional(readOnly = true)
    public void validatePhotoBelongsToUser(String photoId, String userId) {
        String resolvedUserId = defaultIfBlank(userId, DEMO_USER_ID);
        boolean belongsToUser = photoRepository.findById(photoId)
                .filter(entity -> !entity.deleted())
                .filter(entity -> resolvedUserId.equals(defaultIfBlank(entity.userId(), DEMO_USER_ID)))
                .isPresent();
        if (!belongsToUser) {
            throw new IllegalArgumentException("Photo does not belong to authenticated user");
        }
    }

    /**
     * 读取单张照片的解密文件数据。
     *
     * @param photoId 照片 ID
     * @return 已存储照片数据
     */
    @Transactional(readOnly = true)
    public Optional<StoredPhotoData> findStoredPhotoData(String photoId) {
        return photoRepository.findById(photoId)
                .filter(entity -> !entity.deleted())
                .flatMap(this::toStoredPhotoData);
    }

    /**
     * 读取上传摘要关联的未删除照片。
     *
     * @param uploads 上传照片摘要
     * @return 已存储照片数据
     */
    @Transactional(readOnly = true)
    public List<StoredPhotoData> loadStoredPhotos(List<UploadSummaryRequest> uploads) {
        if (uploads == null) {
            return List.of();
        }
        return uploads.stream()
                .map(UploadSummaryRequest::photoId)
                .filter(photoId -> photoId != null && !photoId.isBlank())
                .distinct()
                .map(photoRepository::findById)
                .flatMap(Optional::stream)
                .filter(entity -> !entity.deleted())
                .map(this::toStoredPhotoData)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 生成新的用户 ID。
     *
     * @return 用户 ID
     */
    public String newUserId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 转换照片实体为文件数据。
     *
     * @param entity 照片实体
     * @return 文件数据
     */
    private Optional<StoredPhotoData> toStoredPhotoData(PhotoEntity entity) {
        return photoObjectStorageService.read(entity.storagePath())
                .map(storedBytes -> new StoredPhotoData(
                    entity.photoId(),
                    entity.slot(),
                    entity.contentType(),
                    photoEncryptionService.decrypt(storedBytes, entity.storageMode())
                ));
    }

    /**
     * 收集有效上传照片槽位。
     *
     * @param userId 用户 ID
     * @param upload 上传摘要
     * @param validSlots 有效槽位集合
     */
    private void collectValidUploadedSlot(String userId, UploadSummaryRequest upload, Set<UploadSlot> validSlots) {
        UploadSlot requestedSlot = UploadSlot.requireStyleAssessmentKey(upload.slot());
        photoRepository.findById(upload.photoId())
                .filter(entity -> !entity.deleted())
                .filter(entity -> userId.equals(defaultIfBlank(entity.userId(), DEMO_USER_ID)))
                .filter(entity -> UploadSlot.fromKey(entity.slot()) == requestedSlot)
                .map(entity -> UploadSlot.fromKey(entity.slot()))
                .ifPresent(validSlots::add);
    }

    /**
     * 转换衣橱单品快照为 API 响应。
     *
     * @param snapshot 衣橱单品快照
     * @return 衣橱单品响应
     */
    private ClosetItemResponse toClosetItemResponse(ClosetItemEntity.ClosetItemSnapshot snapshot) {
        return new ClosetItemResponse(
                snapshot.itemId(),
                snapshot.photoId(),
                displayClosetItemName(snapshot.color(), snapshot.category()),
                snapshot.category(),
                snapshot.color(),
                snapshot.brand(),
                snapshot.season(),
                snapshot.style(),
                snapshot.source()
        );
    }

    /**
     * 生成衣橱单品展示名。
     *
     * @param color 颜色
     * @param category 品类
     * @return 展示名
     */
    private String displayClosetItemName(String color, String category) {
        return "%s %s".formatted(color, category).trim();
    }

    /**
     * 判断订阅状态是否表示有效权益。
     *
     * @param status 订阅状态
     * @return 是否有效
     */
    private boolean isActiveSubscriptionStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalizedStatus = status.trim().toLowerCase();
        return "active".equals(normalizedStatus)
                || normalizedStatus.endsWith("_active")
                || "subscribed".equals(normalizedStatus);
    }

    /**
     * 转换订阅实体为状态响应。
     *
     * @param entity 订阅实体
     * @return 订阅状态响应
     */
    private SubscriptionStatusResponse toSubscriptionStatusResponse(SubscriptionEntity entity) {
        boolean active = isSubscriptionCurrentlyActive(entity);
        return new SubscriptionStatusResponse(
                active,
                entity.tier(),
                entity.plan(),
                entity.price(),
                active ? entity.status() : "expired",
                entity.expiresAt() == null ? "" : entity.expiresAt().toString()
        );
    }

    /**
     * 判断订阅实体当前是否有效。
     *
     * @param entity 订阅实体
     * @return 当前是否有效
     */
    private boolean isSubscriptionCurrentlyActive(SubscriptionEntity entity) {
        return isActiveSubscriptionStatus(entity.status())
                && entity.expiresAt() != null
                && entity.expiresAt().isAfter(Instant.now());
    }

    /**
     * 序列化对象为 JSON。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON serialization failed", exception);
        }
    }

    /**
     * 转换风格报告实体为 API 响应。
     *
     * @param entity 风格报告实体
     * @return 风格报告响应
     */
    private StyleReportResponse toStyleReportResponse(StyleReportEntity entity) {
        try {
            return objectMapper.readValue(entity.reportJson(), StyleReportResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Style report JSON parsing failed", exception);
        }
    }

    /**
     * 返回非空字符串或默认值。
     *
     * @param value 原始字符串
     * @param fallback 默认字符串
     * @return 非空字符串
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 要求用户 ID 非空。
     *
     * @param userId 用户 ID
     * @return 用户 ID
     */
    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
        return userId.trim();
    }

    /**
     * 查询未删除的风格评估照片，并过滤历史版本中被衣橱单品引用的 outfit 槽位照片。
     *
     * @param userId 已校验的用户 ID
     * @return 风格评估照片实体列表
     */
    private List<PhotoEntity> findUndeletedStylePhotos(String userId) {
        List<String> stylePhotoSlots = UploadSlot.styleAssessmentSlots().stream()
                .map(UploadSlot::key)
                .toList();
        Set<String> closetPhotoIds = closetItemRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(entity -> entity.toSnapshot().photoId())
                .collect(java.util.stream.Collectors.toSet());
        return photoRepository.findByUserIdAndDeletedFalseAndSlotInOrderByCreatedAtDesc(userId, stylePhotoSlots).stream()
                .filter(entity -> !closetPhotoIds.contains(entity.photoId()))
                .toList();
    }

    /**
     * 已存储照片数据。
     *
     * @param photoId 照片 ID
     * @param slot 照片槽位
     * @param contentType 文件 MIME 类型
     * @param bytes 文件字节
     */
    public record StoredPhotoData(
            String photoId,
            String slot,
            String contentType,
            byte[] bytes
    ) {
    }

    /**
     * 已存储照片对象引用。
     *
     * @param photoId 照片 ID
     * @param storagePath 存储路径
     */
    public record StoredPhotoReference(
            String photoId,
            String storagePath
    ) {
    }
}
