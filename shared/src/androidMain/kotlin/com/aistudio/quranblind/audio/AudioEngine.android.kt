package com.aistudio.quranblind.audio

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Media3 actual. Wraps any [Player] - in :app that is the MediaController bound to
 * QuranAudioService, so the service, its cache and its MediaSession stay untouched.
 * The caller owns the Player's lifecycle; [release] only removes the listener.
 */
actual class AudioEngine(private val player: Player) {

    private val _events = MutableSharedFlow<AudioEngineEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    actual val events: Flow<AudioEngineEvent> = _events.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _events.tryEmit(AudioEngineEvent.IsPlayingChanged(isPlaying))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _events.tryEmit(AudioEngineEvent.StatusChanged(playbackState.toStatus()))
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _events.tryEmit(
                AudioEngineEvent.TrackChanged(
                    trackId = mediaItem?.mediaId,
                    automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                )
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            _events.tryEmit(AudioEngineEvent.PlaybackFailed(error.isNetworkRelated(), error.message))
        }
    }

    init {
        player.addListener(listener)
    }

    actual val isPlaying: Boolean get() = player.isPlaying
    actual val playWhenReady: Boolean get() = player.playWhenReady
    actual val status: PlaybackStatus get() = player.playbackState.toStatus()
    actual val positionMs: Long get() = player.currentPosition
    actual val durationMs: Long get() = player.duration.let { if (it == C.TIME_UNSET) -1L else it }
    actual val currentTrack: AudioTrack? get() = player.currentMediaItem?.toTrack()
    actual val currentIndex: Int get() = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
    actual val queueSize: Int get() = player.mediaItemCount

    actual fun setQueue(tracks: List<AudioTrack>, startIndex: Int) {
        player.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
    }

    actual fun replaceUpcoming(tracks: List<AudioTrack>) {
        clearUpcoming()
        player.addMediaItems(player.currentMediaItemIndex + 1, tracks.map { it.toMediaItem() })
    }

    actual fun clearUpcoming() {
        val next = player.currentMediaItemIndex + 1
        if (player.mediaItemCount > next) {
            player.removeMediaItems(next, player.mediaItemCount)
        }
    }

    actual fun prepare() = player.prepare()
    actual fun play() = player.play()
    actual fun pause() = player.pause()
    actual fun stop() = player.stop()
    actual fun clearQueue() = player.clearMediaItems()

    actual fun release() {
        player.removeListener(listener)
    }
}

private fun AudioTrack.toMediaItem(): MediaItem =
    MediaItem.Builder().setUri(url).setMediaId(id).build()

private fun MediaItem.toTrack(): AudioTrack =
    AudioTrack(id = mediaId, url = localConfiguration?.uri?.toString() ?: "")

private fun Int.toStatus(): PlaybackStatus = when (this) {
    Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
    Player.STATE_READY -> PlaybackStatus.READY
    Player.STATE_ENDED -> PlaybackStatus.ENDED
    else -> PlaybackStatus.IDLE
}

private fun PlaybackException.isNetworkRelated(): Boolean = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_TIMEOUT -> true
    else -> false
}
