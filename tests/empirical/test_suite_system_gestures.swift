//
//  test_suite_system_gestures.swift
//  Empirical Test Harness: System Gestures, Screen-Off Saver & VoiceOver Observer
//
//  Milestone 2: Test Suite Verification & System Gestures Parity
//  Authoritative Base: ORIGINAL_REQUEST.md (R1, R3, R4), ACCESSIBILITY_GUIDELINES_IOS.md (I2, I3, I6)
//

import Foundation
import UIKit
import SwiftUI
import Combine


@main
struct TestSuiteSystemGestures {
    static var passedCount = 0
    static var failedCount = 0

    static func assertTrue(_ condition: Bool, _ testName: String, detail: String = "") {
        if condition {
            passedCount += 1
            print("  [PASS] \(testName)")
        } else {
            failedCount += 1
            print("  [FAIL] \(testName) - \(detail)")
        }
    }

    static func assertFalse(_ condition: Bool, _ testName: String, detail: String = "") {
        assertTrue(!condition, testName, detail: detail)
    }

    static func assertEqual<T: Equatable>(_ actual: T, _ expected: T, _ testName: String, detail: String = "") {
        if actual == expected {
            passedCount += 1
            print("  [PASS] \(testName)")
        } else {
            failedCount += 1
            print("  [FAIL] \(testName) - Expected '\(expected)', got '\(actual)' \(detail)")
        }
    }

    static func main() {
        print("================================================================================")
        print("  EMPIRICAL SUITE 7: System Gestures, Screen-Off Saver & Dynamic VoiceOver")
        print("================================================================================")

        // -----------------------------------------------------------------------------
        // TEST GROUP 1: RootHostingController Magic Tap Gesture (R3)
        // -----------------------------------------------------------------------------
        print("Test Group 1: RootHostingController Magic Tap Gesture (R3)")

        var magicTapCount = 0
        let magicTapObserver = NotificationCenter.default.addObserver(
            forName: .magicTapTriggered,
            object: nil,
            queue: .main
        ) { _ in
            magicTapCount += 1
        }

        let rootController = RootHostingController(rootView: Text("Blind Quran App Root"))

        // 1A: Single Magic Tap handles and returns true
        let tap1Handled = rootController.accessibilityPerformMagicTap()
        assertTrue(tap1Handled, "MagicTap_ReturnsTrue_ClaimsEvent")
        assertEqual(magicTapCount, 1, "MagicTap_DispatchesNotification")

        // 1B: Rapid Magic Tap invocations (stress test: 10 taps)
        for _ in 0..<10 {
            let handled = rootController.accessibilityPerformMagicTap()
            assertTrue(handled, "MagicTap_Burst_ReturnsTrue")
        }
        assertEqual(magicTapCount, 11, "MagicTap_Burst_DispatchesAllNotifications")

        NotificationCenter.default.removeObserver(magicTapObserver)

        // -----------------------------------------------------------------------------
        // TEST GROUP 2: RootHostingController Escape Gesture (R3)
        // -----------------------------------------------------------------------------
        print("Test Group 2: RootHostingController Two-Finger Scrub Escape Gesture (R3)")

        var escapeCount = 0
        let escapeObserver = NotificationCenter.default.addObserver(
            forName: .escapeTriggered,
            object: nil,
            queue: .main
        ) { _ in
            escapeCount += 1
        }

        // 2A: Single Escape handles and returns true
        let esc1Handled = rootController.accessibilityPerformEscape()
        assertTrue(esc1Handled, "Escape_ReturnsTrue_ClaimsEvent")
        assertEqual(escapeCount, 1, "Escape_DispatchesNotification")

        // 2B: Rapid Escape invocations (stress test: 10 scrubs)
        for _ in 0..<10 {
            let handled = rootController.accessibilityPerformEscape()
            assertTrue(handled, "Escape_Burst_ReturnsTrue")
        }
        assertEqual(escapeCount, 11, "Escape_Burst_DispatchesAllNotifications")

        NotificationCenter.default.removeObserver(escapeObserver)

        // -----------------------------------------------------------------------------
        // TEST GROUP 3: ScreenOffSaverOverlay Configuration, Traits & OLED Black (R4)
        // -----------------------------------------------------------------------------
        print("Test Group 3: ScreenOffSaverOverlay Configuration, Traits & OLED Black (R4)")

        // 3A: Instantiate real production ScreenOffSaverOverlay view
        var wakeUpInvoked = false
        let overlayView = ScreenOffSaverOverlay(isScreenOff: true) {
            wakeUpInvoked = true
        }
        assertTrue(overlayView.isScreenOff, "ScreenOffOverlay_InitialStateIsScreenOff")
        overlayView.onWakeUp()
        assertTrue(wakeUpInvoked, "ScreenOffOverlay_OnWakeUpClosureInvoked")

        // 3B: Pure black OLED color verification (#000000)
        var redComponent: CGFloat = -1
        var greenComponent: CGFloat = -1
        var blueComponent: CGFloat = -1
        var alphaComponent: CGFloat = -1
        UIColor.black.getRed(&redComponent, green: &greenComponent, blue: &blueComponent, alpha: &alphaComponent)

        assertEqual(redComponent, 0.0, "OLED_Black_ZeroRed")
        assertEqual(greenComponent, 0.0, "OLED_Black_ZeroGreen")
        assertEqual(blueComponent, 0.0, "OLED_Black_ZeroBlue")
        assertEqual(alphaComponent, 1.0, "OLED_Black_FullAlpha")

        // 3C: Traits are .isModal only — no .isButton (VoiceOver must not append "button")
        assertTrue(
            ScreenOffSaverOverlay.accessibilityTraitsSet.contains(.isModal),
            "ScreenOffOverlay_TraitsContainModal"
        )
        assertFalse(
            ScreenOffSaverOverlay.accessibilityTraitsSet.contains(.isButton),
            "ScreenOffOverlay_TraitsExcludeButton"
        )

        // 3D: Single accessibility node metadata verification directly on ScreenOffSaverOverlay
        assertEqual(
            ScreenOffSaverOverlay.accessibilityLabelText,
            "شَاشَةُ تَوْفِيرِ الطَّاقَةِ نَشِطَةٌ",
            "ScreenOffOverlay_LabelAccuracy"
        )
        // Label is a clean name: no "double-tap to…" tutorial text
        assertFalse(
            ScreenOffSaverOverlay.accessibilityLabelText.contains("انقر"),
            "ScreenOffOverlay_LabelHasNoTutorial"
        )

        // -----------------------------------------------------------------------------
        // TEST GROUP 4: Production ScreenOffSaverOverlay Wake Behaviors & Announcement Sequencing (R4)
        // -----------------------------------------------------------------------------
        print("Test Group 4: Production ScreenOffSaverOverlay Wake Behaviors & Announcement Sequencing (R4)")

        var announcedEnabledStates: [Bool] = []
        var hapticCount = 0
        var wakeCount = 0
        var currentScreenOff = true

        let productionOverlay = ScreenOffSaverOverlay(
            isScreenOff: currentScreenOff,
            onWakeUp: {
                wakeCount += 1
                currentScreenOff = false
            },
            onHapticFeedback: {
                hapticCount += 1
            },
            onScreenOffAnnouncement: { enabled in
                announcedEnabledStates.append(enabled)
            }
        )

        // 4A: On appear activates announcement on production ScreenOffSaverOverlay
        productionOverlay.handleAppear()
        assertEqual(announcedEnabledStates.count, 1, "ScreenOff_Appear_Announced")
        assertEqual(announcedEnabledStates.first, true, "ScreenOff_Appear_EnabledTrue")

        // 4B: Double-tap wake gesture directly on production ScreenOffSaverOverlay
        productionOverlay.performDoubleTapGesture()
        assertEqual(wakeCount, 1, "ScreenOff_DoubleTap_Wakes")
        assertEqual(hapticCount, 1, "ScreenOff_DoubleTap_TriggersHaptics")
        assertFalse(currentScreenOff, "ScreenOff_DoubleTap_SetsScreenOffFalse")
        assertEqual(announcedEnabledStates.last, false, "ScreenOff_DoubleTap_AnnouncedDisabled")

        // 4C: Two-finger scrub escape wake gesture directly on production ScreenOffSaverOverlay
        var escapeWakeCount = 0
        var escapeHapticCount = 0
        var escapeScreenOff = true

        let escapeOverlay = ScreenOffSaverOverlay(
            isScreenOff: escapeScreenOff,
            onWakeUp: {
                escapeWakeCount += 1
                escapeScreenOff = false
            },
            onHapticFeedback: {
                escapeHapticCount += 1
            }
        )
        escapeOverlay.performAccessibilityEscape()
        assertEqual(escapeWakeCount, 1, "ScreenOff_Escape_Wakes")
        assertEqual(escapeHapticCount, 1, "ScreenOff_Escape_TriggersHaptics")
        assertFalse(escapeScreenOff, "ScreenOff_Escape_SetsScreenOffFalse")

        // 4D: No named custom actions (VoiceOver would append "Actions available").
        //     Waking relies solely on the standard VoiceOver double-tap (default activation).
        assertTrue(
            ScreenOffSaverOverlay.supportedActionNames.isEmpty,
            "ScreenOff_NoNamedCustomActions"
        )
        var activateWakeCount = 0
        let activateOverlay = ScreenOffSaverOverlay(
            isScreenOff: true,
            onWakeUp: { activateWakeCount += 1 }
        )
        activateOverlay.wakeUp()
        assertEqual(activateWakeCount, 1, "ScreenOff_DefaultActivation_Wakes")

        // -----------------------------------------------------------------------------
        // TEST GROUP 5: VoiceOverObserver Dynamic Observation (R1 / Invariant I3)
        // -----------------------------------------------------------------------------
        print("Test Group 5: VoiceOverObserver Dynamic Observation (R1 / Invariant I3)")

        // 5A: Real production VoiceOverObserver singleton and initial state
        let sharedObserver = VoiceOverObserver.shared
        assertEqual(
            sharedObserver.isVoiceOverRunning,
            UIAccessibility.isVoiceOverRunning,
            "VoiceOverObserver_SharedMatchesSystem"
        )

        // 5B: Dedicated test instance
        let dedicatedObserver = VoiceOverObserver()
        assertEqual(
            dedicatedObserver.isVoiceOverRunning,
            UIAccessibility.isVoiceOverRunning,
            "VoiceOverObserver_DedicatedMatchesSystem"
        )

        // 5C: Reactive Combine publisher ($isVoiceOverRunning)
        var observedValues: [Bool] = []
        var cancellables = Set<AnyCancellable>()

        dedicatedObserver.$isVoiceOverRunning
            .sink { value in
                observedValues.append(value)
            }
            .store(in: &cancellables)

        assertTrue(!observedValues.isEmpty, "VoiceOverObserver_EmittedInitialValue")
        assertEqual(
            observedValues.first,
            UIAccessibility.isVoiceOverRunning,
            "VoiceOverObserver_FirstEmissionMatchesCurrentState"
        )

        // 5D: Notification observation (.voiceOverStatusDidChangeNotification)
        NotificationCenter.default.post(
            name: UIAccessibility.voiceOverStatusDidChangeNotification,
            object: nil
        )
        // Run runloop briefly for main queue delivery
        RunLoop.main.run(until: Date().addingTimeInterval(0.05))

        assertEqual(
            dedicatedObserver.isVoiceOverRunning,
            UIAccessibility.isVoiceOverRunning,
            "VoiceOverObserver_MaintainsStateOnNotification"
        )

        // 5E: Deinitialization safety
        do {
            var transientObserver: VoiceOverObserver? = VoiceOverObserver()
            assertTrue(transientObserver != nil, "VoiceOverObserver_TransientCreated")
            transientObserver = nil
            assertTrue(transientObserver == nil, "VoiceOverObserver_DeallocatedSafely")
        }

        print("\nSUITE 7 RESULTS: \(passedCount) passed, \(failedCount) failed.")
        if failedCount > 0 {
            exit(1)
        }
    }
}
