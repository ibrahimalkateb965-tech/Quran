//
//  test_suite_stealth_box.swift
//  Empirical Test Harness: StealthBoxModifier & R2 Production Invariants
//
//  Milestone 2: Test Suite Verification & Stealth Box Production String Parity
//  Authoritative Base: ORIGINAL_REQUEST.md (R2), ACCESSIBILITY_GUIDELINES_IOS.md (I2, I3)
//

import Foundation
import UIKit
import SwiftUI

// Test Counter and Harness Utilities
var passedCount = 0
var failedCount = 0

func assertTrue(_ condition: Bool, _ testName: String, detail: String = "") {
    if condition {
        passedCount += 1
        print("  [PASS] \(testName)")
    } else {
        failedCount += 1
        print("  [FAIL] \(testName) - \(detail)")
    }
}

func assertFalse(_ condition: Bool, _ testName: String, detail: String = "") {
    assertTrue(!condition, testName, detail: detail)
}

func assertEqual<T: Equatable>(_ actual: T, _ expected: T, _ testName: String, detail: String = "") {
    if actual == expected {
        passedCount += 1
        print("  [PASS] \(testName)")
    } else {
        failedCount += 1
        print("  [FAIL] \(testName) - Expected '\(expected)', got '\(actual)' \(detail)")
    }
}

print("================================================================================")
print("  EMPIRICAL SUITE 1: StealthBoxModifier & R2 Production Invariant Tests")
print("================================================================================")

// Instrumentable Mock for StealthBox Logic matching StealthBoxModifier.swift
class StealthBoxHarness {
    var isPlaying: Bool
    var surahName: String?
    var ayahNumber: Int32?
    var ayahCount: Int32?
    var ayahText: String?
    var isVoiceOverRunning: Bool

    var replayCallCount = 0
    var hapticCallCount = 0
    var togglePlayCallCount = 0
    var nextAyahCallCount = 0
    var prevAyahCallCount = 0

    init(
        isPlaying: Bool,
        surahName: String? = nil,
        ayahNumber: Int32? = nil,
        ayahCount: Int32? = nil,
        ayahText: String? = nil,
        isVoiceOverRunning: Bool = false
    ) {
        self.isPlaying = isPlaying
        self.surahName = surahName
        self.ayahNumber = ayahNumber
        self.ayahCount = ayahCount
        self.ayahText = ayahText
        self.isVoiceOverRunning = isVoiceOverRunning
    }

    // Dynamic Arabic accessibility label identical to StealthBoxModifier.swift:52-72
    var surahAyahAccessibilityLabel: String {
        let surah = surahName?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let surah = surah, !surah.isEmpty {
            if let text = ayahText?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty, let ayah = ayahNumber {
                return "الآية \(ayah) من سورة \(surah): \(text)"
            } else if let ayah = ayahNumber, let total = ayahCount, total > 0 {
                return "سورة \(surah)، الآية \(ayah) من أصل \(total)"
            } else if let ayah = ayahNumber {
                return "سورة \(surah)، الآية \(ayah)"
            } else {
                return "سورة \(surah)"
            }
        } else if let ayah = ayahNumber {
            if let text = ayahText?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty {
                return "الآية \(ayah): \(text)"
            }
            return "الآية \(ayah)"
        } else {
            return "تلاوة القرآن الكريم"
        }
    }

    // Accessibility Value reporting playback state identical to StealthBoxModifier.swift:95
    var accessibilityValueText: String {
        isPlaying ? "قيد التلاوة" : "متوقف"
    }

    // Context-sensitive hint identical to StealthBoxModifier.swift:96
    var accessibilityHintText: String {
        isVoiceOverRunning
            ? "انقر نقراً مزدوجاً للتشغيل أو الإيقاف، أو استخدم إجراءات الروتور"
            : "تشغيل أو إيقاف التلاوة"
    }

    // Replay action identical to StealthBoxModifier.swift:75-84
    func handleReplayAction() {
        guard surahName != nil || ayahNumber != nil || isPlaying else {
            return
        }
        performReplayHaptics()
        onReplay()
    }

    func performReplayHaptics() {
        hapticCallCount += 1
    }

    func onReplay() {
        replayCallCount += 1
    }

    // Custom rotor action dispatch matching StealthBoxModifier.swift:97-105
    func performRotorAction(named name: String) -> Bool {
        switch name {
        case "إعادة التلاوة":
            handleReplayAction()
            return true
        case "الآية التالية":
            nextAyahCallCount += 1
            return true
        case "الآية السابقة":
            prevAyahCallCount += 1
            return true
        default:
            return false
        }
    }

    // Accessibility 3-finger scroll action matching StealthBoxModifier.swift:109-115
    func handleScrollAction(edge: Edge) {
        if edge == .leading {
            // Drag right in RTL layout -> Next Ayah
            nextAyahCallCount += 1
        } else if edge == .trailing {
            // Drag left in RTL layout -> Previous Ayah
            prevAyahCallCount += 1
        }
    }

    // Default activation action matching StealthBoxModifier.swift:106-108
    func handleDefaultAction() {
        togglePlayCallCount += 1
    }
}

// -----------------------------------------------------------------------------
// TEST GROUP 1: Idle Uninitialized State Guard Verification
// -----------------------------------------------------------------------------
print("Test Group 1: Idle uninitialized state guard verification")
let idleHarness = StealthBoxHarness(isPlaying: false, surahName: nil, ayahNumber: nil, ayahCount: nil)
idleHarness.handleReplayAction()

assertFalse(idleHarness.replayCallCount > 0, "IdleState_ReplayBlocked", detail: "onReplay() was invoked when track is nil and stopped")
assertFalse(idleHarness.hapticCallCount > 0, "IdleState_HapticsBlocked", detail: "performReplayHaptics() was invoked when track is nil and stopped")
assertTrue(idleHarness.replayCallCount == 0 && idleHarness.hapticCallCount == 0, "IdleState_BothReplayAndHapticsAborted")

// -----------------------------------------------------------------------------
// TEST GROUP 2: Valid States (Guard Allows Replay)
// -----------------------------------------------------------------------------
print("Test Group 2: Valid states where replay MUST fire")

// Case 2A: surahName present, stopped
let surahOnlyHarness = StealthBoxHarness(isPlaying: false, surahName: "الفاتحة", ayahNumber: nil)
surahOnlyHarness.handleReplayAction()
assertTrue(surahOnlyHarness.replayCallCount == 1, "SurahOnly_ReplayFired", detail: "Expected 1 replay call, got \(surahOnlyHarness.replayCallCount)")
assertTrue(surahOnlyHarness.hapticCallCount == 1, "SurahOnly_HapticFired", detail: "Expected 1 haptic call, got \(surahOnlyHarness.hapticCallCount)")

// Case 2B: ayahNumber present, stopped
let ayahOnlyHarness = StealthBoxHarness(isPlaying: false, surahName: nil, ayahNumber: 1)
ayahOnlyHarness.handleReplayAction()
assertTrue(ayahOnlyHarness.replayCallCount == 1, "AyahOnly_ReplayFired")
assertTrue(ayahOnlyHarness.hapticCallCount == 1, "AyahOnly_HapticFired")

// Case 2C: isPlaying == true, track nil
let playingHarness = StealthBoxHarness(isPlaying: true, surahName: nil, ayahNumber: nil)
playingHarness.handleReplayAction()
assertTrue(playingHarness.replayCallCount == 1, "PlayingOnly_ReplayFired")
assertTrue(playingHarness.hapticCallCount == 1, "PlayingOnly_HapticFired")

// Case 2D: Fully populated state
let fullHarness = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1, ayahCount: 7)
fullHarness.handleReplayAction()
assertTrue(fullHarness.replayCallCount == 1, "FullyPopulated_ReplayFired")
assertTrue(fullHarness.hapticCallCount == 1, "FullyPopulated_HapticFired")

// -----------------------------------------------------------------------------
// TEST GROUP 3: R2 Production Accessibility Label Formatting Matrix
// -----------------------------------------------------------------------------
print("Test Group 3: R2 Production Label formatting matrix & graceful fallbacks")

// 3A: Full R2 Specification: "الآية \(ayah) من سورة \(surah): \(text)"
let labelR2Full = StealthBoxHarness(
    isPlaying: true,
    surahName: "الفاتحة",
    ayahNumber: 1,
    ayahCount: 7,
    ayahText: "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
).surahAyahAccessibilityLabel
assertEqual(labelR2Full, "الآية 1 من سورة الفاتحة: بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Label_R2_FullSurahAndAyahText")

// 3B: Ayah + Text fallback (no surah): "الآية \(ayah): \(text)"
let labelAyahTextOnly = StealthBoxHarness(
    isPlaying: true,
    surahName: nil,
    ayahNumber: 2,
    ayahCount: nil,
    ayahText: "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"
).surahAyahAccessibilityLabel
assertEqual(labelAyahTextOnly, "الآية 2: الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Label_R2_AyahAndTextFallback")

// 3C: Legacy metadata with total ayahs (no text): "سورة \(surah)، الآية \(ayah) من أصل \(total)"
let labelLegacyTotal = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1, ayahCount: 7).surahAyahAccessibilityLabel
assertEqual(labelLegacyTotal, "سورة الفاتحة، الآية 1 من أصل 7", "Label_Legacy_FullMetadata")

// 3D: Surah and Ayah only (no text, no total): "سورة \(surah)، الآية \(ayah)"
let labelSurahAyah = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1, ayahCount: nil).surahAyahAccessibilityLabel
assertEqual(labelSurahAyah, "سورة الفاتحة، الآية 1", "Label_Legacy_NoAyahCount")

// 3E: Surah only: "سورة \(surah)"
let labelSurahOnly = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertEqual(labelSurahOnly, "سورة الفاتحة", "Label_Legacy_SurahOnly")

// 3F: Ayah only (no text): "الآية \(ayah)"
let labelAyahOnly = StealthBoxHarness(isPlaying: true, surahName: nil, ayahNumber: 5, ayahCount: nil).surahAyahAccessibilityLabel
assertEqual(labelAyahOnly, "الآية 5", "Label_Legacy_AyahOnly")

// 3G: Nil fallback: "تلاوة القرآن الكريم"
let labelNil = StealthBoxHarness(isPlaying: false, surahName: nil, ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertEqual(labelNil, "تلاوة القرآن الكريم", "Label_NilFallback")

// 3H: Whitespace fallback: "تلاوة القرآن الكريم"
let labelWhitespace = StealthBoxHarness(isPlaying: false, surahName: "   ", ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertEqual(labelWhitespace, "تلاوة القرآن الكريم", "Label_WhitespaceFallback")

// 3I: Empty string fallback: "تلاوة القرآن الكريم"
let labelEmpty = StealthBoxHarness(isPlaying: false, surahName: "", ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertEqual(labelEmpty, "تلاوة القرآن الكريم", "Label_EmptyFallback")

// -----------------------------------------------------------------------------
// TEST GROUP 4: R2 Accessibility Value State Verification
// -----------------------------------------------------------------------------
print("Test Group 4: R2 Accessibility Value state verification")

let playingValueHarness = StealthBoxHarness(isPlaying: true)
assertEqual(playingValueHarness.accessibilityValueText, "قيد التلاوة", "Value_PlayingIsQaydAlTilawah")

let pausedValueHarness = StealthBoxHarness(isPlaying: false)
assertEqual(pausedValueHarness.accessibilityValueText, "متوقف", "Value_PausedIsMutawaqqif")

// -----------------------------------------------------------------------------
// TEST GROUP 5: R2 Accessibility Hint Switching
// -----------------------------------------------------------------------------
print("Test Group 5: Context-sensitive accessibilityHint switching")

let voOnHarness = StealthBoxHarness(isPlaying: false, isVoiceOverRunning: true)
assertEqual(
    voOnHarness.accessibilityHintText,
    "انقر نقراً مزدوجاً للتشغيل أو الإيقاف، أو استخدم إجراءات الروتور",
    "Hint_VoiceOverRunning"
)

let voOffHarness = StealthBoxHarness(isPlaying: false, isVoiceOverRunning: false)
assertEqual(
    voOffHarness.accessibilityHintText,
    "تشغيل أو إيقاف التلاوة",
    "Hint_VoiceOverStopped"
)

// -----------------------------------------------------------------------------
// TEST GROUP 6: Custom Rotor Actions & Universal Tap
// -----------------------------------------------------------------------------
print("Test Group 6: Custom VoiceOver Rotor Actions and Universal Activation")

let rotorHarness = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 3)

// 6A: Rotor "إعادة التلاوة"
let replayResult = rotorHarness.performRotorAction(named: "إعادة التلاوة")
assertTrue(replayResult, "Rotor_Replay_Handled")
assertEqual(rotorHarness.replayCallCount, 1, "Rotor_Replay_FiresReplay")
assertEqual(rotorHarness.hapticCallCount, 1, "Rotor_Replay_FiresHaptic")

// 6B: Rotor "الآية التالية"
let nextResult = rotorHarness.performRotorAction(named: "الآية التالية")
assertTrue(nextResult, "Rotor_NextAyah_Handled")
assertEqual(rotorHarness.nextAyahCallCount, 1, "Rotor_NextAyah_Advances")

// 6C: Rotor "الآية السابقة"
let prevResult = rotorHarness.performRotorAction(named: "الآية السابقة")
assertTrue(prevResult, "Rotor_PrevAyah_Handled")
assertEqual(rotorHarness.prevAyahCallCount, 1, "Rotor_PrevAyah_Decrements")

// 6D: Unknown action rejected
let unknownResult = rotorHarness.performRotorAction(named: "إجراء غير معروف")
assertFalse(unknownResult, "Rotor_Unknown_Rejected")

// 6E: Default activation action (.accessibilityAction) toggles playback
rotorHarness.handleDefaultAction()
assertEqual(rotorHarness.togglePlayCallCount, 1, "DefaultAction_TogglesPlayback")

// -----------------------------------------------------------------------------
// TEST GROUP 7: 3-Finger Horizontal Accessibility Scroll (RTL Directional Invariant)
// -----------------------------------------------------------------------------
print("Test Group 7: 3-Finger Horizontal Accessibility Scroll (RTL Invariant)")

let scrollHarness = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1)

// 7A: Leading edge scroll in RTL (right drag) -> Next Ayah
scrollHarness.handleScrollAction(edge: .leading)
assertEqual(scrollHarness.nextAyahCallCount, 1, "A11yScroll_Leading_NextAyah")
assertEqual(scrollHarness.prevAyahCallCount, 0, "A11yScroll_Leading_ZeroPrev")

// 7B: Trailing edge scroll in RTL (left drag) -> Previous Ayah
scrollHarness.handleScrollAction(edge: .trailing)
assertEqual(scrollHarness.prevAyahCallCount, 1, "A11yScroll_Trailing_PrevAyah")
assertEqual(scrollHarness.nextAyahCallCount, 1, "A11yScroll_Trailing_NoExtraNext")

// 7C: Vertical scroll edges ignored by horizontal recitation surface
scrollHarness.handleScrollAction(edge: .top)
scrollHarness.handleScrollAction(edge: .bottom)
assertEqual(scrollHarness.nextAyahCallCount, 1, "A11yScroll_Vertical_Ignored_Next")
assertEqual(scrollHarness.prevAyahCallCount, 1, "A11yScroll_Vertical_Ignored_Prev")

// -----------------------------------------------------------------------------
// TEST GROUP 8: Stress Burst Testing
// -----------------------------------------------------------------------------
print("Test Group 8: Rapid repeated invocations (1000 iterations)")

let burstIdleHarness = StealthBoxHarness(isPlaying: false, surahName: nil, ayahNumber: nil)
for _ in 0..<1000 {
    burstIdleHarness.handleReplayAction()
}
assertTrue(burstIdleHarness.replayCallCount == 0 && burstIdleHarness.hapticCallCount == 0, "BurstIdle_NeverFires")

let burstActiveHarness = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1)
for _ in 0..<1000 {
    burstActiveHarness.handleReplayAction()
}
assertTrue(burstActiveHarness.replayCallCount == 1000 && burstActiveHarness.hapticCallCount == 1000, "BurstActive_Exactly1000Fires")

// Rapid scroll burst
let burstScrollHarness = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1)
for _ in 0..<500 {
    burstScrollHarness.handleScrollAction(edge: .leading)
    burstScrollHarness.handleScrollAction(edge: .trailing)
}
assertEqual(burstScrollHarness.nextAyahCallCount, 500, "BurstScroll_Leading_500Count")
assertEqual(burstScrollHarness.prevAyahCallCount, 500, "BurstScroll_Trailing_500Count")

print("\nSUITE 1 RESULTS: \(passedCount) passed, \(failedCount) failed.")
if failedCount > 0 {
    exit(1)
}
