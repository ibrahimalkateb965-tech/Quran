import Foundation
import UIKit
import AVFoundation
import SharedKit

// Drives the shared AudioEngine for one surah and mirrors its events into SwiftUI
// state. The engine contract is main-thread only; observeAudioEvents delivers on
// Dispatchers.Main and SwiftUI calls these methods from the main thread, so no
// extra hopping is needed.
final class SurahPlayer: ObservableObject, NowPlayingControllable {
    @Published private(set) var currentSurah: Surah?
    @Published private(set) var currentAyahs: [Ayah] = []
    @Published private(set) var currentAyah: Ayah?
    @Published var currentAyahIndex: Int = 0
    @Published var isContinuousPlayEnabled: Bool = false
    @Published private(set) var isPlaying = false
    @Published private(set) var hasEnded = false
    @Published private(set) var failure: String?

    @Published var currentReciterId: String = Reciter.companion.DEFAULT_RECITER.serverIdentifier
    @Published var currentReciterName: String = Reciter.companion.DEFAULT_RECITER.nameArabic

    // MARK: - Computed accessors for cross-screen observation

    /// The surah id of the currently playing ayah, if any.
    var activeSurahId: Int32? { currentAyah?.surahId ?? currentSurah?.id }

    /// The index into the queued ayahs of the currently playing ayah, if any.
    var activeAyahIndex: Int? { currentAyah != nil ? currentAyahIndex : nil }

    private let engine: AudioEngine = KoinIosKt.iosAudioEngine()
    private let repository: QuranRepository = KoinIosKt.iosQuranRepository()
    private var subscription: AudioEventSubscription?
    private var queued: [Ayah] = []

    // Ghost resume suppression window (2000ms / 2.0s) matching Android MediaButtonPolicy
    private var wasPlayingBeforeInterruption = false
    private var lastInterruptionEndTime: Date?
    private let ghostResumeWindowMs: Double = 2000.0

    init() {
        configureAudioSession()
        setupInterruptionObservers()
        NowPlayingManager.shared.setup(with: self)

        subscription = AudioBridgeIosKt.observeAudioEvents(engine: engine) { [weak self] event in
            self?.handle(event)
        }
    }

    deinit {
        subscription?.cancel()
        NotificationCenter.default.removeObserver(self)
        NowPlayingManager.shared.clear()
        deactivateAudioSession()
    }

    // MARK: - Audio Session Management

    func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio, policy: .longFormAudio)
            try session.setActive(true)
        } catch {
            print("AudioSession configuration failed: \(error)")
        }
    }

    func deactivateAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("AudioSession deactivation failed: \(error)")
        }
    }

    // MARK: - Interruption & Becoming Noisy Handling

    private func setupInterruptionObservers() {
        NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.handleAudioInterruption(notification)
        }

        NotificationCenter.default.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            self?.handleRouteChange(notification)
        }
    }

    private func handleAudioInterruption(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: typeValue) else {
            return
        }

        switch type {
        case .began:
            // Crucial Rule 1: Record whether we were actively playing before the call
            wasPlayingBeforeInterruption = isPlaying
            pause()
        case .ended:
            lastInterruptionEndTime = Date()
            var shouldResume = false
            if let optionsValue = userInfo[AVAudioSessionInterruptionOptionKey] as? UInt {
                let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
                shouldResume = options.contains(.shouldResume)
            }
            // Crucial Rule 2: Resume ONLY IF wasPlayingBeforeInterruption == true AND shouldResume == true
            if wasPlayingBeforeInterruption && shouldResume {
                playCurrent()
            } else {
                pause()
            }
            wasPlayingBeforeInterruption = false
        @unknown default:
            break
        }
    }

    private func handleRouteChange(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let reasonValue = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
              let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else {
            return
        }

        switch reason {
        case .oldDeviceUnavailable:
            // Headphones unplugged / becomingNoisy - auto-pause immediately
            pause()
        default:
            break
        }
    }

    // MARK: - Playback Commands

    /// Resumes playback of current Ayah or queued Ayahs, respecting the Ghost Resume suppression guard.
    func playCurrent() {
        if isPlaying { return }

        // Enforce 2000ms GhostResume suppression window after telephony/interruption ends
        if let lastEnd = lastInterruptionEndTime {
            let elapsedMs = Date().timeIntervalSince(lastEnd) * 1000.0
            if elapsedMs < ghostResumeWindowMs {
                return
            }
        }

        configureAudioSession()
        if engine.currentTrack != nil {
            engine.play()
        } else if !queued.isEmpty && currentAyahIndex < queued.count {
            play(queued, surah: currentSurah, from: currentAyahIndex, reciterName: currentReciterName, reciterId: currentReciterId)
        }
    }

    /// Queues and starts reciting at `index` (position in `ayahs`).
    func play(
        _ ayahs: [Ayah],
        surah: Surah? = nil,
        from index: Int,
        reciterName: String? = nil,
        reciterId: String? = nil
    ) {
        guard index >= 0 && index < ayahs.count else { return }
        failure = nil
        hasEnded = false
        queued = ayahs
        currentAyahs = ayahs
        currentAyahIndex = index
        currentAyah = ayahs[index]

        if let surah = surah {
            self.currentSurah = surah
        } else if self.currentSurah == nil || self.currentSurah?.id != ayahs[index].surahId {
            self.currentSurah = repository.getAllSurahs().first { $0.id == ayahs[index].surahId }
        }

        if let reciterName = reciterName {
            self.currentReciterName = reciterName
        }
        if let reciterId = reciterId {
            self.currentReciterId = reciterId
        }

        configureAudioSession()

        if isContinuousPlayEnabled {
            let tracks = ayahs.dropFirst(index).map { ayah in
                AudioTrack(id: trackId(of: ayah), url: ayah.audioUrl)
            }
            engine.setQueue(tracks: tracks, startIndex: 0)
        } else {
            let singleTrack = [AudioTrack(id: trackId(of: ayahs[index]), url: ayahs[index].audioUrl)]
            engine.setQueue(tracks: singleTrack, startIndex: 0)
        }
        engine.prepare()
        engine.play()

        updateNowPlaying()
    }

    /// Convenience overload supporting existing callers.
    func play(_ ayahs: [Ayah], from index: Int) {
        play(ayahs, surah: currentSurah, from: index, reciterName: currentReciterName, reciterId: currentReciterId)
    }

    /// Convenience overload supporting explicit continuous flag.
    func play(_ ayahs: [Ayah], from index: Int, continuous: Bool) {
        isContinuousPlayEnabled = continuous
        play(ayahs, from: index)
    }

    /// Convenience entry point used by callers that already hold a surah's ayah list.
    func play(surahAyahs ayahs: [Ayah], fromIndex index: Int) {
        play(ayahs, from: index)
    }

    /// Advances to the next Ayah in the Surah (or next Surah if at the end).
    func skipToNextAyah() {
        if currentAyahIndex < queued.count - 1 {
            let nextIndex = currentAyahIndex + 1
            play(queued, surah: currentSurah, from: nextIndex, reciterName: currentReciterName, reciterId: currentReciterId)
            if let ayah = currentAyah {
                A11yAnnouncer.shared.announce("الآية \(ayah.numberInSurah)")
            }
        } else {
            // Reached the end of the Surah: advance to next Surah if available
            let nextSurahId = (currentSurah?.id ?? 1) + 1
            if nextSurahId <= 114 {
                loadAndPlaySurah(id: nextSurahId, startAyahIndex: 0)
            } else {
                stop()
                if let surah = currentSurah {
                    A11yAnnouncer.shared.announceSurahCompletion(name: surah.nameArabic)
                }
            }
        }
    }

    /// Rewinds to previous Ayah (or starts current Ayah from 0s if elapsed > 3s).
    func skipToPreviousAyah() {
        let elapsed = Double(engine.positionMs) / 1000.0
        if elapsed > 3.0 {
            // Restart current Ayah from 0s
            play(queued, surah: currentSurah, from: currentAyahIndex, reciterName: currentReciterName, reciterId: currentReciterId)
        } else if currentAyahIndex > 0 {
            let prevIndex = currentAyahIndex - 1
            play(queued, surah: currentSurah, from: prevIndex, reciterName: currentReciterName, reciterId: currentReciterId)
            if let ayah = currentAyah {
                A11yAnnouncer.shared.announce("الآية \(ayah.numberInSurah)")
            }
        } else if let surah = currentSurah, surah.id > 1 {
            // Rewind to previous Surah, last Ayah
            loadAndPlaySurah(id: surah.id - 1, startAyahIndex: -1)
        } else {
            // First Ayah of first Surah: restart from beginning
            play(queued, surah: currentSurah, from: 0, reciterName: currentReciterName, reciterId: currentReciterId)
        }
    }

    /// Loads a Surah asynchronously and begins playback seamlessly.
    func loadAndPlaySurah(id: Int32, startAyahIndex: Int = 0) {
        let all = repository.getAllSurahs()
        guard let targetSurah = all.first(where: { $0.id == id }) else { return }
        self.currentSurah = targetSurah

        Task { @MainActor [weak self] in
            guard let self = self else { return }
            do {
                let loaded = try await QuranBridgeIosKt.loadAyahsOnce(
                    repository: self.repository,
                    surahId: id,
                    audioBaseUrl: self.currentReciterId
                )
                guard !loaded.isEmpty else { return }
                let targetIndex: Int
                if startAyahIndex == -1 {
                    targetIndex = loaded.count - 1
                } else {
                    targetIndex = max(0, min(startAyahIndex, loaded.count - 1))
                }
                self.play(
                    loaded,
                    surah: targetSurah,
                    from: targetIndex,
                    reciterName: self.currentReciterName,
                    reciterId: self.currentReciterId
                )
                A11yAnnouncer.shared.announceSurahChange(name: targetSurah.nameArabic, ayahCount: targetSurah.ayahCount)
            } catch {
                print("Failed to auto-load surah \(id): \(error)")
                self.failure = "تعذر تحميل السورة التالية"
            }
        }
    }

    /// Toggles continuous recitation mode and updates the audio engine queue dynamically.
    func toggleContinuousPlay() {
        isContinuousPlayEnabled.toggle()
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        A11yAnnouncer.shared.announceContinuousPlay(enabled: isContinuousPlayEnabled)
        if isContinuousPlayEnabled {
            let nextIndex = currentAyahIndex + 1
            if nextIndex < queued.count {
                let upcomingTracks = queued.dropFirst(nextIndex).map { ayah in
                    AudioTrack(id: trackId(of: ayah), url: ayah.audioUrl)
                }
                engine.replaceUpcoming(tracks: upcomingTracks)
            }
        } else {
            engine.clearUpcoming()
        }
    }

    /// Dynamically switches continuous playback mode mid-recitation.
    func updateContinuousPlay(_ continuous: Bool, ayahs: [Ayah], currentIndex: Int) {
        if isContinuousPlayEnabled != continuous {
            toggleContinuousPlay()
        }
    }

    func pause() {
        if engine.isPlaying {
            engine.pause()
        }
        updateNowPlayingPlaybackState()
    }

    func togglePause() {
        if isPlaying {
            pause()
            return
        }

        // Enforce 2000ms GhostResume suppression window after telephony/interruption ends
        if let lastEnd = lastInterruptionEndTime {
            let elapsedMs = Date().timeIntervalSince(lastEnd) * 1000.0
            if elapsedMs < ghostResumeWindowMs {
                // Block ghost resume within 2000ms window
                return
            }
        }

        playCurrent()
    }

    func stop() {
        engine.stop()
        currentAyah = nil
        hasEnded = false
        NowPlayingManager.shared.clear()
        deactivateAudioSession()
    }

    // MARK: - Now Playing Synchronization

    private func updateNowPlaying() {
        guard let surah = currentSurah ?? repository.getAllSurahs().first(where: { $0.id == currentAyah?.surahId }),
              let ayah = currentAyah else {
            return
        }
        let duration = Double(engine.durationMs) / 1000.0
        let elapsed = Double(engine.positionMs) / 1000.0
        NowPlayingManager.shared.update(
            surah: surah,
            ayah: ayah,
            reciterName: currentReciterName,
            isPlaying: isPlaying,
            duration: duration > 0 ? duration : 5.0,
            elapsed: max(0.0, elapsed)
        )
    }

    private func updateNowPlayingPlaybackState() {
        let duration = Double(engine.durationMs) / 1000.0
        let elapsed = Double(engine.positionMs) / 1000.0
        NowPlayingManager.shared.updatePlaybackState(
            isPlaying: isPlaying,
            elapsed: max(0.0, elapsed),
            duration: duration > 0 ? duration : nil
        )
    }

    private func trackId(of ayah: Ayah) -> String {
        AyahTrackId.shared.encode(surahId: ayah.surahId, ayahNumber: ayah.numberInSurah)
    }

    private func handlePlaybackEnded() {
        hasEnded = true
        stop()
        if let surah = currentSurah {
            A11yAnnouncer.shared.announceSurahCompletion(name: surah.nameArabic)
        }
    }

    private func handle(_ event: AudioEngineEvent) {
        switch event {
        case let changed as AudioEngineEventTrackChanged:
            if let index = queued.firstIndex(where: { trackId(of: $0) == changed.trackId }) {
                currentAyah = queued[index]
                currentAyahIndex = index
                updateNowPlaying()
            } else {
                currentAyah = nil
                NowPlayingManager.shared.clear()
            }
        case let playing as AudioEngineEventIsPlayingChanged:
            isPlaying = playing.isPlaying
            updateNowPlayingPlaybackState()
        case let status as AudioEngineEventStatusChanged:
            if status.status == PlaybackStatus.ended {
                handlePlaybackEnded()
            } else {
                hasEnded = false
            }
        case let failed as AudioEngineEventPlaybackFailed:
            failure = failed.message ?? "تعذر تشغيل التلاوة"
        default:
            break
        }
    }
}
