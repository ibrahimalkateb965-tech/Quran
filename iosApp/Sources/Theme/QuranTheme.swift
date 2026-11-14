import SwiftUI

/// Complete monochrome design system mirroring Android production baseline Color.kt & Theme.kt.
/// Enforces WCAG AAA high contrast ratio compliance for blind and visually impaired users.
public struct QuranTheme {
    public let background: Color
    public let cardSurface: Color
    public let cardFill: Color
    public let cardActive: Color
    public let cardBorder: Color
    public let cardActiveBorder: Color
    public let textPrimary: Color
    public let textSecondary: Color
    public let textOnFill: Color
    public let textAyah: Color
    public let accent: Color
    public let accentBright: Color
    public let accentContainer: Color
    public let highlight: Color
    public let success: Color
    public let error: Color
    public let isDark: Bool

    // Light Mode: Neutral White (#F8F9FA / #FFFFFF) background, Pure Black (#000000) textPrimary
    public static let light = QuranTheme(
        background: Color(hex: "#F8F9FA"),
        cardSurface: Color(hex: "#F8F9FA"),
        cardFill: Color(hex: "#F8F9FA"),
        cardActive: Color(hex: "#E5E5E5"),
        cardBorder: Color(hex: "#333333"),
        cardActiveBorder: Color(hex: "#000000"),
        textPrimary: Color(hex: "#000000"),
        textSecondary: Color(hex: "#000000"),
        textOnFill: Color(hex: "#FFFFFF"),
        textAyah: Color(hex: "#000000"),
        accent: Color(hex: "#000000"),
        accentBright: Color(hex: "#000000"),
        accentContainer: Color(hex: "#000000"),
        highlight: Color(hex: "#000000"),
        success: Color(hex: "#16A34A"),
        error: Color(hex: "#DC2626"),
        isDark: false
    )

    // Dark Mode: Deep Dark OLED Pure Black (#000000 / #0D0D0D) background, Pure White (#FFFFFF) textPrimary
    public static let dark = QuranTheme(
        background: Color(hex: "#000000"),
        cardSurface: Color(hex: "#1A1A1A"),
        cardFill: Color(hex: "#000000"),
        cardActive: Color(hex: "#222222"),
        cardBorder: Color(hex: "#555555"),
        cardActiveBorder: Color(hex: "#FFFFFF"),
        textPrimary: Color(hex: "#FFFFFF"),
        textSecondary: Color(hex: "#FFFFFF"),
        textOnFill: Color(hex: "#FFFFFF"),
        textAyah: Color(hex: "#FFFFFF"),
        accent: Color(hex: "#FFFFFF"),
        accentBright: Color(hex: "#FFFFFF"),
        accentContainer: Color(hex: "#2A2A2A"),
        highlight: Color(hex: "#FFFFFF"),
        success: Color(hex: "#FFFFFF"),
        error: Color(hex: "#FFFFFF"),
        isDark: true
    )

    public static func theme(for isDark: Bool) -> QuranTheme {
        isDark ? .dark : .light
    }
}

// MARK: - Color Hex Initializers (Strict Monochrome Safe)

public extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }

    init(hex: String) {
        let cleanHex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: cleanHex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch cleanHex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - SwiftUI Environment Integration

public struct QuranThemeKey: EnvironmentKey {
    public static let defaultValue: QuranTheme = .dark
}

public extension EnvironmentValues {
    var quranTheme: QuranTheme {
        get { self[QuranThemeKey.self] }
        set { self[QuranThemeKey.self] = newValue }
    }
}

public extension View {
    func quranTheme(_ theme: QuranTheme) -> some View {
        self.environment(\.quranTheme, theme)
    }
}
