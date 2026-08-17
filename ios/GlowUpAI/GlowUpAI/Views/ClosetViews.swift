import PhotosUI
import SwiftUI

/// 虚拟衣橱页。
struct ClosetView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 衣橱照片选择项。
    @State private var closetItem: PhotosPickerItem?

    /// 衣橱页内容。
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    if viewModel.hasActiveSubscription {
                        SectionTitle(title: "AI Closet", subtitle: "Your saved wardrobe pieces.")
                        ClosetAvatarCard(itemCount: viewModel.closetItems.count)
                        ClosetUploadCard(isBusy: viewModel.isBusy, selection: $closetItem)
                            .onChange(of: closetItem) { _, newItem in
                                Task { await importSelectedClosetPhoto(newItem) }
                            }
                        ClosetOutfitCard(
                            outfit: viewModel.closetOutfit,
                            hasGeneratedOutfit: viewModel.hasGeneratedClosetOutfit,
                            isBusy: viewModel.isBusy
                        )
                        ClosetPlannerCard(items: viewModel.closetItems)
                        if viewModel.closetItems.isEmpty {
                            ClosetEmptyState()
                        } else {
                            ForEach(groupedSections) { section in
                                ClosetSectionView(section: section)
                            }
                        }
                    } else {
                        SubscriptionGateCard(feature: .aiCloset) {
                            viewModel.requireSubscription(for: .aiCloset)
                        }
                    }
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(GlowTheme.surface)
            .safeAreaInset(edge: .top) {
                AppTopBar(title: "Virtual Wardrobe", subtitle: "Closet")
            }
            .task {
                if viewModel.hasActiveSubscription {
                    await viewModel.loadClosetItems()
                }
            }
            .onChange(of: viewModel.hasActiveSubscription) { _, isActive in
                if isActive {
                    Task { await viewModel.loadClosetItems() }
                }
            }
        }
    }

    /// 按品类分组后的衣橱分区。
    private var groupedSections: [ClosetSection] {
        let groups = Dictionary(grouping: viewModel.closetItems, by: \.category)
        let preferredOrder = ["Outerwear", "Top", "Bottom", "Dress", "Shoes", "Accessory", "Activewear"]
        return groups.keys
            .sorted { first, second in
                let firstIndex = preferredOrder.firstIndex(of: first) ?? preferredOrder.count
                let secondIndex = preferredOrder.firstIndex(of: second) ?? preferredOrder.count
                if firstIndex == secondIndex {
                    return first < second
                }
                return firstIndex < secondIndex
            }
            .map { category in
                let items = groups[category] ?? []
                return ClosetSection(
                    title: category,
                    subtitle: "\(items.count) \(items.count == 1 ? "item" : "items")",
                    items: items
                )
            }
    }

    /// 导入衣橱照片选择器中的图片。
    private func importSelectedClosetPhoto(_ item: PhotosPickerItem?) async {
        guard let item else {
            return
        }
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else {
                throw AppError.photoReadFailed
            }
            let payload = try PhotoImportSupport.payload(from: data, supportedContentTypes: item.supportedContentTypes)
            await viewModel.uploadClosetItemData(
                payload.data,
                fileExtension: payload.descriptor.fileExtension,
                mimeType: payload.descriptor.mimeType
            )
        } catch {
            viewModel.errorMessage = error.localizedDescription
        }
    }
}

/// 衣橱今日穿搭卡。
struct ClosetOutfitCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 衣橱穿搭。
    let outfit: ClosetOutfitResponse
    /// 是否已有生成结果。
    let hasGeneratedOutfit: Bool
    /// 是否忙碌。
    let isBusy: Bool

    /// 今日穿搭内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SectionTitle(title: "What should I wear today?", subtitle: "Use your saved closet, occasion, and weather.")
            occasionPicker
            TextField("mild weather", text: $viewModel.closetWeatherDraft)
                .textInputAutocapitalization(.never)
                .textFieldStyle(GlowTextFieldStyle())
            PrimaryButton(
                title: isBusy ? "Building Outfit..." : "Build From My Closet",
                iconName: "wand.and.stars",
                isDisabled: isBusy
            ) {
                Task { await viewModel.generateClosetOutfit() }
            }
            if hasGeneratedOutfit {
                VStack(spacing: 10) {
                    ClosetLookRow(label: "Top", value: outfit.top, iconName: "tshirt.fill")
                    ClosetLookRow(label: "Bottom", value: outfit.bottom, iconName: "figure.stand")
                    ClosetLookRow(label: "Shoes", value: outfit.shoes, iconName: "shoeprints.fill")
                    ClosetLookRow(label: "Layer", value: outfit.layer, iconName: "jacket.fill")
                    ClosetLookRow(label: "Accessory", value: outfit.accessory, iconName: "handbag.fill")
                }
                VStack(alignment: .leading, spacing: 8) {
                    GlowChip(text: "\(outfit.occasion) · \(outfit.style)", isSelected: true)
                    Text(outfit.why)
                        .font(.system(size: 14))
                        .foregroundStyle(GlowTheme.textSecondary)
                    Text(outfit.missingItem)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(GlowTheme.roseGold)
                }
            } else {
                ClosetOutfitEmptyState()
            }
        }
        .glowCard()
    }

    /// 场景选择器。
    private var occasionPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(Occasion.allCases) { occasion in
                    Button {
                        Task {
                            viewModel.selectOccasion(occasion)
                        }
                    } label: {
                        GlowChip(text: occasion.rawValue, isSelected: viewModel.selectedOccasion == occasion)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 2)
        }
    }
}

/// 衣橱穿搭未生成状态。
struct ClosetOutfitEmptyState: View {
    /// 空状态内容。
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "wand.and.stars")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 38, height: 38)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 5) {
                Text("No closet outfit yet")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("Add a few pieces, choose an occasion and weather, then build your first closet-based look.")
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

/// 衣橱穿搭条目。
struct ClosetLookRow: View {
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
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 32, height: 32)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            Text(label)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(GlowTheme.textSecondary)
                .frame(width: 70, alignment: .leading)
            Text(value)
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

/// 衣橱虚拟形象卡。
struct ClosetAvatarCard: View {
    /// 衣橱单品数量。
    let itemCount: Int

    /// 卡片内容。
    var body: some View {
        HStack(spacing: 18) {
            ZStack {
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(LinearGradient(colors: [GlowTheme.blush, GlowTheme.cardMuted], startPoint: .topLeading, endPoint: .bottomTrailing))
                Image(systemName: "figure.dress.line.vertical.figure")
                    .font(.system(size: 54, weight: .light))
                    .foregroundStyle(GlowTheme.roseGold)
            }
            .frame(width: 118, height: 152)
            VStack(alignment: .leading, spacing: 10) {
                GlowChip(text: "AI Closet")
                Text("\(itemCount) saved pieces")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text(closetSummary)
                    .font(.system(size: 14))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            Spacer()
        }
        .glowCard()
    }

    /// 衣橱摘要文案。
    private var closetSummary: String {
        itemCount == 0 ? "Build your first outfit-ready wardrobe set." : "Ready for outfit formulas and shopping gaps."
    }
}

/// 衣橱上传卡。
struct ClosetUploadCard: View {
    /// 是否正在上传识别。
    let isBusy: Bool
    /// 照片选择项。
    @Binding var selection: PhotosPickerItem?

    /// 上传卡内容。
    var body: some View {
        PhotosPicker(selection: $selection, matching: .images) {
            HStack(spacing: 14) {
                Image(systemName: isBusy ? "sparkles" : "plus")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
                    .background(GlowTheme.roseGold)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 5) {
                    Text(isBusy ? "Recognizing item..." : "Add Closet Item")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text("Category, color, season, and style")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(GlowTheme.textSecondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            .glowCard()
        }
        .buttonStyle(.plain)
        .disabled(isBusy)
    }
}

/// 衣橱规划卡。
struct ClosetPlannerCard: View {
    /// 衣橱单品。
    let items: [ClosetItemResponse]

    /// 规划卡内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(title: "Closet Signal", subtitle: closetSignal)
            HStack(spacing: 10) {
                ClosetMetric(title: "Tops", value: count(for: "Top"), iconName: "tshirt.fill")
                ClosetMetric(title: "Bottoms", value: count(for: "Bottom"), iconName: "figure.stand")
                ClosetMetric(title: "Shoes", value: count(for: "Shoes"), iconName: "shoeprints.fill")
            }
        }
        .glowCard()
    }

    /// 衣橱信号文案。
    private var closetSignal: String {
        guard !items.isEmpty else {
            return "Start with one top, one bottom, and one shoe."
        }
        let styles = Array(Set(items.map(\.style))).sorted().prefix(2).joined(separator: " + ")
        return styles.isEmpty ? "Your saved pieces are ready." : "Dominant style: \(styles)."
    }

    /// 计算指定品类数量。
    private func count(for category: String) -> String {
        "\(items.filter { $0.category == category }.count)"
    }
}

/// 衣橱指标。
struct ClosetMetric: View {
    /// 指标标题。
    let title: String
    /// 指标值。
    let value: String
    /// 图标名。
    let iconName: String

    /// 指标内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: iconName)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(GlowTheme.roseGold)
            Text(value)
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(GlowTheme.textPrimary)
            Text(title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(GlowTheme.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// 衣橱空状态。
struct ClosetEmptyState: View {
    /// 空状态内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Image(systemName: "hanger")
                .font(.system(size: 24, weight: .semibold))
                .foregroundStyle(GlowTheme.roseGold)
            Text("No saved closet items yet")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(GlowTheme.textPrimary)
            Text("Your first uploaded piece will appear here.")
                .font(.system(size: 14))
                .foregroundStyle(GlowTheme.textSecondary)
        }
        .glowCard()
    }
}

/// 衣橱分区模型。
struct ClosetSection: Identifiable {
    /// 分区 ID。
    var id: String { title }
    /// 分区标题。
    let title: String
    /// 分区说明。
    let subtitle: String
    /// 单品列表。
    let items: [ClosetItemResponse]
}

/// 衣橱分区视图。
struct ClosetSectionView: View {
    /// 分区数据。
    let section: ClosetSection

    /// 分区内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(section.title)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text(section.subtitle)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(GlowTheme.textSecondary)
                }
                Spacer()
                Image(systemName: "chevron.down")
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(section.items) { item in
                    ClosetItemCard(item: item)
                }
            }
        }
        .glowCard()
    }
}

/// 衣橱单品卡。
struct ClosetItemCard: View {
    /// 单品。
    let item: ClosetItemResponse

    /// 单品卡内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(LinearGradient(colors: [GlowTheme.cardMuted, swatchColor.opacity(0.45)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(height: 112)
                .overlay(
                    Image(systemName: iconName)
                        .font(.system(size: 30, weight: .semibold))
                        .foregroundStyle(GlowTheme.roseGold.opacity(0.85))
                )
            HStack(spacing: 8) {
                Circle()
                    .fill(swatchColor)
                    .frame(width: 14, height: 14)
                    .overlay(Circle().stroke(GlowTheme.borderSand, lineWidth: 1))
                Text(item.name)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                    .lineLimit(2)
            }
            Text("\(item.season) · \(item.style)")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(GlowTheme.textSecondary)
                .lineLimit(2)
        }
    }

    /// 单品图标。
    private var iconName: String {
        switch item.category {
        case "Shoes":
            return "shoeprints.fill"
        case "Bottom":
            return "figure.stand"
        case "Outerwear":
            return "jacket.fill"
        case "Dress":
            return "figure.dress.line.vertical.figure"
        case "Accessory":
            return "handbag.fill"
        case "Activewear":
            return "figure.run"
        default:
            return "tshirt.fill"
        }
    }

    /// 单品颜色。
    private var swatchColor: Color {
        switch item.color {
        case "Black":
            return Color(hex: "#1C1C1C")
        case "Ivory":
            return Color(hex: "#F7F3ED")
        case "Beige":
            return Color(hex: "#D8C5B0")
        case "Navy":
            return Color(hex: "#1F2B3F")
        case "Blue":
            return Color(hex: "#5C7EA8")
        case "Rose":
            return Color(hex: "#C7A59B")
        case "Olive":
            return Color(hex: "#6D705E")
        case "Charcoal":
            return Color(hex: "#303030")
        case "Brown":
            return Color(hex: "#5A4236")
        default:
            return GlowTheme.surfaceDim
        }
    }
}
