//
//  ScreenOffSaverOverlay.swift
//  QuranBlind
//
//  1:1 Parity with Android ScreenOffSaverOverlay.kt
//

import SwiftUI

public struct ScreenOffSaverOverlay: View {

    // MARK: - Authoritative Accessibility Constants (R4 / Invariant I7)

    /// Accessibility label text announced when VoiceOver touches or focuses the screen-off saver.
    public static let accessibilityLabelText = "شاشة توفير الطاقة نشطة. انقر مرتين للاستيقاظ"

    /// Accessibility hint text guiding the blind user on waking the screen.
    public static let accessibilityHintText = "انقر مرتين لإلغاء وضع توفير الطاقة وإيقاظ الشاشة"

    /// Modal traits isolating the view hierarchy beneath the overlay.
    /// No `.isButton`: VoiceOver would append the English word "button" after the Arabic label.
    public static let accessibilityTraitsSet: AccessibilityTraits = [.isModal]

    /// Primary accessibility named action for wake.
    public static let wakeActionName = "استيقاظ"

    /// Secondary accessibility named action for cancelling screen-off mode.
    public static let cancelActionName = "إلغاء وضع إيقاف الشاشة"

    /// Supported accessibility custom actions list.
    public static let supportedActionNames: [String] = [wakeActionName, cancelActionName]

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

    /// Dispatches an accessibility named custom action. Returns true if recognized and handled.
    @discardableResult
    public func performNamedAction(_ name: String) -> Bool {
        switch name {
        case Self.wakeActionName, Self.cancelActionName:
            wakeUp()
            return true
        default:
            return false
        }
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
            .accessibilityAction {
                wakeUp()
            }
            .accessibilityAction(.escape) {
                performAccessibilityEscape()
            }
            .accessibilityAction(named: Self.wakeActionName) {
                _ = performNamedAction(Self.wakeActionName)
            }
            .accessibilityAction(named: Self.cancelActionName) {
                _ = performNamedAction(Self.cancelActionName)
            }
            .onAppear {
                handleAppear()
            }
    }
}
