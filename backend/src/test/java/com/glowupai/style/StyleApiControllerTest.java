package com.glowupai.style;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowupai.persistence.PersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlowUp AI API 集成测试。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:glowup-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "glowup.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class StyleApiControllerTest {

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
     * 数据库访问模板。
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 持久化服务。
     */
    @Autowired
    private PersistenceService persistenceService;

    /**
     * 验证健康检查接口。
     *
     * @throws Exception 请求异常
     */
    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.status", is("ok")));
    }

    /**
     * 验证风格分析接口返回报告。
     *
     * @throws Exception 请求异常
     */
    @Test
    void analyzeReturnsStyleReport() throws Exception {
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture("test-user-1", "face", "face.png");
        StyleModels.PhotoUploadResponse bodyPhoto = uploadPhotoFixture("test-user-1", "body", "body.png");

        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "test-user-1",
                                    "name": "Emma",
                                    "authMethod": "Apple",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my personal style",
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
                .andExpect(jsonPath("$.data.score", is(93)))
                .andExpect(jsonPath("$.data.makeup", hasSize(2)))
                .andExpect(jsonPath("$.data.makeup[0]", is("Cream blush")))
                .andExpect(jsonPath("$.data.source", is("backend_mock")));

        Integer reportCount = jdbcTemplate.queryForObject(
                "select count(*) from style_reports where user_id = ?",
                Integer.class,
                "test-user-1"
        );
        assertEquals(1, reportCount);

        mockMvc.perform(get("/api/style/report").param("userId", "test-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.badge", is("Soft Signature")))
                .andExpect(jsonPath("$.data.score", is(93)))
                .andExpect(jsonPath("$.data.source", is("backend_mock")));
    }

    /**
     * 验证 iOS PRD 中的风格目标可被后端枚举正确识别。
     *
     * @throws Exception 请求异常
     */
    @Test
    void analyzeAcceptsIosStyleGoalLabels() throws Exception {
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture("ios-goal-user-1", "face", "face.png");
        StyleModels.PhotoUploadResponse bodyPhoto = uploadPhotoFixture("ios-goal-user-1", "body", "body.png");

        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "ios-goal-user-1",
                                    "name": "Emma",
                                    "authMethod": "Apple",
                                    "email": "emma@example.com",
                                    "styleGoal": "Dating confidence",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "5'6\\"",
                                    "location": "Los Angeles, CA"
                                  },
                                  "uploads": [
                                    {"photoId": "%s", "slot": "face", "name": "face.png", "type": "image/png", "size": 4},
                                    {"photoId": "%s", "slot": "body", "name": "body.png", "type": "image/png", "size": 4}
                                  ]
                                }
                                """.formatted(facePhoto.photoId(), bodyPhoto.photoId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.badge", is("Romantic Polish")))
                .andExpect(jsonPath("$.data.source", is("backend_mock")));
    }

    /**
     * 验证缺少全身照时风格分析会被拒绝。
     *
     * @throws Exception 请求异常
     */
    @Test
    void analyzeRejectsMissingRequiredBodyPhoto() throws Exception {
        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "missing-body-user-1",
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
                                    {"photoId": "face-id", "slot": "face", "name": "face.png", "type": "image/png", "size": 1200}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Face photo and full body photo are required")));
    }

    /**
     * 验证风格分析请求中的未知照片槽位会被拒绝。
     *
     * @throws Exception 请求异常
     */
    @Test
    void analyzeRejectsUnsupportedPhotoSlot() throws Exception {
        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "unsupported-slot-user-1",
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
                                    {"photoId": "avatar-id", "slot": "avatar", "name": "avatar.png", "type": "image/png", "size": 1200},
                                    {"photoId": "body-id", "slot": "body", "name": "body.png", "type": "image/png", "size": 1200}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Photo slot must be face, body, or outfit")));
    }

    /**
     * 验证未真实上传的照片 ID 会被风格分析拒绝。
     *
     * @throws Exception 请求异常
     */
    @Test
    void analyzeRejectsUnknownPhotoIds() throws Exception {
        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "unknown-photo-user-1",
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
                                    {"photoId": "face-id", "slot": "face", "name": "face.png", "type": "image/png", "size": 1200},
                                    {"photoId": "body-id", "slot": "body", "name": "body.png", "type": "image/png", "size": 1200}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Uploaded face photo and full body photo must exist before analysis")));
    }

    /**
     * 验证不能使用其他用户的照片 ID 生成风格分析。
     *
     * @throws Exception 请求异常
     */
    @Test
    void analyzeRejectsPhotoOwnedByAnotherUser() throws Exception {
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture("photo-owner-1", "face", "face.png");
        StyleModels.PhotoUploadResponse bodyPhoto = uploadPhotoFixture("photo-owner-1", "body", "body.png");

        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "photo-attacker-1",
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
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Uploaded face photo and full body photo must exist before analysis")));
    }

    /**
     * 验证用户资料接口会持久化资料。
     *
     * @throws Exception 请求异常
     */
    @Test
    void saveUserProfilePersistsProfile() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "profile-user-1",
                                  "name": "Emma",
                                  "authMethod": "Email",
                                  "email": "emma@example.com",
                                  "styleGoal": "Improve confidence",
                                  "gender": "Female",
                                  "birthday": "1998-01-15",
                                  "height": "5'6\\"",
                                  "weight": "125 lb",
                                  "location": "New York, NY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.userId", is("profile-user-1")));

        Integer profileCount = jdbcTemplate.queryForObject(
                "select count(*) from user_profiles where user_id = ?",
                Integer.class,
                "profile-user-1"
        );
        assertEquals(1, profileCount);

        String email = jdbcTemplate.queryForObject(
                "select email from user_profiles where user_id = ?",
                String.class,
                "profile-user-1"
        );
        assertEquals("emma@example.com", email);

        mockMvc.perform(get("/api/users/profile").param("userId", "profile-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.userId", is("profile-user-1")))
                .andExpect(jsonPath("$.data.email", is("emma@example.com")))
                .andExpect(jsonPath("$.data.styleGoal", is("Improve confidence")))
                .andExpect(jsonPath("$.data.weight", is("125 lb")));
    }

    /**
     * 验证用户资料接口会拒绝缺少核心风格资料的请求。
     *
     * @throws Exception 请求异常
     */
    @Test
    void saveUserProfileRejectsIncompleteProfile() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "incomplete-profile-user-1",
                                  "name": "Emma",
                                  "authMethod": "Email",
                                  "email": "emma@example.com",
                                  "styleGoal": "",
                                  "gender": "Female",
                                  "birthday": "1998-01-15",
                                  "height": "",
                                  "location": "New York, NY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", containsString("styleGoal")));
    }

    /**
     * 验证用户资料接口会拒绝未知风格目标。
     *
     * @throws Exception 请求异常
     */
    @Test
    void saveUserProfileRejectsUnsupportedStyleGoal() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "unsupported-style-goal-user-1",
                                  "name": "Emma",
                                  "authMethod": "Email",
                                  "email": "emma@example.com",
                                  "styleGoal": "Become a celebrity",
                                  "gender": "Female",
                                  "birthday": "1998-01-15",
                                  "height": "",
                                  "location": "New York, NY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Style goal must be a supported onboarding option")));
    }

    /**
     * 验证照片上传和删除接口。
     *
     * @throws Exception 请求异常
     */
    @Test
    void uploadAndDeletePhoto() throws Exception {
        byte[] originalBytes = new byte[]{1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "face.png",
                "image/png",
                originalBytes
        );

        String responseBody = mockMvc.perform(multipart("/api/photos")
                        .file(file)
                        .param("slot", "face"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.slot", is("face")))
                .andExpect(jsonPath("$.data.storageMode", is("encrypted_local_file")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(responseBody);
        String photoId = root.path("data").path("photoId").asText();
        String storagePath = jdbcTemplate.queryForObject(
                "select storage_path from photos where photo_id = ?",
                String.class,
                photoId
        );
        byte[] storedBytes = Files.readAllBytes(Path.of(storagePath));
        assertFalse(Arrays.equals(originalBytes, storedBytes));

        List<PersistenceService.StoredPhotoData> storedPhotos = persistenceService.loadStoredPhotos(new StyleModels.StyleAnalyzeRequest(
                new StyleModels.UserProfileRequest(
                        "test-user-1",
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
                List.of(new StyleModels.UploadSummaryRequest(photoId, "face", "face.png", "image/png", originalBytes.length))
        ));
        assertEquals(1, storedPhotos.size());
        assertArrayEquals(originalBytes, storedPhotos.getFirst().bytes());

        mockMvc.perform(delete("/api/photos/{photoId}", photoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.deleted", is(true)));

        Boolean deleted = jdbcTemplate.queryForObject(
                "select deleted from photos where photo_id = ?",
                Boolean.class,
                photoId
        );
        assertEquals(Boolean.TRUE, deleted);
    }

    /**
     * 验证可以查询指定用户的照片列表。
     *
     * @throws Exception 请求异常
     */
    @Test
    void listPhotosReturnsRequestedUsersPhotos() throws Exception {
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture("list-photo-user-1", "face", "list-face.png");
        StyleModels.PhotoUploadResponse bodyPhoto = uploadPhotoFixture("list-photo-user-1", "body", "list-body.png");
        uploadPhotoFixture("other-list-photo-user-1", "face", "other-list-face.png");

        String responseBody = mockMvc.perform(get("/api/photos").param("userId", "list-photo-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(responseBody).path("data");
        List<String> slots = new java.util.ArrayList<>();
        List<String> photoIds = new java.util.ArrayList<>();
        for (JsonNode item : data) {
            slots.add(item.path("slot").asText());
            photoIds.add(item.path("photoId").asText());
        }

        assertTrue(slots.contains("face"));
        assertTrue(slots.contains("body"));
        assertTrue(photoIds.contains(facePhoto.photoId()));
        assertTrue(photoIds.contains(bodyPhoto.photoId()));
    }

    /**
     * 验证可以下载照片原始字节。
     *
     * @throws Exception 请求异常
     */
    @Test
    void downloadPhotoReturnsDecryptedBytes() throws Exception {
        byte[] originalBytes = new byte[]{9, 8, 7, 6};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "download-face.png",
                "image/png",
                originalBytes
        );

        String responseBody = mockMvc.perform(multipart("/api/photos")
                        .file(file)
                        .param("userId", "download-photo-user-1")
                        .param("slot", "face"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String photoId = objectMapper.readTree(responseBody).path("data").path("photoId").asText();

        mockMvc.perform(get("/api/photos/{photoId}/content", photoId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(originalBytes));
    }

    /**
     * 验证可以按用户删除所有照片且不会删除其他用户照片。
     *
     * @throws Exception 请求异常
     */
    @Test
    void deleteUserPhotosRemovesOnlyRequestedUsersPhotos() throws Exception {
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture("bulk-photo-user-1", "face", "bulk-face.png");
        StyleModels.PhotoUploadResponse bodyPhoto = uploadPhotoFixture("bulk-photo-user-1", "body", "bulk-body.png");
        StyleModels.PhotoUploadResponse otherPhoto = uploadPhotoFixture("other-photo-user-1", "face", "other-face.png");
        String faceStoragePath = storagePathForPhoto(facePhoto.photoId());
        String bodyStoragePath = storagePathForPhoto(bodyPhoto.photoId());
        String otherStoragePath = storagePathForPhoto(otherPhoto.photoId());

        mockMvc.perform(delete("/api/photos").param("userId", "bulk-photo-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.userId", is("bulk-photo-user-1")))
                .andExpect(jsonPath("$.data.photoMetadataDeleted", is(2)))
                .andExpect(jsonPath("$.data.photoObjectsDeleted", is(2)));

        Integer deletedPhotoCount = jdbcTemplate.queryForObject(
                "select count(*) from photos where user_id = ? and deleted = true",
                Integer.class,
                "bulk-photo-user-1"
        );
        Integer otherDeletedPhotoCount = jdbcTemplate.queryForObject(
                "select count(*) from photos where user_id = ? and deleted = true",
                Integer.class,
                "other-photo-user-1"
        );
        assertEquals(2, deletedPhotoCount);
        assertEquals(0, otherDeletedPhotoCount);
        assertFalse(Files.exists(Path.of(faceStoragePath)));
        assertFalse(Files.exists(Path.of(bodyStoragePath)));
        assertTrue(Files.exists(Path.of(otherStoragePath)));
    }

    /**
     * 验证删除风格评估照片不会影响衣橱单品照片。
     *
     * @throws Exception 请求异常
     */
    @Test
    void deleteStylePhotosLeavesClosetPhotosIntact() throws Exception {
        String userId = "style-photo-closet-user-1";
        startSubscriptionFixture(userId, "Monthly");
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture(userId, "face", "style-face.png");
        StyleModels.ClosetItemResponse closetItem = uploadClosetFixture(userId, "closet-blazer.png");
        String faceStoragePath = storagePathForPhoto(facePhoto.photoId());
        String closetStoragePath = storagePathForPhoto(closetItem.photoId());

        mockMvc.perform(get("/api/photos").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].photoId", is(facePhoto.photoId())));

        mockMvc.perform(delete("/api/photos").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.photoMetadataDeleted", is(1)))
                .andExpect(jsonPath("$.data.photoObjectsDeleted", is(1)));

        Boolean faceDeleted = jdbcTemplate.queryForObject(
                "select deleted from photos where photo_id = ?",
                Boolean.class,
                facePhoto.photoId()
        );
        Boolean closetPhotoDeleted = jdbcTemplate.queryForObject(
                "select deleted from photos where photo_id = ?",
                Boolean.class,
                closetItem.photoId()
        );
        String closetSlot = jdbcTemplate.queryForObject(
                "select slot from photos where photo_id = ?",
                String.class,
                closetItem.photoId()
        );
        assertEquals(Boolean.TRUE, faceDeleted);
        assertEquals(Boolean.FALSE, closetPhotoDeleted);
        assertEquals("closet", closetSlot);
        assertFalse(Files.exists(Path.of(faceStoragePath)));
        assertTrue(Files.exists(Path.of(closetStoragePath)));

        mockMvc.perform(get("/api/closet/items").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].photoId", is(closetItem.photoId())));
    }

    /**
     * 验证衣橱单品上传、识别和查询。
     *
     * @throws Exception 请求异常
     */
    @Test
    void uploadClosetItemRecognizesAndPersistsItem() throws Exception {
        startSubscriptionFixture("closet-user-1", "Monthly");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "black-work-blazer.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/closet/items")
                        .file(file)
                        .param("userId", "closet-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.category", is("Outerwear")))
                .andExpect(jsonPath("$.data.color", is("Black")))
                .andExpect(jsonPath("$.data.season", is("Spring")))
                .andExpect(jsonPath("$.data.style", is("Professional")))
                .andExpect(jsonPath("$.data.source", is("local_rule_mvp")));

        mockMvc.perform(get("/api/closet/items").param("userId", "closet-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Black Outerwear")));

        Integer itemCount = jdbcTemplate.queryForObject(
                "select count(*) from closet_items where user_id = ?",
                Integer.class,
                "closet-user-1"
        );
        assertEquals(1, itemCount);
    }

    /**
     * 验证衣橱穿搭会使用已保存单品。
     *
     * @throws Exception 请求异常
     */
    @Test
    void closetOutfitUsesSavedWardrobeItems() throws Exception {
        startSubscriptionFixture("closet-outfit-user-1", "Monthly");
        uploadClosetFixture("closet-outfit-user-1", "white-silk-top.png");
        uploadClosetFixture("closet-outfit-user-1", "black-work-trouser.png");
        uploadClosetFixture("closet-outfit-user-1", "leather-loafer.png");
        uploadClosetFixture("closet-outfit-user-1", "black-work-blazer.png");

        mockMvc.perform(post("/api/closet/outfit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "closet-outfit-user-1",
                                  "occasion": "Work",
                                  "weather": "cool weather"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.occasion", is("Work")))
                .andExpect(jsonPath("$.data.weather", is("cool weather")))
                .andExpect(jsonPath("$.data.top", is("Ivory Top")))
                .andExpect(jsonPath("$.data.bottom", is("Black Bottom")))
                .andExpect(jsonPath("$.data.shoes", is("Neutral Shoes")))
                .andExpect(jsonPath("$.data.layer", is("Black Outerwear")))
                .andExpect(jsonPath("$.data.missingItem", is("Your closet has enough core pieces for this look.")));
    }

    /**
     * 验证不支持的照片类型会被拒绝。
     *
     * @throws Exception 请求异常
     */
    @Test
    void uploadRejectsUnsupportedPhotoType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "face.pdf",
                "application/pdf",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/photos")
                        .file(file)
                        .param("slot", "face"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Only jpg, png, heic, or heif photos are supported")));
    }

    /**
     * 验证不支持的照片槽位会被拒绝。
     *
     * @throws Exception 请求异常
     */
    @Test
    void uploadRejectsUnsupportedPhotoSlot() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/photos")
                        .file(file)
                        .param("slot", "avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Photo slot must be face, body, or outfit")));
    }

    /**
     * 验证场景穿搭接口返回三套穿搭。
     *
     * @throws Exception 请求异常
     */
    @Test
    void generateOutfitsReturnsThreeLooks() throws Exception {
        mockMvc.perform(post("/api/outfits/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "outfit-user-1",
                                    "name": "Emma",
                                    "authMethod": "Email",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my personal style",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "",
                                    "location": "New York, NY"
                                  },
                                  "occasion": "Date"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].occasion", is("Date")));
    }

    /**
     * 验证穿搭生成接口会拒绝未知场景。
     *
     * @throws Exception 请求异常
     */
    @Test
    void generateOutfitsRejectsUnsupportedOccasion() throws Exception {
        mockMvc.perform(post("/api/outfits/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "unsupported-occasion-user-1",
                                    "name": "Emma",
                                    "authMethod": "Email",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my style",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "",
                                    "location": "New York, NY"
                                  },
                                  "occasion": "Red Carpet"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Occasion must be Daily, Work, Date, Party, Travel, Gym, Wedding, or Interview")));
    }

    /**
     * 验证购物推荐接口返回商品卡数据。
     *
     * @throws Exception 请求异常
     */
    @Test
    void shoppingReturnsProducts() throws Exception {
        startSubscriptionFixture("shopping-user-1", "Monthly");

        mockMvc.perform(get("/api/shopping/recommendations")
                        .param("userId", "shopping-user-1")
                        .param("occasion", "Date"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name", is("Draped Satin Top")));
    }

    /**
     * 验证免费用户不能调用购物推荐接口。
     *
     * @throws Exception 请求异常
     */
    @Test
    void shoppingRejectsFreeUser() throws Exception {
        mockMvc.perform(get("/api/shopping/recommendations")
                        .param("userId", "free-shopping-user-1")
                        .param("occasion", "Date"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("GlowUp Plus required for Shopping Recommendations")));
    }

    /**
     * 验证聊天接口会使用穿搭照片上下文。
     *
     * @throws Exception 请求异常
     */
    @Test
    void chatUsesOutfitPhotoContext() throws Exception {
        startSubscriptionFixture("chat-user-1", "Monthly");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "chat-user-1",
                                    "name": "Emma",
                                    "authMethod": "Apple",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my personal style",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "5'6\\"",
                                    "location": "New York, NY"
                                  },
                                  "message": "Can I wear this?",
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
     * 验证聊天请求中的未知照片槽位会被拒绝。
     *
     * @throws Exception 请求异常
     */
    @Test
    void chatRejectsUnsupportedPhotoSlot() throws Exception {
        startSubscriptionFixture("chat-unsupported-slot-user-1", "Monthly");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "chat-unsupported-slot-user-1",
                                    "name": "Emma",
                                    "authMethod": "Apple",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my personal style",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "5'6\\"",
                                    "location": "New York, NY"
                                  },
                                  "message": "Can I wear this?",
                                  "uploads": [
                                    {"photoId": "avatar-id", "slot": "avatar", "name": "avatar.png", "type": "image/png", "size": 1200}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Photo slot must be face, body, or outfit")));
    }

    /**
     * 验证免费用户不能调用 AI 聊天接口。
     *
     * @throws Exception 请求异常
     */
    @Test
    void chatRejectsFreeUser() throws Exception {
        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "free-chat-user-1",
                                    "name": "Emma",
                                    "authMethod": "Apple",
                                    "email": "emma@example.com",
                                    "styleGoal": "Find my personal style",
                                    "gender": "Female",
                                    "birthday": "1998-01-15",
                                    "height": "5'6\\"",
                                    "location": "New York, NY"
                                  },
                                  "message": "Can I wear this?",
                                  "uploads": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("GlowUp Plus required for AI Stylist Chat")));
    }

    /**
     * 验证分享点击埋点会被持久化。
     *
     * @throws Exception 请求异常
     */
    @Test
    void trackShareClickedPersistsAnalyticsEvent() throws Exception {
        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "share_clicked",
                                  "payload": {
                                    "userId": "share-user-1",
                                    "surface": "style_report",
                                    "styleType": "Soft Signature"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.status", is("accepted")));

        Integer eventCount = jdbcTemplate.queryForObject(
                "select count(*) from analytics_events where user_id = ? and event_name = ?",
                Integer.class,
                "share-user-1",
                "share_clicked"
        );
        assertEquals(1, eventCount);
    }

    /**
     * 验证未知埋点事件会被拒绝，避免埋点口径失控。
     *
     * @throws Exception 请求异常
     */
    @Test
    void trackRejectsUnsupportedAnalyticsEvent() throws Exception {
        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "unknown_event",
                                  "payload": {
                                    "userId": "analytics-user-1"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Unsupported analytics event: unknown_event")));
    }

    /**
     * 验证未订阅用户返回免费状态。
     *
     * @throws Exception 请求异常
     */
    @Test
    void subscriptionStatusReturnsInactiveForFreeUser() throws Exception {
        mockMvc.perform(get("/api/subscriptions/status").param("userId", "free-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.active", is(false)))
                .andExpect(jsonPath("$.data.tier", is("Free")))
                .andExpect(jsonPath("$.data.status", is("inactive")));
    }

    /**
     * 验证订阅开始后可查询到有效 Plus 状态。
     *
     * @throws Exception 请求异常
     */
    @Test
    void subscriptionStartPersistsActiveStatus() throws Exception {
        mockMvc.perform(post("/api/subscriptions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "plus-user-1",
                                  "plan": "Yearly"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.tier", is("Plus")))
                .andExpect(jsonPath("$.data.plan", is("Yearly")))
                .andExpect(jsonPath("$.data.status", is("active")))
                .andExpect(jsonPath("$.data.expiresAt", containsString("T")));

        mockMvc.perform(get("/api/subscriptions/status").param("userId", "plus-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.active", is(true)))
                .andExpect(jsonPath("$.data.tier", is("Plus")))
                .andExpect(jsonPath("$.data.plan", is("Yearly")))
                .andExpect(jsonPath("$.data.expiresAt", containsString("T")));
    }

    /**
     * 验证已过期订阅不会继续返回有效 Plus 状态。
     *
     * @throws Exception 请求异常
     */
    @Test
    void subscriptionStatusReturnsExpiredWhenSubscriptionIsPastExpiresAt() throws Exception {
        startSubscriptionFixture("expired-plus-user-1", "Monthly");
        jdbcTemplate.update(
                "update subscriptions set expires_at = ? where user_id = ?",
                Timestamp.from(Instant.now().minusSeconds(60)),
                "expired-plus-user-1"
        );

        mockMvc.perform(get("/api/subscriptions/status").param("userId", "expired-plus-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.active", is(false)))
                .andExpect(jsonPath("$.data.tier", is("Plus")))
                .andExpect(jsonPath("$.data.plan", is("Monthly")))
                .andExpect(jsonPath("$.data.status", is("expired")));
    }

    /**
     * 验证订阅接口会拒绝未知套餐。
     *
     * @throws Exception 请求异常
     */
    @Test
    void subscriptionStartRejectsUnsupportedPlan() throws Exception {
        mockMvc.perform(post("/api/subscriptions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "unsupported-plan-user-1",
                                  "plan": "Lifetime"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Subscription plan must be Weekly, Monthly, or Yearly")));
    }

    /**
     * 验证用户数据删除会清除照片对象和所有用户维度记录。
     *
     * @throws Exception 请求异常
     */
    @Test
    void deleteUserDataRemovesPhotosAndDerivedRecords() throws Exception {
        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "privacy-user-1",
                                  "name": "Privacy User",
                                  "authMethod": "Email",
                                  "email": "privacy@example.com",
                                  "styleGoal": "Find my style",
                                  "gender": "Female",
                                  "birthday": "1995-04-18",
                                  "height": "5'5\\"",
                                  "location": "Austin, TX"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
        StyleModels.PhotoUploadResponse facePhoto = uploadPhotoFixture("privacy-user-1", "face", "privacy-face.png");
        StyleModels.PhotoUploadResponse bodyPhoto = uploadPhotoFixture("privacy-user-1", "body", "privacy-body.png");
        startSubscriptionFixture("privacy-user-1", "Monthly");
        uploadClosetFixture("privacy-user-1", "privacy-blazer.png");
        mockMvc.perform(post("/api/style/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": {
                                    "userId": "privacy-user-1",
                                    "name": "Privacy User",
                                    "authMethod": "Email",
                                    "email": "privacy@example.com",
                                    "styleGoal": "Find my style",
                                    "gender": "Female",
                                    "birthday": "1995-04-18",
                                    "height": "5'5\\"",
                                    "location": "Austin, TX"
                                  },
                                  "uploads": [
                                    {"photoId": "%s", "slot": "face", "name": "privacy-face.png", "type": "image/png", "size": 4},
                                    {"photoId": "%s", "slot": "body", "name": "privacy-body.png", "type": "image/png", "size": 4}
                                  ]
                                }
                                """.formatted(facePhoto.photoId(), bodyPhoto.photoId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "share_clicked",
                                  "payload": {
                                    "userId": "privacy-user-1",
                                    "surface": "profile"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
        List<String> storagePaths = jdbcTemplate.queryForList(
                "select storage_path from photos where user_id = ?",
                String.class,
                "privacy-user-1"
        );
        assertEquals(3, storagePaths.size());
        storagePaths.forEach(path -> assertTrue(Files.exists(Path.of(path))));

        mockMvc.perform(delete("/api/users/{userId}", "privacy-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.userId", is("privacy-user-1")))
                .andExpect(jsonPath("$.data.profileDeleted", is(true)))
                .andExpect(jsonPath("$.data.photoMetadataDeleted", is(3)))
                .andExpect(jsonPath("$.data.photoObjectsDeleted", is(3)))
                .andExpect(jsonPath("$.data.closetItemsDeleted", is(1)))
                .andExpect(jsonPath("$.data.styleReportsDeleted", is(1)))
                .andExpect(jsonPath("$.data.subscriptionsDeleted", is(1)))
                .andExpect(jsonPath("$.data.analyticsEventsDeleted", is(1)));

        storagePaths.forEach(path -> assertFalse(Files.exists(Path.of(path))));
        assertEquals(0, countRows("user_profiles", "privacy-user-1"));
        assertEquals(0, countRows("photos", "privacy-user-1"));
        assertEquals(0, countRows("closet_items", "privacy-user-1"));
        assertEquals(0, countRows("style_reports", "privacy-user-1"));
        assertEquals(0, countRows("subscriptions", "privacy-user-1"));
        assertEquals(0, countRows("analytics_events", "privacy-user-1"));
    }

    /**
     * 上传衣橱测试单品。
     *
     * @param userId 用户 ID
     * @param fileName 文件名
     * @throws Exception 请求异常
     */
    private StyleModels.ClosetItemResponse uploadClosetFixture(String userId, String fileName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "image/png",
                new byte[]{1, 2, 3, 4}
        );
        String responseBody = mockMvc.perform(multipart("/api/closet/items")
                        .file(file)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        return objectMapper.treeToValue(data, StyleModels.ClosetItemResponse.class);
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

    /**
     * 查询测试照片存储路径。
     *
     * @param photoId 照片 ID
     * @return 存储路径
     */
    private String storagePathForPhoto(String photoId) {
        return jdbcTemplate.queryForObject(
                "select storage_path from photos where photo_id = ?",
                String.class,
                photoId
        );
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
     * 查询指定表的用户记录数。
     *
     * @param tableName 表名
     * @param userId 用户 ID
     * @return 记录数量
     */
    private Integer countRows(String tableName, String userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + tableName + " where user_id = ?",
                Integer.class,
                userId
        );
    }
}
