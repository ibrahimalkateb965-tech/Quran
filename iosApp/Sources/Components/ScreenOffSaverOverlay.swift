//
//  ScreenOffSaverOverlay.swift
//  QuranBlind
//
//  1:1 Parity with Android ScreenOffSaverOverlay.kt
//

import SwiftUI

public struct ScreenOffSaverOverlay: View {
    public var isScreenOff: Bool = true
    public let onWakeUp: () -> Void

    public init(isScreenOff: Bool = true, onWakeUp: @escaping () -> Void) {
        self.isScreenOff = isScreenOff
        self.onWakeUp = onWakeUp
    }

    public var body: some View {
        Color.black
            .ignoresSafeArea()
            .overlay(
                VStack(spacing: 12) {
                    Text("تم تفعيل وضع إيقاف الشاشة")
                        .font(.headline)
                        .foregroundColor(.white.opacity(0.3))

                    Text("انقر مرتين في أي مكان لإيقاظ الشاشة")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.2))
                }
            )
            .contentShape(Rectangle())
            .onTapGesture(count: 2) {
                onWakeUp()
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("شاشة توفير الطاقة نشطة. انقر مرتين للاستيقاظ")
            .accessibilityHint("انقر مرتين لإلغاء وضع توفير الطاقة وإيقاظ الشاشة")
            .accessibilityAddTraits([.isButton, .isModal])
            .accessibilityAction {
                onWakeUp()
            }
            .accessibilityAction(.escape) {
                onWakeUp()
            }
            .accessibilityAction(named: "استيقاظ") {
                onWakeUp()
            }
            .accessibilityAction(named: "إلغاء وضع إيقاف الشاشة") {
                onWakeUp()
            }
        .onAppear {
            // إعلان توفير البطارية عبر A11yAnnouncer
        }
    }
}
