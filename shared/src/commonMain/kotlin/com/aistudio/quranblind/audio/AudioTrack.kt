package com.aistudio.quranblind.audio

/**
 * One queue entry. [id] is opaque to the engine and is echoed back in
 * [AudioEngineEvent.TrackChanged]; the app builds it with [AyahTrackId].
 */
data class AudioTrack(val id: String, val url: String)
