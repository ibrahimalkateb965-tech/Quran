package com.example.domain.repository

import com.aistudio.quranblind.domain.repository.QuranRepository as SharedQuranRepository
import com.example.data.local.BookmarkEntity
import kotlinx.coroutines.flow.Flow

interface QuranRepository : SharedQuranRepository {
    val allBookmarks: Flow<List<BookmarkEntity>>
    suspend fun toggleBookmark(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean
    suspend fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean
}
