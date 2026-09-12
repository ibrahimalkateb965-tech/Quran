package com.aistudio.quranblind.audio

import kotlinx.coroutines.flow.Flow

/**
 * Platform audio engine. Android wraps a Media3 Player, iOS an AVPlayer over a [TrackQueue].
 *
 * Threading: every member must be used from the platform's main thread, like the
 * player it wraps. The engine never owns the player; [release] only detaches listeners.
 * Constructors are platform-specific and therefore not declared here.
 */
expect class AudioEngine {
    /** Hot stream of state changes; late subscribers miss earlier events. */
    val events: Flow<AudioEngineEvent>

    val isPlaying: Boolean
    val playWhenReady: Boolean
    val status: PlaybackStatus
    val positionMs: Long
    /** -1 when unknown. */
    val durationMs: Long
    val currentTrack: AudioTrack?
    /** Index of the current entry, or -1 when the queue is empty. */
    val currentIndex: Int
    val queueSize: Int

    /** Replaces the whole queue and positions on [startIndex] at 0 ms. Does not prepare or play. */
    fun setQueue(tracks: List<AudioTrack>, startIndex: Int)
    /** Drops every entry after the current one, then appends [tracks]. */
    fun replaceUpcoming(tracks: List<AudioTrack>)
    /** Drops every entry after the current one. */
    fun clearUpcoming()
    fun prepare()
    fun play()
    fun pause()
    fun stop()
    fun clearQueue()
    /** Detaches from the player. The engine must not be used afterwards. */
    fun release()
}
