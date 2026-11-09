package com.aistudio.quranblind.audio

/**
 * Ordered queue with a cursor, for platforms whose player does not keep the queue
 * itself (iOS: AVPlayer holds one item at a time). Mirrors the Media3 semantics the
 * Android actual gets for free: [index] is -1 only while the queue is empty.
 */
internal class TrackQueue {
    private val items = mutableListOf<AudioTrack>()

    var index: Int = -1
        private set

    val size: Int get() = items.size
    val current: AudioTrack? get() = items.getOrNull(index)

    /** Replaces everything and positions on [startIndex], clamped into range. */
    fun set(tracks: List<AudioTrack>, startIndex: Int) {
        items.clear()
        items += tracks
        index = if (tracks.isEmpty()) -1 else startIndex.coerceIn(0, tracks.lastIndex)
    }

    /** Drops every entry after the current one, then appends [tracks]. */
    fun replaceUpcoming(tracks: List<AudioTrack>) {
        clearUpcoming()
        items += tracks
        if (index < 0 && items.isNotEmpty()) index = 0
    }

    /** Drops every entry after the current one. */
    fun clearUpcoming() {
        if (index >= 0 && items.size > index + 1) {
            items.subList(index + 1, items.size).clear()
        }
    }

    /** Moves to the next entry; returns false (and stays put) at the end. */
    fun advance(): Boolean {
        if (index + 1 >= items.size) return false
        index++
        return true
    }

    fun clear() {
        items.clear()
        index = -1
    }
}
