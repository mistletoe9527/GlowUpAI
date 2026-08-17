import Combine
import Foundation

/// App 级状态和业务编排。
@MainActor
final class AppViewModel: ObservableObject {
    /// 当前路由。
    @Published var route: AppRoute = .onboarding

    /// 当前 Onboarding 步骤。
    @Published var onboardingStep: OnboardingStep = .welcome

    /// 当前底部标签。
    @Published var activeTab: AppTab = .home

    /// 用户资料。
    @Published var profile: UserProfile = .empty

    /// 生日选择值。
    @Published var birthdayDate: Date = AppViewModel.defaultBirthdayDate()

    /// 身高英尺选择值。
    @Published var heightFeet: Int = 5

    /// 身高英寸选择值。
    @Published var heightInches: Int = 6

    /// 体重磅数选择值。
    @Published var weightPounds: Int = 185

    /// 已上传照片。
    @Published var uploads: [UploadSlot: PhotoUpload] = [:]

    /// 当前风格报告。
    @Published var report: StyleReportResponse = SampleData.report

    /// 是否已经生成过风格报告。
    @Published var hasCompletedStyleReport: Bool = false

    /// 当前穿搭列表。
    @Published var outfits: [OutfitResponse] = []

    /// 当前商品推荐。
    @Published var products: [ProductResponse] = []

    /// 当前衣橱单品。
    @Published var closetItems: [ClosetItemResponse] = []

    /// 当前衣橱穿搭推荐。
    @Published var closetOutfit: ClosetOutfitResponse = SampleData.closetOutfit

    /// 是否已经生成过衣橱穿搭。
    @Published var hasGeneratedClosetOutfit: Bool = false

    /// 衣橱天气输入草稿。
    @Published var closetWeatherDraft: String = "mild weather"

    /// 聊天消息。
    @Published var chatMessages: [ChatMessage] = [
        ChatMessage(isUser: false, text: "Upload photos or ask me what to wear. I will keep the advice positive and practical.")
    ]

    /// 聊天输入草稿。
    @Published var chatDraft: String = ""

    /// 是否展示独立聊天页。
    @Published var isChatPresented: Bool = false

    /// 当前穿搭场景。
    @Published var selectedOccasion: Occasion = .daily

    /// 当前订阅套餐。
    @Published var selectedPlan: SubscriptionPlan = .monthly

    /// StoreKit 订阅商品。
    @Published var subscriptionProducts: [SubscriptionPlan: StoreProduct] = [:]

    /// 当前订阅购买状态。
    @Published var subscriptionPurchaseState: SubscriptionPurchaseState = .idle

    /// 是否拥有有效订阅。
    @Published var hasActiveSubscription: Bool = false

    /// 订阅状态提示。
    @Published var subscriptionStatusMessage: String?

    /// 是否展示订阅页。
    @Published var isPaywallPresented: Bool = false

    /// 是否正在执行主要任务。
    @Published var isBusy: Bool = false

    /// 分析进度。
    @Published var analysisProgress: Double = 0

    /// AI 分析阶段。
    @Published var analysisStage: AnalysisStage = .ready

    /// 后端连接状态。
    @Published var apiStatus: ApiStatus = .idle

    /// 页面错误提示。
    @Published var errorMessage: String?

    /// 是否展示 Email 登录页。
    @Published var isEmailSignInPresented: Bool = false

    /// Email 登录输入草稿。
    @Published var emailDraft: String = ""

    /// 后端 API 客户端。
    private let apiClient: APIClient

    /// Google OAuth 登录服务。
    private let googleOAuthService: GoogleOAuthService

    /// StoreKit 购买服务。
    private let purchaseService: StoreKitPurchaseService

    /// 后端记录的订阅是否有效。
    private var backendSubscriptionActive: Bool = false

    /// 后端订阅状态文案。
    private var backendSubscriptionStatusMessage: String?

    /// 本地会话快照存储 key。
    private static let sessionSnapshotKey = "com.glowupai.sessionSnapshot.v1"

    /// 默认美区国家码，使用 ISO 3166-1 alpha-2 便于存储和分析。
    private static let defaultRegionCode = RegionCode.unitedStates.rawValue

    /// 默认美区展示名称。
    static let defaultRegionDisplayName = RegionCode.unitedStates.displayName

    /// 跳过风格目标时使用的中性默认目标。
    private static let defaultStyleGoal: StyleGoal = .findMyStyle

    /// CDC 美国成年男性平均身高四舍五入后的英寸数。
    private static let maleDefaultHeightInches = 69

    /// CDC 美国成年男性平均体重四舍五入后的磅数。
    private static let maleDefaultWeightPounds = 199

    /// CDC 美国成年女性平均身高四舍五入后的英寸数。
    private static let femaleDefaultHeightInches = 64

    /// CDC 美国成年女性平均体重四舍五入后的磅数。
    private static let femaleDefaultWeightPounds = 172

    /// 未透露性别时使用的美国成年人中性默认身高英寸数。
    private static let neutralDefaultHeightInches = 66

    /// 未透露性别时使用的美国成年人中性默认体重磅数。
    private static let neutralDefaultWeightPounds = 185

    /// 生日字符串格式化器。
    private static let birthdayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    /// 最早可选择生日。
    ///
    /// - Returns: 最早生日日期
    private static func minimumBirthdayDate() -> Date {
        Calendar.current.date(from: DateComponents(year: 1900, month: 1, day: 1)) ?? Date.distantPast
    }

    /// 最晚可选择生日。
    ///
    /// - Returns: 最晚生日日期
    private static func maximumBirthdayDate() -> Date {
        Calendar.current.startOfDay(for: Date())
    }

    /// 默认生日选择值。
    ///
    /// - Returns: 默认生日日期
    private static func defaultBirthdayDate() -> Date {
        Calendar.current.date(byAdding: .year, value: -25, to: maximumBirthdayDate()) ?? minimumBirthdayDate()
    }

    /// 创建 App 状态模型。
    init(
        apiClient: APIClient = APIClient(),
        purchaseService: StoreKitPurchaseService? = nil,
        googleOAuthService: GoogleOAuthService? = nil
    ) {
        self.apiClient = apiClient
        self.purchaseService = purchaseService ?? StoreKitPurchaseService()
        self.googleOAuthService = googleOAuthService ?? GoogleOAuthService()
        if restoreLocalSession() {
            Task {
                await restoreSavedAppState()
            }
        }
    }

    /// 以指定方式进入资料流程。
    func start(authMethod: AuthMethod = .demo) {
        profile.authMethod = authMethod.rawValue
        onboardingStep = .gender
        errorMessage = nil
    }

    /// 开始 Email 登录流程。
    func beginEmailSignIn() {
        emailDraft = ""
        isEmailSignInPresented = true
        errorMessage = nil
    }

    /// 完成 Email 登录 MVP。
    func continueWithEmail() async {
        let email = emailDraft.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard email.isValidEmail else {
            errorMessage = "Please enter a valid email address."
            return
        }
        isBusy = true
        defer { isBusy = false }
        let userId = "email-\(email.stableProfileKey)"
        if await restoreExistingProfile(userId: userId) {
            isEmailSignInPresented = false
            return
        }
        profile.userId = "email-\(email.stableProfileKey)"
        profile.authMethod = AuthMethod.email.rawValue
        profile.email = email
        profile.name = email.emailDisplayName
        isEmailSignInPresented = false
        onboardingStep = .gender
        errorMessage = nil
    }

    /// 尝试恢复已保存的用户资料。
    ///
    /// - Parameter userId: 登录提供方对应的稳定用户 ID
    /// - Returns: 是否恢复成功
    private func restoreExistingProfile(userId: String) async -> Bool {
        do {
            profile = try await apiClient.fetchProfile(userId: userId)
            populateProfileControls(from: profile)
            persistLocalSession()
            route = .main
            onboardingStep = .styleGoal
            activeTab = .home
            apiStatus = .backend
            await restoreSavedAppState()
            return true
        } catch {
            return false
        }
    }

    /// 完成 Apple 登录。
    func continueWithApple(userId: String, fullName: PersonNameComponents?, email: String?) async {
        isBusy = true
        defer { isBusy = false }
        profile.authMethod = AuthMethod.apple.rawValue
        profile.userId = "apple-\(userId)"
        if let email, !email.isEmpty {
            profile.email = email
        }
        if let displayName = fullName?.formattedName, !displayName.isEmpty {
            profile.name = displayName
        } else if let email, !email.isEmpty {
            profile.name = email.emailDisplayName
        }
        if await restoreExistingProfile(userId: profile.userId) {
            return
        }
        onboardingStep = .gender
        errorMessage = nil
    }

    /// 完成 Google 登录。
    func continueWithGoogle() async {
        isBusy = true
        errorMessage = nil
        defer { isBusy = false }
        do {
            let googleProfile = try await googleOAuthService.signIn()
            profile.userId = googleProfile.userId
            profile.authMethod = AuthMethod.google.rawValue
            profile.email = googleProfile.email
            profile.name = googleProfile.name
            if await restoreExistingProfile(userId: googleProfile.userId) {
                return
            }
            onboardingStep = .gender
        } catch {
            profile.authMethod = AuthMethod.google.rawValue
            errorMessage = error.localizedDescription
        }
    }

    /// 返回上一个 Onboarding 步骤。
    func goBackOnboarding() {
        guard onboardingStep.rawValue > OnboardingStep.welcome.rawValue else {
            return
        }
        onboardingStep = OnboardingStep(rawValue: onboardingStep.rawValue - 1) ?? .welcome
        errorMessage = nil
    }

    /// 选择风格目标。
    func chooseGoal(_ goal: StyleGoal) {
        profile.styleGoal = goal.rawValue
        errorMessage = nil
    }

    /// 选择性别。
    func chooseGender(_ gender: GenderOption) {
        profile.gender = gender.rawValue
        applyDefaultBodyMetrics(for: gender)
        errorMessage = nil
    }

    /// 进入下一个 Onboarding 步骤。
    func nextOnboardingStep() {
        guard canContinueCurrentOnboardingStep else {
            errorMessage = onboardingValidationMessage
            return
        }
        normalizeCurrentOnboardingStep()
        advanceToNextOnboardingStep()
        errorMessage = nil
    }

    /// 跳过当前可选 Onboarding 步骤。
    func skipCurrentOnboardingStep() async {
        guard canSkipCurrentOnboardingStep else {
            return
        }
        switch onboardingStep {
        case .styleGoal:
            applyDefaultStyleGoal()
            await finishOnboarding()
        case .profileDetails:
            normalizeCurrentOnboardingStep()
            advanceToNextOnboardingStep()
        case .welcome, .gender:
            return
        }
        errorMessage = nil
    }

    /// 准备基础资料页默认值。
    func prepareProfileDetailsDefaults() {
        normalizeProfileDetails()
    }

    /// 完成 Onboarding 并保存资料。
    func finishOnboarding() async {
        guard canContinueCurrentOnboardingStep else {
            errorMessage = onboardingValidationMessage
            return
        }
        normalizeProfileDetails()
        isBusy = true
        defer { isBusy = false }
        do {
            _ = try await apiClient.saveProfile(profile)
            try await apiClient.track(
                name: "signup_complete",
                payload: [
                    "userId": profile.userId,
                    "authMethod": profile.authMethod,
                    "styleGoal": profile.styleGoal
                ]
            )
            apiStatus = .backend
        } catch {
            apiStatus = .localFallback(error.localizedDescription)
        }
        await refreshSubscriptionStatus()
        persistLocalSession()
        route = .main
        activeTab = .analyze
    }

    /// 导入并上传照片数据。
    ///
    /// - Parameters:
    ///   - data: 照片二进制数据
    ///   - slot: 照片槽位
    ///   - fileExtension: 文件扩展名
    ///   - mimeType: MIME 类型
    /// - Returns: 是否成功导入到本地状态
    @discardableResult
    func importPhotoData(_ data: Data, slot: UploadSlot, fileExtension: String, mimeType: String) async -> Bool {
        do {
            guard data.count <= 10 * 1024 * 1024 else {
                throw AppError.photoTooLarge
            }
            var upload = PhotoUpload(
                slot: slot,
                name: "\(slot.rawValue)-\(Int(Date().timeIntervalSince1970)).\(fileExtension)",
                mimeType: mimeType,
                size: data.count,
                data: data
            )
            uploads[slot] = upload
            do {
                let response = try await apiClient.uploadPhoto(userId: profile.userId, slot: slot, upload: upload)
                upload.photoId = response.photoId
                upload.status = .synced
                uploads[slot] = upload
                apiStatus = .backend
                try? await apiClient.track(
                    name: "photo_uploaded",
                    payload: [
                        "userId": profile.userId,
                        "slot": slot.rawValue,
                        "size": "\(data.count)"
                    ]
                )
            } catch {
                upload.status = .local
                uploads[slot] = upload
                apiStatus = .localFallback(error.localizedDescription)
            }
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    /// 删除所有风格评估照片。
    func deletePhotos() async {
        isBusy = true
        errorMessage = nil
        defer { isBusy = false }
        do {
            let response = try await apiClient.deleteUserPhotos(userId: profile.userId)
            uploads.removeAll()
            apiStatus = .backend
            errorMessage = "Style photos deleted: \(response.photoMetadataDeleted) records and \(response.photoObjectsDeleted) files removed."
            try? await apiClient.track(name: "photos_deleted", payload: ["userId": profile.userId])
        } catch {
            uploads.removeAll()
            apiStatus = .localFallback(error.localizedDescription)
            errorMessage = "Style photos cleared locally. Backend delete failed: \(error.localizedDescription)"
        }
    }

    /// 删除账户数据并重置本地状态。
    func deleteAccountData() async {
        isBusy = true
        errorMessage = nil
        defer { isBusy = false }
        do {
            let response = try await apiClient.deleteUserData(userId: profile.userId)
            resetLocalDataAfterAccountDeletion()
            apiStatus = .backend
            syncSubscriptionState()
            errorMessage = "Account data deleted: \(response.photoMetadataDeleted) photos and \(response.closetItemsDeleted) closet items removed."
        } catch {
            errorMessage = error.localizedDescription
            apiStatus = .localFallback(error.localizedDescription)
        }
    }

    /// 生成风格报告和关联推荐。
    func generateReport() async {
        guard hasRequiredPhotos else {
            errorMessage = "Please upload your face and full body photos first."
            return
        }
        isBusy = true
        analysisProgress = 0.18
        analysisStage = .face
        errorMessage = nil
        do {
            try await animateAnalysisProgress()
            let summaries = uploadSummaries()
            async let reportResponse = apiClient.analyze(profile: profile, uploads: summaries)
            async let outfitResponse = apiClient.generateOutfits(profile: profile, occasion: selectedOccasion)
            report = try await reportResponse
            outfits = try await outfitResponse
            if hasActiveSubscription {
                products = try await apiClient.recommendProducts(userId: profile.userId, occasion: selectedOccasion)
            } else {
                products = SampleData.products
            }
            hasCompletedStyleReport = true
            apiStatus = .backend
            persistLocalSession()
            try? await apiClient.track(
                name: "style_report_generated",
                payload: [
                    "userId": profile.userId,
                    "source": "backend_api"
                ]
            )
        } catch {
            report = SampleData.report
            outfits = SampleData.outfits
            products = SampleData.products
            hasCompletedStyleReport = true
            apiStatus = .localFallback(error.localizedDescription)
            persistLocalSession()
            try? await apiClient.track(
                name: "style_report_generated",
                payload: [
                    "userId": profile.userId,
                    "source": "local_fallback"
                ]
            )
        }
        analysisProgress = 1
        analysisStage = .completed
        isBusy = false
    }

    /// 按场景重新生成穿搭。
    func refreshOutfits() async {
        do {
            outfits = try await apiClient.generateOutfits(profile: profile, occasion: selectedOccasion)
            if hasActiveSubscription {
                products = try await apiClient.recommendProducts(userId: profile.userId, occasion: selectedOccasion)
            } else {
                products = SampleData.products
            }
            apiStatus = .backend
            persistLocalSession()
        } catch {
            outfits = SampleData.outfits
            products = SampleData.products
            apiStatus = .localFallback(error.localizedDescription)
            persistLocalSession()
        }
    }

    /// 上传衣橱单品并识别结构化属性。
    func uploadClosetItemData(_ data: Data, fileExtension: String, mimeType: String) async {
        guard requireSubscription(for: .aiCloset) else {
            return
        }
        do {
            guard data.count <= 10 * 1024 * 1024 else {
                throw AppError.photoTooLarge
            }
            isBusy = true
            defer { isBusy = false }
            let fileName = "closet-\(Int(Date().timeIntervalSince1970)).\(fileExtension)"
            let response = try await apiClient.uploadClosetItem(
                userId: profile.userId,
                fileName: fileName,
                mimeType: mimeType,
                data: data
            )
            closetItems.removeAll { $0.itemId == response.itemId }
            closetItems.insert(response, at: 0)
            apiStatus = .backend
            persistLocalSession()
            try? await apiClient.track(
                name: "closet_item_uploaded",
                payload: [
                    "userId": profile.userId,
                    "category": response.category,
                    "style": response.style
                ]
            )
        } catch {
            apiStatus = .localFallback(error.localizedDescription)
            errorMessage = error.localizedDescription
        }
    }

    /// 刷新衣橱单品列表。
    func loadClosetItems() async {
        guard hasActiveSubscription else {
            return
        }
        do {
            let response = try await apiClient.listClosetItems(userId: profile.userId)
            closetItems = response
            apiStatus = .backend
            persistLocalSession()
        } catch {
            apiStatus = .localFallback(error.localizedDescription)
        }
    }

    /// 基于衣橱生成今日穿搭。
    func generateClosetOutfit() async {
        guard requireSubscription(for: .aiCloset) else {
            return
        }
        let weather = closetWeatherDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        isBusy = true
        defer { isBusy = false }
        do {
            closetOutfit = try await apiClient.generateClosetOutfit(
                userId: profile.userId,
                occasion: selectedOccasion,
                weather: weather.isEmpty ? "mild weather" : weather
            )
            hasGeneratedClosetOutfit = true
            apiStatus = .backend
            persistLocalSession()
            try? await apiClient.track(
                name: "closet_outfit_generated",
                payload: [
                    "userId": profile.userId,
                    "occasion": closetOutfit.occasion,
                    "style": closetOutfit.style
                ]
            )
        } catch {
            closetOutfit = SampleData.closetOutfit
            hasGeneratedClosetOutfit = true
            apiStatus = .localFallback(error.localizedDescription)
            persistLocalSession()
        }
    }

    /// 发送 AI Stylist 聊天消息。
    func sendChatMessage() async {
        guard requireSubscription(for: .aiStylistChat) else {
            return
        }
        let message = chatDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !message.isEmpty else {
            return
        }
        chatDraft = ""
        chatMessages.append(ChatMessage(isUser: true, text: message))
        do {
            let response = try await apiClient.chat(profile: profile, message: message, uploads: uploadSummaries())
            chatMessages.append(ChatMessage(isUser: false, text: response.reply))
            apiStatus = .backend
            try? await apiClient.track(name: "ai_chat_message_sent", payload: ["userId": profile.userId])
        } catch {
            let reply = "I would keep the outfit balanced: choose one structured piece, one soft layer, and a clean shoe. That keeps the look confident without feeling overdone."
            chatMessages.append(ChatMessage(isUser: false, text: reply))
            apiStatus = .localFallback(error.localizedDescription)
        }
    }

    /// 上传穿搭照片并向 AI Stylist 发送照片问题。
    ///
    /// - Parameters:
    ///   - data: 照片二进制数据
    ///   - fileExtension: 文件扩展名
    ///   - mimeType: MIME 类型
    func sendChatOutfitPhotoData(_ data: Data, fileExtension: String, mimeType: String) async {
        guard requireSubscription(for: .aiStylistChat) else {
            return
        }
        isBusy = true
        defer { isBusy = false }
        errorMessage = nil
        let imported = await importPhotoData(data, slot: .outfit, fileExtension: fileExtension, mimeType: mimeType)
        guard imported, uploads[.outfit] != nil else {
            return
        }
        let message = "Can I wear this?"
        chatMessages.append(ChatMessage(isUser: true, text: "\(message) [Outfit photo]"))
        do {
            let response = try await apiClient.chat(profile: profile, message: message, uploads: uploadSummaries(for: [.outfit]))
            chatMessages.append(ChatMessage(isUser: false, text: response.reply))
            apiStatus = .backend
            try? await apiClient.track(
                name: "ai_chat_photo_sent",
                payload: [
                    "userId": profile.userId,
                    "slot": UploadSlot.outfit.rawValue
                ]
            )
        } catch {
            let reply = "I can still help from the outfit photo prompt: keep the strongest piece as the focal point, simplify one accessory, and check the shoe line before you go."
            chatMessages.append(ChatMessage(isUser: false, text: reply))
            apiStatus = .localFallback(error.localizedDescription)
        }
    }

    /// 开始订阅流程。
    func startSubscription() async {
        isBusy = true
        subscriptionPurchaseState = .purchasing
        defer {
            isBusy = false
            syncSubscriptionState()
        }
        do {
            if subscriptionProducts.isEmpty {
                await loadSubscriptionProducts()
            }
            let purchased = try await purchaseService.purchase(plan: selectedPlan)
            guard purchased else {
                subscriptionPurchaseState = .ready
                subscriptionStatusMessage = "Purchase cancelled."
                return
            }
            subscriptionPurchaseState = .subscribed
            hasActiveSubscription = true
            subscriptionStatusMessage = "Subscription active."
            do {
                let response = try await apiClient.startSubscription(userId: profile.userId, plan: selectedPlan)
                backendSubscriptionActive = isActiveSubscriptionStatus(response.status)
                backendSubscriptionStatusMessage = "\(response.tier) \(response.plan) is active until \(response.expiresAt)."
                syncSubscriptionState()
                try? await apiClient.track(
                    name: "subscription_started",
                    payload: [
                        "userId": profile.userId,
                        "plan": selectedPlan.rawValue
                    ]
                )
                apiStatus = .backend
                await restorePremiumContentIfNeeded()
            } catch {
                apiStatus = .localFallback(error.localizedDescription)
                subscriptionStatusMessage = "Subscription active. Backend sync failed: \(error.localizedDescription)"
                await restorePremiumContentIfNeeded()
            }
            isPaywallPresented = false
        } catch {
            subscriptionPurchaseState = .unavailable(error.localizedDescription)
            subscriptionStatusMessage = error.localizedDescription
        }
    }

    /// 恢复主界面依赖后端的持久化状态。
    private func restoreSavedAppState() async {
        await refreshSubscriptionStatus()
        await restoreUploadedPhotos()
        await restoreLatestStyleReport()
        await restorePremiumContentIfNeeded()
    }

    /// 从后端恢复已上传照片。
    private func restoreUploadedPhotos() async {
        do {
            let responses = try await apiClient.listPhotos(userId: profile.userId)
            var restoredUploads: [UploadSlot: PhotoUpload] = [:]
            for response in responses {
                guard let slot = UploadSlot(rawValue: response.slot),
                      restoredUploads[slot] == nil else {
                    continue
                }
                let data: Data?
                let status: UploadStatus
                do {
                    data = try await apiClient.downloadPhotoContent(userId: profile.userId, photoId: response.photoId)
                    status = .synced
                } catch {
                    data = nil
                    status = .previewUnavailable
                }
                restoredUploads[slot] = PhotoUpload(
                    slot: slot,
                    name: response.name,
                    mimeType: response.type,
                    size: response.size,
                    data: data,
                    photoId: response.photoId,
                    status: status
                )
            }
            uploads = restoredUploads
            apiStatus = .backend
        } catch {
            apiStatus = .localFallback(error.localizedDescription)
        }
    }

    /// 在 Plus 有效时恢复付费内容数据。
    private func restorePremiumContentIfNeeded() async {
        guard hasActiveSubscription else {
            closetItems = []
            return
        }
        await loadClosetItems()
        if hasCompletedStyleReport {
            do {
                products = try await apiClient.recommendProducts(userId: profile.userId, occasion: selectedOccasion)
                persistLocalSession()
            } catch {
                if products.isEmpty {
                    products = SampleData.products
                }
            }
        }
    }

    /// 从后端刷新订阅状态。
    func refreshSubscriptionStatus() async {
        do {
            let response = try await apiClient.subscriptionStatus(userId: profile.userId)
            backendSubscriptionActive = response.active
            if response.active {
                backendSubscriptionStatusMessage = "\(response.tier) \(response.plan) is active until \(response.expiresAt)."
            } else if response.status == "expired" {
                backendSubscriptionStatusMessage = "\(response.plan) subscription expired. Free plan active."
            } else {
                backendSubscriptionStatusMessage = "Free plan active."
            }
            if let plan = SubscriptionPlan(rawValue: response.plan) {
                selectedPlan = plan
            }
            syncSubscriptionState()
            if hasActiveSubscription {
                subscriptionPurchaseState = .subscribed
            } else if subscriptionPurchaseState == .subscribed {
                subscriptionPurchaseState = subscriptionProducts.isEmpty ? .idle : .ready
            }
            apiStatus = .backend
        } catch {
            syncSubscriptionState()
            apiStatus = .localFallback(error.localizedDescription)
        }
    }

    /// 从后端恢复用户最近一次风格报告。
    func restoreLatestStyleReport() async {
        guard route == .main else {
            return
        }
        let hadCachedReport = hasCompletedStyleReport
        do {
            report = try await apiClient.latestStyleReport(userId: profile.userId)
            hasCompletedStyleReport = true
            outfits = (try? await apiClient.generateOutfits(profile: profile, occasion: selectedOccasion)) ?? []
            if hasActiveSubscription {
                products = (try? await apiClient.recommendProducts(userId: profile.userId, occasion: selectedOccasion)) ?? []
            }
            apiStatus = .backend
            persistLocalSession()
        } catch {
            if !hadCachedReport {
                hasCompletedStyleReport = false
            }
            apiStatus = .localFallback(error.localizedDescription)
        }
    }

    /// 加载 App Store 订阅商品。
    func loadSubscriptionProducts() async {
        subscriptionPurchaseState = .loading
        await purchaseService.loadProducts()
        syncSubscriptionState()
        if hasActiveSubscription {
            subscriptionPurchaseState = .subscribed
        } else if subscriptionProducts.isEmpty {
            subscriptionPurchaseState = .unavailable(purchaseService.statusMessage ?? "StoreKit products are not configured yet.")
        } else {
            subscriptionPurchaseState = .ready
        }
    }

    /// 恢复 App Store 购买。
    func restoreSubscriptionPurchases() async {
        isBusy = true
        defer { isBusy = false }
        await purchaseService.restorePurchases()
        syncSubscriptionState()
        await restorePremiumContentIfNeeded()
        subscriptionPurchaseState = hasActiveSubscription ? .subscribed : .ready
    }

    /// 获取套餐展示价格。
    ///
    /// - Parameter plan: 订阅套餐
    /// - Returns: 展示价格
    func displayPrice(for plan: SubscriptionPlan) -> String {
        subscriptionProducts[plan]?.displayPrice ?? plan.price
    }

    /// 打开 AI Stylist 聊天页。
    func openChat() {
        guard requireSubscription(for: .aiStylistChat) else {
            return
        }
        isChatPresented = true
    }

    /// 检查 Plus 权益。
    ///
    /// - Parameter feature: 需要访问的功能
    /// - Returns: 是否允许继续访问
    @discardableResult
    func requireSubscription(for feature: PremiumFeature) -> Bool {
        guard hasActiveSubscription else {
            subscriptionStatusMessage = feature.paywallReason
            isPaywallPresented = true
            return false
        }
        return true
    }

    /// 生成当前报告分享文案。
    ///
    /// - Returns: 可分享的报告摘要
    func shareReportText() -> String {
        """
        My GlowUp AI style profile is \(report.badge) with a style score of \(report.score)/100.
        Today's outfit: \(report.dailyLook.top), \(report.dailyLook.bottom), and \(report.dailyLook.shoes).
        Best colors: \(report.bestColors.joined(separator: ", ")).
        Hair ideas: \(report.hair.joined(separator: ", ")).
        Makeup ideas: \(report.makeup.joined(separator: ", ")).
        """
    }

    /// 上报分享点击事件。
    ///
    /// - Parameter surface: 分享入口位置
    func trackShareClicked(surface: String) async {
        try? await apiClient.track(
            name: "share_clicked",
            payload: [
                "userId": profile.userId,
                "surface": surface,
                "styleType": report.badge
            ]
        )
    }

    /// 上报商品点击事件。
    ///
    /// - Parameter product: 被点击的商品
    func trackProductClick(product: ProductResponse) async {
        try? await apiClient.track(
            name: "affiliate_product_clicked",
            payload: [
                "userId": profile.userId,
                "brand": product.brand,
                "product": product.name,
                "occasion": selectedOccasion.rawValue
            ]
        )
    }

    /// 同步 StoreKit 订阅状态。
    private func syncSubscriptionState() {
        subscriptionProducts = purchaseService.productsByPlan
        hasActiveSubscription = purchaseService.isSubscribed || backendSubscriptionActive
        if purchaseService.isSubscribed {
            subscriptionStatusMessage = purchaseService.statusMessage ?? "Subscription active."
        } else if backendSubscriptionActive {
            subscriptionStatusMessage = backendSubscriptionStatusMessage ?? "Subscription active."
        } else {
            subscriptionStatusMessage = purchaseService.statusMessage ?? backendSubscriptionStatusMessage
        }
    }

    /// 从本地恢复已完成 Onboarding 的会话。
    ///
    /// - Returns: 是否恢复成功
    private func restoreLocalSession() -> Bool {
        guard let data = UserDefaults.standard.data(forKey: Self.sessionSnapshotKey),
              let snapshot = try? JSONDecoder().decode(AppSessionSnapshot.self, from: data),
              snapshot.version >= 2,
              snapshot.version <= AppSessionSnapshot.currentVersion,
              isCompleteProfile(snapshot.profile) else {
            return false
        }
        profile = snapshot.profile
        selectedOccasion = snapshot.selectedOccasion
        selectedPlan = snapshot.selectedPlan
        if let report = snapshot.report {
            self.report = report
        }
        hasCompletedStyleReport = snapshot.hasCompletedStyleReport ?? (snapshot.report != nil)
        if let outfits = snapshot.outfits {
            self.outfits = outfits
        }
        if let products = snapshot.products {
            self.products = products
        }
        if let closetItems = snapshot.closetItems {
            self.closetItems = closetItems
        }
        if let closetOutfit = snapshot.closetOutfit {
            self.closetOutfit = closetOutfit
        }
        hasGeneratedClosetOutfit = snapshot.hasGeneratedClosetOutfit ?? (snapshot.closetOutfit != nil)
        populateProfileControls(from: snapshot.profile)
        route = .main
        onboardingStep = .styleGoal
        activeTab = .home
        return true
    }

    /// 持久化已完成 Onboarding 的本地会话。
    private func persistLocalSession() {
        guard isCompleteProfile(profile) else {
            return
        }
        let snapshot = AppSessionSnapshot(
            version: AppSessionSnapshot.currentVersion,
            profile: profile,
            selectedOccasion: selectedOccasion,
            selectedPlan: selectedPlan,
            hasCompletedStyleReport: hasCompletedStyleReport,
            report: hasCompletedStyleReport ? report : nil,
            outfits: hasCompletedStyleReport ? outfits : nil,
            products: hasActiveSubscription ? products : nil,
            closetItems: hasActiveSubscription ? closetItems : nil,
            closetOutfit: hasGeneratedClosetOutfit ? closetOutfit : nil,
            hasGeneratedClosetOutfit: hasGeneratedClosetOutfit
        )
        guard let data = try? JSONEncoder().encode(snapshot) else {
            return
        }
        UserDefaults.standard.set(data, forKey: Self.sessionSnapshotKey)
    }

    /// 清除本地会话快照。
    private func clearLocalSession() {
        UserDefaults.standard.removeObject(forKey: Self.sessionSnapshotKey)
    }

    /// 判断资料是否足以恢复主界面会话。
    ///
    /// - Parameter profile: 用户资料
    /// - Returns: 是否完整
    private func isCompleteProfile(_ profile: UserProfile) -> Bool {
        profile.userId.hasVisibleContent
            && profile.name.hasVisibleContent
            && profile.authMethod.hasVisibleContent
            && profile.gender.hasVisibleContent
            && profile.birthday.hasVisibleContent
            && profile.location.hasVisibleContent
            && profile.styleGoal.hasVisibleContent
    }

    /// 按已保存资料回填选择控件。
    ///
    /// - Parameter profile: 已保存用户资料
    private func populateProfileControls(from profile: UserProfile) {
        populateBirthdayDate(from: profile.birthday)
        populateHeightSelection(from: profile.height)
        populateWeightSelection(from: profile.weight)
    }

    /// 按已保存生日回填生日选择值。
    ///
    /// - Parameter birthday: yyyy-MM-dd 格式生日
    private func populateBirthdayDate(from birthday: String) {
        guard let date = Self.birthdayFormatter.date(from: birthday) else {
            birthdayDate = Self.defaultBirthdayDate()
            return
        }
        birthdayDate = min(max(date, Self.minimumBirthdayDate()), Self.maximumBirthdayDate())
    }

    /// 账户删除后重置本地演示数据。
    private func resetLocalDataAfterAccountDeletion() {
        clearLocalSession()
        profile = .empty
        birthdayDate = Self.defaultBirthdayDate()
        applyDefaultBodyMetrics(for: .preferNotToSay)
        uploads.removeAll()
        report = SampleData.report
        hasCompletedStyleReport = false
        outfits = []
        products = []
        closetItems = []
        closetOutfit = SampleData.closetOutfit
        hasGeneratedClosetOutfit = false
        closetWeatherDraft = "mild weather"
        chatMessages = [
            ChatMessage(isUser: false, text: "Upload photos or ask me what to wear. I will keep the advice positive and practical.")
        ]
        chatDraft = ""
        selectedOccasion = .daily
        selectedPlan = .monthly
        backendSubscriptionActive = false
        backendSubscriptionStatusMessage = nil
        route = .onboarding
        onboardingStep = .welcome
        activeTab = .home
        isChatPresented = false
        isPaywallPresented = false
    }

    /// 判断订阅状态是否有效。
    ///
    /// - Parameter status: 后端订阅状态
    /// - Returns: 是否表示有效订阅
    private func isActiveSubscriptionStatus(_ status: String) -> Bool {
        let normalizedStatus = status.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return normalizedStatus == "active"
            || normalizedStatus.hasSuffix("_active")
            || normalizedStatus == "subscribed"
    }

    /// 选择穿搭场景并刷新推荐。
    func chooseOccasion(_ occasion: Occasion) async {
        selectOccasion(occasion)
        await refreshOutfits()
    }

    /// 选择穿搭场景并持久化到本地会话。
    ///
    /// - Parameter occasion: 用户选择的穿搭场景
    func selectOccasion(_ occasion: Occasion) {
        selectedOccasion = occasion
        persistLocalSession()
    }

    /// 选择订阅套餐并持久化到本地会话。
    ///
    /// - Parameter plan: 用户选择的订阅套餐
    func chooseSubscriptionPlan(_ plan: SubscriptionPlan) {
        selectedPlan = plan
        persistLocalSession()
    }

    /// 是否已经具备必填照片。
    var hasRequiredPhotos: Bool {
        UploadSlot.allCases.filter(\.isRequired).allSatisfy { uploads[$0] != nil }
    }

    /// 当前资料地区展示名称。
    var profileRegionDisplayName: String {
        let normalizedLocation = profile.location.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalizedLocation.hasVisibleContent else {
            return Self.defaultRegionDisplayName
        }
        if normalizedLocation.caseInsensitiveCompare(Self.defaultRegionCode) == .orderedSame
            || normalizedLocation.caseInsensitiveCompare(Self.defaultRegionDisplayName) == .orderedSame
            || normalizedLocation.caseInsensitiveCompare("USA") == .orderedSame {
            return Self.defaultRegionDisplayName
        }
        return normalizedLocation
    }

    /// 当前 Onboarding 步骤是否可以继续。
    var canContinueCurrentOnboardingStep: Bool {
        switch onboardingStep {
        case .welcome:
            return true
        case .gender:
            return profile.gender.hasVisibleContent
        case .profileDetails:
            return isBirthdayDateInRange
        case .styleGoal:
            return profile.styleGoal.hasVisibleContent
        }
    }

    /// 当前 Onboarding 步骤是否允许跳过。
    var canSkipCurrentOnboardingStep: Bool {
        switch onboardingStep {
        case .profileDetails, .styleGoal:
            return true
        case .welcome, .gender:
            return false
        }
    }

    /// Onboarding 当前步骤的校验提示。
    private var onboardingValidationMessage: String {
        switch onboardingStep {
        case .welcome:
            return ""
        case .gender:
            return "Please choose your gender preference."
        case .profileDetails:
            if !isBirthdayDateInRange {
                return "Please choose a valid birthday."
            }
            return ""
        case .styleGoal:
            return "Please choose your style goal."
        }
    }

    /// 生日选择范围。
    var birthdayDateRange: ClosedRange<Date> {
        Self.minimumBirthdayDate()...Self.maximumBirthdayDate()
    }

    /// 身高英尺选择范围。
    var heightFeetRange: ClosedRange<Int> {
        3...8
    }

    /// 身高英寸选择范围。
    var heightInchesRange: ClosedRange<Int> {
        0...11
    }

    /// 体重磅数选择范围。
    var weightPoundsRange: ClosedRange<Int> {
        50...400
    }

    /// 生日日期是否在允许范围内。
    private var isBirthdayDateInRange: Bool {
        birthdayDateRange.contains(birthdayDate)
    }

    /// 进入下一个 Onboarding 步骤。
    private func advanceToNextOnboardingStep() {
        guard let nextStep = OnboardingStep(rawValue: onboardingStep.rawValue + 1) else {
            return
        }
        onboardingStep = nextStep
    }

    /// 归一化当前 Onboarding 步骤对应的资料字段。
    private func normalizeCurrentOnboardingStep() {
        switch onboardingStep {
        case .profileDetails:
            normalizeProfileDetails()
        case .welcome, .gender, .styleGoal:
            return
        }
    }

    /// 归一化基础资料页字段，跳过时也能得到完整默认资料。
    private func normalizeProfileDetails() {
        updateBirthday()
        updateHeight()
        updateWeight()
        profile.location = Self.defaultRegionCode
    }

    /// 应用跳过风格目标时的默认目标。
    private func applyDefaultStyleGoal() {
        profile.styleGoal = Self.defaultStyleGoal.rawValue
    }

    /// 生成当前上传照片摘要。
    ///
    /// - Parameter slots: 需要输出的照片槽位
    /// - Returns: 上传照片摘要列表
    private func uploadSummaries(for slots: [UploadSlot] = UploadSlot.allCases) -> [UploadSummaryRequest] {
        slots.compactMap { slot in
            uploads[slot]
        }
        .map { upload in
            UploadSummaryRequest(
                photoId: upload.photoId,
                slot: upload.slot.rawValue,
                name: upload.name,
                type: upload.mimeType,
                size: upload.size
            )
        }
    }

    /// 更新生日字段。
    private func updateBirthday() {
        let normalizedBirthdayDate = min(max(birthdayDate, Self.minimumBirthdayDate()), Self.maximumBirthdayDate())
        birthdayDate = normalizedBirthdayDate
        profile.birthday = Self.birthdayFormatter.string(from: normalizedBirthdayDate)
    }

    /// 更新身高字段。
    func updateHeight() {
        heightFeet = min(max(heightFeet, heightFeetRange.lowerBound), heightFeetRange.upperBound)
        heightInches = min(max(heightInches, heightInchesRange.lowerBound), heightInchesRange.upperBound)
        profile.height = "\(heightFeet)'\(heightInches)\""
    }

    /// 更新体重字段。
    func updateWeight() {
        weightPounds = min(max(weightPounds, weightPoundsRange.lowerBound), weightPoundsRange.upperBound)
        profile.weight = "\(weightPounds) lb"
    }

    /// 按性别应用美国地区成年人的默认身高体重。
    ///
    /// - Parameter gender: 性别选项
    private func applyDefaultBodyMetrics(for gender: GenderOption) {
        let defaults = defaultBodyMetrics(for: gender)
        heightFeet = defaults.heightInches / 12
        heightInches = defaults.heightInches % 12
        weightPounds = defaults.weightPounds
        updateHeight()
        updateWeight()
    }

    /// 获取性别对应的默认身高体重。
    ///
    /// - Parameter gender: 性别选项
    /// - Returns: 默认身高英寸数和体重磅数
    private func defaultBodyMetrics(for gender: GenderOption) -> (heightInches: Int, weightPounds: Int) {
        switch gender {
        case .male:
            return (Self.maleDefaultHeightInches, Self.maleDefaultWeightPounds)
        case .female:
            return (Self.femaleDefaultHeightInches, Self.femaleDefaultWeightPounds)
        case .nonBinary, .preferNotToSay:
            return (Self.neutralDefaultHeightInches, Self.neutralDefaultWeightPounds)
        }
    }

    /// 获取当前资料性别对应的默认身高体重。
    ///
    /// - Returns: 默认身高英寸数和体重磅数
    private func defaultBodyMetricsForCurrentGender() -> (heightInches: Int, weightPounds: Int) {
        defaultBodyMetrics(for: GenderOption(rawValue: profile.gender) ?? .preferNotToSay)
    }

    /// 按已保存身高回填身高选择值。
    ///
    /// - Parameter height: 身高字符串
    private func populateHeightSelection(from height: String) {
        let numbers = height.integerTokens
        guard height.hasVisibleContent, let feet = numbers.first else {
            let defaults = defaultBodyMetricsForCurrentGender()
            heightFeet = defaults.heightInches / 12
            heightInches = defaults.heightInches % 12
            return
        }
        heightFeet = min(max(feet, heightFeetRange.lowerBound), heightFeetRange.upperBound)
        heightInches = min(max(numbers.dropFirst().first ?? 0, heightInchesRange.lowerBound), heightInchesRange.upperBound)
    }

    /// 按已保存体重回填体重选择值。
    ///
    /// - Parameter weight: 体重字符串
    private func populateWeightSelection(from weight: String) {
        guard weight.hasVisibleContent, let pounds = weight.integerTokens.first else {
            weightPounds = defaultBodyMetricsForCurrentGender().weightPounds
            return
        }
        weightPounds = min(max(pounds, weightPoundsRange.lowerBound), weightPoundsRange.upperBound)
    }

    /// 模拟 AI 分析进度。
    private func animateAnalysisProgress() async throws {
        let stages: [(Double, AnalysisStage)] = [
            (0.18, .face),
            (0.42, .style),
            (0.66, .profile),
            (0.86, .board)
        ]
        for stage in stages {
            analysisProgress = stage.0
            analysisStage = stage.1
            try await Task.sleep(nanoseconds: 240_000_000)
        }
    }

}

/// 本地兜底数据。
enum SampleData {
    /// 默认报告。
    static let report = StyleReportResponse(
        badge: "Modern Minimalist",
        heroTitle: "Effortless Friday",
        heroCopy: "Tailored comfort meets modern polish.",
        score: 87,
        description: "Clean lines, quiet structure, and sharp proportions make the look feel polished without trying too hard.",
        faceShape: "Oval",
        hair: ["Shoulder-length layers", "Soft bangs"],
        makeup: ["Soft matte base", "Champagne highlight"],
        bodyRatio: "Lengthened vertical line",
        bodyTips: ["High-waist pants", "Short jackets"],
        colors: ["Black", "White", "Beige"],
        bestColors: ["Black", "White", "Beige"],
        strengths: ["Good color matching", "Clean silhouette"],
        improvements: ["Add accessories", "Try more layering"],
        palette: [
            PaletteResponse(name: "Black", color: "#1C1C1C"),
            PaletteResponse(name: "Ivory", color: "#F6F3F2"),
            PaletteResponse(name: "Rose", color: "#835244")
        ],
        dailyLook: DailyLookResponse(
            occasion: "Daily",
            top: "White shirt",
            bottom: "Straight jeans",
            shoes: "Loafers",
            why: "Creates a sharp, easy base for everyday dressing."
        ),
        source: "local_preview"
    )

    /// 默认穿搭列表。
    static let outfits: [OutfitResponse] = [
        OutfitResponse(order: 1, occasion: "Daily", style: "Modern Minimalist", top: "White blouse", bottom: "Black trousers", shoes: "Loafers", why: "Creates a refined daily base with clean proportions."),
        OutfitResponse(order: 2, occasion: "Work", style: "Soft Tailoring", top: "Fine knit top", bottom: "High-waist trouser", shoes: "Low block heel", why: "Keeps your work look composed without feeling stiff."),
        OutfitResponse(order: 3, occasion: "Date", style: "Warm Polished", top: "Draped satin top", bottom: "Straight denim", shoes: "Pointed slingback", why: "Adds a romantic texture while staying effortless.")
    ]

    /// 默认商品列表。
    static let products: [ProductResponse] = [
        ProductResponse(brand: "Studio Nicholson", name: "Linen Tailored Blazer", tag: "AI Pick", price: "$475", reason: "Adds structure without breaking your soft neutral palette.", buyUrl: "https://www.amazon.com/s?k=linen+tailored+blazer", image: "https://images.unsplash.com/photo-1496747611176-843222e1e57c"),
        ProductResponse(brand: "Madewell", name: "Clean Straight Denim", tag: "Wardrobe", price: "$128", reason: "Easy base for minimal daily outfits.", buyUrl: "https://www.amazon.com/s?k=straight+denim+women", image: "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f"),
        ProductResponse(brand: "Cole Haan", name: "Low Block Heel", tag: "Polished", price: "$140", reason: "A practical finishing piece for work or date looks.", buyUrl: "https://www.amazon.com/s?k=low+block+heel+women", image: "https://images.unsplash.com/photo-1543163521-1bf539c55dd2")
    ]

    /// 默认衣橱单品。
    static let closetItems: [ClosetItemResponse] = [
        ClosetItemResponse(itemId: "sample-closet-1", photoId: "sample-photo-1", name: "Ivory Top", category: "Top", color: "Ivory", brand: "Unknown", season: "Spring", style: "Minimal", source: "local_preview"),
        ClosetItemResponse(itemId: "sample-closet-2", photoId: "sample-photo-2", name: "Black Bottom", category: "Bottom", color: "Black", brand: "Unknown", season: "All season", style: "Professional", source: "local_preview"),
        ClosetItemResponse(itemId: "sample-closet-3", photoId: "sample-photo-3", name: "Rose Dress", category: "Dress", color: "Rose", brand: "Unknown", season: "Summer", style: "Romantic", source: "local_preview")
    ]

    /// 默认衣橱穿搭推荐。
    static let closetOutfit = ClosetOutfitResponse(
        occasion: "Daily",
        weather: "mild weather",
        style: "Minimal",
        top: "Ivory Top",
        bottom: "Black Bottom",
        shoes: "Add a polished shoe",
        layer: "Optional",
        accessory: "Optional simple accessory",
        why: "Built from your saved closet pieces for a practical daily outfit.",
        missingItem: "Add one polished shoe to finish daily looks."
    )
}

/// App 业务错误。
enum AppError: LocalizedError {
    /// 照片读取失败。
    case photoReadFailed
    /// 照片超过 10MB。
    case photoTooLarge

    /// 错误说明。
    var errorDescription: String? {
        switch self {
        case .photoReadFailed:
            return "Photo could not be read."
        case .photoTooLarge:
            return "Each photo must be 10MB or smaller."
        }
    }
}

/// 字符串资料工具。
private extension String {
    /// 是否包含去除空白后的可见字符。
    var hasVisibleContent: Bool {
        !trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// 字符串中的整数片段。
    var integerTokens: [Int] {
        var values: [Int] = []
        var current = ""
        for character in self {
            if character.isNumber {
                current.append(character)
            } else if !current.isEmpty {
                if let value = Int(current) {
                    values.append(value)
                }
                current = ""
            }
        }
        if !current.isEmpty, let value = Int(current) {
            values.append(value)
        }
        return values
    }

    /// 是否是基础合法 Email。
    var isValidEmail: Bool {
        let parts = split(separator: "@")
        guard parts.count == 2,
              let local = parts.first,
              let domain = parts.last,
              !local.isEmpty,
              domain.contains(".") else {
            return false
        }
        return !contains(" ")
    }

    /// 从 Email 生成展示名。
    var emailDisplayName: String {
        let localPart = split(separator: "@").first.map(String.init) ?? self
        let cleaned = localPart
            .replacingOccurrences(of: ".", with: " ")
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
        return cleaned
            .split(separator: " ")
            .map { word in
                word.prefix(1).uppercased() + word.dropFirst()
            }
            .joined(separator: " ")
    }

    /// 稳定的非加密 profile key，用于 MVP 本地账号 ID。
    var stableProfileKey: String {
        let bytes = Array(utf8)
        var hash: UInt64 = 14_695_981_039_346_656_037
        for byte in bytes {
            hash ^= UInt64(byte)
            hash &*= 1_099_511_628_211
        }
        return String(hash, radix: 16)
    }
}

private extension PersonNameComponents {
    /// Apple 返回姓名的展示文案。
    var formattedName: String {
        PersonNameComponentsFormatter().string(from: self)
    }
}
