import Foundation
import MediaPlayer
import UIKit
import SharedKit

/// Protocol defining the transport controls required by NowPlayingManager.
public protocol NowPlayingControllable: AnyObject {
    func playCurrent()
    func pause()
    func togglePause()
    func skipToNextAyah()
    func skipToPreviousAyah()
}

/// Manages system Now Playing information and remote transport commands (Lock Screen,
/// Control Center, Dynamic Island, and Bluetooth / Headset controls) for QuranBlind.
final class NowPlayingManager {
    static let shared = NowPlayingManager()

    private weak var player: NowPlayingControllable?
    private var isSetup = false

    private init() {}

    func setup(with player: NowPlayingControllable) {
        self.player = player
        if !isSetup {
            isSetup = true
            setupRemoteCommands()
            setupNotifications()
        }
    }

    // MARK: - Remote Transport Commands (MPRemoteCommandCenter)

    private func setupRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()

        // Remove any prior targets to prevent duplicate handler invocations
        center.playCommand.removeTarget(nil)
        center.pauseCommand.removeTarget(nil)
        center.togglePlayPauseCommand.removeTarget(nil)
        center.nextTrackCommand.removeTarget(nil)
        center.previousTrackCommand.removeTarget(nil)

        // 1. Play Command
        center.playCommand.isEnabled = true
        center.playCommand.addTarget { [weak self] _ in
            guard let self = self, let player = self.player else { return .commandFailed }
            player.playCurrent()
            return .success
        }

        // 2. Pause Command
        center.pauseCommand.isEnabled = true
        center.pauseCommand.addTarget { [weak self] _ in
            guard let self = self, let player = self.player else { return .commandFailed }
            player.pause()
            return .success
        }

        // 3. Toggle Play/Pause Command
        center.togglePlayPauseCommand.isEnabled = true
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            guard let self = self, let player = self.player else { return .commandFailed }
            player.togglePause()
            return .success
        }

        // 4. Next Track Command (Advances to next Ayah, or next Surah at end)
        center.nextTrackCommand.isEnabled = true
        center.nextTrackCommand.addTarget { [weak self] _ in
            guard let self = self, let player = self.player else { return .commandFailed }
            player.skipToNextAyah()
            return .success
        }

        // 5. Previous Track Command (Rewinds to prev Ayah, or restarts current if >3s)
        center.previousTrackCommand.isEnabled = true
        center.previousTrackCommand.addTarget { [weak self] _ in
            guard let self = self, let player = self.player else { return .commandFailed }
            player.skipToPreviousAyah()
            return .success
        }

        // Explicitly disable unused transport commands
        center.changePlaybackPositionCommand.isEnabled = false
        center.skipForwardCommand.isEnabled = false
        center.skipBackwardCommand.isEnabled = false
        center.seekForwardCommand.isEnabled = false
        center.seekBackwardCommand.isEnabled = false
        center.ratingCommand.isEnabled = false
        center.likeCommand.isEnabled = false
        center.dislikeCommand.isEnabled = false
        center.bookmarkCommand.isEnabled = false
        center.changePlaybackRateCommand.isEnabled = false
    }

    // MARK: - Now Playing Info (MPNowPlayingInfoCenter)

    func update(
        surah: Surah,
        ayah: Ayah,
        reciterName: String? = nil,
        isPlaying: Bool,
        duration: Double,
        elapsed: Double
    ) {
        var info = [String: Any]()
        info[MPMediaItemPropertyTitle] = "سورة \(surah.nameArabic) - الآية \(ayah.numberInSurah)"

        let artist: String
        if let reciter = reciterName, !reciter.isEmpty {
            artist = reciter.contains("القارئ") ? reciter : "القارئ: \(reciter)"
        } else {
            artist = "القرآن الكريم للمكفوفين"
        }
        info[MPMediaItemPropertyArtist] = artist
        info[MPMediaItemPropertyAlbumTitle] = "\(surah.revelationType) • \(surah.ayahCount) آية"

        info[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0
        info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = 1.0
        info[MPMediaItemPropertyPlaybackDuration] = duration > 0 ? duration : 5.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = max(0.0, elapsed)

        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    func updatePlaybackState(isPlaying: Bool, elapsed: Double, duration: Double? = nil) {
        guard var info = MPNowPlayingInfoCenter.default().nowPlayingInfo else { return }
        info[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = max(0.0, elapsed)
        if let duration = duration, duration > 0 {
            info[MPMediaItemPropertyPlaybackDuration] = duration
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    func clear() {
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }

    // MARK: - Route Changes ("Becoming Noisy" Protection)

    private func setupNotifications() {
        NotificationCenter.default.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let reasonValue = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
                  let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else { return }
            if reason == .oldDeviceUnavailable {
                self?.player?.pause()
            }
        }
    }
}
