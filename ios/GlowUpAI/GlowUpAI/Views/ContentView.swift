import SwiftUI

/// App 根视图。
struct ContentView: View {
    /// App 全局状态。
    @EnvironmentObject private var viewModel: AppViewModel

    /// 根视图内容。
    var body: some View {
        Group {
            switch viewModel.route {
            case .onboarding:
                OnboardingRootView()
            case .main:
                MainTabView()
            }
        }
        .background(GlowTheme.surface.ignoresSafeArea())
        .preferredColorScheme(.light)
        .alert("GlowUp AI", isPresented: errorBinding) {
            Button("OK", role: .cancel) {
                viewModel.errorMessage = nil
            }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    /// 错误提示绑定。
    private var errorBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { isPresented in
                if !isPresented {
                    viewModel.errorMessage = nil
                }
            }
        )
    }
}
