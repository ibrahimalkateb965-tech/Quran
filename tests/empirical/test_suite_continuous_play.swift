//
//  test_suite_continuous_play.swift
//  QuranBlind
//
//  Empirical Suite 5: Continuous Recitation ("الاستماع المتواصل") AudioEngine Queue Tests
//

import Foundation
import SharedKit

print("================================================================================")
print("  EMPIRICAL SUITE 5: Continuous Recitation Queue & Auto-Advancement Tests")
print("================================================================================")

var passed = 0
var failed = 0

func assertTrue(_ condition: Bool, _ name: String) {
    if condition {
        print("  [PASS] \(name)")
        passed += 1
    } else {
        print("  [FAIL] \(name)")
        failed += 1
    }
}

// Test 1: AudioEngine basic queue operations
print("Test Group 1: AudioEngine Queue Operations")
let engine = AudioEngine()
assertTrue(engine.currentIndex == -1, "InitialIndex_IsMinusOne")
assertTrue(engine.queueSize == 0, "InitialSize_IsZero")
assertTrue(engine.currentTrack == nil, "InitialCurrent_IsNil")

let t1 = AudioTrack(id: "1_1", url: "https://example.com/1.mp3")
let t2 = AudioTrack(id: "1_2", url: "https://example.com/2.mp3")
let t3 = AudioTrack(id: "1_3", url: "https://example.com/3.mp3")

engine.setQueue(tracks: [t1, t2, t3], startIndex: 0)
assertTrue(engine.currentIndex == 0, "SetQueue_IndexIsZero")
assertTrue(engine.queueSize == 3, "SetQueue_SizeIsThree")
assertTrue(engine.currentTrack?.id == "1_1", "SetQueue_CurrentIsTrack1")

// Test 2: Dynamic upcoming tracks manipulation
print("Test Group 2: Dynamic Upcoming Tracks Manipulation")
engine.setQueue(tracks: [t1], startIndex: 0)
assertTrue(engine.queueSize == 1, "SingleTrackQueue_SizeIsOne")

engine.replaceUpcoming(tracks: [t2, t3])
assertTrue(engine.queueSize == 3, "ReplaceUpcoming_AppendsRemainingTracks")
assertTrue(engine.currentIndex == 0, "ReplaceUpcoming_PreservesCurrentIndex")
assertTrue(engine.currentTrack?.id == "1_1", "ReplaceUpcoming_PreservesCurrentTrack")

engine.clearUpcoming()
assertTrue(engine.queueSize == 1, "ClearUpcoming_DropsRemainingTracks")
assertTrue(engine.currentIndex == 0, "ClearUpcoming_PreservesCurrentIndex")
assertTrue(engine.currentTrack?.id == "1_1", "ClearUpcoming_PreservesCurrentTrack")

// Test 3: AyahTrackId encoding / decoding
print("Test Group 3: AyahTrackId Invariants")
let encoded = AyahTrackId.shared.encode(surahId: 1, ayahNumber: 7)
assertTrue(encoded == "1_7", "AyahTrackId_EncodedCorrectly")

let decoded = AyahTrackId.shared.decode(id: "1_7")
assertTrue(decoded?.first?.intValue == 1, "AyahTrackId_DecodesSurahCorrectly")
assertTrue(decoded?.second?.intValue == 7, "AyahTrackId_DecodesAyahCorrectly")

print("")
print("SUITE 5 RESULTS: \(passed) passed, \(failed) failed.")
if failed > 0 {
    exit(1)
}
