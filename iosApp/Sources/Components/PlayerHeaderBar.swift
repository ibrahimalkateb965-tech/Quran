import SwiftUI

/// Top HeaderBar containing three 68pt circular accessible buttons in RTL layout:
/// 1. Continuous Play toggle ("الاستماع المتواصل")
/// 2. Surah selection ("اختيار السورة")
/// 3. Reciter selection ("اختيار القارئ")
public struct PlayerHeaderBar: View {
    @Environment(\.quranTheme) private var theme

    public let isContinuousPlayEnabled: Bool
    public let onToggleContinuousPlay: () -> Void
    public let onOpenSurahIndex: () -> Void
    public let onOpenReciters: () -> Void

    public init(
        isContinuousPlayEnabled: Bool,
        onToggleContinuousPlay: @escaping () -> Void,
        onOpenSurahIndex: @escaping () -> Void,
        onOpenReciters: @escaping () -> Void
    ) {
        self.isContinuousPlayEnabled = isContinuousPlayEnabled
        self.onToggleContinuousPlay = onToggleContinuousPlay
        self.onOpenSurahIndex = onOpenSurahIndex
        self.onOpenReciters = onOpenReciters
    }

    public init(
        isContinuousPlay: Bool,
        onToggleContinuousPlay: @escaping () -> Void,
        onOpenSurahSheet: @escaping () -> Void,
        onOpenReciterSheet: @escaping () -> Void
    ) {
        self.isContinuousPlayEnabled = isContinuousPlay
        self.onToggleContinuousPlay = onToggleContinuousPlay
        self.onOpenSurahIndex = onOpenSurahSheet
        self.onOpenReciters = onOpenReciterSheet
    }

    public var body: some View {
        HStack {
            // Button 1: Continuous Play ("الاستماع المتواصل")
            Button(action: {
                onToggleContinuousPlay()
            }) {
                ZStack {
                    Circle()
                        .fill(isContinuousPlayEnabled ? (theme.isDark ? Color.white : Color.black) : theme.cardSurface)
                    Circle()
                        .stroke(isContinuousPlayEnabled ? (theme.isDark ? Color.white : Color.black) : theme.cardBorder, lineWidth: 1.5)
                    Text("الاستماع\nالمتواصل")
                        .font(.system(size: 13, weight: .bold))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .foregroundColor(isContinuousPlayEnabled ? (theme.isDark ? Color.black : Color.white) : theme.accent)
                }
                .frame(width: 68, height: 68)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .contentShape(Rectangle())
            .accessibilityLabel("الاستماع المتواصل")
            .accessibilityValue(isContinuousPlayEnabled ? "مفعل" : "معطل")
            .accessibilityHint(isContinuousPlayEnabled ? "انقر لإيقاف الاستماع المتواصل" : "انقر لتفعيل الاستماع المتواصل")
            .accessibilityAddTraits(.isButton)
            .accessibilityAction {
                onToggleContinuousPlay()
            }

            Spacer()

            // Button 2: Surah Selection ("اختيار السورة")
            Button(action: onOpenSurahIndex) {
                ZStack {
                    Circle()
                        .fill(theme.cardSurface)
                    Circle()
                        .stroke(theme.cardBorder, lineWidth: 1.5)
                    Text("اختيار\nالسورة")
                        .font(.system(size: 13, weight: .bold))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .foregroundColor(theme.accent)
                }
                .frame(width: 68, height: 68)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .contentShape(Rectangle())
            .accessibilityLabel(Text("اختيار السورة"))
            .accessibilityHint(Text("يفتح قائمة السور المائة وأربعة عشر"))
            .accessibilityAddTraits(.isButton)
            .accessibilityAction {
                onOpenSurahIndex()
            }

            Spacer()

            // Button 3: Reciter Selection ("اختيار القارئ")
            Button(action: onOpenReciters) {
                ZStack {
                    Circle()
                        .fill(theme.cardSurface)
                    Circle()
                        .stroke(theme.cardBorder, lineWidth: 1.5)
                    Text("اختيار\nالقارئ")
                        .font(.system(size: 13, weight: .bold))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .foregroundColor(theme.accent)
                }
                .frame(width: 68, height: 68)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .contentShape(Rectangle())
            .accessibilityLabel(Text("اختيار القارئ"))
            .accessibilityHint(Text("يفتح قائمة اختيار القارئ"))
            .accessibilityAddTraits(.isButton)
            .accessibilityAction {
                onOpenReciters()
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
    }
}
