import Foundation
import SharedKit

// Drives the shared AudioEngine for one surah and mirrors its events into SwiftUI
// state. The engine contract is main-thread only; observeAudioEvents delivers on
// Dispatchers.Main and SwiftUI calls these methods from the main thread, so no
// extra hopping is needed.
final class SurahPlayer: ObservableObject {
    @Published private(set) var currentAyah: Ayah?
    @Published private(set) var isPlaying = false
    @Published private(set) var hasEnded = false
    @Published private(set) var failure: String?

    private let engine: AudioEngine = KoinIosKt.iosAudioEngine()
    private var subscription: AudioEventSubscription?
    private var queued: [Ayah] = []

    init() {
        subscription = AudioBridgeIosKt.observeAudioEvents(engine: engine) { [weak self] event in
            self?.handle(event)
        }
    }

    deinit {
        subscription?.cancel()
    }

    /// Queues the whole surah and starts reciting at `index` (position in `ayahs`).
    func play(_ ayahs: [Ayah], from index: Int) {
        failure = nil
        hasEnded = false
        queued = ayahs
        let tracks = ayahs.map { ayah in
            AudioTrack(id: trackId(of: ayah), url: ayah.audioUrl)
        }
        engine.setQueue(tracks: tracks, startIndex: Int32(index))
        engine.prepare()
        engine.play()
    }

    func togglePause() {
        if engine.isPlaying {
            engine.pause()
        } else {
            engine.play()
        }
    }

    func stop() {
        engine.stop()
        currentAyah = nil
        hasEnded = false
    }

    private func trackId(of ayah: Ayah) -> String {
        AyahTrackId.shared.encode(surahId: ayah.surahId, ayahNumber: ayah.numberInSurah)
    }

    private func handle(_ event: AudioEngineEvent) {
        switch event {
        case let changed as AudioEngineEventTrackChanged:
            currentAyah = queued.first { trackId(of: $0) == changed.trackId }
        case let playing as AudioEngineEventIsPlayingChanged:
            isPlaying = playing.isPlaying
        case let status as AudioEngineEventStatusChanged:
            hasEnded = status.status == PlaybackStatus.ended
        case let failed as AudioEngineEventPlaybackFailed:
            failure = failed.message ?? "تعذر تشغيل التلاوة"
        default:
            break
        }
    }
}
