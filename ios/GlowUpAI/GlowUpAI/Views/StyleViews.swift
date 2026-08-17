import SwiftUI

/// 穿搭生成页。
struct StyleView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 穿搭页内容。
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    if viewModel.hasCompletedStyleReport {
                        SectionTitle(title: "AI Outfit Generator", subtitle: "Choose an occasion and get three practical looks.")
                        occasionPicker
                        ForEach(viewModel.outfits) { outfit in
                            OutfitCard(outfit: outfit)
                        }
                        ProductCarousel(products: viewModel.products)
                    } else {
                        SectionTitle(title: "AI Outfit Generator", subtitle: "Generate your style report before creating occasion looks.")
                        NewUserStyleCard()
                    }
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(GlowTheme.surface)
            .safeAreaInset(edge: .top) {
                AppTopBar(title: "Style Studio", subtitle: viewModel.selectedOccasion.rawValue)
            }
        }
    }

    /// 场景选择器。
    private var occasionPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(Occasion.allCases) { occasion in
                    Button {
                        Task {
                            await viewModel.chooseOccasion(occasion)
                        }
                    } label: {
                        GlowChip(text: occasion.rawValue, isSelected: viewModel.selectedOccasion == occasion)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 4)
        }
    }
}

/// 穿搭卡片。
struct OutfitCard: View {
    /// 穿搭数据。
    let outfit: OutfitResponse

    /// 穿搭卡内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 8) {
                    GlowChip(text: outfit.occasion, isSelected: true)
                    Text(outfit.style)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text(outfit.why)
                        .font(.system(size: 14))
                        .foregroundStyle(GlowTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer()
                Text("0\(outfit.order)")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(GlowTheme.borderSand)
            }
            VStack(spacing: 10) {
                OutfitRow(label: "Top", value: outfit.top, iconName: "tshirt.fill")
                OutfitRow(label: "Bottom", value: outfit.bottom, iconName: "figure.stand")
                OutfitRow(label: "Shoes", value: outfit.shoes, iconName: "shoeprints.fill")
            }
        }
        .glowCard()
    }
}

/// 穿搭条目。
struct OutfitRow: View {
    /// 条目名称。
    let label: String
    /// 条目值。
    let value: String
    /// 图标名。
    let iconName: String

    /// 条目内容。
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconName)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 32, height: 32)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            Text(label)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(GlowTheme.textSecondary)
                .frame(width: 58, alignment: .leading)
            Text(value)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(GlowTheme.textPrimary)
            Spacer()
        }
        .padding(12)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
