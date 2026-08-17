import SwiftUI

/// 主应用 Tab 容器。
struct MainTabView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 主应用内容。
    var body: some View {
        TabView(selection: $viewModel.activeTab) {
            HomeView()
                .tabItem { Label(AppTab.home.rawValue, systemImage: AppTab.home.iconName) }
                .tag(AppTab.home)
            AnalyzeView()
                .tabItem { Label(AppTab.analyze.rawValue, systemImage: AppTab.analyze.iconName) }
                .tag(AppTab.analyze)
            StyleView()
                .tabItem { Label(AppTab.style.rawValue, systemImage: AppTab.style.iconName) }
                .tag(AppTab.style)
            ClosetView()
                .tabItem { Label(AppTab.closet.rawValue, systemImage: AppTab.closet.iconName) }
                .tag(AppTab.closet)
            ProfileView()
                .tabItem { Label(AppTab.profile.rawValue, systemImage: AppTab.profile.iconName) }
                .tag(AppTab.profile)
        }
        .tint(GlowTheme.roseGold)
        .sheet(isPresented: $viewModel.isPaywallPresented) {
            SubscriptionView()
        }
        .task {
            await viewModel.refreshSubscriptionStatus()
        }
    }
}

/// 首页。
struct HomeView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 首页内容。
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    if viewModel.isBusy && viewModel.analysisProgress > 0 && !viewModel.hasCompletedStyleReport {
                        HomeAnalysisProgressCard()
                    } else if viewModel.hasCompletedStyleReport {
                        HeroOutfitCard(report: viewModel.report)
                        HStack(spacing: 14) {
                            DashboardMetric(title: "Style confidence", value: "\(viewModel.report.score)%", iconName: "chart.line.uptrend.xyaxis")
                            DashboardMetric(title: "Profile", value: viewModel.report.badge, iconName: "sparkles")
                        }
                        ImproveStyleCard(report: viewModel.report)
                    } else {
                        NewUserStyleCard()
                        HStack(spacing: 14) {
                            DashboardMetric(title: "Style confidence", value: "Start", iconName: "chart.line.uptrend.xyaxis")
                            DashboardMetric(title: "Profile", value: "Pending", iconName: "sparkles")
                        }
                    }
                    AIStylistHomeCard()
                    QuickActionGrid()
                    ProductCarousel(products: viewModel.products)
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(GlowTheme.surface)
            .safeAreaInset(edge: .top) {
                AppTopBar(title: "Good morning,", subtitle: viewModel.profile.name)
            }
            .sheet(isPresented: $viewModel.isChatPresented) {
                AIChatView()
            }
        }
    }
}

/// 新用户首页引导卡片。
struct NewUserStyleCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 引导卡片内容。
    var body: some View {
        VStack(spacing: 22) {
            Image(systemName: "sparkles")
                .font(.system(size: 38, weight: .bold))
                .foregroundStyle(GlowTheme.sparkleGold)
                .frame(width: 86, height: 86)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(spacing: 8) {
                Text("Discover Your Best Version")
                    .font(.system(size: 28, weight: .bold))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("Upload your face and full-body photos to unlock your personal style report and daily looks.")
                    .font(.system(size: 15))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(GlowTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            PrimaryButton(title: "Generate Your Style", iconName: "camera.viewfinder") {
                viewModel.activeTab = .analyze
            }
        }
        .frame(maxWidth: .infinity)
        .padding(26)
        .background(
            LinearGradient(
                colors: [GlowTheme.card, GlowTheme.blush.opacity(0.38)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .stroke(GlowTheme.borderSand, lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 24, x: 0, y: 14)
    }
}

/// 首页分析进度卡片。
struct HomeAnalysisProgressCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 进度卡片内容。
    var body: some View {
        VStack(spacing: 18) {
            Image(systemName: viewModel.analysisStage.iconName)
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 70, height: 70)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(spacing: 8) {
                Text("Curating your aesthetic")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text(viewModel.analysisStage.rawValue)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(GlowTheme.textSecondary)
                    .multilineTextAlignment(.center)
            }
            VStack(spacing: 8) {
                ProgressView(value: viewModel.analysisProgress)
                    .tint(GlowTheme.sparkleGold)
                HStack {
                    Text("PROGRESS")
                    Spacer()
                    Text("\(Int(viewModel.analysisProgress * 100))%")
                }
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(GlowTheme.textSecondary)
            }
        }
        .glowCard(padding: 24)
    }
}

/// 首页主穿搭卡片。
struct HeroOutfitCard: View {
    /// 风格报告。
    let report: StyleReportResponse

    /// 卡片内容。
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RemoteImageCard(urlString: "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f")
                .frame(height: 390)
            LinearGradient(
                colors: [.clear, .black.opacity(0.72)],
                startPoint: .center,
                endPoint: .bottom
            )
            VStack(alignment: .leading, spacing: 12) {
                GlowChip(text: "GlowUp AI", isSelected: true)
                Text(report.heroTitle)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(.white)
                Text(report.heroCopy)
                    .font(.system(size: 15))
                    .foregroundStyle(.white.opacity(0.82))
                HStack(spacing: 8) {
                    Text(report.dailyLook.top)
                    Text("•")
                    Text(report.dailyLook.bottom)
                    Text("•")
                    Text(report.dailyLook.shoes)
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(.white.opacity(0.9))
            }
            .padding(22)
        }
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: Color.black.opacity(0.1), radius: 30, x: 0, y: 18)
    }
}

/// 首页指标卡片。
struct DashboardMetric: View {
    /// 指标标题。
    let title: String
    /// 指标值。
    let value: String
    /// 图标名。
    let iconName: String

    /// 指标视图。
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: iconName)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(GlowTheme.roseGold)
            Text(value)
                .font(.system(size: value.count > 8 ? 19 : 26, weight: .bold))
                .foregroundStyle(GlowTheme.textPrimary)
                .lineLimit(2)
                .minimumScaleFactor(0.82)
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(GlowTheme.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .glowCard()
    }
}

/// 首页风格提升卡片。
struct ImproveStyleCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 风格报告。
    let report: StyleReportResponse

    /// 卡片内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                SectionTitle(title: "Improve Your Style", subtitle: "Small refinements from your current style profile.")
                Image(systemName: "wand.and.stars")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(width: 42, height: 42)
                    .background(GlowTheme.blush)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            VStack(spacing: 10) {
                ForEach(improvementItems, id: \.offset) { item in
                    ImproveStyleRow(order: item.offset + 1, text: item.element)
                }
            }
            HStack(spacing: 10) {
                ImproveStyleAction(title: "Generate Looks", iconName: "wand.and.stars") {
                    viewModel.activeTab = .style
                }
                ImproveStyleAction(title: "Analyze Again", iconName: "camera.viewfinder") {
                    viewModel.activeTab = .analyze
                }
            }
        }
        .glowCard()
    }

    /// 展示的提升建议。
    private var improvementItems: [(offset: Int, element: String)] {
        let items = Array(report.improvements.prefix(3).enumerated())
        if items.isEmpty {
            return [(offset: 0, element: "Generate your first style report to get personalized next steps.")]
        }
        return items
    }
}

/// 首页风格提升建议行。
struct ImproveStyleRow: View {
    /// 建议序号。
    let order: Int

    /// 建议文案。
    let text: String

    /// 行内容。
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text("\(order)")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 28, height: 28)
                .background(GlowTheme.roseGold)
                .clipShape(Circle())
            Text(text)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(GlowTheme.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// 首页风格提升快捷按钮。
struct ImproveStyleAction: View {
    /// 按钮标题。
    let title: String

    /// 图标名。
    let iconName: String

    /// 点击动作。
    let action: () -> Void

    /// 按钮内容。
    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                Image(systemName: iconName)
                    .font(.system(size: 12, weight: .bold))
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .foregroundStyle(GlowTheme.roseGold)
            .frame(maxWidth: .infinity)
            .frame(height: 42)
            .background(GlowTheme.blush)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

/// 首页 AI Stylist 入口卡片。
struct AIStylistHomeCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 入口卡片内容。
    var body: some View {
        Button {
            viewModel.openChat()
        } label: {
            HStack(spacing: 14) {
                Image(systemName: "sparkles")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(width: 54, height: 54)
                    .background(GlowTheme.blush)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 6) {
                    Text("Your AI Stylist")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text("Ask what to wear, attach a look, or get a quick polish check.")
                        .font(.system(size: 14))
                        .foregroundStyle(GlowTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(GlowTheme.roseGold)
                    .clipShape(Circle())
            }
            .glowCard()
        }
        .buttonStyle(.plain)
    }
}

/// 首页快捷入口。
struct QuickActionGrid: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 快捷入口内容。
    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 14) {
            QuickAction(title: "Analyze", subtitle: "Upload photos", iconName: "camera.viewfinder") {
                viewModel.activeTab = .analyze
            }
            QuickAction(title: "Outfits", subtitle: "Generate looks", iconName: "wand.and.stars") {
                viewModel.activeTab = .style
            }
            QuickAction(title: "AI Closet", subtitle: "Plan wardrobe", iconName: "tshirt") {
                if viewModel.requireSubscription(for: .aiCloset) {
                    viewModel.activeTab = .closet
                }
            }
            QuickAction(title: "Premium", subtitle: "Unlock stylist", iconName: "crown.fill") {
                viewModel.isPaywallPresented = true
            }
        }
    }
}

/// 快捷入口卡片。
struct QuickAction: View {
    /// 标题。
    let title: String
    /// 副标题。
    let subtitle: String
    /// 图标名。
    let iconName: String
    /// 点击动作。
    let action: () -> Void

    /// 卡片内容。
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 12) {
                Image(systemName: iconName)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(width: 38, height: 38)
                    .background(GlowTheme.blush)
                    .clipShape(Circle())
                Text(title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .glowCard()
        }
        .buttonStyle(.plain)
    }
}

/// 商品横向列表。
struct ProductCarousel: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 商品列表。
    let products: [ProductResponse]

    /// 列表内容。
    var body: some View {
        Group {
            if viewModel.hasActiveSubscription {
                VStack(alignment: .leading, spacing: 14) {
                    SectionTitle(title: "Shopping Picks", subtitle: "Affiliate-ready recommendations for your profile.")
                    if products.isEmpty {
                        ProductEmptyState()
                    } else {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 14) {
                                ForEach(products) { product in
                                    ProductCard(product: product)
                                        .frame(width: 220)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
            } else {
                SubscriptionGateCard(feature: .shoppingRecommendations) {
                    viewModel.requireSubscription(for: .shoppingRecommendations)
                }
            }
        }
    }
}

/// 商品推荐空状态。
struct ProductEmptyState: View {
    /// 空状态内容。
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "bag")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 38, height: 38)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 5) {
                Text("Shopping picks are not ready yet")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("Generate a style report first, then pick an occasion to load product recommendations.")
                    .font(.system(size: 13))
                    .foregroundStyle(GlowTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(14)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

/// 商品卡片。
struct ProductCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 系统打开 URL 环境。
    @Environment(\.openURL) private var openURL

    /// 商品数据。
    let product: ProductResponse

    /// 卡片内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            RemoteImageCard(urlString: product.image)
                .frame(height: 150)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            GlowChip(text: product.tag)
            Text(product.brand)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(GlowTheme.textSecondary)
            Text(product.name)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(GlowTheme.textPrimary)
                .lineLimit(2)
            Text(product.reason)
                .font(.system(size: 13))
                .foregroundStyle(GlowTheme.textSecondary)
                .lineLimit(3)
            HStack {
                Text(product.price)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                Spacer()
                if let url = URL(string: product.buyUrl) {
                    Button {
                        Task {
                            await viewModel.trackProductClick(product: product)
                            openURL(url)
                        }
                    } label: {
                        Image(systemName: "arrow.up.right")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 34, height: 34)
                            .background(GlowTheme.roseGold)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Open product")
                }
            }
        }
        .glowCard()
    }
}
