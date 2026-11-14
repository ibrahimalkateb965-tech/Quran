import SwiftUI
import UIKit

public enum ThemeMode: String, CaseIterable {
    case system = "SYSTEM"
    case light = "LIGHT"
    case dark = "DARK"
}

@MainActor
public final class ThemeManager: ObservableObject {
    public static let shared = ThemeManager()
    public static let KEY_THEME_MODE = "theme_mode"

    @Published public var currentMode: ThemeMode {
        didSet {
            UserDefaults.standard.set(currentMode.rawValue, forKey: Self.KEY_THEME_MODE)
        }
    }

    public init() {
        if let saved = UserDefaults.standard.string(forKey: Self.KEY_THEME_MODE),
           let mode = ThemeMode(rawValue: saved) {
            self.currentMode = mode
        } else {
            self.currentMode = ThemeMode.system
        }
    }

    public var isDark: Bool {
        switch currentMode {
        case .dark:
            return true
        case .light:
            return false
        case .system:
            return UITraitCollection.current.userInterfaceStyle == .dark
        }
    }

    public var theme: QuranTheme {
        QuranTheme.theme(for: isDark)
    }

    public func toggleTheme() {
        let wasDark = isDark
        let newMode: ThemeMode = wasDark ? .light : .dark
        self.currentMode = newMode
        UserDefaults.standard.set(newMode.rawValue, forKey: Self.KEY_THEME_MODE)

        UIImpactFeedbackGenerator(style: .medium).impactOccurred()

        A11yAnnouncer.shared.announceTheme(isDark: currentMode == .dark)
    }
}
