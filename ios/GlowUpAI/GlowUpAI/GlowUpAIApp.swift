import SwiftUI

/// GlowUp AI iOS App 入口。
@main
struct GlowUpAIApp: App {
    /// App 全局状态。
    @StateObject private var viewModel = AppViewModel()

    /// App 场景。
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(viewModel)
        }
    }
}
