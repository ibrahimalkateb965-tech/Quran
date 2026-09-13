package com.aistudio.quranblind.audio

/**
 * Encodes an ayah as a track id. The format is "<surahId>_<ayahNumber>" and must stay
 * byte-identical to the media ids the Android ViewModel builds today.
 */
object AyahTrackId {
    fun encode(surahId: Int, ayahNumber: Int): String = "${surahId}_$ayahNumber"

    /** Returns (surahId, ayahNumber) or null when [id] is not of the form "<int>_<int>". */
    fun decode(id: String): Pair<Int, Int>? {
        val parts = id.split("_")
        if (parts.size != 2) return null
        val surahId = parts[0].toIntOrNull() ?: return null
        val ayahNumber = parts[1].toIntOrNull() ?: return null
        return surahId to ayahNumber
    }
}
