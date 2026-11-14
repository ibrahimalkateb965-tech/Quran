import SwiftUI

/// 52pt circular accessible icon button for blind users with 1.5pt border stroke.
/// Enforces minimum 52pt touch target; inner icons do NOT use frames under 52pt.
/// VoiceOver speaks the label only: no hint, no trait words (standard tap-to-inspect, double-tap-to-activate).
public struct BlindAccessibleIconButton: View {
    @Environment(\.quranTheme) private var theme

    private let systemName: String
    private let accessibilityLabel: String
    private let action: () -> Void

    public init(
        systemName: String,
        accessibilityLabel: String,
        action: @escaping () -> Void
    ) {
        self.systemName = systemName
        self.accessibilityLabel = accessibilityLabel
        self.action = action
    }

    /// Convenience initializer for theme toggle button
    public init(
        isDark: Bool,
        action: @escaping () -> Void
    ) {
        self.systemName = isDark ? "sun.max.fill" : "moon.stars.fill"
        self.accessibilityLabel = A11yLabels.dayNightToggle(isDark: isDark)
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(theme.cardSurface)
                Circle()
                    .stroke(theme.cardBorder, lineWidth: 1.5)
                Image(systemName: systemName)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(theme.accent)
            }
            .frame(width: 52, height: 52)
        }
        .buttonStyle(AccessibleHapticButtonStyle(haptic: .click))
        .accessibilityLabel(accessibilityLabel)
        .accessibilityRemoveTraits(.isButton)
        .accessibilitySpeechLanguage(A11yLabels.speechLanguage)
    }
}
