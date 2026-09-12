package com.aistudio.quranblind.audio

sealed interface AudioEngineEvent {
    data class IsPlayingChanged(val isPlaying: Boolean) : AudioEngineEvent

    data class StatusChanged(val status: PlaybackStatus) : AudioEngineEvent

    /**
     * The engine moved to another queue entry. [automatic] is true only when the previous
     * track finished on its own (Media3: MEDIA_ITEM_TRANSITION_REASON_AUTO), false for
     * setQueue / seek / repeat.
     */
    data class TrackChanged(val trackId: String?, val automatic: Boolean) : AudioEngineEvent

    /** [isNetworkRelated] drives the retry-with-backoff path; anything else is announced once. */
    data class PlaybackFailed(val isNetworkRelated: Boolean, val message: String?) : AudioEngineEvent
}
