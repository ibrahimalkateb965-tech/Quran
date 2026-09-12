package com.aistudio.quranblind.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS placeholder so SharedKit links. The AVQueuePlayer + AVAudioSession implementation
 * (interruption handling, VoiceOver-safe focus) is ORDER-P1-006-IOS and needs a macOS
 * verifier; nothing on iOS calls this yet.
 */
actual class AudioEngine {
    actual val events: Flow<AudioEngineEvent> = emptyFlow()

    actual val isPlaying: Boolean get() = false
    actual val playWhenReady: Boolean get() = false
    actual val status: PlaybackStatus get() = PlaybackStatus.IDLE
    actual val positionMs: Long get() = 0L
    actual val durationMs: Long get() = -1L
    actual val currentTrack: AudioTrack? get() = null
    actual val currentIndex: Int get() = -1
    actual val queueSize: Int get() = 0

    actual fun setQueue(tracks: List<AudioTrack>, startIndex: Int) = notReady()
    actual fun replaceUpcoming(tracks: List<AudioTrack>) = notReady()
    actual fun clearUpcoming() = notReady()
    actual fun prepare() = notReady()
    actual fun play() = notReady()
    actual fun pause() = notReady()
    actual fun stop() = notReady()
    actual fun clearQueue() = notReady()
    actual fun release() = notReady()

    private fun notReady(): Nothing =
        throw NotImplementedError("AudioEngine on iOS is ORDER-P1-006-IOS")
}
