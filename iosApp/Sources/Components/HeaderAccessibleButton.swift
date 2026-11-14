import SwiftUI

/// 68pt circular accessible button for header controls with 1.5pt border stroke.
/// Enforces minimum 68pt touch target; no frames under 68pt are permitted.
public struct HeaderAccessibleButton: View {
    @Environment(\.quranTheme) private var theme

    private let icon: String?
    private let title: String
    private let hint: String?
    private let accessibilityLabel: String?
    private let accessibilityValue: String?
    private let isActive: Bool
    private let action: () -> Void

    public init(
        icon: String? = nil,
        title: String,
        hint: String? = nil,
        accessibilityLabel: String? = nil,
        accessibilityValue: String? = nil,
        isActive: Bool = false,
        action: @escaping () -> Void
    ) {
        self.icon = icon
        self.title = title
        self.hint = hint
        self.accessibilityLabel = accessibilityLabel
        self.accessibilityValue = accessibilityValue
        self.isActive = isActive
        self.action = action
    }

    public var body: some View {
        let bgColor = isActive ? theme.accentContainer : theme.cardSurface
        let borderColor = isActive ? theme.cardActiveBorder : theme.cardBorder
        let textColor = isActive ? theme.textOnFill : theme.accent

        Button(action: action) {
            ZStack {
                Circle()
                    .fill(bgColor)
                Circle()
                    .stroke(borderColor, lineWidth: 1.5)
                if let icon = icon, !icon.isEmpty {
                    Image(systemName: icon)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(textColor)
                } else {
                    Text(title)
                        .font(.system(size: 13, weight: .bold))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .foregroundColor(textColor)
                }
            }
            .frame(width: 68, height: 68)
        }
        .accessibilityLabel(Text(accessibilityLabel ?? title.replacingOccurrences(of: "\n", with: " ")))
        .accessibilityValue(accessibilityValue != nil ? Text(accessibilityValue!) : Text(""))
        .accessibilityHint(Text(hint ?? ""))
        .accessibilityAddTraits(.isButton)
    }
}
