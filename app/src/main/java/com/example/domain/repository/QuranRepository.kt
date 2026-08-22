package com.example.domain.repository

import com.example.data.local.BookmarkEntity
import com.example.data.model.Ayah
import com.example.data.model.Surah
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    val allBookmarks: Flow<List<BookmarkEntity>>
    suspend fun toggleBookmark(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean
    suspend fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean
    fun getAllSurahs(): List<Surah>
    fun getSurahById(id: Int): Surah?
    fun findSurahByName(query: String): Surah?
    fun getAyahs(surahId: Int, reciterIdentifier: String = "ar.alafasy"): Flow<List<Ayah>>
    fun sanitizeUthmanicText(text: String): String
}
