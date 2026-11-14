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
    public let isCurrent: Bool
    public let isPlaying: Bool
    public let onTap: (() -> Void)?
    public let onDoubleTap: () -> Void
    public let onNext: (() -> Void)?
    public let onPrevious: (() -> Void)?

    public init(
        ayah: Ayah,
        isCurrent: Bool,
        isPlaying: Bool,
        onTap: (() -> Void)? = nil,
        onDoubleTap: @escaping () -> Void = {},
        onNext: (() -> Void)? = nil,
        onPrevious: (() -> Void)? = nil
    ) {
        self.ayah = ayah
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
                        onDoubleTap()
                    case .second:
                        onTap?()
                    }
                }
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("الآية \(ayah.numberInSurah): \(ayah.textArabic)")
        .accessibilityValue(isCurrent && isPlaying ? "قيد التلاوة" : "")
        .accessibilityAction(named: "إعادة التلاوة") {
            onDoubleTap()
        }
        .accessibilityAction(named: "الآية التالية") {
            onNext?()
        }
        .accessibilityAction(named: "الآية السابقة") {
            onPrevious?()
        }
        .accessibilityAction {
            onTap?()
        }
        .accessibilityScrollAction { edge in
            if edge == .leading {
                onNext?()
            } else if edge == .trailing {
                onPrevious?()
            }
        }
    }
}
