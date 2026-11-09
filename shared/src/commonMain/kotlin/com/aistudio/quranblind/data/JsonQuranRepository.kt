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
        json.decodeFromString<Map<String, List<AyahJson>>>(source.readQuranJson())
            .mapKeys { (key, _) -> key.toInt() }
    }

    override fun getAllSurahs(): List<Surah> = SurahData.SURAH_LIST

    override fun getSurahById(id: Int): Surah? = SurahData.SURAH_LIST.find { it.id == id }

    override fun findSurahByName(query: String): Surah? {
        val q = query.trim()
        if (q.isEmpty()) return null
        return SurahData.SURAH_LIST.find {
            it.nameArabic.contains(q) || it.nameEnglish.contains(q, ignoreCase = true)
        }
    }

    override fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>> = flow {
        val rows = ayahsBySurah[surahId].orEmpty()
        emit(rows.map { it.toAyah(reciterIdentifier) })
    }.flowOn(Dispatchers.Default)

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
