package com.aistudio.quranblind.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SurahDataTest {

    @Test
    fun tableHas114Surahs() {
        assertEquals(114, SurahData.SURAH_LIST.size)
    }

    @Test
    fun idsAreOneTo114InOrder() {
        assertEquals((1..114).toList(), SurahData.SURAH_LIST.map { it.id })
    }

    @Test
    fun firstSurahIsFatihahWithSevenAyahs() {
        val first = SurahData.SURAH_LIST.first()
        assertEquals("Al-Fatihah", first.nameEnglish)
        assertEquals(7, first.ayahCount)
    }

    @Test
    fun lastSurahIdIs114() {
        assertEquals(114, SurahData.SURAH_LIST.last().id)
    }

    @Test
    fun totalAyahCountIs6236() {
        assertEquals(6236, SurahData.SURAH_LIST.sumOf { it.ayahCount })
    }
}
