//
//  ScreenOffSaverOverlay.swift
//  QuranBlind
//
//  1:1 Parity with Android ScreenOffSaverOverlay.kt
//

import SwiftUI

public struct ScreenOffSaverOverlay: View {

    // MARK: - Authoritative Accessibility Constants (R4 / Invariant I7)

    /// Accessibility label: the element's name only. No "double-tap to…" tutorial and no hint —
    /// VoiceOver's standard double-tap (default activation action) wakes the screen.
    public static let accessibilityLabelText = A11yLabels.screenOffActive

    /// Modal traits isolating the view hierarchy beneath the overlay.
    /// No `.isButton`: VoiceOver would append the English word "button" after the Arabic label.
    public static let accessibilityTraitsSet: AccessibilityTraits = [.isModal]

    /// No named custom actions: they make VoiceOver append "Actions available" after the label.
    public static let supportedActionNames: [String] = []

    // MARK: - Properties & Callbacks

    public var isScreenOff: Bool = true
    public let onWakeUp: () -> Void

    // Optional observability hooks for empirical testing and verification
    public var onHapticFeedback: (() -> Void)? = nil
    public var onScreenOffAnnouncement: ((Bool) -> Void)? = nil

    public init(
        isScreenOff: Bool = true,
        onWakeUp: @escaping () -> Void,
        onHapticFeedback: (() -> Void)? = nil,
        onScreenOffAnnouncement: ((Bool) -> Void)? = nil
    ) {
        self.isScreenOff = isScreenOff
        self.onWakeUp = onWakeUp
        self.onHapticFeedback = onHapticFeedback
        self.onScreenOffAnnouncement = onScreenOffAnnouncement
    }

    // MARK: - Lifecycle & Gesture Handling (Testable Production Methods)

    /// Handles view appearance: dispatches the initial screen-off announcement.
    public func handleAppear() {
        A11yAnnouncer.shared.announceScreenOff(enabled: true)
        onScreenOffAnnouncement?(true)
    }

    /// Triggers wake-up sequence: double-tap haptic pulse, deactivation announcement, and onWakeUp callback.
    public func wakeUp() {
        HapticManager.shared.doubleTap()
        onHapticFeedback?()
        A11yAnnouncer.shared.announceScreenOff(enabled: false)
        onScreenOffAnnouncement?(false)
        onWakeUp()
    }

    /// Simulates/executes double-tap gesture.
    public func performDoubleTapGesture() {
        wakeUp()
    }

    /// Simulates/executes two-finger scrub escape gesture.
    public func performAccessibilityEscape() {
        wakeUp()
    }

    // MARK: - View Body

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
                performDoubleTapGesture()
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Self.accessibilityLabelText)
            .accessibilityAddTraits(Self.accessibilityTraitsSet)
            .accessibilitySpeechLanguage(A11yLabels.speechLanguage)
            .accessibilityAction {
                wakeUp()
            }
            .accessibilityAction(.escape) {
                performAccessibilityEscape()
            }
            .onAppear {
                handleAppear()
            }
    }
}
