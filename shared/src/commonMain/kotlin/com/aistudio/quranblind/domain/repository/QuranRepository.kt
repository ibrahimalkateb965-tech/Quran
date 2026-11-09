package com.aistudio.quranblind.domain.repository

import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getAllSurahs(): List<Surah>
    fun getSurahById(id: Int): Surah?
    fun findSurahByName(query: String): Surah?
    fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>>
}
