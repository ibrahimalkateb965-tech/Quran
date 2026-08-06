package com.example.data.repository

import android.content.Context
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.QuranDatabase
import com.example.data.model.Ayah
import com.example.data.model.Reciter
import com.example.data.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class QuranRepository(private val context: Context) {
    private val bookmarkDao: BookmarkDao = QuranDatabase.getDatabase(context).bookmarkDao()

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

    suspend fun fetchAyahsForSurah(
        surahId: Int,
        reciterIdentifier: String = "ar.alafasy"
    ): List<Ayah> = withContext(Dispatchers.IO) {
        val surah = getSurahById(surahId) ?: return@withContext emptyList()
        try {
            return@withContext kotlinx.coroutines.withTimeout(15_000) {
                val urlString = "https://api.alquran.cloud/v1/surah/$surahId/$reciterIdentifier"
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val rootObj = JSONObject(responseText)
                    val dataObj = rootObj.getJSONObject("data")
                    val ayahsArray = dataObj.getJSONArray("ayahs")

                    val ayahs = mutableListOf<Ayah>()
                    for (i in 0 until ayahsArray.length()) {
                        val item = ayahsArray.getJSONObject(i)
                        val numberInSurah = item.getInt("numberInSurah")
                        val globalNumber = item.getInt("number")
                        val textArabic = item.getString("text")
                        val audioUrl = item.optString("audio", "")

                        val finalAudioUrl = if (reciterIdentifier == "ar.husary") {
                            val formattedSurah = surahId.toString().padStart(3, '0')
                            val formattedAyah = numberInSurah.toString().padStart(3, '0')
                            "https://verse.mp3quran.net/data/Husary_64kbps/$formattedSurah$formattedAyah.mp3"
                        } else if (audioUrl.isNotBlank()) {
                            audioUrl
                        } else {
                            "https://cdn.islamic.network/quran/audio/128/$reciterIdentifier/$globalNumber.mp3"
                        }

                        ayahs.add(
                            Ayah(
                                numberInSurah = numberInSurah,
                                globalNumber = globalNumber,
                                textArabic = textArabic,
                                audioUrl = finalAudioUrl,
                                surahId = surahId,
                                page = item.optInt("page", 1),
                                juz = item.optInt("juz", 1)
                            )
                        )
                    }
                    return@withTimeout ayahs
                }
                return@withTimeout emptyList<Ayah>()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: Offline generated audio URLs & placeholder text for essential continuous operation
        generateFallbackAyahs(surah, reciterIdentifier)
    }

    private fun generateFallbackAyahs(surah: Surah, reciterIdentifier: String): List<Ayah> {
        // Calculate global start ayah index roughly or use standard audio stream CDN pattern
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

            val finalAudioUrl = if (reciterIdentifier == "ar.husary") {
                val formattedSurah = surah.id.toString().padStart(3, '0')
                val formattedAyah = i.toString().padStart(3, '0')
                "https://verse.mp3quran.net/data/Husary_64kbps/$formattedSurah$formattedAyah.mp3"
            } else {
                "https://cdn.islamic.network/quran/audio/128/$reciterIdentifier/$globalNum.mp3"
            }

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

}
