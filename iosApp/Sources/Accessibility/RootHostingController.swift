//
//  RootHostingController.swift
//  QuranBlind
//
//  Milestone 2: Test Suite Verification & System Gestures Parity
//  Requirement R3: System Gestures (Magic Tap & Escape)
//  Authoritative Base: ACCESSIBILITY_GUIDELINES_IOS.md Section 8 (System Gestures)
//

import UIKit
import SwiftUI

// MARK: - Notification Names for System Accessibility Gestures (R3)

public extension Notification.Name {
    static let magicTapTriggered = Notification.Name("quranblind_magic_tap_triggered")
    static let escapeTriggered = Notification.Name("quranblind_escape_triggered")
}

// MARK: - Root Hosting Controller Subclass (Magic Tap & Escape - R3)

/// Custom UIHostingController subclass serving as the root accessibility host controller.
/// Overrides system accessibility gesture handlers to dispatch global application actions:
/// - `accessibilityPerformMagicTap()`: Universal Play/Pause toggle across the entire application (R3).
/// - `accessibilityPerformEscape()`: Universal two-finger scrub ("Z" gesture) to dismiss modal bottom sheets & overlays (R3).
open class RootHostingController<Content: View>: UIHostingController<Content> {

    // MARK: - Magic Tap (Two-Finger Double Tap)

    public override func accessibilityPerformMagicTap() -> Bool {
        NotificationCenter.default.post(name: .magicTapTriggered, object: nil)
        return true
    }

    // MARK: - Escape (Two-Finger Scrub / "Z" Gesture)

    public override func accessibilityPerformEscape() -> Bool {
        NotificationCenter.default.post(name: .escapeTriggered, object: nil)
        return true
    }
}

// MARK: - RootHostingView (SwiftUI Lifecycle Bridge)

/// SwiftUI bridge embedding `RootHostingController` into the active application scene hierarchy.
/// Ensures `RootHostingController` is genuinely instantiated and linked in the responder chain.
public struct RootHostingView<Content: View>: UIViewControllerRepresentable {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public func makeUIViewController(context: Context) -> RootHostingController<Content> {
        let controller = RootHostingController(rootView: content)
        NotificationCenter.default.post(
            name: Notification.Name("quranblind_root_controller_attached"),
            object: controller
        )
        return controller
    }

    public func updateUIViewController(_ uiViewController: RootHostingController<Content>, context: Context) {
        uiViewController.rootView = content
    }
}
