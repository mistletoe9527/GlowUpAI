import Foundation

/// App 的顶层显示状态。
enum AppRoute {
    /// 首次进入和资料收集流程。
    case onboarding
    /// 已进入主应用。
    case main
}

/// Onboarding 的步骤。
enum OnboardingStep: Int, CaseIterable {
    /// 欢迎页。
    case welcome = 0
    /// 性别选择页。
    case gender = 1
    /// 生日和基础资料页。
    case profileDetails = 2
    /// 风格目标选择页。
    case styleGoal = 3

    /// 不包含欢迎页的资料填写步骤数量。
    static let contentStepCount = 3
}

/// 登录方式。
enum AuthMethod: String, CaseIterable, Identifiable {
    /// Apple 登录。
    case apple = "Apple"
    /// Google 登录。
    case google = "Google"
    /// Email 登录。
    case email = "Email"
    /// 本地演示登录。
    case demo = "Demo"

    /// 登录方式唯一 ID。
    var id: String { rawValue }
}

/// 主应用底部导航标签。
enum AppTab: String, CaseIterable, Identifiable {
    /// 首页。
    case home = "Home"
    /// 照片分析页。
    case analyze = "Analyze"
    /// 穿搭生成页。
    case style = "Style"
    /// 虚拟衣橱页。
    case closet = "Closet"
    /// 个人资料页。
    case profile = "Profile"

    /// 标签唯一 ID。
    var id: String { rawValue }

    /// 标签图标。
    var iconName: String {
        switch self {
        case .home:
            return "sparkles"
        case .analyze:
            return "camera.viewfinder"
        case .style:
            return "wand.and.stars"
        case .closet:
            return "tshirt"
        case .profile:
            return "person"
        }
    }
}

/// 用户风格目标。
enum StyleGoal: String, CaseIterable, Identifiable, Codable {
    /// 提升吸引力。
    case lookMoreAttractive = "Look more attractive"
    /// 职场专业形象。
    case professional = "Professional"
    /// 找到个人风格。
    case findMyStyle = "Find my style"
    /// 约会自信。
    case datingConfidence = "Dating confidence"
    /// 日常穿搭。
    case everydayOutfit = "Everyday outfit"

    /// 目标唯一 ID。
    var id: String { rawValue }

    /// 目标说明文案。
    var subtitle: String {
        switch self {
        case .lookMoreAttractive:
            return "Highlight your best features with polished daily choices."
        case .professional:
            return "Build a sharper wardrobe for work and interviews."
        case .findMyStyle:
            return "Discover a signature look that feels like you."
        case .datingConfidence:
            return "Create romantic looks that still feel natural."
        case .everydayOutfit:
            return "Make getting dressed simple every morning."
        }
    }

    /// 目标图标。
    var iconName: String {
        switch self {
        case .lookMoreAttractive:
            return "heart.fill"
        case .professional:
            return "briefcase.fill"
        case .findMyStyle:
            return "sparkles"
        case .datingConfidence:
            return "flame.fill"
        case .everydayOutfit:
            return "sun.max.fill"
        }
    }
}

/// 用户性别选项。
enum GenderOption: String, CaseIterable, Identifiable, Codable {
    /// 女性。
    case female = "Female"
    /// 男性。
    case male = "Male"
    /// 非二元。
    case nonBinary = "Non-binary"
    /// 不透露。
    case preferNotToSay = "Prefer not to say"

    /// 性别唯一 ID。
    var id: String { rawValue }

    /// 性别图标。
    var iconName: String {
        switch self {
        case .female:
            return "person.fill"
        case .male:
            return "person.fill"
        case .nonBinary:
            return "person.2.fill"
        case .preferNotToSay:
            return "lock.fill"
        }
    }
}

/// 支持的地区代码。
enum RegionCode: String, Codable {
    /// 美国市场，使用 ISO 3166-1 alpha-2 国家码。
    case unitedStates = "US"

    /// 地区展示名称。
    var displayName: String {
        switch self {
        case .unitedStates:
            return "United States"
        }
    }
}

/// 穿搭使用场景。
enum Occasion: String, CaseIterable, Identifiable, Codable {
    /// 日常。
    case daily = "Daily"
    /// 工作。
    case work = "Work"
    /// 约会。
    case date = "Date"
    /// 派对。
    case party = "Party"
    /// 旅行。
    case travel = "Travel"
    /// 健身。
    case gym = "Gym"
    /// 婚礼。
    case wedding = "Wedding"
    /// 面试。
    case interview = "Interview"

    /// 场景唯一 ID。
    var id: String { rawValue }
}

/// 上传照片槽位。
enum UploadSlot: String, CaseIterable, Identifiable, Codable {
    /// 面部照片。
    case face
    /// 全身照片。
    case body
    /// 当前穿搭照片。
    case outfit

    /// 槽位唯一 ID。
    var id: String { rawValue }

    /// 照片标题。
    var title: String {
        switch self {
        case .face:
            return "Face photo"
        case .body:
            return "Full body photo"
        case .outfit:
            return "Current outfit"
        }
    }

    /// 照片说明。
    var description: String {
        switch self {
        case .face:
            return "Required to read face balance and hair direction."
        case .body:
            return "Required to assess fit, proportions, and silhouette."
        case .outfit:
            return "Optional. Improves current outfit feedback."
        }
    }

    /// 是否为生成报告的必填照片。
    var isRequired: Bool {
        self == .face || self == .body
    }
}

/// 照片上传状态。
enum UploadStatus: String {
    /// 未上传。
    case empty = "Upload"
    /// 正在上传。
    case uploading = "Uploading"
    /// 已同步后端。
    case synced = "Synced"
    /// 后端照片已同步，但当前设备无法下载预览。
    case previewUnavailable = "Preview unavailable"
    /// 仅保存在本机。
    case local = "Local"
}

/// 订阅套餐。
enum SubscriptionPlan: String, CaseIterable, Identifiable, Codable {
    /// 周付套餐。
    case weekly = "Weekly"
    /// 月付套餐。
    case monthly = "Monthly"
    /// 年付套餐。
    case yearly = "Yearly"

    /// 套餐唯一 ID。
    var id: String { rawValue }

    /// App Store Connect 商品 ID。
    var productId: String {
        switch self {
        case .weekly:
            return "com.glowupai.plus.weekly"
        case .monthly:
            return "com.glowupai.plus.monthly"
        case .yearly:
            return "com.glowupai.plus.yearly"
        }
    }

    /// 套餐价格。
    var price: String {
        switch self {
        case .weekly:
            return "$4.99"
        case .monthly:
            return "$14.99"
        case .yearly:
            return "$79.99"
        }
    }

    /// 套餐说明。
    var caption: String {
        switch self {
        case .weekly:
            return "Try the full stylist experience"
        case .monthly:
            return "Best for steady wardrobe upgrades"
        case .yearly:
            return "Best value for daily guidance"
        }
    }
}

/// StoreKit 商品展示模型。
struct StoreProduct: Identifiable, Equatable {
    /// 商品 ID。
    let id: String
    /// 对应订阅套餐。
    let plan: SubscriptionPlan
    /// App Store 商品名。
    let displayName: String
    /// App Store 展示价格。
    let displayPrice: String
    /// App Store 商品描述。
    let description: String
}

/// 订阅购买状态。
enum SubscriptionPurchaseState: Equatable {
    /// 尚未加载。
    case idle
    /// 正在加载商品。
    case loading
    /// 可以购买。
    case ready
    /// 正在购买。
    case purchasing
    /// 已订阅。
    case subscribed
    /// StoreKit 不可用或产品未配置。
    case unavailable(String)
}

/// Plus 权益功能。
enum PremiumFeature {
    /// AI Stylist 聊天。
    case aiStylistChat
    /// AI 衣橱。
    case aiCloset
    /// 购物推荐。
    case shoppingRecommendations

    /// 功能标题。
    var title: String {
        switch self {
        case .aiStylistChat:
            return "AI Stylist Chat"
        case .aiCloset:
            return "AI Closet"
        case .shoppingRecommendations:
            return "Shopping Recommendations"
        }
    }

    /// 功能说明。
    var subtitle: String {
        switch self {
        case .aiStylistChat:
            return "Ask outfit questions and attach a look before you go."
        case .aiCloset:
            return "Save wardrobe pieces and build outfits from your closet."
        case .shoppingRecommendations:
            return "Get affiliate-ready product picks for your occasion."
        }
    }

    /// 订阅页提示文案。
    var paywallReason: String {
        "\(title) is included with GlowUp Plus."
    }

    /// 功能图标。
    var iconName: String {
        switch self {
        case .aiStylistChat:
            return "sparkles"
        case .aiCloset:
            return "tshirt"
        case .shoppingRecommendations:
            return "bag"
        }
    }
}

/// 用户资料请求。
struct UserProfile: Codable, Equatable {
    /// 用户 ID。
    var userId: String
    /// 用户昵称。
    var name: String
    /// 登录方式。
    var authMethod: String
    /// 登录邮箱。
    var email: String
    /// 风格目标。
    var styleGoal: String
    /// 性别。
    var gender: String
    /// 生日，格式为 yyyy-MM-dd。
    var birthday: String
    /// 身高。
    var height: String
    /// 体重。
    var weight: String
    /// 所在地区国家码。
    var location: String

    /// 用户资料字段。
    enum CodingKeys: String, CodingKey {
        /// 用户 ID。
        case userId
        /// 用户昵称。
        case name
        /// 登录方式。
        case authMethod
        /// 登录邮箱。
        case email
        /// 风格目标。
        case styleGoal
        /// 性别。
        case gender
        /// 生日。
        case birthday
        /// 身高。
        case height
        /// 体重。
        case weight
        /// 所在地区国家码。
        case location
    }

    /// 创建用户资料。
    ///
    /// - Parameters:
    ///   - userId: 用户 ID
    ///   - name: 用户昵称
    ///   - authMethod: 登录方式
    ///   - email: 登录邮箱
    ///   - styleGoal: 风格目标
    ///   - gender: 性别
    ///   - birthday: 生日
    ///   - height: 身高
    ///   - weight: 体重
    ///   - location: 所在地区国家码
    init(
        userId: String,
        name: String,
        authMethod: String,
        email: String,
        styleGoal: String,
        gender: String,
        birthday: String,
        height: String,
        weight: String,
        location: String
    ) {
        self.userId = userId
        self.name = name
        self.authMethod = authMethod
        self.email = email
        self.styleGoal = styleGoal
        self.gender = gender
        self.birthday = birthday
        self.height = height
        self.weight = weight
        self.location = location
    }

    /// 从 JSON 解码用户资料，兼容旧版本缺失的可选字段。
    ///
    /// - Parameter decoder: 解码器
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        userId = try container.decode(String.self, forKey: .userId)
        name = try container.decode(String.self, forKey: .name)
        authMethod = try container.decode(String.self, forKey: .authMethod)
        email = try container.decodeIfPresent(String.self, forKey: .email) ?? ""
        styleGoal = try container.decode(String.self, forKey: .styleGoal)
        gender = try container.decode(String.self, forKey: .gender)
        birthday = try container.decode(String.self, forKey: .birthday)
        height = try container.decodeIfPresent(String.self, forKey: .height) ?? ""
        weight = try container.decodeIfPresent(String.self, forKey: .weight) ?? ""
        location = try container.decode(String.self, forKey: .location)
    }

    /// 默认用户资料。
    static var empty: UserProfile {
        UserProfile(
            userId: UUID().uuidString,
            name: "Emma",
            authMethod: AuthMethod.demo.rawValue,
            email: "",
            styleGoal: "",
            gender: "",
            birthday: "",
            height: "",
            weight: "",
            location: RegionCode.unitedStates.rawValue
        )
    }
}

/// 本地 App 会话快照。
struct AppSessionSnapshot: Codable, Equatable {
    /// 快照版本。
    let version: Int
    /// 已完成 Onboarding 的用户资料。
    let profile: UserProfile
    /// 最近选择的穿搭场景。
    let selectedOccasion: Occasion
    /// 最近选择的订阅套餐。
    let selectedPlan: SubscriptionPlan
    /// 是否已生成风格报告。
    let hasCompletedStyleReport: Bool?
    /// 最近的风格报告。
    let report: StyleReportResponse?
    /// 最近的穿搭列表。
    let outfits: [OutfitResponse]?
    /// 最近的商品推荐。
    let products: [ProductResponse]?
    /// 最近的衣橱单品。
    let closetItems: [ClosetItemResponse]?
    /// 最近的衣橱穿搭。
    let closetOutfit: ClosetOutfitResponse?
    /// 是否已生成衣橱穿搭。
    let hasGeneratedClosetOutfit: Bool?

    /// 当前快照版本。
    static let currentVersion = 3
}

/// 本地照片状态。
struct PhotoUpload: Identifiable, Equatable {
    /// 本地唯一 ID。
    let id: UUID
    /// 照片槽位。
    let slot: UploadSlot
    /// 文件名。
    let name: String
    /// MIME 类型。
    let mimeType: String
    /// 文件大小。
    let size: Int
    /// 当前设备可用的原始图片数据；恢复时下载失败则为空。
    let data: Data?
    /// 后端照片 ID。
    var photoId: String
    /// 当前同步状态。
    var status: UploadStatus

    /// 创建本地照片状态。
    init(
        id: UUID = UUID(),
        slot: UploadSlot,
        name: String,
        mimeType: String,
        size: Int,
        data: Data?,
        photoId: String = "",
        status: UploadStatus = .uploading
    ) {
        self.id = id
        self.slot = slot
        self.name = name
        self.mimeType = mimeType
        self.size = size
        self.data = data
        self.photoId = photoId
        self.status = status
    }
}

/// 后端统一响应。
struct ApiResult<T: Decodable>: Decodable {
    /// 业务状态码。
    let code: Int
    /// 业务提示。
    let message: String
    /// 响应数据。
    let data: T?
}

/// 用户资料保存响应。
struct UserProfileResponse: Decodable {
    /// 用户 ID。
    let userId: String
    /// 保存状态。
    let status: String
}

/// 用户数据删除响应。
struct UserDataDeletionResponse: Decodable {
    /// 用户 ID。
    let userId: String
    /// 用户资料是否删除。
    let profileDeleted: Bool
    /// 照片元数据删除数量。
    let photoMetadataDeleted: Int
    /// 照片对象删除数量。
    let photoObjectsDeleted: Int
    /// 衣橱单品删除数量。
    let closetItemsDeleted: Int
    /// 风格报告删除数量。
    let styleReportsDeleted: Int
    /// 订阅记录删除数量。
    let subscriptionsDeleted: Int
    /// 埋点事件删除数量。
    let analyticsEventsDeleted: Int
}

/// 照片删除响应。
struct PhotoDeletionResponse: Decodable {
    /// 用户 ID。
    let userId: String
    /// 照片元数据删除数量。
    let photoMetadataDeleted: Int
    /// 照片对象删除数量。
    let photoObjectsDeleted: Int
}

/// 上传照片摘要。
struct UploadSummaryRequest: Codable {
    /// 后端照片 ID。
    let photoId: String
    /// 照片槽位。
    let slot: String
    /// 文件名。
    let name: String
    /// MIME 类型。
    let type: String
    /// 文件大小。
    let size: Int
}

/// 照片上传响应。
struct PhotoUploadResponse: Decodable {
    /// 后端照片 ID。
    let photoId: String
    /// 照片槽位。
    let slot: String
    /// 原始文件名。
    let name: String
    /// MIME 类型。
    let type: String
    /// 文件大小。
    let size: Int
    /// 存储方式。
    let storageMode: String
}

/// 风格分析请求。
struct StyleAnalyzeRequest: Encodable {
    /// 用户资料。
    let profile: UserProfile
    /// 上传照片摘要。
    let uploads: [UploadSummaryRequest]
}

/// 穿搭生成请求。
struct OutfitGenerateRequest: Encodable {
    /// 用户资料。
    let profile: UserProfile
    /// 穿搭场景。
    let occasion: String
}

/// 聊天消息请求。
struct ChatMessageRequest: Encodable {
    /// 用户资料。
    let profile: UserProfile
    /// 用户消息。
    let message: String
    /// 聊天附带照片摘要。
    let uploads: [UploadSummaryRequest]
}

/// 埋点事件请求。
struct AnalyticsEventRequest: Encodable {
    /// 事件名称。
    let name: String
    /// 事件参数。
    let payload: [String: String]
}

/// 订阅开始请求。
struct SubscriptionStartRequest: Encodable {
    /// 用户 ID。
    let userId: String
    /// 套餐名称。
    let plan: String
}

/// 色板响应。
struct PaletteResponse: Codable, Identifiable, Equatable {
    /// 色板唯一 ID。
    var id: String { name + color }
    /// 颜色名。
    let name: String
    /// 十六进制色值。
    let color: String
}

/// 单套穿搭响应。
struct OutfitResponse: Codable, Identifiable, Equatable {
    /// 排序 ID。
    var id: Int { order }
    /// 排序。
    let order: Int
    /// 场景。
    let occasion: String
    /// 风格名。
    let style: String
    /// 上装。
    let top: String
    /// 下装。
    let bottom: String
    /// 鞋履。
    let shoes: String
    /// 推荐理由。
    let why: String
}

/// 日常穿搭响应。
struct DailyLookResponse: Codable, Equatable {
    /// 场景。
    let occasion: String
    /// 上装。
    let top: String
    /// 下装。
    let bottom: String
    /// 鞋履。
    let shoes: String
    /// 推荐理由。
    let why: String
}

/// 风格报告响应。
struct StyleReportResponse: Codable, Equatable {
    /// 风格标签。
    let badge: String
    /// 首页标题。
    let heroTitle: String
    /// 首页说明。
    let heroCopy: String
    /// 风格分数。
    let score: Int
    /// 风格说明。
    let description: String
    /// 脸型维度。
    let faceShape: String
    /// 发型建议。
    let hair: [String]
    /// 妆容建议。
    let makeup: [String]
    /// 身形比例维度。
    let bodyRatio: String
    /// 身形穿搭建议。
    let bodyTips: [String]
    /// 推荐颜色名。
    let colors: [String]
    /// 最佳颜色名。
    let bestColors: [String]
    /// 优势列表。
    let strengths: [String]
    /// 改进列表。
    let improvements: [String]
    /// 色板。
    let palette: [PaletteResponse]
    /// 日常推荐穿搭。
    let dailyLook: DailyLookResponse
    /// 数据来源。
    let source: String
}

/// 商品推荐响应。
struct ProductResponse: Codable, Identifiable, Equatable {
    /// 商品唯一 ID。
    var id: String { brand + name }
    /// 品牌。
    let brand: String
    /// 商品名。
    let name: String
    /// 标签。
    let tag: String
    /// 价格。
    let price: String
    /// 推荐理由。
    let reason: String
    /// 购买链接。
    let buyUrl: String
    /// 商品图片链接。
    let image: String
}

/// 衣橱单品响应。
struct ClosetItemResponse: Codable, Identifiable, Equatable {
    /// 单品 ID。
    let itemId: String
    /// 照片 ID。
    let photoId: String
    /// 单品名称。
    let name: String
    /// 单品品类。
    let category: String
    /// 单品颜色。
    let color: String
    /// 识别品牌。
    let brand: String
    /// 适用季节。
    let season: String
    /// 风格标签。
    let style: String
    /// 识别来源。
    let source: String

    /// 列表唯一 ID。
    var id: String { itemId }
}

/// 衣橱穿搭推荐请求。
struct ClosetOutfitRequest: Encodable {
    /// 用户 ID。
    let userId: String
    /// 场景。
    let occasion: String
    /// 天气描述。
    let weather: String
}

/// 衣橱穿搭推荐响应。
struct ClosetOutfitResponse: Codable, Equatable {
    /// 场景。
    let occasion: String
    /// 天气描述。
    let weather: String
    /// 风格标签。
    let style: String
    /// 上装。
    let top: String
    /// 下装。
    let bottom: String
    /// 鞋履。
    let shoes: String
    /// 外套。
    let layer: String
    /// 配饰。
    let accessory: String
    /// 推荐理由。
    let why: String
    /// 衣橱缺口建议。
    let missingItem: String
}

/// 聊天回复响应。
struct ChatMessageResponse: Decodable {
    /// AI 回复。
    let reply: String
}

/// 订阅开始响应。
struct SubscriptionStartResponse: Decodable {
    /// 订阅层级。
    let tier: String
    /// 套餐名。
    let plan: String
    /// 价格。
    let price: String
    /// 状态。
    let status: String
    /// 到期时间。
    let expiresAt: String
}

/// 订阅状态响应。
struct SubscriptionStatusResponse: Decodable {
    /// 是否拥有有效订阅。
    let active: Bool
    /// 订阅层级。
    let tier: String
    /// 套餐名。
    let plan: String
    /// 价格。
    let price: String
    /// 状态。
    let status: String
    /// 到期时间。
    let expiresAt: String
}

/// 聊天消息。
struct ChatMessage: Identifiable, Equatable {
    /// 消息 ID。
    let id: UUID
    /// 是否来自用户。
    let isUser: Bool
    /// 消息文本。
    let text: String

    /// 创建聊天消息。
    init(id: UUID = UUID(), isUser: Bool, text: String) {
        self.id = id
        self.isUser = isUser
        self.text = text
    }
}

/// 后端连接状态。
enum ApiStatus: Equatable {
    /// 尚未请求。
    case idle
    /// 已连接后端。
    case backend
    /// 使用本地兜底数据。
    case localFallback(String)

    /// 展示文案。
    var label: String {
        switch self {
        case .idle:
            return "Ready to analyze"
        case .backend:
            return "Backend API connected"
        case .localFallback:
            return "Local preview mode"
        }
    }
}

/// AI 分析阶段。
enum AnalysisStage: String, CaseIterable {
    /// 等待上传照片。
    case ready = "Ready for your style assessment."
    /// 分析脸型阶段。
    case face = "Analyzing your face shape..."
    /// 理解风格阶段。
    case style = "Understanding your style..."
    /// 创建档案阶段。
    case profile = "Creating your profile..."
    /// 生成风格板阶段。
    case board = "Curating your style board..."
    /// 分析完成阶段。
    case completed = "Your style profile is ready."

    /// 阶段图标。
    var iconName: String {
        switch self {
        case .ready:
            return "camera.viewfinder"
        case .face:
            return "face.smiling"
        case .style:
            return "sparkles"
        case .profile:
            return "person.text.rectangle"
        case .board:
            return "rectangle.on.rectangle.angled"
        case .completed:
            return "checkmark.seal.fill"
        }
    }
}
