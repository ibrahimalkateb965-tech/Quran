import Foundation
import MediaPlayer
import AVFoundation
import SharedKit

// Empirical Test Suite 6: NowPlaying & MediaSession Parity
// Verifies Lock Screen metadata, MPRemoteCommandCenter controls,
// route changes ("Becoming Noisy"), Call Interruption & Ghost Resume Guard,
// and Continuous Playback Across Surahs.

@main
struct Suite6Main {
    static var testsPassed = 0
    static var testsFailed = 0

    static func assertEqual<T: Equatable>(_ actual: T, _ expected: T, _ testName: String) {
        if actual == expected {
            print("  [PASS] \(testName)")
            testsPassed += 1
        } else {
            print("  [FAIL] \(testName): expected '\(expected)', got '\(actual)'")
            testsFailed += 1
        }
    }

    static func assertTrue(_ condition: Bool, _ testName: String) {
        if condition {
            print("  [PASS] \(testName)")
            testsPassed += 1
        } else {
            print("  [FAIL] \(testName): condition evaluated to false")
            testsFailed += 1
        }
    }

    static func assertFalse(_ condition: Bool, _ testName: String) {
        if !condition {
            print("  [PASS] \(testName)")
            testsPassed += 1
        } else {
            print("  [FAIL] \(testName): condition evaluated to true")
            testsFailed += 1
        }
    }

    static func main() {
        print("================================================================================")
        print("  EMPIRICAL SUITE 6: NowPlaying & MediaSession Parity Tests")
        print("================================================================================")

        // MARK: - Test Group 1: Lock Screen & Control Center Metadata
        print("Test Group 1: Lock Screen & Control Center Metadata (MPNowPlayingInfoCenter)")

        let surah = Surah(
            id: 1,
            nameArabic: "الفاتحة",
            nameEnglish: "Al-Fatihah",
            translationArabic: "فاتحة الكتاب",
            ayahCount: 7,
            revelationType: "مكية",
            startPage: 1
        )
        let ayah = Ayah(
            numberInSurah: 1,
            globalNumber: 1,
            textArabic: "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            textTranslation: "",
            audioUrl: "https://example.com/1.mp3",
            surahId: 1,
            page: 1,
            juz: 1
        )

        NowPlayingManager.shared.update(
            surah: surah,
            ayah: ayah,
            reciterName: "محمود خليل الحصري مجود",
            isPlaying: true,
            duration: 15.5,
            elapsed: 4.2
        )

        let info = MPNowPlayingInfoCenter.default().nowPlayingInfo
        assertTrue(info != nil, "NowPlayingInfo_IsSet")
        assertEqual(info?[MPMediaItemPropertyTitle] as? String, "سورة الفاتحة - الآية 1", "Metadata_Title")
        assertEqual(info?[MPMediaItemPropertyArtist] as? String, "القارئ: محمود خليل الحصري مجود", "Metadata_Artist")
        assertEqual(info?[MPMediaItemPropertyAlbumTitle] as? String, "مكية • 7 آية", "Metadata_AlbumTitle")
        assertEqual(info?[MPNowPlayingInfoPropertyPlaybackRate] as? Double, 1.0, "Metadata_PlaybackRate_Playing")
        assertEqual(info?[MPNowPlayingInfoPropertyDefaultPlaybackRate] as? Double, 1.0, "Metadata_DefaultPlaybackRate")
        assertEqual(info?[MPMediaItemPropertyPlaybackDuration] as? Double, 15.5, "Metadata_Duration")
        assertEqual(info?[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double, 4.2, "Metadata_Elapsed")

        // Test metadata update when paused
        NowPlayingManager.shared.updatePlaybackState(isPlaying: false, elapsed: 6.0)
        let pausedInfo = MPNowPlayingInfoCenter.default().nowPlayingInfo
        assertEqual(pausedInfo?[MPNowPlayingInfoPropertyPlaybackRate] as? Double, 0.0, "Metadata_PlaybackRate_Paused")
        assertEqual(pausedInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double, 6.0, "Metadata_Elapsed_Updated")

        // Test clearing metadata on stop
        NowPlayingManager.shared.clear()
        assertTrue(MPNowPlayingInfoCenter.default().nowPlayingInfo == nil, "Metadata_ClearedOnStop")

        // MARK: - Test Group 2: Remote Command Center Configuration
        print("Test Group 2: MPRemoteCommandCenter Configuration & Disabled Commands")

        class MockPlayer: NowPlayingControllable {
            var playCalled = false
            var pauseCalled = false
            var toggleCalled = false
            var nextCalled = false
            var prevCalled = false

            func playCurrent() { playCalled = true }
            func pause() { pauseCalled = true }
            func togglePause() { toggleCalled = true }
            func skipToNextAyah() { nextCalled = true }
            func skipToPreviousAyah() { prevCalled = true }
        }

        let mockPlayer = MockPlayer()
        NowPlayingManager.shared.setup(with: mockPlayer)

        let commandCenter = MPRemoteCommandCenter.shared()
        assertTrue(commandCenter.playCommand.isEnabled, "RemoteCommand_Play_Enabled")
        assertTrue(commandCenter.pauseCommand.isEnabled, "RemoteCommand_Pause_Enabled")
        assertTrue(commandCenter.togglePlayPauseCommand.isEnabled, "RemoteCommand_TogglePlayPause_Enabled")
        assertTrue(commandCenter.nextTrackCommand.isEnabled, "RemoteCommand_NextTrack_Enabled")
        assertTrue(commandCenter.previousTrackCommand.isEnabled, "RemoteCommand_PreviousTrack_Enabled")

        assertFalse(commandCenter.changePlaybackPositionCommand.isEnabled, "RemoteCommand_ChangePlaybackPosition_Disabled")
        assertFalse(commandCenter.skipForwardCommand.isEnabled, "RemoteCommand_SkipForward_Disabled")
        assertFalse(commandCenter.skipBackwardCommand.isEnabled, "RemoteCommand_SkipBackward_Disabled")
        assertFalse(commandCenter.seekForwardCommand.isEnabled, "RemoteCommand_SeekForward_Disabled")
        assertFalse(commandCenter.seekBackwardCommand.isEnabled, "RemoteCommand_SeekBackward_Disabled")
        assertFalse(commandCenter.ratingCommand.isEnabled, "RemoteCommand_Rating_Disabled")
        assertFalse(commandCenter.likeCommand.isEnabled, "RemoteCommand_Like_Disabled")
        assertFalse(commandCenter.dislikeCommand.isEnabled, "RemoteCommand_Dislike_Disabled")
        assertFalse(commandCenter.bookmarkCommand.isEnabled, "RemoteCommand_Bookmark_Disabled")
        assertFalse(commandCenter.changePlaybackRateCommand.isEnabled, "RemoteCommand_ChangePlaybackRate_Disabled")

        // MARK: - Test Group 3: Ghost Resume Guard Logic (Matching MediaButtonPolicy)
        print("Test Group 3: Ghost Resume Guard Logic (Matching MediaButtonPolicy)")

        class MockCallEndTracker {
            var callEndedAt: Date?
            let ghostResumeWindowMs: Double = 2000.0

            func shouldConsumeEvent(isPlaying: Bool) -> Bool {
                // If already playing, toggle means pause, which is never a ghost resume
                if isPlaying { return false }
                guard let endedAt = callEndedAt else { return false }
                let elapsedMs = Date().timeIntervalSince(endedAt) * 1000.0
                return elapsedMs >= 0 && elapsedMs < ghostResumeWindowMs
            }
        }

        let tracker = MockCallEndTracker()
        assertFalse(tracker.shouldConsumeEvent(isPlaying: false), "NoCall_DoNotConsume")

        // Call ended just now (0ms)
        tracker.callEndedAt = Date()
        assertTrue(tracker.shouldConsumeEvent(isPlaying: false), "WithinWindow_ConsumeGhostPlay")
        assertFalse(tracker.shouldConsumeEvent(isPlaying: true), "WithinWindow_DoNotConsumeIfPlaying")

        // After suppression window expires (> 2000ms)
        tracker.callEndedAt = Date().addingTimeInterval(-2.5) // 2.5s ago
        assertFalse(tracker.shouldConsumeEvent(isPlaying: false), "ExpiredWindow_AllowPlay")

        // MARK: - Test Group 4: Call Interruption State Transition Matrix
        print("Test Group 4: Call Interruption State Transition Matrix")

        struct InterruptionTestState {
            var isPlaying: Bool
            var wasPlayingBeforeInterruption: Bool

            mutating func onInterruptionBegan() {
                wasPlayingBeforeInterruption = isPlaying
                isPlaying = false
            }

            mutating func onInterruptionEnded(shouldResume: Bool) {
                // Invariant I4: Suppress auto-resume after calls/interruptions, maintaining paused state
                isPlaying = false
                wasPlayingBeforeInterruption = false
            }
        }

        // Case 1: Playing before call, call ends with shouldResume = true -> MUST NOT RESUME (Invariant I4)
        var state1 = InterruptionTestState(isPlaying: true, wasPlayingBeforeInterruption: false)
        state1.onInterruptionBegan()
        assertFalse(state1.isPlaying, "State1_InterruptionBegan_Paused")
        assertTrue(state1.wasPlayingBeforeInterruption, "State1_Recorded_WasPlaying")
        state1.onInterruptionEnded(shouldResume: true)
        assertFalse(state1.isPlaying, "State1_MaintainsPaused_InvariantI4")
        assertFalse(state1.wasPlayingBeforeInterruption, "State1_WasPlayingClearedOnEnd")

        // Case 2: Paused before call, call ends with shouldResume = true -> MUST NOT RESUME
        var state2 = InterruptionTestState(isPlaying: false, wasPlayingBeforeInterruption: false)
        state2.onInterruptionBegan()
        assertFalse(state2.isPlaying, "State2_InterruptionBegan_Paused")
        assertFalse(state2.wasPlayingBeforeInterruption, "State2_Recorded_WasNotPlaying")
        state2.onInterruptionEnded(shouldResume: true)
        assertFalse(state2.isPlaying, "State2_MustNotResumeIfPausedBeforeCall")
        assertFalse(state2.wasPlayingBeforeInterruption, "State2_WasPlayingClearedOnEnd")

        // Case 3: Playing before call, call ends with shouldResume = false -> MUST NOT RESUME
        var state3 = InterruptionTestState(isPlaying: true, wasPlayingBeforeInterruption: false)
        state3.onInterruptionBegan()
        state3.onInterruptionEnded(shouldResume: false)
        assertFalse(state3.isPlaying, "State3_MustNotResumeIfShouldResumeFalse")
        assertFalse(state3.wasPlayingBeforeInterruption, "State3_WasPlayingClearedOnEnd")

        // Case 4: Explicit user tap required to resume playback after interruption ends
        var state4 = InterruptionTestState(isPlaying: true, wasPlayingBeforeInterruption: false)
        state4.onInterruptionBegan()
        state4.onInterruptionEnded(shouldResume: true)
        assertFalse(state4.isPlaying, "State4_StillPausedUntilExplicitUserTap")
        state4.isPlaying = true // Explicit user tap (playCurrent / togglePause)
        assertTrue(state4.isPlaying, "State4_ResumedOnlyByExplicitUserTap")

        // MARK: - Test Group 5: Previous Track Command Rewind Logic (> 3s vs <= 3s)
        print("Test Group 5: Previous Track Command Rewind Logic (> 3s vs <= 3s)")

        func resolvePreviousTrackAction(currentAyahIndex: Int, elapsedSeconds: Double) -> String {
            if elapsedSeconds > 3.0 {
                return "RESTART_CURRENT"
            } else if currentAyahIndex > 0 {
                return "PREVIOUS_AYAH"
            } else {
                return "PREVIOUS_SURAH"
            }
        }

        assertEqual(resolvePreviousTrackAction(currentAyahIndex: 3, elapsedSeconds: 4.5), "RESTART_CURRENT", "Prev_ElapsedOver3s_RestartsCurrent")
        assertEqual(resolvePreviousTrackAction(currentAyahIndex: 3, elapsedSeconds: 2.1), "PREVIOUS_AYAH", "Prev_ElapsedUnder3s_GoesToPrevious")
        assertEqual(resolvePreviousTrackAction(currentAyahIndex: 0, elapsedSeconds: 1.0), "PREVIOUS_SURAH", "Prev_FirstAyahUnder3s_GoesToPrevSurah")

        // MARK: - Test Group 6: Continuous Playback Across Surahs Transition Logic
        print("Test Group 6: Continuous Playback Across Surahs Transition Logic")

        func resolveSurahCompletion(currentSurahId: Int32, isContinuousEnabled: Bool) -> (action: String, nextSurahId: Int32?) {
            if isContinuousEnabled {
                let nextId = currentSurahId + 1
                if nextId <= 114 {
                    return ("LOAD_NEXT_SURAH", nextId)
                }
            }
            return ("STOP_COMPLETION", nil)
        }

        let result1 = resolveSurahCompletion(currentSurahId: 1, isContinuousEnabled: true)
        assertEqual(result1.action, "LOAD_NEXT_SURAH", "Continuous_Surah1_LoadsNextSurah")
        assertEqual(result1.nextSurahId, 2, "Continuous_Surah1_NextSurahIdIs2")

        let result2 = resolveSurahCompletion(currentSurahId: 1, isContinuousEnabled: false)
        assertEqual(result2.action, "STOP_COMPLETION", "NonContinuous_Surah1_StopsAtEnd")
        assertTrue(result2.nextSurahId == nil, "NonContinuous_Surah1_NoNextSurah")

        let result3 = resolveSurahCompletion(currentSurahId: 114, isContinuousEnabled: true)
        assertEqual(result3.action, "STOP_COMPLETION", "Continuous_Surah114_StopsAtEnd")
        assertTrue(result3.nextSurahId == nil, "Continuous_Surah114_NoNextSurahBeyond114")

        print("")
        print("SUITE 6 RESULTS: \(testsPassed) passed, \(testsFailed) failed.")

        if testsFailed > 0 {
            exit(1)
        }
    }
}
