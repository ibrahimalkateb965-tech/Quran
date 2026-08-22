package com.example.data.repository

import android.content.Context
import com.example.data.local.AyahDao
import com.example.data.local.BookmarkDao
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuranRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockBookmarkDao: BookmarkDao
    private lateinit var mockAyahDao: AyahDao
    private lateinit var repository: QuranRepositoryImpl

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockBookmarkDao = mockk(relaxed = true)
        mockAyahDao = mockk(relaxed = true)
        repository = QuranRepositoryImpl(mockContext, mockBookmarkDao, mockAyahDao)
    }

    @Test
    fun testGetAllSurahs_returns114Surahs() {
        val surahs = repository.getAllSurahs()
        assertEquals(114, surahs.size)
    }

    @Test
    fun testGetSurahById_returnsCorrectSurah() {
        val fatiha = repository.getSurahById(1)
        assertNotNull(fatiha)
        assertEquals("الفاتحة", fatiha?.nameArabic)
        assertEquals("Al-Faatiha", fatiha?.nameEnglish)
    }

    @Test
    fun testFindSurahByName_normalizedArabic() {
        // Search with 'الفاتحه' (with Ha instead of Ta Marbuta)
        val surah1 = repository.findSurahByName("الفاتحه")
        assertNotNull("Should find Surah with normalized Ta Marbuta", surah1)
        assertEquals(1, surah1?.id)

        // Search with 'البقره'
        val surah2 = repository.findSurahByName("البقره")
        assertNotNull("Should find Surah Al-Baqarah", surah2)
        assertEquals(2, surah2?.id)

        // Search with 'ال عمران' (without Hamza)
        val surah3 = repository.findSurahByName("ال عمران")
        assertNotNull("Should find Surah Aal-Imran", surah3)
        assertEquals(3, surah3?.id)
    }

    @Test
    fun testSanitizeUthmanicText_cleansSpecialZeroWidthCharacters() {
        val textWithSpecialChars = "بِسْمِ \uFEFFاللَّهِ \u200Aالرَّحْمَٰنِ \u2060الرَّحِيمِ"
        val sanitized = repository.sanitizeUthmanicText(textWithSpecialChars)
        assertTrue(!sanitized.contains("\uFEFF"))
        assertTrue(!sanitized.contains("\u200A"))
        assertTrue(!sanitized.contains("\u2060"))
    }
}
