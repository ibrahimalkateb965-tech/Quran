package com.aistudio.quranblind.domain.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AyahAudioUrlTest {

    private val husaryBase = "https://verse.mp3quran.net/data/Husary_128kbps/"

    @Test
    fun husarySecondSurahFifthAyahBuildsPaddedUrl() {
        assertEquals(
            "https://verse.mp3quran.net/data/Husary_128kbps/002005.mp3",
            ayahAudioUrl(husaryBase, 2, 5)
        )
    }

    @Test
    fun baseWithoutTrailingSlashGetsOneAppended() {
        assertEquals(
            "https://verse.mp3quran.net/data/Husary_128kbps/002005.mp3",
            ayahAudioUrl("https://verse.mp3quran.net/data/Husary_128kbps", 2, 5)
        )
    }

    @Test
    fun lastSurahLastAyahBuildsPaddedUrl() {
        val url = ayahAudioUrl(husaryBase, 114, 6)
        assertTrue(url.endsWith("114006.mp3"), "Unexpected url: $url")
        assertEquals("https://verse.mp3quran.net/data/Husary_128kbps/114006.mp3", url)
    }
}
