import SwiftUI
import UIKit

/// 主按钮。
struct PrimaryButton: View {
    /// 按钮标题。
    let title: String
    /// SF Symbol 图标。
    let iconName: String?
    /// 是否禁用。
    var isDisabled: Bool = false
    /// 点击回调。
    let action: () -> Void

    /// 按钮视图。
    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                if let iconName {
                    Image(systemName: iconName)
                        .font(.system(size: 15, weight: .semibold))
                }
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(isDisabled ? GlowTheme.textSecondary.opacity(0.35) : GlowTheme.roseGold)
            .clipShape(RoundedRectangle(cornerRadius: GlowTheme.controlRadius, style: .continuous))
            .shadow(color: GlowTheme.roseGold.opacity(isDisabled ? 0 : 0.24), radius: 18, x: 0, y: 8)
        }
        .disabled(isDisabled)
    }
}

/// 次级按钮。
struct SecondaryButton: View {
    /// 按钮标题。
    let title: String
    /// SF Symbol 图标。
    let iconName: String?
    /// 点击回调。
    let action: () -> Void

    /// 按钮视图。
    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if let iconName {
                    Image(systemName: iconName)
                }
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(.white.opacity(0.18))
            .clipShape(RoundedRectangle(cornerRadius: GlowTheme.controlRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: GlowTheme.controlRadius, style: .continuous)
                    .stroke(.white.opacity(0.42), lineWidth: 1.2)
            )
        }
    }
}

/// 页面标题区域。
struct SectionTitle: View {
    /// 标题。
    let title: String
    /// 副标题。
    let subtitle: String?

    /// 标题视图。
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(GlowTheme.textPrimary)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 15))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Plus 权益锁定卡。
struct SubscriptionGateCard: View {
    /// 被锁定的功能。
    let feature: PremiumFeature
    /// 解锁点击回调。
    let action: () -> Void

    /// 卡片内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 14) {
                Image(systemName: feature.iconName)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(width: 50, height: 50)
                    .background(GlowTheme.blush)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 5) {
                    Text(feature.title)
                        .font(.system(size: 21, weight: .bold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text(feature.subtitle)
                        .font(.system(size: 14))
                        .foregroundStyle(GlowTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            PrimaryButton(title: "Unlock GlowUp Plus", iconName: "crown.fill", action: action)
        }
        .glowCard()
    }
}

/// 可选择选项卡。
struct OptionCard: View {
    /// 标题。
    let title: String
    /// 说明。
    let subtitle: String
    /// 图标。
    let iconName: String
    /// 是否选中。
    let isSelected: Bool
    /// 点击回调。
    let action: () -> Void

    /// 选项视图。
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(isSelected ? GlowTheme.blush : GlowTheme.cardMuted)
                        .frame(width: 48, height: 48)
                    Image(systemName: iconName)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(isSelected ? GlowTheme.roseGold : GlowTheme.primary)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundStyle(GlowTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(isSelected ? GlowTheme.roseGold : GlowTheme.borderSand)
            }
            .padding(16)
            .background(isSelected ? GlowTheme.blush.opacity(0.45) : GlowTheme.card)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(isSelected ? GlowTheme.roseGold.opacity(0.5) : GlowTheme.borderSand, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

/// 顶部导航条。
struct AppTopBar: View {
    /// 标题。
    let title: String
    /// 副标题。
    let subtitle: String
    /// 是否展示头像。
    var showsAvatar: Bool = true

    /// 顶部栏视图。
    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(GlowTheme.textSecondary)
                Text(subtitle)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
            }
            Spacer()
            if showsAvatar {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [GlowTheme.blush, GlowTheme.sparkleGold.opacity(0.55)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 44, height: 44)
                    .overlay(
                        Image(systemName: "person.crop.circle.fill")
                            .font(.system(size: 24))
                            .foregroundStyle(GlowTheme.roseGold)
                    )
            }
        }
        .padding(.horizontal, GlowTheme.pagePadding)
        .padding(.top, 12)
        .padding(.bottom, 10)
        .background(.ultraThinMaterial)
    }
}

/// 胶囊标签。
struct GlowChip: View {
    /// 标签文案。
    let text: String
    /// 是否选中。
    var isSelected: Bool = false

    /// 标签视图。
    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 11, weight: .bold))
            .foregroundStyle(isSelected ? .white : GlowTheme.roseGold)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(isSelected ? GlowTheme.roseGold : GlowTheme.blush)
            .clipShape(Capsule())
    }
}

/// 圆形进度分数。
struct ScoreRing: View {
    /// 分数。
    let score: Int

    /// 分数视图。
    var body: some View {
        ZStack {
            Circle()
                .stroke(GlowTheme.borderSand.opacity(0.8), lineWidth: 10)
            Circle()
                .trim(from: 0, to: CGFloat(min(score, 100)) / 100)
                .stroke(GlowTheme.roseGold, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 0) {
                Text("\(score)")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("/100")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
        }
        .frame(width: 116, height: 116)
    }
}

/// 远程图片卡片。
struct RemoteImageCard: View {
    /// 图片地址。
    let urlString: String
    /// 兜底渐变色。
    var fallbackColors: [Color] = [GlowTheme.blush, GlowTheme.surfaceDim]

    /// 图片视图。
    var body: some View {
        AsyncImage(url: URL(string: urlString)) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
            default:
                LinearGradient(colors: fallbackColors, startPoint: .topLeading, endPoint: .bottomTrailing)
                    .overlay(
                        Image(systemName: "sparkles")
                            .font(.system(size: 28, weight: .semibold))
                            .foregroundStyle(GlowTheme.roseGold.opacity(0.8))
                    )
            }
        }
        .clipped()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// 本地上传图片预览。
struct UploadImagePreview: View {
    /// 图片数据。
    let data: Data?

    /// 预览视图。
    var body: some View {
        Group {
            if let data, let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                GlowTheme.cardMuted
                    .overlay(
                        Image(systemName: "photo.badge.plus")
                            .font(.system(size: 26, weight: .semibold))
                            .foregroundStyle(GlowTheme.textSecondary)
                    )
            }
        }
        .clipped()
    }
}

/// 系统分享面板。
struct ShareSheet: UIViewControllerRepresentable {
    /// 需要分享的内容。
    let items: [Any]

    /// 创建分享控制器。
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    /// 更新分享控制器。
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {
    }
}

/// 字段输入样式。
struct GlowTextFieldStyle: TextFieldStyle {
    /// 构建输入框样式。
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .font(.system(size: 16, weight: .medium))
            .padding(.horizontal, 16)
            .frame(height: 56)
            .background(GlowTheme.card)
            .clipShape(RoundedRectangle(cornerRadius: GlowTheme.controlRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: GlowTheme.controlRadius, style: .continuous)
                    .stroke(GlowTheme.borderSand, lineWidth: 1)
            )
    }
}
