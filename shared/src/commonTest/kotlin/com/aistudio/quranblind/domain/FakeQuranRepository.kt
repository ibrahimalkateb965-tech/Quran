package com.aistudio.quranblind.domain

import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import com.aistudio.quranblind.domain.model.SurahData
import com.aistudio.quranblind.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Test double for later orders. No test uses it yet.
class FakeQuranRepository : QuranRepository {
    override fun getAllSurahs(): List<Surah> = SurahData.SURAH_LIST

    override fun getSurahById(id: Int): Surah? = SurahData.SURAH_LIST.find { it.id == id }

    override fun findSurahByName(query: String): Surah? =
        SurahData.SURAH_LIST.find { it.nameEnglish.contains(query, ignoreCase = true) }

    override fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>> =
        flowOf(emptyList())
}
