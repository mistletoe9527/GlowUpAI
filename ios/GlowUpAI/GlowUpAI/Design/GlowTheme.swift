import SwiftUI

/// GlowUp AI 设计系统。
enum GlowTheme {
    /// 主背景色。
    static let surface = Color(hex: "#FCF9F8")
    /// 弱背景色。
    static let surfaceDim = Color(hex: "#DCD9D9")
    /// 卡片背景色。
    static let card = Color(hex: "#FFFFFF")
    /// 次级卡片背景色。
    static let cardMuted = Color(hex: "#F6F3F2")
    /// 主文字色。
    static let textPrimary = Color(hex: "#1C1C1C")
    /// 次级文字色。
    static let textSecondary = Color(hex: "#7A7A7A")
    /// 品牌主色。
    static let primary = Color(hex: "#625E58")
    /// 玫瑰金强调色。
    static let roseGold = Color(hex: "#835244")
    /// 柔和玫瑰背景。
    static let blush = Color(hex: "#FFECE8")
    /// 沙色边框。
    static let borderSand = Color(hex: "#E5D8CF")
    /// 金色点缀。
    static let sparkleGold = Color(hex: "#E5C07B")
    /// 成功鼠尾草色。
    static let successSage = Color(hex: "#7DA87B")

    /// 标准页面横向间距。
    static let pagePadding: CGFloat = 20
    /// 标准卡片圆角。
    static let cardRadius: CGFloat = 24
    /// 控件圆角。
    static let controlRadius: CGFloat = 16
}

/// 十六进制颜色初始化工具。
extension Color {
    /// 使用十六进制字符串创建颜色。
    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var value: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&value)
        let red: UInt64
        let green: UInt64
        let blue: UInt64
        let alpha: UInt64
        switch cleaned.count {
        case 3:
            red = (value >> 8) * 17
            green = (value >> 4 & 0xF) * 17
            blue = (value & 0xF) * 17
            alpha = 255
        case 6:
            red = value >> 16
            green = value >> 8 & 0xFF
            blue = value & 0xFF
            alpha = 255
        case 8:
            red = value >> 24
            green = value >> 16 & 0xFF
            blue = value >> 8 & 0xFF
            alpha = value & 0xFF
        default:
            red = 0
            green = 0
            blue = 0
            alpha = 255
        }
        self.init(
            .sRGB,
            red: Double(red) / 255,
            green: Double(green) / 255,
            blue: Double(blue) / 255,
            opacity: Double(alpha) / 255
        )
    }
}

/// 卡片通用样式。
struct GlowCardStyle: ViewModifier {
    /// 卡片内边距。
    let padding: CGFloat

    /// 构建卡片样式。
    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background(GlowTheme.card)
            .clipShape(RoundedRectangle(cornerRadius: GlowTheme.cardRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: GlowTheme.cardRadius, style: .continuous)
                    .stroke(GlowTheme.borderSand.opacity(0.8), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.04), radius: 22, x: 0, y: 12)
    }
}

/// View 的 Glow 扩展。
extension View {
    /// 应用卡片样式。
    func glowCard(padding: CGFloat = 20) -> some View {
        modifier(GlowCardStyle(padding: padding))
    }
}
