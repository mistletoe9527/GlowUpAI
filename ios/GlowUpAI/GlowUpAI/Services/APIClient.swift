import Foundation

/// GlowUp Java 后端 API 客户端。
final class APIClient {
    /// 后端基础地址。
    private let baseURL: URL

    /// JSON 编码器。
    private let encoder: JSONEncoder

    /// JSON 解码器。
    private let decoder: JSONDecoder

    /// Firebase ID token 提供器。
    private let idTokenProvider: (() async -> String?)?

    /// 用户身份请求头。
    private static let userIdHeaderName = "X-GlowUp-User-Id"

    /// Authorization 请求头。
    private static let authorizationHeaderName = "Authorization"

    /// 默认后端基础地址。
    private static var defaultBaseURL: URL {
        if let value = Bundle.main.object(forInfoDictionaryKey: "GLOWUP_API_BASE_URL") as? String,
           let url = URL(string: value) {
            return url
        }
        return URL(string: "http://127.0.0.1:8080/api")!
    }

    /// 创建 API 客户端。
    init(baseURL: URL = APIClient.defaultBaseURL, idTokenProvider: (() async -> String?)? = nil) {
        self.baseURL = baseURL
        self.encoder = JSONEncoder()
        self.decoder = JSONDecoder()
        self.idTokenProvider = idTokenProvider
    }

    /// 保存用户资料。
    func saveProfile(_ profile: UserProfile) async throws -> UserProfileResponse {
        try await postJSON(path: "/users/profile", body: profile, userId: profile.userId)
    }

    /// 查询用户资料。
    func fetchProfile(userId: String) async throws -> UserProfile {
        var components = URLComponents(url: baseURL.appendingPathComponent("users/profile"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "userId", value: userId)]
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 上传照片文件。
    func uploadPhoto(userId: String, slot: UploadSlot, upload: PhotoUpload) async throws -> PhotoUploadResponse {
        guard let photoData = upload.data, !photoData.isEmpty else {
            throw APIError.photoDataUnavailable
        }
        let boundary = "Boundary-\(UUID().uuidString)"
        var request = URLRequest(url: baseURL.appendingPathComponent("photos"))
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        await attachAuthHeaders(userId: userId, to: &request)
        request.httpBody = multipartBody(
            boundary: boundary,
            fields: [
                "userId": userId,
                "slot": slot.rawValue
            ],
            fileField: "file",
            fileName: upload.name,
            mimeType: upload.mimeType,
            data: photoData
        )
        return try await decodeResponse(request)
    }

    /// 删除照片文件。
    func deletePhoto(userId: String, photoId: String) async throws {
        var request = URLRequest(url: baseURL.appendingPathComponent("photos/\(photoId)"))
        request.httpMethod = "DELETE"
        await attachAuthHeaders(userId: userId, to: &request)
        let _: EmptyObject = try await decodeResponse(request)
    }

    /// 删除用户全部照片文件。
    func deleteUserPhotos(userId: String) async throws -> PhotoDeletionResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent("photos"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "userId", value: userId)]
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 查询用户已上传照片。
    func listPhotos(userId: String) async throws -> [PhotoUploadResponse] {
        var components = URLComponents(url: baseURL.appendingPathComponent("photos"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "userId", value: userId)]
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 下载单张照片的原始内容。
    func downloadPhotoContent(userId: String, photoId: String) async throws -> Data {
        let url = baseURL
            .appendingPathComponent("photos")
            .appendingPathComponent(photoId)
            .appendingPathComponent("content")
        var request = URLRequest(url: url)
        await attachAuthHeaders(userId: userId, to: &request)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw APIError.httpFailure
        }
        return data
    }

    /// 删除用户账户数据。
    func deleteUserData(userId: String) async throws -> UserDataDeletionResponse {
        var request = URLRequest(url: baseURL.appendingPathComponent("users").appendingPathComponent(userId))
        request.httpMethod = "DELETE"
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 生成风格报告。
    func analyze(profile: UserProfile, uploads: [UploadSummaryRequest]) async throws -> StyleReportResponse {
        try await postJSON(path: "/style/analyze", body: StyleAnalyzeRequest(profile: profile, uploads: uploads), userId: profile.userId)
    }

    /// 查询用户最近一次风格报告。
    func latestStyleReport(userId: String) async throws -> StyleReportResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent("style/report"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "userId", value: userId)]
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 生成穿搭列表。
    func generateOutfits(profile: UserProfile, occasion: Occasion) async throws -> [OutfitResponse] {
        try await postJSON(path: "/outfits/generate", body: OutfitGenerateRequest(profile: profile, occasion: occasion.rawValue), userId: profile.userId)
    }

    /// 查询购物推荐。
    func recommendProducts(userId: String, occasion: Occasion) async throws -> [ProductResponse] {
        var components = URLComponents(url: baseURL.appendingPathComponent("shopping/recommendations"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "userId", value: userId),
            URLQueryItem(name: "occasion", value: occasion.rawValue)
        ]
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 上传并识别衣橱单品。
    func uploadClosetItem(userId: String, fileName: String, mimeType: String, data: Data) async throws -> ClosetItemResponse {
        let boundary = "Boundary-\(UUID().uuidString)"
        var request = URLRequest(url: baseURL.appendingPathComponent("closet/items"))
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        await attachAuthHeaders(userId: userId, to: &request)
        request.httpBody = multipartBody(
            boundary: boundary,
            fields: [
                "userId": userId
            ],
            fileField: "file",
            fileName: fileName,
            mimeType: mimeType,
            data: data
        )
        return try await decodeResponse(request)
    }

    /// 查询用户衣橱单品。
    func listClosetItems(userId: String) async throws -> [ClosetItemResponse] {
        var components = URLComponents(url: baseURL.appendingPathComponent("closet/items"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "userId", value: userId)]
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 基于用户衣橱生成今日穿搭。
    func generateClosetOutfit(userId: String, occasion: Occasion, weather: String) async throws -> ClosetOutfitResponse {
        try await postJSON(
            path: "/closet/outfit",
            body: ClosetOutfitRequest(userId: userId, occasion: occasion.rawValue, weather: weather),
            userId: userId
        )
    }

    /// 生成聊天回复。
    func chat(profile: UserProfile, message: String, uploads: [UploadSummaryRequest] = []) async throws -> ChatMessageResponse {
        try await postJSON(path: "/chat/message", body: ChatMessageRequest(profile: profile, message: message, uploads: uploads), userId: profile.userId)
    }

    /// 上报埋点事件。
    func track(name: String, payload: [String: String]) async throws {
        let _: EmptyObject = try await postJSON(path: "/analytics/events", body: AnalyticsEventRequest(name: name, payload: payload), userId: payload["userId"])
    }

    /// 开始订阅。
    func startSubscription(userId: String, plan: SubscriptionPlan) async throws -> SubscriptionStartResponse {
        try await postJSON(path: "/subscriptions/start", body: SubscriptionStartRequest(userId: userId, plan: plan.rawValue), userId: userId)
    }

    /// 查询订阅状态。
    func subscriptionStatus(userId: String) async throws -> SubscriptionStatusResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent("subscriptions/status"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "userId", value: userId)]
        guard let url = components?.url else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        await attachAuthHeaders(userId: userId, to: &request)
        return try await decodeResponse(request)
    }

    /// 发送 JSON POST 请求。
    private func postJSON<T: Encodable, R: Decodable>(path: String, body: T, userId: String? = nil) async throws -> R {
        var request = URLRequest(url: baseURL.appendingPathComponent(path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        await attachAuthHeaders(userId: userId, to: &request)
        request.httpBody = try encoder.encode(body)
        return try await decodeResponse(request)
    }

    /// 写入请求身份头。
    private func attachAuthHeaders(userId: String?, to request: inout URLRequest) async {
        guard let userId, !userId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            await attachBearerToken(to: &request)
            return
        }
        request.setValue(userId, forHTTPHeaderField: APIClient.userIdHeaderName)
        await attachBearerToken(to: &request)
    }

    /// 写入 Firebase Bearer token。
    private func attachBearerToken(to request: inout URLRequest) async {
        guard let token = await idTokenProvider?(),
              !token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return
        }
        request.setValue("Bearer \(token)", forHTTPHeaderField: APIClient.authorizationHeaderName)
    }

    /// 解码统一响应。
    private func decodeResponse<R: Decodable>(_ request: URLRequest) async throws -> R {
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw APIError.httpFailure
        }
        let result = try decoder.decode(ApiResult<R>.self, from: data)
        guard result.code == 0, let payload = result.data else {
            throw APIError.businessFailure(result.message)
        }
        return payload
    }

    /// 构造 multipart 请求体。
    private func multipartBody(
        boundary: String,
        fields: [String: String],
        fileField: String,
        fileName: String,
        mimeType: String,
        data: Data
    ) -> Data {
        var body = Data()
        fields.forEach { key, value in
            body.appendString("--\(boundary)\r\n")
            body.appendString("Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n")
            body.appendString("\(value)\r\n")
        }
        body.appendString("--\(boundary)\r\n")
        body.appendString("Content-Disposition: form-data; name=\"\(fileField)\"; filename=\"\(fileName)\"\r\n")
        body.appendString("Content-Type: \(mimeType)\r\n\r\n")
        body.append(data)
        body.appendString("\r\n--\(boundary)--\r\n")
        return body
    }
}

/// 空对象响应。
private struct EmptyObject: Decodable {
}

/// API 错误。
enum APIError: LocalizedError {
    /// URL 无效。
    case invalidURL
    /// HTTP 请求失败。
    case httpFailure
    /// 当前设备没有可上传的照片数据。
    case photoDataUnavailable
    /// 业务失败。
    case businessFailure(String)

    /// 错误说明。
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid API URL."
        case .httpFailure:
            return "Backend request failed."
        case .photoDataUnavailable:
            return "Photo data is unavailable on this device."
        case .businessFailure(let message):
            return message
        }
    }
}

/// Data 写入字符串工具。
private extension Data {
    /// 追加 UTF-8 字符串。
    mutating func appendString(_ string: String) {
        append(Data(string.utf8))
    }
}
