//
//  AyahCardView.swift
//  QuranBlind
//
//  1:1 Horizontal Ayah Card Paging Component mirroring Android production baseline.
//

import SwiftUI
import SharedKit

public struct AyahCardView: View {
    @Environment(\.quranTheme) private var theme

    public let ayah: Ayah
    public let surahName: String?
    public let isCurrent: Bool
    public let isPlaying: Bool
    public let onTap: (() -> Void)?
    public let onDoubleTap: () -> Void
    public let onNext: (() -> Void)?
    public let onPrevious: (() -> Void)?

    public init(
        ayah: Ayah,
        surahName: String? = nil,
        isCurrent: Bool,
        isPlaying: Bool,
        onTap: (() -> Void)? = nil,
        onDoubleTap: @escaping () -> Void = {},
        onNext: (() -> Void)? = nil,
        onPrevious: (() -> Void)? = nil
    ) {
        self.ayah = ayah
        self.surahName = surahName
        self.isCurrent = isCurrent
        self.isPlaying = isPlaying
        self.onTap = onTap
        self.onDoubleTap = onDoubleTap
        self.onNext = onNext
        self.onPrevious = onPrevious
    }

    private var borderColor: Color {
        if isCurrent {
            return theme.cardActiveBorder
        } else {
            return theme.isDark ? Color(hex: "#2A2A2A") : theme.cardBorder
        }
    }

    private var surfaceColor: Color {
        theme.isDark ? Color(hex: "#1A1A1A") : Color(hex: "#FFFFFF")
    }

    public var body: some View {
        GeometryReader { geometry in
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 16) {
                    Spacer(minLength: 16)

                    Text(ayah.textArabic)
                        .font(.custom("KFGQPCHAFSUthmanicScript-Regula", size: 28))
                        .foregroundColor(theme.textPrimary)
                        .multilineTextAlignment(.center)
                        .lineSpacing(16)
                        .padding(.horizontal, 20)

                    if isCurrent && isPlaying {
                        AudioEqualizerBars(isPlaying: true)
                            .padding(.top, 12)
                    }

                    Spacer(minLength: 16)
                }
                .frame(maxWidth: .infinity, minHeight: geometry.size.height)
                .padding(.vertical, 24)
            }
        }
        .background(surfaceColor)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(borderColor, lineWidth: 1.5)
        )
        .shadow(color: Color.black.opacity(0.08), radius: isCurrent ? 4 : 2, x: 0, y: 2)
        .contentShape(Rectangle())
        .gesture(
            TapGesture(count: 2)
                .exclusively(before: TapGesture(count: 1))
                .onEnded { value in
                    switch value {
                    case .first:
                        HapticManager.shared.doubleTap()
                        onDoubleTap()
                    case .second:
                        HapticManager.shared.click()
                        onTap?()
                    }
                }
        )
        // VoiceOver silence (Android AyahCard.kt:66 `clearAndSetSemantics { }`): no label,
        // value, hint or actions here. While VoiceOver runs, QuranPlayerScreen hides the whole
        // pager and overlays the single silent `SilentRecitationSurface`, so this card speaks nothing.
    }
}
