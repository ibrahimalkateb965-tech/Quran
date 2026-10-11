package com.example.data.repository

import android.content.Context
import com.aistudio.quranblind.domain.text.sanitizeUthmanicText
import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import com.aistudio.quranblind.domain.model.SurahData
import com.example.domain.repository.QuranRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : QuranRepository {

    @Volatile
    private var cachedQuranJson: JSONObject? = null

    @Volatile
    private var cachedPageMap: Map<Int, List<Ayah>>? = null

    override fun getAllSurahs(): List<Surah> {
        return SurahData.SURAH_LIST
    }

    override fun getSurahById(id: Int): Surah? {
        return SurahData.SURAH_LIST.find { it.id == id }
    }

    override fun findSurahByName(query: String): Surah? {
        val cleanQuery = normalizeArabicText(query)
        return SurahData.SURAH_LIST.find { surah ->
            val cleanSurahName = normalizeArabicText(surah.nameArabic)
            cleanSurahName.contains(cleanQuery) || cleanQuery.contains(cleanSurahName)
        }
    }

    private fun normalizeArabicText(text: String): String {
        return text.replace(Regex("[ًٌٍَُِّْـ]"), "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .trim()
    }

    override fun getAyahs(
        surahId: Int,
        reciterIdentifier: String
    ): Flow<List<Ayah>> = flow {
        val surah = getSurahById(surahId)
        if (surah == null) {
            emit(emptyList())
            return@flow
        }

        // 1. Fetch from Offline Verified Dataset in Assets (Primary Official Source)
        val assetAyahs = loadSurahFromAssets(surahId, reciterIdentifier)
        emit(assetAyahs)
    }.flowOn(Dispatchers.IO)

    override fun getPageAyahs(
        pageNumber: Int,
        reciterIdentifier: String
    ): Flow<List<Ayah>> = flow {
        if (pageNumber !in 1..604) {
            emit(emptyList())
            return@flow
        }
        val pageMap = getOrBuildPageMap()
        val baseAyahs = pageMap[pageNumber] ?: emptyList()
        val ayahsWithAudio = if (reciterIdentifier.isNotBlank()) {
            baseAyahs.map { ayah ->
                ayah.copy(audioUrl = resolveAudioEndpoint(reciterIdentifier, ayah.surahId, ayah.numberInSurah))
            }
        } else {
            baseAyahs
        }
        emit(ayahsWithAudio)
    }.flowOn(Dispatchers.IO)

    override fun getPageForAyah(surahId: Int, ayahIndex: Int): Int {
        val jsonObject = getOrLoadQuranJson() ?: return SurahData.SURAH_LIST.find { it.id == surahId }?.startPage ?: 1
        val ayahsArray = jsonObject.optJSONArray(surahId.toString()) ?: return SurahData.SURAH_LIST.find { it.id == surahId }?.startPage ?: 1
        if (ayahIndex in 0 until ayahsArray.length()) {
            val item = ayahsArray.optJSONObject(ayahIndex)
            val p = item?.optInt("page", 0) ?: 0
            if (p in 1..604) return p
        }
        return SurahData.SURAH_LIST.find { it.id == surahId }?.startPage ?: 1
    }

    private fun getOrLoadQuranJson(): JSONObject? {
        cachedQuranJson?.let { return it }
        return synchronized(this) {
            cachedQuranJson ?: try {
                val jsonString = context.assets.open("quran/quran_uthmani_tanzil.json").bufferedReader().use { it.readText() }
                JSONObject(jsonString).also { cachedQuranJson = it }
            } catch (e: Exception) {
                try {
                    android.util.Log.e("QuranRepository", "Error reading Quran assets JSON", e)
                } catch (_: Throwable) {}
                null
            }
        }
    }

    private fun getOrBuildPageMap(): Map<Int, List<Ayah>> {
        cachedPageMap?.let { return it }
        return synchronized(this) {
            cachedPageMap ?: run {
                val jsonObject = getOrLoadQuranJson() ?: return emptyMap()
                val map = mutableMapOf<Int, MutableList<Ayah>>()
                for (surahId in 1..114) {
                    val ayahsArray = jsonObject.optJSONArray(surahId.toString()) ?: continue
                    for (i in 0 until ayahsArray.length()) {
                        val item = ayahsArray.getJSONObject(i)
                        val numberInSurah = item.getInt("numberInSurah")
                        val globalNumber = item.getInt("globalNumber")
                        val text = sanitizeUthmanicText(item.getString("textArabic"))
                        val page = item.optInt("page", 1)
                        val juz = item.optInt("juz", 1)

                        val ayah = Ayah(
                            numberInSurah = numberInSurah,
                            globalNumber = globalNumber,
                            textArabic = text,
                            textTranslation = "",
                            audioUrl = "",
                            surahId = surahId,
                            page = page,
                            juz = juz
                        )
                        map.getOrPut(page) { mutableListOf() }.add(ayah)
                    }
                }
                map.also { cachedPageMap = it }
            }
        }
    }

    private fun loadSurahFromAssets(surahId: Int, reciterIdentifier: String): List<Ayah> {
        return try {
            val jsonObject = getOrLoadQuranJson() ?: return emptyList()
            val ayahsArray = jsonObject.optJSONArray(surahId.toString()) ?: return emptyList()
            val list = mutableListOf<Ayah>()

            for (i in 0 until ayahsArray.length()) {
                val item = ayahsArray.getJSONObject(i)
                val numberInSurah = item.getInt("numberInSurah")
                val globalNumber = item.getInt("globalNumber")
                val text = sanitizeUthmanicText(item.getString("textArabic"))
                val page = item.optInt("page", 1)
                val juz = item.optInt("juz", 1)
                val audioUrl = resolveAudioEndpoint(reciterIdentifier, surahId, numberInSurah)

                list.add(
                    Ayah(
                        numberInSurah = numberInSurah,
                        globalNumber = globalNumber,
                        textArabic = text,
                        textTranslation = "",
                        audioUrl = audioUrl,
                        surahId = surahId,
                        page = page,
                        juz = juz
                    )
                )
            }
            list
        } catch (e: Exception) {
            try {
                android.util.Log.e("QuranRepository", "Error parsing Ayahs for Surah $surahId", e)
            } catch (_: Throwable) {}
            emptyList()
        }
    }

    private fun resolveAudioEndpoint(
        audioBaseUrl: String,
        surahId: Int,
        ayahInSurah: Int
    ): String {
        val sanitizedBaseUrl = if (audioBaseUrl.endsWith("/")) audioBaseUrl else "$audioBaseUrl/"
        val formattedSurah = surahId.toString().padStart(3, '0')
        val formattedAyah = ayahInSurah.toString().padStart(3, '0')
        return "$sanitizedBaseUrl$formattedSurah$formattedAyah.mp3"
    }
}
