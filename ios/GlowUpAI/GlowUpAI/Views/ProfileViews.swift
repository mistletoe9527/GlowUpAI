import SwiftUI

/// 个人资料页。
struct ProfileView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 是否展示删除账户确认弹窗。
    @State private var isDeleteAccountConfirmationPresented = false

    /// 个人资料页内容。
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    profileHeader
                    subscriptionSummary
                    profileDetails
                    dataControls
                    PrimaryButton(title: viewModel.hasActiveSubscription ? "GlowUp Plus Active" : "Unlock Your AI Stylist", iconName: "crown.fill") {
                        viewModel.isPaywallPresented = true
                    }
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(GlowTheme.surface)
            .safeAreaInset(edge: .top) {
                AppTopBar(title: "Your Profile", subtitle: "Settings")
            }
            .confirmationDialog(
                "Delete account data?",
                isPresented: $isDeleteAccountConfirmationPresented,
                titleVisibility: .visible
            ) {
                Button("Delete account data", role: .destructive) {
                    Task { await viewModel.deleteAccountData() }
                }
                Button("Cancel", role: .cancel) {
                }
            } message: {
                Text("This removes backend profile, photos, reports, closet, subscription records, and analytics for this app user. App Store subscription cancellation is managed by Apple.")
            }
        }
    }

    /// 资料头部。
    private var profileHeader: some View {
        VStack(spacing: 14) {
            Circle()
                .fill(LinearGradient(colors: [GlowTheme.blush, GlowTheme.sparkleGold.opacity(0.5)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 94, height: 94)
                .overlay(
                    Image(systemName: "person.crop.circle.fill")
                        .font(.system(size: 54))
                        .foregroundStyle(GlowTheme.roseGold)
                )
            Text(viewModel.profile.name)
                .font(.system(size: 26, weight: .bold))
                .foregroundStyle(GlowTheme.textPrimary)
            GlowChip(text: viewModel.hasCompletedStyleReport ? viewModel.report.badge : "Profile pending", isSelected: true)
        }
        .frame(maxWidth: .infinity)
        .glowCard()
    }

    /// 订阅状态摘要。
    private var subscriptionSummary: some View {
        HStack(spacing: 12) {
            Image(systemName: viewModel.hasActiveSubscription ? "crown.fill" : "lock.fill")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 42, height: 42)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(viewModel.hasActiveSubscription ? "GlowUp Plus" : "Free Plan")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text(viewModel.subscriptionStatusMessage ?? "Upgrade for AI chat, closet, and shopping picks.")
                    .font(.system(size: 13))
                    .foregroundStyle(GlowTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer()
        }
        .glowCard()
    }

    /// 资料明细。
    private var profileDetails: some View {
        VStack(spacing: 12) {
            ProfileRow(title: "Goal", value: viewModel.profile.styleGoal)
            ProfileRow(title: "Gender", value: viewModel.profile.gender)
            ProfileRow(title: "Birthday", value: viewModel.profile.birthday.isEmpty ? "Not set" : viewModel.profile.birthday)
            ProfileRow(title: "Height", value: viewModel.profile.height.isEmpty ? "Optional" : viewModel.profile.height)
            ProfileRow(title: "Weight", value: viewModel.profile.weight.isEmpty ? "Optional" : viewModel.profile.weight)
            ProfileRow(title: "Region", value: viewModel.profileRegionDisplayName)
        }
        .glowCard()
    }

    /// 数据管理区。
    private var dataControls: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(title: "Privacy & Data", subtitle: "Style assessment photos can be removed without deleting your closet.")
            Button {
                Task { await viewModel.deletePhotos() }
            } label: {
                Label("Delete style photos", systemImage: "trash")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(GlowTheme.blush)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            Button {
                isDeleteAccountConfirmationPresented = true
            } label: {
                Label("Delete account data", systemImage: "exclamationmark.triangle.fill")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.red.opacity(0.08))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .disabled(viewModel.isBusy)
        }
        .glowCard()
    }
}

/// 资料行。
struct ProfileRow: View {
    /// 标题。
    let title: String
    /// 值。
    let value: String

    /// 行内容。
    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(GlowTheme.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(GlowTheme.textPrimary)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 5)
    }
}

/// 订阅页。
struct SubscriptionView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 关闭环境。
    @Environment(\.dismiss) private var dismiss

    /// 订阅页内容。
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    VStack(spacing: 12) {
                        Image(systemName: "crown.fill")
                            .font(.system(size: 36, weight: .semibold))
                            .foregroundStyle(GlowTheme.sparkleGold)
                            .frame(width: 78, height: 78)
                            .background(GlowTheme.blush)
                            .clipShape(Circle())
                        Text("Unlock Your AI Stylist")
                            .font(.system(size: 30, weight: .bold))
                            .foregroundStyle(GlowTheme.textPrimary)
                            .multilineTextAlignment(.center)
                        Text("Unlimited style analysis, daily outfit suggestions, AI closet, and shopping recommendations.")
                            .font(.system(size: 15))
                            .foregroundStyle(GlowTheme.textSecondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.top, 18)
                    subscriptionStatus
                    VStack(spacing: 12) {
                        ForEach(SubscriptionPlan.allCases) { plan in
                            PlanCard(
                                plan: plan,
                                price: viewModel.displayPrice(for: plan),
                                isSelected: viewModel.selectedPlan == plan
                            ) {
                                viewModel.chooseSubscriptionPlan(plan)
                            }
                        }
                    }
                    PrimaryButton(
                        title: primaryButtonTitle,
                        iconName: "arrow.right",
                        isDisabled: purchaseButtonDisabled
                    ) {
                        Task {
                            await viewModel.startSubscription()
                            if viewModel.hasActiveSubscription {
                                dismiss()
                            }
                        }
                    }
                    Button {
                        Task {
                            await viewModel.restoreSubscriptionPurchases()
                        }
                    } label: {
                        Text("Restore Purchases")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(GlowTheme.roseGold)
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                    }
                    .disabled(viewModel.isBusy)
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.bottom, 30)
            }
            .background(GlowTheme.surface)
            .task {
                await viewModel.loadSubscriptionProducts()
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") {
                        dismiss()
                    }
                    .foregroundStyle(GlowTheme.roseGold)
                }
            }
        }
    }

    /// 订阅状态视图。
    private var subscriptionStatus: some View {
        HStack(spacing: 10) {
            Image(systemName: statusIconName)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(GlowTheme.roseGold)
            Text(statusText)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(GlowTheme.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    /// 状态图标。
    private var statusIconName: String {
        switch viewModel.subscriptionPurchaseState {
        case .idle, .loading:
            return "arrow.triangle.2.circlepath"
        case .ready:
            return "checkmark.seal"
        case .purchasing:
            return "creditcard"
        case .subscribed:
            return "crown.fill"
        case .unavailable:
            return "exclamationmark.triangle"
        }
    }

    /// 状态文案。
    private var statusText: String {
        switch viewModel.subscriptionPurchaseState {
        case .idle:
            return "Preparing App Store products..."
        case .loading:
            return "Loading App Store products..."
        case .ready:
            return viewModel.subscriptionStatusMessage ?? "App Store subscriptions are ready."
        case .purchasing:
            return "Waiting for App Store purchase confirmation..."
        case .subscribed:
            return viewModel.subscriptionStatusMessage ?? "Your Plus subscription is active."
        case .unavailable(let message):
            return message
        }
    }

    /// 主按钮标题。
    private var primaryButtonTitle: String {
        if viewModel.hasActiveSubscription {
            return "Subscription Active"
        }
        if viewModel.isBusy {
            return "Processing..."
        }
        return "Start \(viewModel.selectedPlan.rawValue)"
    }

    /// 购买按钮是否禁用。
    private var purchaseButtonDisabled: Bool {
        viewModel.isBusy
            || viewModel.hasActiveSubscription
            || viewModel.subscriptionProducts[viewModel.selectedPlan] == nil
    }
}

/// 套餐卡片。
struct PlanCard: View {
    /// 套餐。
    let plan: SubscriptionPlan
    /// 展示价格。
    let price: String
    /// 是否选中。
    let isSelected: Bool
    /// 点击动作。
    let action: () -> Void

    /// 套餐卡内容。
    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 5) {
                    HStack(spacing: 8) {
                        Text(plan.rawValue)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(GlowTheme.textPrimary)
                        if plan == .yearly {
                            GlowChip(text: "Best Value", isSelected: true)
                        }
                    }
                    Text(plan.caption)
                        .font(.system(size: 13))
                        .foregroundStyle(GlowTheme.textSecondary)
                }
                Spacer()
                Text(price)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(isSelected ? GlowTheme.roseGold : GlowTheme.borderSand)
            }
            .glowCard()
        }
        .buttonStyle(.plain)
    }
}
