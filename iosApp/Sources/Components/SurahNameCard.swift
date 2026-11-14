//
//  SurahNameCard.swift
//  QuranBlind
//
//  1:1 Parity with Android SurahNameCard (AyahCard.kt:156)
//

import SwiftUI

public struct SurahNameCard: View {
    @Environment(\.quranTheme) private var theme

    public let name: String
    public let onClick: () -> Void

    public init(name: String, onClick: @escaping () -> Void = {}) {
        self.name = name
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            Text("سورة \(name)")
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
        .accessibilityLabel("سورة \(name)")
        .accessibilityHint("انقر للاستماع إلى اسم السورة")
        .accessibilityAddTraits(.isButton)
    }
}
