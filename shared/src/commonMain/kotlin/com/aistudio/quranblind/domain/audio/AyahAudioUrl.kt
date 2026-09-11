package com.aistudio.quranblind.domain.audio

fun ayahAudioUrl(audioBaseUrl: String, surahId: Int, ayahInSurah: Int): String {
    val base = if (audioBaseUrl.endsWith("/")) audioBaseUrl else "$audioBaseUrl/"
    return base + surahId.toString().padStart(3, '0') + ayahInSurah.toString().padStart(3, '0') + ".mp3"
}
