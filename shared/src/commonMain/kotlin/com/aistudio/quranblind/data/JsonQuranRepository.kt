package com.aistudio.quranblind.data

import com.aistudio.quranblind.domain.audio.ayahAudioUrl
import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import com.aistudio.quranblind.domain.model.SurahData
import com.aistudio.quranblind.domain.repository.QuranRepository
import com.aistudio.quranblind.domain.text.sanitizeUthmanicText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AyahJson(
    val numberInSurah: Int,
    val globalNumber: Int,
    val textArabic: String,
    val surahId: Int,
    val page: Int = 1,
    val juz: Int = 1,
)

class JsonQuranRepository(private val source: QuranJsonSource) : QuranRepository {
    private val json = Json { ignoreUnknownKeys = true }

    // Parsed once, on first use, then shared. `lazy` is synchronized by default.
    private val ayahsBySurah: Map<Int, List<AyahJson>> by lazy {
        try {
            val raw = source.readQuranJson().trim()
            if (raw.isEmpty()) emptyMap()
            else json.decodeFromString<Map<String, List<AyahJson>>>(raw)
                .mapKeys { (key, _) -> key.toInt() }
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private val ayahsByPage: Map<Int, List<AyahJson>> by lazy {
        ayahsBySurah.values.flatten().groupBy { it.page }
    }

    override fun getAllSurahs(): List<Surah> = SurahData.SURAH_LIST

    override fun getSurahById(id: Int): Surah? = SurahData.SURAH_LIST.find { it.id == id }

    override fun findSurahByName(query: String): Surah? {
        val q = normalizeArabicText(query)
        if (q.isEmpty()) return null
        return SurahData.SURAH_LIST.find { surah ->
            val cleanSurahName = normalizeArabicText(surah.nameArabic)
            cleanSurahName.contains(q) || q.contains(cleanSurahName) ||
                surah.nameEnglish.contains(query.trim(), ignoreCase = true)
        }
    }

    private fun normalizeArabicText(text: String): String {
        return text.replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .trim()
    }

    override fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>> = flow {
        val rows = ayahsBySurah[surahId].orEmpty()
        emit(rows.map { it.toAyah(reciterIdentifier) })
    }.flowOn(Dispatchers.Default)

    override fun getPageAyahs(pageNumber: Int, reciterIdentifier: String): Flow<List<Ayah>> = flow {
        if (pageNumber !in 1..604) {
            emit(emptyList())
            return@flow
        }
        val rows = ayahsByPage[pageNumber].orEmpty()
        emit(rows.map { it.toAyah(reciterIdentifier) })
    }.flowOn(Dispatchers.Default)

    override fun getPageForAyah(surahId: Int, ayahIndex: Int): Int {
        val rows = ayahsBySurah[surahId].orEmpty()
        return rows.getOrNull(ayahIndex)?.page ?: SurahData.SURAH_LIST.find { it.id == surahId }?.startPage ?: 1
    }


    private fun AyahJson.toAyah(audioBaseUrl: String): Ayah = Ayah(
        numberInSurah = numberInSurah,
        globalNumber = globalNumber,
        textArabic = sanitizeUthmanicText(textArabic),
        textTranslation = "",
        audioUrl = ayahAudioUrl(audioBaseUrl, surahId, numberInSurah),
        surahId = surahId,
        page = page,
        juz = juz,
    )
}
