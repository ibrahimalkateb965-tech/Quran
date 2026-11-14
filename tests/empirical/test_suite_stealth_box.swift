//
//  test_suite_stealth_box.swift
//  Empirical Test Harness: StealthBoxModifier & handleReplayAction()
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

print("================================================================================")
print("  EMPIRICAL SUITE 1: StealthBoxModifier & handleReplayAction() Stress Tests")
print("================================================================================")

// Instrumentable Mock for StealthBox Replay Logic matching StealthBoxModifier.swift
class StealthBoxHarness {
    var isPlaying: Bool
    var surahName: String?
    var ayahNumber: Int32?
    var ayahCount: Int32?
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
        isVoiceOverRunning: Bool = false
    ) {
        self.isPlaying = isPlaying
        self.surahName = surahName
        self.ayahNumber = ayahNumber
        self.ayahCount = ayahCount
        self.isVoiceOverRunning = isVoiceOverRunning
    }

    // Dynamic Arabic accessibility label identical to StealthBoxModifier.swift:49-64
    var surahAyahAccessibilityLabel: String {
        let surah = surahName?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let surah = surah, !surah.isEmpty {
            if let ayah = ayahNumber, let total = ayahCount, total > 0 {
                return "سورة \(surah)، الآية \(ayah) من أصل \(total)"
            } else if let ayah = ayahNumber {
                return "سورة \(surah)، الآية \(ayah)"
            } else {
                return "سورة \(surah)"
            }
        } else if let ayah = ayahNumber {
            return "الآية \(ayah)"
        } else {
            return "تلاوة القرآن الكريم"
        }
    }

    // Context-sensitive hint identical to StealthBoxModifier.swift:88
    var accessibilityHintText: String {
        isVoiceOverRunning ? "تشغيل أو إيقاف" : "تشغيل أو إيقاف التلاوة"
    }

    // Replay action identical to StealthBoxModifier.swift:67-75
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
}

// -----------------------------------------------------------------------------
// TEST CASE 1: Idle Uninitialized State (surahName == nil && ayahNumber == nil && !isPlaying)
// -----------------------------------------------------------------------------
print("Test Group 1: Idle uninitialized state guard verification")
let idleHarness = StealthBoxHarness(isPlaying: false, surahName: nil, ayahNumber: nil, ayahCount: nil)
idleHarness.handleReplayAction()

assertFalse(idleHarness.replayCallCount > 0, "IdleState_ReplayBlocked", detail: "onReplay() was invoked when track is nil and stopped")
assertFalse(idleHarness.hapticCallCount > 0, "IdleState_HapticsBlocked", detail: "performReplayHaptics() was invoked when track is nil and stopped")
assertTrue(idleHarness.replayCallCount == 0 && idleHarness.hapticCallCount == 0, "IdleState_BothReplayAndHapticsAborted")

// -----------------------------------------------------------------------------
// TEST CASE 2: Positive States (Guard Allows Replay)
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
// TEST CASE 3: Accessibility Label Null-Safety Matrix
// -----------------------------------------------------------------------------
print("Test Group 3: Null-safe label formatting matrix")

let label1 = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1, ayahCount: 7).surahAyahAccessibilityLabel
assertTrue(label1 == "سورة الفاتحة، الآية 1 من أصل 7", "Label_FullMetadata", detail: "Got: \(label1)")

let label2 = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: 1, ayahCount: nil).surahAyahAccessibilityLabel
assertTrue(label2 == "سورة الفاتحة، الآية 1", "Label_NoAyahCount", detail: "Got: \(label2)")

let label3 = StealthBoxHarness(isPlaying: true, surahName: "الفاتحة", ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertTrue(label3 == "سورة الفاتحة", "Label_SurahOnly", detail: "Got: \(label3)")

let label4 = StealthBoxHarness(isPlaying: true, surahName: nil, ayahNumber: 5, ayahCount: nil).surahAyahAccessibilityLabel
assertTrue(label4 == "الآية 5", "Label_AyahOnly", detail: "Got: \(label4)")

let label5 = StealthBoxHarness(isPlaying: false, surahName: nil, ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertTrue(label5 == "تلاوة القرآن الكريم", "Label_NilFallback", detail: "Got: \(label5)")

let label6 = StealthBoxHarness(isPlaying: false, surahName: "   ", ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertTrue(label6 == "تلاوة القرآن الكريم", "Label_WhitespaceFallback", detail: "Got: \(label6)")

let label7 = StealthBoxHarness(isPlaying: false, surahName: "", ayahNumber: nil, ayahCount: nil).surahAyahAccessibilityLabel
assertTrue(label7 == "تلاوة القرآن الكريم", "Label_EmptyFallback", detail: "Got: \(label7)")

// -----------------------------------------------------------------------------
// TEST CASE 4: VoiceOver Reactive Hint Switching
// -----------------------------------------------------------------------------
print("Test Group 4: Context-sensitive accessibilityHint switching")

let voOnHarness = StealthBoxHarness(isPlaying: false, isVoiceOverRunning: true)
assertTrue(voOnHarness.accessibilityHintText == "تشغيل أو إيقاف", "Hint_VoiceOverRunning", detail: "Got: \(voOnHarness.accessibilityHintText)")

let voOffHarness = StealthBoxHarness(isPlaying: false, isVoiceOverRunning: false)
assertTrue(voOffHarness.accessibilityHintText == "تشغيل أو إيقاف التلاوة", "Hint_VoiceOverStopped", detail: "Got: \(voOffHarness.accessibilityHintText)")

// -----------------------------------------------------------------------------
// TEST CASE 5: Stress Burst Testing
// -----------------------------------------------------------------------------
print("Test Group 5: Rapid repeated invocations (1000 iterations)")

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

print("\nSUITE 1 RESULTS: \(passedCount) passed, \(failedCount) failed.")
if failedCount > 0 {
    exit(1)
}
