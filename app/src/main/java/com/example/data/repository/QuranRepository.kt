package com.example.data.repository

import android.content.Context
import com.example.data.local.AyahDao
import com.example.data.local.AyahEntity
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.QuranDatabase
import com.example.data.model.Ayah
import com.example.data.model.Surah
import com.example.data.model.SurahData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

class QuranRepository(private val context: Context) {
    private val database = QuranDatabase.getDatabase(context)
    private val bookmarkDao: BookmarkDao = database.bookmarkDao()
    private val ayahDao: AyahDao = database.ayahDao()

    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun toggleBookmark(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean {
        val isBookmarked = bookmarkDao.isBookmarked(surahId, ayahNumber)
        if (isBookmarked) {
            bookmarkDao.deleteBySurahAndAyah(surahId, ayahNumber)
            return false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    surahId = surahId,
                    surahNameAr = surahNameAr,
                    ayahNumber = ayahNumber
                )
            )
            return true
        }
    }

    suspend fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean {
        return bookmarkDao.isBookmarked(surahId, ayahNumber)
    }

    fun getAllSurahs(): List<Surah> {
        return SurahData.SURAH_LIST
    }

    fun getSurahById(id: Int): Surah? {
        return SurahData.SURAH_LIST.find { it.id == id }
    }

    fun findSurahByName(query: String): Surah? {
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

    fun getAyahs(
        surahId: Int,
        reciterIdentifier: String = "ar.alafasy"
    ): Flow<List<Ayah>> = flow {
        val surah = getSurahById(surahId)
        if (surah == null) {
            emit(emptyList())
            return@flow
        }

        // 1. Fetch from Offline Verified Dataset in Assets (Primary Official Source)
        val assetAyahs = loadSurahFromAssets(surahId, reciterIdentifier)
        if (assetAyahs.isNotEmpty()) {
            emit(assetAyahs)
        } else {
            // 2. Fallback to Local Database
            val localAyahs = ayahDao.getAyahsForSurah(surahId, reciterIdentifier)
            if (localAyahs.isNotEmpty()) {
                emit(localAyahs.map { it.toDomainModel().copy(textArabic = sanitizeUthmanicText(it.textArabic)) })
            } else {
                emit(emptyList())
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun loadSurahFromAssets(surahId: Int, reciterIdentifier: String): List<Ayah> {
        return try {
            val jsonString = context.assets.open("quran/quran_uthmani_tanzil.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val ayahsArray = jsonObject.optJSONArray(surahId.toString()) ?: return emptyList()
            val list = mutableListOf<Ayah>()
            val entities = mutableListOf<AyahEntity>()
            
            for (i in 0 until ayahsArray.length()) {
                val item = ayahsArray.getJSONObject(i)
                val numberInSurah = item.getInt("numberInSurah")
                val globalNumber = item.getInt("globalNumber")
                val text = sanitizeUthmanicText(item.getString("textArabic"))
                val page = item.optInt("page", 1)
                val juz = item.optInt("juz", 1)
                val audioUrl = resolveAudioEndpoint(reciterIdentifier, surahId, numberInSurah)
                
                val entity = AyahEntity(
                    globalNumber = globalNumber,
                    numberInSurah = numberInSurah,
                    textArabic = text,
                    textTranslation = "",
                    audioUrl = audioUrl,
                    surahId = surahId,
                    page = page,
                    juz = juz,
                    reciterIdentifier = reciterIdentifier
                )
                entities.add(entity)
                list.add(entity.toDomainModel())
            }
            if (entities.isNotEmpty()) {
                ayahDao.insertAyahs(entities)
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun sanitizeUthmanicText(text: String): String {
        val bareNoonNextLetters = "[يرملونصذثكجشقسدطزفتضظب]"
        val pattern = Regex("(ن)[\\u0652\\u06DF\\u06E0\\u06E1](?=\\s*$bareNoonNextLetters)")
        return text.replace(pattern, "$1")
            .replace('\u06E4', '\u0653')
            .replace("\uFEFF", "")
            .replace("\u200A", "")
            .replace("\u2060", "")
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
