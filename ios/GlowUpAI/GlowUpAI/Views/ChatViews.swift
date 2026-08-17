import PhotosUI
import SwiftUI

/// 独立 AI 聊天页。
struct AIChatView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 关闭当前页面。
    @Environment(\.dismiss) private var dismiss

    /// 聊天页内容。
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if viewModel.hasActiveSubscription {
                        AIChatHeroPanel()
                        ForEach(viewModel.chatMessages) { message in
                            ChatBubble(message: message)
                        }
                    } else {
                        SubscriptionGateCard(feature: .aiStylistChat) {
                            viewModel.requireSubscription(for: .aiStylistChat)
                        }
                    }
                }
                .padding(.horizontal, GlowTheme.pagePadding)
                .padding(.top, 18)
                .padding(.bottom, 92)
            }
            .background(GlowTheme.surface)
            .safeAreaInset(edge: .bottom) {
                if viewModel.hasActiveSubscription {
                    ChatComposerBar()
                        .padding(.horizontal, GlowTheme.pagePadding)
                        .padding(.top, 12)
                        .padding(.bottom, 10)
                        .background(.ultraThinMaterial)
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("AI Stylist")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(GlowTheme.textSecondary)
                        Text("Chat")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(GlowTheme.textPrimary)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") {
                        dismiss()
                    }
                    .foregroundStyle(GlowTheme.roseGold)
                }
            }
        }
    }
}

/// AI 聊天页头部说明。
struct AIChatHeroPanel: View {
    /// 头部内容。
    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "sparkles")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(GlowTheme.roseGold)
                .frame(width: 58, height: 58)
                .background(GlowTheme.blush)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 6) {
                Text("Your AI Stylist")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(GlowTheme.textPrimary)
                Text("Ask what to wear, attach an outfit photo, or get a quick polish check before you go.")
                    .font(.system(size: 14))
                    .foregroundStyle(GlowTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .glowCard()
    }
}

/// 聊天输入条。
struct ChatComposerBar: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 聊天穿搭照片选择项。
    @State private var chatPhotoItem: PhotosPickerItem?

    /// 输入条内容。
    var body: some View {
        HStack(spacing: 10) {
            TextField("Ask your AI stylist...", text: $viewModel.chatDraft)
                .textFieldStyle(GlowTextFieldStyle())
            PhotosPicker(selection: $chatPhotoItem, matching: .images) {
                Image(systemName: "photo.on.rectangle.angled")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(GlowTheme.roseGold)
                    .frame(width: 52, height: 52)
                    .background(GlowTheme.blush)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(viewModel.isBusy)
            .accessibilityLabel("Attach outfit photo")
            .onChange(of: chatPhotoItem) { _, newItem in
                Task { await importSelectedChatPhoto(newItem) }
            }
            Button {
                Task { await viewModel.sendChatMessage() }
            } label: {
                Image(systemName: "paperplane.fill")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 52, height: 52)
                    .background(GlowTheme.roseGold)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .disabled(sendDisabled)
            .opacity(sendDisabled ? 0.55 : 1)
        }
    }

    /// 是否禁用发送。
    private var sendDisabled: Bool {
        viewModel.chatDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isBusy
    }

    /// 导入聊天穿搭照片并发送默认问题。
    ///
    /// - Parameter item: 系统照片选择项
    private func importSelectedChatPhoto(_ item: PhotosPickerItem?) async {
        guard let item else {
            return
        }
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else {
                throw AppError.photoReadFailed
            }
            let payload = try PhotoImportSupport.payload(from: data, supportedContentTypes: item.supportedContentTypes)
            await viewModel.sendChatOutfitPhotoData(
                payload.data,
                fileExtension: payload.descriptor.fileExtension,
                mimeType: payload.descriptor.mimeType
            )
        } catch {
            viewModel.errorMessage = error.localizedDescription
        }
    }
}
