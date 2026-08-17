import PhotosUI
import SwiftUI

/// 风格分析页。
struct AnalyzeView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 面部照片选择项。
    @State private var faceItem: PhotosPickerItem?

    /// 全身照片选择项。
    @State private var bodyItem: PhotosPickerItem?

    /// 当前穿搭照片选择项。
    @State private var outfitItem: PhotosPickerItem?

    /// 分析页内容。
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    analysisHeader
                    uploadSection
                    PrimaryButton(
                        title: viewModel.isBusy ? "Analyzing..." : "Generate Report",
                        iconName: "sparkles",
                        isDisabled: !viewModel.hasRequiredPhotos || viewModel.isBusy
                    ) {
                        Task {
                            await viewModel.generateReport()
                        }
                    }
                    if viewModel.hasCompletedStyleReport {
                        StyleReportCard(report: viewModel.report)
                    } else {
                        StyleReportEmptyCard()
                    }
                    AIChatCard()
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(GlowTheme.surface)
            .safeAreaInset(edge: .top) {
                AppTopBar(title: "Style Assessment", subtitle: "Upload photos")
            }
        }
    }

    /// 分析状态卡片。
    private var analysisHeader: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                GlowChip(text: viewModel.apiStatus.label, isSelected: matchesBackend)
                Spacer()
                Text("\(Int(viewModel.analysisProgress * 100))%")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
            }
            ProgressView(value: viewModel.analysisProgress)
                .tint(GlowTheme.roseGold)
            HStack(spacing: 10) {
                Image(systemName: viewModel.analysisStage.iconName)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(width: 28, height: 28)
                    .background(GlowTheme.blush)
                    .clipShape(Circle())
                Text(viewModel.analysisStage.rawValue)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(GlowTheme.textPrimary)
                    .lineLimit(2)
                Spacer(minLength: 0)
            }
            .padding(10)
            .background(GlowTheme.cardMuted)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            Text("Upload a face photo and a full-body photo. An outfit shot is optional.")
                .font(.system(size: 15))
                .foregroundStyle(GlowTheme.textSecondary)
        }
        .glowCard()
    }

    /// 是否连接后端。
    private var matchesBackend: Bool {
        if case .backend = viewModel.apiStatus {
            return true
        }
        return false
    }

    /// 上传区域。
    private var uploadSection: some View {
        VStack(spacing: 14) {
            UploadTile(slot: .face, upload: viewModel.uploads[.face], selection: $faceItem)
                .onChange(of: faceItem) { _, newItem in
                    Task { await importSelectedPhoto(newItem, slot: .face) }
                }
            UploadTile(slot: .body, upload: viewModel.uploads[.body], selection: $bodyItem)
                .onChange(of: bodyItem) { _, newItem in
                    Task { await importSelectedPhoto(newItem, slot: .body) }
                }
            UploadTile(slot: .outfit, upload: viewModel.uploads[.outfit], selection: $outfitItem)
                .onChange(of: outfitItem) { _, newItem in
                    Task { await importSelectedPhoto(newItem, slot: .outfit) }
                }
            Button {
                Task { await viewModel.deletePhotos() }
            } label: {
                Label("Delete Style Photos", systemImage: "trash")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(maxWidth: .infinity)
                    .frame(height: 46)
            }
        }
    }

    /// 导入照片选择器中的图片。
    private func importSelectedPhoto(_ item: PhotosPickerItem?, slot: UploadSlot) async {
        guard let item else {
            return
        }
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else {
                throw AppError.photoReadFailed
            }
            let payload = try PhotoImportSupport.payload(from: data, supportedContentTypes: item.supportedContentTypes)
            await viewModel.importPhotoData(
                payload.data,
                slot: slot,
                fileExtension: payload.descriptor.fileExtension,
                mimeType: payload.descriptor.mimeType
            )
        } catch {
            viewModel.errorMessage = error.localizedDescription
        }
    }
}

/// 风格报告未生成状态卡片。
struct StyleReportEmptyCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 空状态卡片内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SectionTitle(
                title: "Your Style Report",
                subtitle: "Your report appears here after the face and full-body photos are analyzed."
            )
            HStack(spacing: 12) {
                Image(systemName: viewModel.hasRequiredPhotos ? "sparkles" : "camera.viewfinder")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(width: 44, height: 44)
                    .background(GlowTheme.blush)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 4) {
                    Text(viewModel.hasRequiredPhotos ? "Ready to generate" : "Upload required photos")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(GlowTheme.textPrimary)
                    Text(viewModel.hasRequiredPhotos ? "Tap Generate Report to start the AI analysis." : "Face photo and full-body photo are required for the MVP assessment.")
                        .font(.system(size: 13))
                        .foregroundStyle(GlowTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(14)
            .background(GlowTheme.cardMuted)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .glowCard()
    }
}

/// 上传照片卡片。
struct UploadTile: View {
    /// 槽位。
    let slot: UploadSlot
    /// 上传状态。
    let upload: PhotoUpload?
    /// 照片选择项。
    @Binding var selection: PhotosPickerItem?

    /// 上传卡片内容。
    var body: some View {
        PhotosPicker(selection: $selection, matching: .images) {
            HStack(spacing: 14) {
                UploadImagePreview(data: upload?.data)
                    .frame(width: 76, height: 90)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 6) {
                        Text(slot.title)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(GlowTheme.textPrimary)
                        if slot.isRequired {
                            Text("Required")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(GlowTheme.roseGold)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(GlowTheme.blush)
                                .clipShape(Capsule())
                        }
                    }
                    Text(slot.description)
                        .font(.system(size: 13))
                        .foregroundStyle(GlowTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(statusText)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(upload?.status == .synced ? GlowTheme.successSage : GlowTheme.roseGold)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(GlowTheme.textSecondary)
            }
            .glowCard()
        }
        .buttonStyle(.plain)
    }

    /// 状态文案。
    private var statusText: String {
        guard let upload else {
            return UploadStatus.empty.rawValue
        }
        return "\(upload.status.rawValue) · \(ByteCountFormatter.string(fromByteCount: Int64(upload.size), countStyle: .file))"
    }
}

/// 风格报告摘要卡片。
struct StyleReportCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 是否展示系统分享面板。
    @State private var isShareSheetPresented = false

    /// 风格报告。
    let report: StyleReportResponse

    /// 报告卡内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .top, spacing: 12) {
                SectionTitle(title: "Your Style Report", subtitle: report.description)
                Button {
                    Task {
                        await viewModel.trackShareClicked(surface: "style_report")
                    }
                    isShareSheetPresented = true
                } label: {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(GlowTheme.roseGold)
                        .frame(width: 42, height: 42)
                        .background(GlowTheme.blush)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .accessibilityLabel("Share style report")
            }
            HStack(spacing: 18) {
                ScoreRing(score: report.score)
                VStack(alignment: .leading, spacing: 10) {
                    GlowChip(text: report.badge, isSelected: true)
                    Text("Face: \(report.faceShape)")
                        .font(.system(size: 14, weight: .semibold))
                    Text("Body: \(report.bodyRatio)")
                        .font(.system(size: 14, weight: .semibold))
                    paletteRow
                }
                Spacer()
            }
            StyleAdviceGrid(report: report)
            RecommendationColumns(report: report)
        }
        .glowCard()
        .sheet(isPresented: $isShareSheetPresented) {
            ShareSheet(items: [viewModel.shareReportText()])
        }
    }

    /// 色板行。
    private var paletteRow: some View {
        HStack(spacing: 8) {
            ForEach(report.palette.prefix(4)) { color in
                Circle()
                    .fill(Color(hex: color.color))
                    .frame(width: 22, height: 22)
                    .overlay(Circle().stroke(GlowTheme.borderSand, lineWidth: 1))
            }
        }
    }
}

/// 风格细分建议。
struct StyleAdviceGrid: View {
    /// 风格报告。
    let report: StyleReportResponse

    /// 建议内容。
    var body: some View {
        VStack(spacing: 10) {
            StyleAdviceRow(title: "Hair", iconName: "scissors", items: report.hair)
            StyleAdviceRow(title: "Makeup", iconName: "paintbrush.pointed.fill", items: report.makeup)
            StyleAdviceRow(title: "Fit", iconName: "ruler", items: report.bodyTips)
        }
    }
}

/// 单组风格建议。
struct StyleAdviceRow: View {
    /// 标题。
    let title: String
    /// 图标。
    let iconName: String
    /// 建议项。
    let items: [String]

    /// 建议行内容。
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: iconName)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 32, height: 32)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text(items.joined(separator: " · "))
                    .font(.system(size: 13))
                    .foregroundStyle(GlowTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// 建议双列。
struct RecommendationColumns: View {
    /// 风格报告。
    let report: StyleReportResponse

    /// 建议列内容。
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            RecommendationList(title: "Strengths", iconName: "checkmark.circle.fill", items: report.strengths)
            RecommendationList(title: "Improve", iconName: "plus.circle.fill", items: report.improvements)
        }
    }
}

/// 建议列表。
struct RecommendationList: View {
    /// 标题。
    let title: String
    /// 图标。
    let iconName: String
    /// 建议项。
    let items: [String]

    /// 列表内容。
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label(title, systemImage: iconName)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
            ForEach(items, id: \.self) { item in
                Text(item)
                    .font(.system(size: 13))
                    .foregroundStyle(GlowTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(GlowTheme.cardMuted)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

/// AI 聊天卡片。
struct AIChatCard: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 聊天卡内容。
    var body: some View {
        Group {
            if viewModel.hasActiveSubscription {
                VStack(alignment: .leading, spacing: 14) {
                    SectionTitle(title: "AI Stylist Chat", subtitle: "Ask practical questions about an outfit or occasion.")
                    ForEach(viewModel.chatMessages.suffix(4)) { message in
                        ChatBubble(message: message)
                    }
                    ChatComposerBar()
                }
                .glowCard()
            } else {
                SubscriptionGateCard(feature: .aiStylistChat) {
                    viewModel.requireSubscription(for: .aiStylistChat)
                }
            }
        }
    }
}

/// 聊天气泡。
struct ChatBubble: View {
    /// 消息。
    let message: ChatMessage

    /// 气泡内容。
    var body: some View {
        HStack {
            if message.isUser {
                Spacer(minLength: 40)
            }
            Text(message.text)
                .font(.system(size: 14))
                .foregroundStyle(GlowTheme.textPrimary)
                .padding(14)
                .background(message.isUser ? GlowTheme.cardMuted : GlowTheme.blush)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            if !message.isUser {
                Spacer(minLength: 40)
            }
        }
    }
}
