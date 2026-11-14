//
//  test_suite_gestures.swift
//  Empirical Test Harness: Tap Gesture Disambiguation & Haptic Pulse Deduplication
//

import Foundation
import UIKit
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
print("  EMPIRICAL SUITE 2: Tap Gesture Disambiguation & Haptic Deduplication")
print("================================================================================")

// State Machine Simulating SwiftUI ExclusiveGesture<TapGesture, TapGesture>
// First = TapGesture(count: 2)
// Second = TapGesture(count: 1)
class ExclusiveTapGestureSimulator {
    enum GestureResult {
        case doubleTapFirst
        case singleTapSecond
    }

    private let doubleTapTimeout: TimeInterval = 0.25 // 250ms window
    private var lastTapTime: TimeInterval? = nil
    private var tapCount: Int = 0

    // Feed a sequence of tap timestamps and return the recognized gestures
    func processTapStream(timestamps: [TimeInterval]) -> [GestureResult] {
        var results: [GestureResult] = []
        var i = 0

        while i < timestamps.count {
            let current = timestamps[i]
            if i + 1 < timestamps.count {
                let next = timestamps[i + 1]
                if (next - current) <= doubleTapTimeout {
                    // Double tap matched exclusively before single tap!
                    // Single tap is excluded and cancelled.
                    results.append(.doubleTapFirst)
                    i += 2
                    continue
                }
            }
            // If next tap is outside the window or this is the last tap, single tap resolves
            results.append(.singleTapSecond)
            i += 1
        }

        return results
    }
}

// -----------------------------------------------------------------------------
// TEST CASE 1: Tap Gesture Disambiguation
// -----------------------------------------------------------------------------
print("Test Group 1: ExclusiveGesture Disambiguation")

let sim = ExclusiveTapGestureSimulator()

// Scenario 1: Double Tap (two taps at t=0.0s and t=0.12s)
let doubleTapResults = sim.processTapStream(timestamps: [0.0, 0.12])
assertTrue(doubleTapResults.count == 1, "DoubleTap_RecognizedSingleEvent", detail: "Expected 1 event, got \(doubleTapResults.count)")
assertTrue(doubleTapResults.first == .doubleTapFirst, "DoubleTap_ProducedFirstAction", detail: "Produced: \(String(describing: doubleTapResults.first))")

// Scenario 2: Single Tap (one tap at t=0.0s)
let singleTapResults = sim.processTapStream(timestamps: [0.0])
assertTrue(singleTapResults.count == 1, "SingleTap_RecognizedSingleEvent")
assertTrue(singleTapResults.first == .singleTapSecond, "SingleTap_ProducedSecondAction", detail: "Produced: \(String(describing: singleTapResults.first))")

// Scenario 3: Two Distinct Single Taps (t=0.0s and t=0.60s - exceeds 250ms window)
let twoDistinctSingleTaps = sim.processTapStream(timestamps: [0.0, 0.60])
assertTrue(twoDistinctSingleTaps.count == 2, "DistinctTaps_RecognizedTwoEvents")
assertTrue(twoDistinctSingleTaps == [.singleTapSecond, .singleTapSecond], "DistinctTaps_BothAreSingleTaps")

// Scenario 4: Double Tap followed by Single Tap (t=0.0s, t=0.10s, t=0.60s)
let doubleThenSingle = sim.processTapStream(timestamps: [0.0, 0.10, 0.60])
assertTrue(doubleThenSingle.count == 2, "DoubleThenSingle_TwoEvents")
assertTrue(doubleThenSingle == [.doubleTapFirst, .singleTapSecond], "DoubleThenSingle_OrderAndTypesMatch")

// -----------------------------------------------------------------------------
// TEST CASE 2: Haptic Pulse Deduplication & Action Isolation
// -----------------------------------------------------------------------------
print("Test Group 2: Haptic Pulse Deduplication Verification")

class PlayerScreenHapticsHarness {
    var totalHapticPulses = 0
    var playbackToggledCount = 0
    var ayahsReplayedCount = 0
    var isPlaying = false

    func togglePlayback() {
        isPlaying.toggle()
        playbackToggledCount += 1
    }

    // Mirroring QuranPlayerScreen.swift:257-264
    func replayCurrentAyah(withHaptics: Bool = true) {
        if withHaptics {
            triggerPhysicalHaptic()
        }
        ayahsReplayedCount += 1
    }

    // Mirroring StealthBoxModifier.swift:77-81
    func handleRotorReplayAction(surahName: String?, ayahNumber: Int32?) {
        guard surahName != nil || ayahNumber != nil || isPlaying else {
            return
        }
        triggerPhysicalHaptic()
        // stealthBox passes withHaptics: false (QuranPlayerScreen.swift:83)
        replayCurrentAyah(withHaptics: false)
    }

    // Mirroring ExclusiveGesture .onEnded in QuranPlayerScreen.swift:104-111
    func handleRecognizedGesture(_ gesture: ExclusiveTapGestureSimulator.GestureResult) {
        switch gesture {
        case .doubleTapFirst:
            replayCurrentAyah(withHaptics: true)
        case .singleTapSecond:
            togglePlayback()
        }
    }

    func triggerPhysicalHaptic() {
        totalHapticPulses += 1
    }
}

// Subtest 2A: VoiceOver Rotor Replay produces EXACTLY ONE haptic pulse
let rotorHarness = PlayerScreenHapticsHarness()
rotorHarness.handleRotorReplayAction(surahName: "الفاتحة", ayahNumber: 1)
assertTrue(rotorHarness.ayahsReplayedCount == 1, "RotorReplay_AyahReplayedOnce")
assertTrue(rotorHarness.totalHapticPulses == 1, "RotorReplay_ExactlyOneHapticPulse", detail: "Got \(rotorHarness.totalHapticPulses) haptics (expected 1, previously bugged at 2)")
assertTrue(rotorHarness.playbackToggledCount == 0, "RotorReplay_PlaybackNotToggled")

// Subtest 2B: Sighted User Double-Tap produces EXACTLY ONE haptic pulse and does NOT toggle playback
let sightedDoubleTapHarness = PlayerScreenHapticsHarness()
sightedDoubleTapHarness.handleRecognizedGesture(.doubleTapFirst)
assertTrue(sightedDoubleTapHarness.ayahsReplayedCount == 1, "SightedDoubleTap_AyahReplayedOnce")
assertTrue(sightedDoubleTapHarness.totalHapticPulses == 1, "SightedDoubleTap_ExactlyOneHapticPulse", detail: "Got \(sightedDoubleTapHarness.totalHapticPulses) haptics")
assertTrue(sightedDoubleTapHarness.playbackToggledCount == 0, "SightedDoubleTap_PlaybackNotToggled", detail: "Double-tap MUST NOT toggle play/pause")

// Subtest 2C: Sighted User Single-Tap toggles playback and produces ZERO haptics
let sightedSingleTapHarness = PlayerScreenHapticsHarness()
sightedSingleTapHarness.handleRecognizedGesture(.singleTapSecond)
assertTrue(sightedSingleTapHarness.playbackToggledCount == 1, "SightedSingleTap_PlaybackToggled")
assertTrue(sightedSingleTapHarness.ayahsReplayedCount == 0, "SightedSingleTap_AyahNotReplayed")
assertTrue(sightedSingleTapHarness.totalHapticPulses == 0, "SightedSingleTap_ZeroHaptics")

// Subtest 2D: Idle Track Rotor Replay produces ZERO haptic pulses and ZERO replays
let idleRotorHarness = PlayerScreenHapticsHarness()
idleRotorHarness.handleRotorReplayAction(surahName: nil, ayahNumber: nil)
assertTrue(idleRotorHarness.totalHapticPulses == 0, "IdleRotor_ZeroHaptics")
assertTrue(idleRotorHarness.ayahsReplayedCount == 0, "IdleRotor_ZeroReplays")

// -----------------------------------------------------------------------------
// TEST CASE 3: Stress Testing Gesture Disambiguation Under Rapid Touch Bursts
// -----------------------------------------------------------------------------
print("Test Group 3: Rapid Touch Bursts Stress Tests")

// 10 consecutive double-taps (20 taps spaced properly)
var burstTimestamps: [TimeInterval] = []
for k in 0..<10 {
    let baseTime = Double(k) * 0.5 // every 500ms
    burstTimestamps.append(baseTime)
    burstTimestamps.append(baseTime + 0.08) // 80ms later
}
let burstResults = sim.processTapStream(timestamps: burstTimestamps)
assertTrue(burstResults.count == 10, "RapidBursts_Exactly10Events", detail: "Expected 10 events, got \(burstResults.count)")
let allDoubleTaps = burstResults.allSatisfy { $0 == .doubleTapFirst }
assertTrue(allDoubleTaps, "RapidBursts_AllAreDoubleTaps")

print("\nSUITE 2 RESULTS: \(passedCount) passed, \(failedCount) failed.")
if failedCount > 0 {
    exit(1)
}
