//
//  test_suite_rtl_drag.swift
//  Empirical Test Harness: RTL Drag Semantics, 30pt Deadband & Boundary Protection
//

import Foundation
import CoreGraphics
import SwiftUI

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
print("  EMPIRICAL SUITE 4: RTL Drag Semantics, Thresholds & Boundary Guards")
print("================================================================================")

class RTLDragControllerHarness {
    var currentIndex: Int = 0
    var totalAyahs: Int = 7 // e.g. Al-Fatihah
    var currentSurahId: Int32 = 1
    var isContinuousPlayEnabled: Bool = false

    var nextAyahCalls = 0
    var prevAyahCalls = 0
    var surahCompletedCalls = 0
    var surahLoadedCalls: [(surahId: Int32, startAyah: Int32)] = []

    func reset(currentIndex: Int = 0, totalAyahs: Int = 7, surahId: Int32 = 1, continuous: Bool = false) {
        self.currentIndex = currentIndex
        self.totalAyahs = totalAyahs
        self.currentSurahId = surahId
        self.isContinuousPlayEnabled = continuous
        self.nextAyahCalls = 0
        self.prevAyahCalls = 0
        self.surahCompletedCalls = 0
        self.surahLoadedCalls.removeAll()
    }

    // Mirroring DragGesture .onEnded in QuranPlayerScreen.swift:88-99
    func handleDrag(translationWidth: CGFloat) {
        if abs(translationWidth) > 30 {
            if translationWidth > 0 {
                // In RTL, dragging rightwards advances to next Ayah
                playNextAyah()
            } else {
                // In RTL, dragging leftwards returns to previous Ayah
                playPreviousAyah()
            }
        }
    }

    // Mirroring accessibilityScrollAction in StealthBoxModifier.swift:95-101
    func handleAccessibilityScroll(edge: Edge) {
        if edge == .leading {
            playNextAyah()
        } else if edge == .trailing {
            playPreviousAyah()
        }
    }

    // Mirroring playNextAyah() in QuranPlayerScreen.swift:266-282
    func playNextAyah() {
        nextAyahCalls += 1
        if currentIndex < totalAyahs - 1 {
            currentIndex += 1
        } else if isContinuousPlayEnabled {
            let nextSurahId = currentSurahId + 1
            if nextSurahId <= 114 {
                currentSurahId = nextSurahId
                currentIndex = 0
                surahLoadedCalls.append((nextSurahId, 1))
            } else {
                surahCompletedCalls += 1
            }
        } else {
            surahCompletedCalls += 1
        }
    }

    // Mirroring playPreviousAyah() in QuranPlayerScreen.swift:284-289
    func playPreviousAyah() {
        prevAyahCalls += 1
        if currentIndex > 0 {
            currentIndex -= 1
        }
    }
}

let harness = RTLDragControllerHarness()

// -----------------------------------------------------------------------------
// TEST CASE 1: 30pt Deadband Threshold Stress Testing
// -----------------------------------------------------------------------------
print("Test Group 1: 30pt Deadband Threshold Boundary Matrix")

// Values <= 30.0 MUST NOT trigger navigation
let deadbandValues: [CGFloat] = [-30.0, -29.9, -15.0, -5.0, 0.0, 5.0, 15.0, 29.9, 30.0]
for width in deadbandValues {
    harness.reset(currentIndex: 2)
    harness.handleDrag(translationWidth: width)
    assertTrue(harness.nextAyahCalls == 0 && harness.prevAyahCalls == 0 && harness.currentIndex == 2,
               "Deadband_Suppressed_\(width)pt",
               detail: "Width \(width) triggered action unexpectedly")
}

// -----------------------------------------------------------------------------
// TEST CASE 2: RTL Drag Directional Semantics
// -----------------------------------------------------------------------------
print("Test Group 2: RTL Direction Mapping (Right = Next, Left = Prev)")

// Drag Right (+31pt, +50pt, +120pt) MUST advance to NEXT Ayah
let rightDragValues: [CGFloat] = [30.1, 31.0, 50.0, 150.0, 300.0]
for width in rightDragValues {
    harness.reset(currentIndex: 2)
    harness.handleDrag(translationWidth: width)
    assertTrue(harness.nextAyahCalls == 1, "RightDrag_\(Int(width))pt_CallsNext")
    assertTrue(harness.prevAyahCalls == 0, "RightDrag_\(Int(width))pt_ZeroPrev")
    assertTrue(harness.currentIndex == 3, "RightDrag_\(Int(width))pt_AdvancesIndex")
}

// Drag Left (-31pt, -50pt, -120pt) MUST return to PREVIOUS Ayah
let leftDragValues: [CGFloat] = [-30.1, -31.0, -50.0, -150.0, -300.0]
for width in leftDragValues {
    harness.reset(currentIndex: 2)
    harness.handleDrag(translationWidth: width)
    assertTrue(harness.prevAyahCalls == 1, "LeftDrag_\(Int(abs(width)))pt_CallsPrev")
    assertTrue(harness.nextAyahCalls == 0, "LeftDrag_\(Int(abs(width)))pt_ZeroNext")
    assertTrue(harness.currentIndex == 1, "LeftDrag_\(Int(abs(width)))pt_DecrementsIndex")
}

// -----------------------------------------------------------------------------
// TEST CASE 3: Boundary Underflow & Overflow Protection
// -----------------------------------------------------------------------------
print("Test Group 3: Boundary Underflow & Overflow Guards")

// Underflow test: At first Ayah (index 0), drag left
harness.reset(currentIndex: 0)
harness.handleDrag(translationWidth: -50)
assertTrue(harness.prevAyahCalls == 1, "Underflow_MethodInvoked")
assertTrue(harness.currentIndex == 0, "Underflow_ClampedAtZero", detail: "Index was \(harness.currentIndex)")

// Overflow test 1: At last Ayah (index 6 of 7), Continuous Play DISABLED
harness.reset(currentIndex: 6, totalAyahs: 7, continuous: false)
harness.handleDrag(translationWidth: +50)
assertTrue(harness.nextAyahCalls == 1, "OverflowDisabled_MethodInvoked")
assertTrue(harness.currentIndex == 6, "OverflowDisabled_ClampedAtLastIndex")
assertTrue(harness.surahCompletedCalls == 1, "OverflowDisabled_AnnouncesCompletion")

// Overflow test 2: At last Ayah (index 6 of 7), Continuous Play ENABLED (Surah 1 -> Surah 2)
harness.reset(currentIndex: 6, totalAyahs: 7, surahId: 1, continuous: true)
harness.handleDrag(translationWidth: +50)
assertTrue(harness.surahLoadedCalls.count == 1, "OverflowEnabled_LoadsNextSurah")
assertTrue(harness.surahLoadedCalls.first?.surahId == 2, "OverflowEnabled_AdvancesSurahId")
assertTrue(harness.currentIndex == 0, "OverflowEnabled_ResetsToFirstAyah")

// Overflow test 3: At last Ayah of Surah 114 (An-Nas), Continuous Play ENABLED -> Clamps
harness.reset(currentIndex: 5, totalAyahs: 6, surahId: 114, continuous: true)
harness.handleDrag(translationWidth: +50)
assertTrue(harness.surahCompletedCalls == 1, "Surah114_CompletedClamps")
assertTrue(harness.surahLoadedCalls.isEmpty, "Surah114_DoesNotLoad115")

// -----------------------------------------------------------------------------
// TEST CASE 4: VoiceOver Accessibility Scroll Action Semantics
// -----------------------------------------------------------------------------
print("Test Group 4: Accessibility Scroll Actions (.leading and .trailing)")

harness.reset(currentIndex: 2)
harness.handleAccessibilityScroll(edge: .leading)
assertTrue(harness.nextAyahCalls == 1 && harness.currentIndex == 3, "A11yScroll_LeadingAdvancesToNext")

harness.reset(currentIndex: 2)
harness.handleAccessibilityScroll(edge: .trailing)
assertTrue(harness.prevAyahCalls == 1 && harness.currentIndex == 1, "A11yScroll_TrailingReturnsToPrev")

print("\nSUITE 4 RESULTS: \(passedCount) passed, \(failedCount) failed.")
if failedCount > 0 {
    exit(1)
}
