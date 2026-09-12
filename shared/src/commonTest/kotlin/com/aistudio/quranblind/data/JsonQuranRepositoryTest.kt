package com.aistudio.quranblind.data

import com.aistudio.quranblind.domain.audio.ayahAudioUrl
import com.aistudio.quranblind.domain.text.sanitizeUthmanicText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Fixtures are escaped code points, as in UthmanicTextTest, so no editor bidi
// reordering can alter them silently. U+0600 (ARABIC NUMBER SIGN) is one of the
// characters sanitizeUthmanicText strips: its presence in the raw JSON and absence
// in the domain model proves the sanitizer sits on the read path (CLAUDE.md 4.4).
class JsonQuranRepositoryTest {

    private val rawWithRosette = "بِسْمِ؀"
    private val plainAyah = "الْحَمْدُ"

    // Surah 1 has two ayahs (second one carries an unknown key and explicit page/juz);
    // surah 2 has one. page/juz are absent on the first row, as in the shipped file.
    private val fixture = """
        {
          "1": [
            { "numberInSurah": 1, "globalNumber": 1, "textArabic": "$rawWithRosette", "surahId": 1 },
            { "numberInSurah": 2, "globalNumber": 2, "textArabic": "$plainAyah", "surahId": 1,
              "page": 3, "juz": 4, "unknownKey": "ignored" }
          ],
          "2": [
            { "numberInSurah": 1, "globalNumber": 8, "textArabic": "$plainAyah", "surahId": 2 }
          ]
        }
    """.trimIndent()

    private class CountingSource(private val text: String) : QuranJsonSource {
        var reads = 0
        override fun readQuranJson(): String { reads++; return text }
    }

    private val base = "https://example.invalid/reciter/"

    @Test
    fun t1_getAyahsParsesEveryRowOfTheRequestedSurah() = runTest {
        val repo = JsonQuranRepository(CountingSource(fixture))
        val ayahs = repo.getAyahs(1, base).first()
        assertEquals(2, ayahs.size)
        assertEquals(listOf(1, 2), ayahs.map { it.numberInSurah })
        assertEquals(listOf(1, 2), ayahs.map { it.globalNumber })
        assertTrue(ayahs.all { it.surahId == 1 })
        assertEquals(1, repo.getAyahs(2, base).first().size)
    }

    @Test
    fun t2_textArabicPassesThroughSanitizeUthmanicText() = runTest {
        val repo = JsonQuranRepository(CountingSource(fixture))
        val first = repo.getAyahs(1, base).first().first()
        assertEquals(sanitizeUthmanicText(rawWithRosette), first.textArabic)
        assertFalse(first.textArabic.contains("؀"))
    }

    @Test
    fun t3_audioUrlIsBuiltFromTheReciterBaseUrl() = runTest {
        val repo = JsonQuranRepository(CountingSource(fixture))
        val ayah = repo.getAyahs(2, base).first().single()
        assertEquals(ayahAudioUrl(base, 2, 1), ayah.audioUrl)
        assertEquals("${base}002001.mp3", ayah.audioUrl)
    }

    @Test
    fun t4_missingPageAndJuzDefaultToOneAndExplicitValuesAreKept() = runTest {
        val repo = JsonQuranRepository(CountingSource(fixture))
        val (first, second) = repo.getAyahs(1, base).first()
        assertEquals(1, first.page)
        assertEquals(1, first.juz)
        assertEquals(3, second.page)
        assertEquals(4, second.juz)
        assertEquals("", second.textTranslation)
    }

    @Test
    fun t5_unknownSurahYieldsEmptyList() = runTest {
        val repo = JsonQuranRepository(CountingSource(fixture))
        assertTrue(repo.getAyahs(114, base).first().isEmpty())
        assertTrue(repo.getAyahs(0, base).first().isEmpty())
    }

    @Test
    fun t6_jsonIsReadFromTheSourceOnlyOnce() = runTest {
        val source = CountingSource(fixture)
        val repo = JsonQuranRepository(source)
        repo.getAyahs(1, base).first()
        repo.getAyahs(2, base).first()
        repo.getAyahs(1, base).first()
        assertEquals(1, source.reads)
    }

    @Test
    fun t7_surahLookupsDelegateToSurahData() {
        val repo = JsonQuranRepository(CountingSource(fixture))
        assertEquals(114, repo.getAllSurahs().size)
        assertEquals("Al-Fatihah", repo.getSurahById(1)?.nameEnglish)
        assertNull(repo.getSurahById(115))
        assertSame(repo.getSurahById(2), repo.findSurahByName("baqarah"))
        assertSame(repo.getSurahById(2), repo.findSurahByName("البقرة"))
        assertNull(repo.findSurahByName(""))
        assertNull(repo.findSurahByName("   "))
    }
}
