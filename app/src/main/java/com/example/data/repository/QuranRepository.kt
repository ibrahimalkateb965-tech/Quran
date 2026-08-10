package com.example.data.repository

import android.content.Context
import com.example.data.local.AyahDao
import com.example.data.local.AyahEntity
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.QuranDatabase
import com.example.data.model.Ayah
import com.example.data.model.Surah
import com.example.data.network.QuranApiService
import com.example.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class QuranRepository(private val context: Context) {
    private val database = QuranDatabase.getDatabase(context)
    private val bookmarkDao: BookmarkDao = database.bookmarkDao()
    private val ayahDao: AyahDao = database.ayahDao()
    private val apiService: QuranApiService = NetworkModule.quranApiService

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
        return com.example.data.model.SurahData.SURAH_LIST
    }

    fun getSurahById(id: Int): Surah? {
        return com.example.data.model.SurahData.SURAH_LIST.find { it.id == id }
    }

    fun findSurahByName(query: String): Surah? {
        val cleanQuery = normalizeArabicText(query)
        return com.example.data.model.SurahData.SURAH_LIST.find { surah ->
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

        // 1. Fetch from Local Database
        val localAyahs = ayahDao.getAyahsForSurah(surahId, reciterIdentifier)
        if (localAyahs.isNotEmpty()) {
            emit(localAyahs.map { it.toDomainModel() })
        } else {
            // 2. Fetch from Network
            try {
                val response = apiService.getSurahAyahs(surahId, "quran-simple")
                if (response.isSuccessful && response.body() != null) {
                    val remoteAyahs = response.body()!!.data.ayahs
                    val entities = remoteAyahs.map {
                        
                        val finalAudioUrl = resolveAudioEndpoint(reciterIdentifier, surahId, it.numberInSurah)
                        
                        AyahEntity(
                            globalNumber = it.globalNumber,
                            numberInSurah = it.numberInSurah,
                            textArabic = it.text,
                            textTranslation = "",
                            audioUrl = finalAudioUrl,
                            surahId = surahId,
                            page = it.page,
                            juz = it.juz,
                            reciterIdentifier = reciterIdentifier
                        )
                    }
                    // Save to DB
                    ayahDao.insertAyahs(entities)
                    
                    // Emit newly saved data
                    emit(entities.map { it.toDomainModel() })
                } else {
                    // Emit fallback if network fails and local is empty
                    emit(generateFallbackAyahs(surah, reciterIdentifier))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Emit fallback on exception
                emit(generateFallbackAyahs(surah, reciterIdentifier))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun generateFallbackAyahs(surah: Surah, reciterIdentifier: String): List<Ayah> {
        var currentGlobalNumber = 1
        for (s in com.example.data.model.SurahData.SURAH_LIST) {
            if (s.id == surah.id) break
            currentGlobalNumber += s.ayahCount
        }

        val list = mutableListOf<Ayah>()
        for (i in 1..surah.ayahCount) {
            val globalNum = currentGlobalNumber + i - 1
            val sampleText = when {
                surah.id == 1 && i == 1 -> "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                surah.id == 1 && i == 2 -> "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"
                surah.id == 1 && i == 3 -> "الرَّحْمَٰنِ الرَّحِيمِ"
                surah.id == 1 && i == 4 -> "مَالِكِ يَوْمِ الدِّينِ"
                surah.id == 1 && i == 5 -> "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ"
                surah.id == 1 && i == 6 -> "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ"
                surah.id == 1 && i == 7 -> "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
                surah.id == 112 -> listOf("قُلْ هُوَ اللَّهُ أَحَدٌ", "اللَّهُ الصَّمَدُ", "لَمْ يَلِدْ وَلَمْ يُولَدْ", "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ").getOrElse(i - 1) { "الآية $i من سورة الإخلاص" }
                surah.id == 113 -> listOf("قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "مِن شَرِّ مَا خَلَقَ", "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ").getOrElse(i - 1) { "الآية $i من سورة الفلق" }
                surah.id == 114 -> listOf("قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "مَلِكِ النَّاسِ", "إِلَٰهِ النَّاسِ", "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "مِنَ الْجِنَّةِ وَالنَّاسِ").getOrElse(i - 1) { "الآية $i من سورة الناس" }
                else -> "آية رقم $i من ${surah.nameArabic}"
            }

            val finalAudioUrl = resolveAudioEndpoint(reciterIdentifier, surah.id, i)

            list.add(
                Ayah(
                    numberInSurah = i,
                    globalNumber = globalNum,
                    textArabic = sampleText,
                    audioUrl = finalAudioUrl,
                    surahId = surah.id
                )
            )
        }
        return list
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
