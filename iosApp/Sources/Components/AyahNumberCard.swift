//
//  AyahNumberCard.swift
//  QuranBlind
//
//  1:1 Parity with Android AyahNumberCard (AyahCard.kt:130)
//

import SwiftUI

public struct AyahNumberCard: View {
    @Environment(\.quranTheme) private var theme

    public let number: Int32
    public var ayahNumber: Int32 { number }
    public let onClick: () -> Void

    public init(number: Int32, onClick: @escaping () -> Void = {}) {
        self.number = number
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            Text("الآية \(number)")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(theme.textPrimary)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(theme.cardSurface)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(theme.cardBorder, lineWidth: 1.5)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(A11yLabels.ayah(number))
        .accessibilityRemoveTraits(.isButton)
        .accessibilitySpeechLanguage(A11yLabels.speechLanguage)
    }
}
