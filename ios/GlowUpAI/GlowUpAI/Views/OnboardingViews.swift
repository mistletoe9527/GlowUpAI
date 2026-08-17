import AuthenticationServices
import SwiftUI

/// Onboarding 根视图。
struct OnboardingRootView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// Onboarding 内容。
    var body: some View {
        switch viewModel.onboardingStep {
        case .welcome:
            WelcomeView()
        case .styleGoal, .gender, .profileDetails:
            OnboardingStepView()
        }
    }
}

/// 欢迎页。
struct WelcomeView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 欢迎页内容。
    var body: some View {
        ZStack(alignment: .bottom) {
            RemoteImageCard(
                urlString: "https://images.unsplash.com/photo-1496747611176-843222e1e57c",
                fallbackColors: [GlowTheme.surfaceDim, GlowTheme.blush]
            )
            .ignoresSafeArea()
            LinearGradient(
                colors: [.black.opacity(0.06), .black.opacity(0.34), .black.opacity(0.78)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            GeometryReader { proxy in
                let contentWidth = max(
                    1,
                    min(proxy.size.width - (GlowTheme.pagePadding * 2), 375)
                )

                ScrollView(.vertical, showsIndicators: false) {
                    VStack {
                        VStack(spacing: 26) {
                            VStack(spacing: 18) {
                                LogoMark()
                                Text("Discover Your\nPersonal Style")
                                    .font(.system(size: 42, weight: .bold))
                                    .multilineTextAlignment(.center)
                                    .foregroundStyle(.white)
                                Text("Your AI stylist that helps you look confident every day.")
                                    .font(.system(size: 17, weight: .regular))
                                    .foregroundStyle(.white.opacity(0.84))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 18)
                            }
                            VStack(spacing: 14) {
                                PrimaryButton(title: "Start My Style Journey", iconName: "arrow.right") {
                                    viewModel.start()
                                }
                                SecondaryButton(title: "Already have an account", iconName: nil) {
                                    viewModel.beginEmailSignIn()
                                }
                                SignInWithAppleButton(.continue) { request in
                                    request.requestedScopes = [.fullName, .email]
                                } onCompletion: { result in
                                    switch result {
                                    case .success(let authorization):
                                        if let credential = authorization.credential as? ASAuthorizationAppleIDCredential {
                                            Task {
                                                await viewModel.continueWithApple(
                                                    userId: credential.user,
                                                    fullName: credential.fullName,
                                                    email: credential.email
                                                )
                                            }
                                        }
                                    case .failure(let error):
                                        viewModel.errorMessage = error.localizedDescription
                                    }
                                }
                                .signInWithAppleButtonStyle(.white)
                                .frame(width: contentWidth, height: 48)
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                                HStack(spacing: 12) {
                                    AuthButton(title: "Google", iconName: "globe") {
                                        Task {
                                            await viewModel.continueWithGoogle()
                                        }
                                    }
                                    AuthButton(title: "Email", iconName: "envelope.fill") {
                                        viewModel.beginEmailSignIn()
                                    }
                                }
                            }
                        }
                        .frame(width: contentWidth)
                    }
                    .frame(width: proxy.size.width)
                    .frame(minHeight: proxy.size.height, alignment: .bottom)
                    .padding(.top, 16)
                    .padding(.bottom, 36)
                }
                .scrollBounceBehavior(.basedOnSize)
            }
        }
        .sheet(isPresented: $viewModel.isEmailSignInPresented) {
            EmailSignInView()
        }
    }
}

/// 品牌标识。
struct LogoMark: View {
    /// 标识内容。
    var body: some View {
        ZStack {
            Circle()
                .fill(.white.opacity(0.18))
                .frame(width: 82, height: 82)
                .blur(radius: 1)
            Circle()
                .stroke(.white.opacity(0.7), lineWidth: 1)
                .frame(width: 76, height: 76)
            Image(systemName: "sparkles")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(.white)
        }
    }
}

/// 第三方登录按钮。
struct AuthButton: View {
    /// 按钮标题。
    let title: String
    /// 图标名称。
    let iconName: String
    /// 点击动作。
    let action: () -> Void

    /// 登录按钮内容。
    var body: some View {
        Button(action: action) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(.white.opacity(0.13))
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(.white.opacity(0.28), lineWidth: 1)
                HStack(spacing: 7) {
                    Image(systemName: iconName)
                        .font(.system(size: 15, weight: .semibold))
                    Text(title)
                        .font(.system(size: 13, weight: .semibold))
                }
                .foregroundStyle(Color.white)
                .fixedSize(horizontal: true, vertical: false)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
        }
        .buttonStyle(.plain)
    }
}

/// Email 登录 MVP 表单。
struct EmailSignInView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 关闭当前弹窗。
    @Environment(\.dismiss) private var dismiss

    /// Email 登录页内容。
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 22) {
                VStack(alignment: .leading, spacing: 10) {
                    Image(systemName: "envelope.fill")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundStyle(GlowTheme.roseGold)
                        .frame(width: 58, height: 58)
                        .background(GlowTheme.blush)
                        .clipShape(Circle())
                    Text("Continue with Email")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text("Use your email to restore a saved profile or create a new style profile.")
                        .font(.system(size: 15))
                        .foregroundStyle(GlowTheme.textSecondary)
                }
                TextField("you@example.com", text: $viewModel.emailDraft)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(GlowTextFieldStyle())
                PrimaryButton(
                    title: viewModel.isBusy ? "Checking profile..." : "Continue",
                    iconName: "arrow.right",
                    isDisabled: viewModel.isBusy
                ) {
                    Task {
                        await viewModel.continueWithEmail()
                    }
                }
                Spacer()
            }
            .padding(GlowTheme.pagePadding)
            .background(GlowTheme.surface.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") {
                        viewModel.isEmailSignInPresented = false
                        dismiss()
                    }
                    .foregroundStyle(GlowTheme.roseGold)
                }
            }
        }
    }
}

/// Onboarding 步骤页。
struct OnboardingStepView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 步骤页内容。
    var body: some View {
        VStack(spacing: 0) {
            OnboardingTopBar()
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    SectionTitle(title: title, subtitle: subtitle)
                    ProgressView(
                        value: Double(viewModel.onboardingStep.rawValue),
                        total: Double(OnboardingStep.contentStepCount)
                    )
                        .tint(GlowTheme.roseGold)
                        .padding(.bottom, 2)
                    stepContent
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 22)
                .padding(.bottom, 104)
            }
            .safeAreaInset(edge: .bottom) {
                VStack(spacing: 0) {
                    footerButton
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 12)
                .padding(.bottom, 12)
                .background(.ultraThinMaterial)
            }
        }
        .background(GlowTheme.surface.ignoresSafeArea())
    }

    /// 当前步骤标题。
    private var title: String {
        switch viewModel.onboardingStep {
        case .welcome:
            return ""
        case .gender:
            return "What is your gender?"
        case .profileDetails:
            return "Tell us your basics"
        case .styleGoal:
            return "What is your style goal?"
        }
    }

    /// 当前步骤副标题。
    private var subtitle: String {
        switch viewModel.onboardingStep {
        case .welcome:
            return ""
        case .gender:
            return "This helps the stylist interpret fit and proportions with better context."
        case .profileDetails:
            return "Age, height, weight, and your US region context help refine recommendations."
        case .styleGoal:
            return "Pick the focus that resonates most. We will tune every recommendation around it."
        }
    }

    /// 当前步骤内容。
    @ViewBuilder
    private var stepContent: some View {
        switch viewModel.onboardingStep {
        case .welcome:
            EmptyView()
        case .gender:
            VStack(spacing: 12) {
                ForEach(GenderOption.allCases) { gender in
                    OptionCard(
                        title: gender.rawValue,
                        subtitle: gender == .preferNotToSay ? "You can still get a full style report." : "Used only to tailor styling language and fit notes.",
                        iconName: gender.iconName,
                        isSelected: viewModel.profile.gender == gender.rawValue
                    ) {
                        viewModel.chooseGender(gender)
                    }
                }
            }
        case .profileDetails:
            ProfileDetailsForm()
        case .styleGoal:
            VStack(spacing: 12) {
                ForEach(StyleGoal.allCases) { goal in
                    OptionCard(
                        title: goal.rawValue,
                        subtitle: goal.subtitle,
                        iconName: goal.iconName,
                        isSelected: viewModel.profile.styleGoal == goal.rawValue
                    ) {
                        viewModel.chooseGoal(goal)
                    }
                }
            }
        }
    }

    /// 当前步骤底部按钮。
    private var footerButton: some View {
        Group {
            if viewModel.onboardingStep == .styleGoal {
                PrimaryButton(
                    title: viewModel.isBusy ? "Saving..." : "Continue to Photo Upload",
                    iconName: "arrow.right",
                    isDisabled: viewModel.isBusy || !viewModel.canContinueCurrentOnboardingStep
                ) {
                    Task {
                        await viewModel.finishOnboarding()
                    }
                }
            } else {
                PrimaryButton(
                    title: "Continue",
                    iconName: "arrow.right",
                    isDisabled: !viewModel.canContinueCurrentOnboardingStep
                ) {
                    viewModel.nextOnboardingStep()
                }
            }
        }
    }
}

/// Onboarding 顶部栏。
struct OnboardingTopBar: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 顶部栏内容。
    var body: some View {
        HStack {
            Button {
                viewModel.goBackOnboarding()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                    .frame(width: 40, height: 40)
                    .background(GlowTheme.card)
                    .clipShape(Circle())
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("GlowUp AI")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("Step 0\(viewModel.onboardingStep.rawValue) / 0\(OnboardingStep.contentStepCount)")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            Spacer()
            if viewModel.canSkipCurrentOnboardingStep {
                Button("Skip") {
                    Task {
                        await viewModel.skipCurrentOnboardingStep()
                    }
                }
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(minWidth: 44, minHeight: 40)
                .disabled(viewModel.isBusy)
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .font(.system(size: 34))
                    .foregroundStyle(GlowTheme.roseGold)
            }
        }
        .padding(.horizontal, GlowTheme.pagePadding)
        .padding(.top, 14)
        .padding(.bottom, 10)
        .background(.ultraThinMaterial)
    }
}

/// 基础资料表单。
struct ProfileDetailsForm: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 表单内容。
    var body: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                Text("Birthday")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(GlowTheme.textSecondary)
                DatePicker(
                    "Birthday",
                    selection: $viewModel.birthdayDate,
                    in: viewModel.birthdayDateRange,
                    displayedComponents: .date
                )
                .datePickerStyle(.wheel)
                .labelsHidden()
                .frame(maxWidth: .infinity)
                .frame(height: 156)
                .clipped()
            }
            .glowCard()
            VStack(alignment: .leading, spacing: 12) {
                Text("Style context")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(GlowTheme.textSecondary)
                HeightMetricPicker()
                WeightMetricPicker()
                RegionMetricRow()
            }
            .glowCard()
        }
        .onAppear {
            viewModel.prepareProfileDetailsDefaults()
        }
    }
}

/// 默认地区展示行。
struct RegionMetricRow: View {
    /// 地区行内容。
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "location.fill")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 34, height: 34)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 3) {
                Text("Region")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("Used for US market recommendations")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            Spacer()
            Text(AppViewModel.defaultRegionDisplayName)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
        }
        .padding(14)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// 身高默认值选择器。
struct HeightMetricPicker: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 身高选择器内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            MetricHeader(title: "Height", value: viewModel.profile.height, iconName: "ruler")
            HStack(spacing: 0) {
                Picker("Feet", selection: $viewModel.heightFeet) {
                    ForEach(Array(viewModel.heightFeetRange), id: \.self) { feet in
                        Text("\(feet) ft").tag(feet)
                    }
                }
                .pickerStyle(.wheel)
                .frame(maxWidth: .infinity)
                .clipped()
                Picker("Inches", selection: $viewModel.heightInches) {
                    ForEach(Array(viewModel.heightInchesRange), id: \.self) { inches in
                        Text("\(inches) in").tag(inches)
                    }
                }
                .pickerStyle(.wheel)
                .frame(maxWidth: .infinity)
                .clipped()
            }
            .frame(height: 116)
            .onChange(of: viewModel.heightFeet) { _, _ in
                viewModel.updateHeight()
            }
            .onChange(of: viewModel.heightInches) { _, _ in
                viewModel.updateHeight()
            }
        }
        .padding(14)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// 体重默认值选择器。
struct WeightMetricPicker: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 体重选择器内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            MetricHeader(title: "Weight", value: viewModel.profile.weight, iconName: "scalemass")
            Picker("Weight", selection: $viewModel.weightPounds) {
                ForEach(Array(viewModel.weightPoundsRange), id: \.self) { pounds in
                    Text("\(pounds) lb").tag(pounds)
                }
            }
            .pickerStyle(.wheel)
            .frame(maxWidth: .infinity)
            .frame(height: 116)
            .clipped()
            .onChange(of: viewModel.weightPounds) { _, _ in
                viewModel.updateWeight()
            }
        }
        .padding(14)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// 身体数据选择器标题。
struct MetricHeader: View {
    /// 指标标题。
    let title: String
    /// 当前指标值。
    let value: String
    /// 指标图标。
    let iconName: String

    /// 标题内容。
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconName)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 34, height: 34)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("Based on your selected profile")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            Spacer()
            Text(value)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
        }
    }
}
