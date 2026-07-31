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
        return SURAH_LIST
    }

    fun getSurahById(id: Int): Surah? {
        return SURAH_LIST.find { it.id == id }
    }

    fun findSurahByName(query: String): Surah? {
        val cleanQuery = normalizeArabicText(query)
        return SURAH_LIST.find { surah ->
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
            val urlString = "https://api.alquran.cloud/v1/surah/$surahId/$reciterIdentifier"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

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
                return@withContext ayahs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: Offline generated audio URLs & placeholder text for essential continuous operation
        generateFallbackAyahs(surah, reciterIdentifier)
    }

    private fun generateFallbackAyahs(surah: Surah, reciterIdentifier: String): List<Ayah> {
        // Calculate global start ayah index roughly or use standard audio stream CDN pattern
        var currentGlobalNumber = 1
        for (s in SURAH_LIST) {
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

    companion object {
        val SURAH_LIST = listOf(
            Surah(1, "الفاتحة", "Al-Fatihah", "سورة الفاتحة", 7, "مكية", 1),
            Surah(2, "البقرة", "Al-Baqarah", "سورة البقرة", 286, "مدنية", 2),
            Surah(3, "آل عمران", "Aal-E-Imran", "سورة آل عمران", 200, "مدنية", 50),
            Surah(4, "النساء", "An-Nisa", "سورة النساء", 176, "مدنية", 77),
            Surah(5, "المائدة", "Al-Ma'idah", "سورة المائدة", 120, "مدنية", 106),
            Surah(6, "الأنعام", "Al-An'am", "سورة الأنعام", 165, "مكية", 128),
            Surah(7, "الأعراف", "Al-A'raf", "سورة الأعراف", 206, "مكية", 151),
            Surah(8, "الأنفال", "Al-Anfal", "سورة الأنفال", 75, "مدنية", 177),
            Surah(9, "التوبة", "At-Tawbah", "سورة التوبة", 129, "مدنية", 187),
            Surah(10, "يونس", "Yunus", "سورة يونس", 109, "مكية", 208),
            Surah(11, "هود", "Hud", "سورة هود", 123, "مكية", 221),
            Surah(12, "يوسف", "Yusuf", "سورة يوسف", 111, "مكية", 235),
            Surah(13, "الرعد", "Ar-Ra'd", "سورة الرعد", 43, "مدنية", 249),
            Surah(14, "إبراهيم", "Ibrahim", "سورة إبراهيم", 52, "مكية", 255),
            Surah(15, "الحجر", "Al-Hijr", "سورة الحجر", 99, "مكية", 262),
            Surah(16, "النحل", "An-Nahl", "سورة النحل", 128, "مكية", 267),
            Surah(17, "الإسراء", "Al-Isra", "سورة الإسراء", 111, "مكية", 282),
            Surah(18, "الكهف", "Al-Kahf", "سورة الكهف", 110, "مكية", 293),
            Surah(19, "مريم", "Maryamm", "سورة مريم", 98, "مكية", 305),
            Surah(20, "طه", "Taha", "سورة طه", 135, "مكية", 312),
            Surah(21, "الأنبياء", "Al-Anbiya", "سورة الأنبياء", 112, "مكية", 322),
            Surah(22, "الحج", "Al-Hajj", "سورة الحج", 78, "مدنية", 332),
            Surah(23, "المؤمنون", "Al-Mu'minun", "سورة المؤمنون", 118, "مكية", 342),
            Surah(24, "النور", "An-Nur", "سورة النور", 64, "مدنية", 350),
            Surah(25, "الفرقان", "Al-Furqan", "سورة الفرقان", 77, "مكية", 359),
            Surah(26, "الشعراء", "Ash-Shu'ara", "سورة الشعراء", 227, "مكية", 367),
            Surah(27, "النمل", "An-Naml", "سورة النمل", 93, "مكية", 377),
            Surah(28, "القصص", "Al-Qasas", "سورة القصص", 88, "مكية", 385),
            Surah(29, "العنكبوت", "Al-Ankabut", "سورة العنكبوت", 69, "مكية", 396),
            Surah(30, "الروم", "Ar-Rum", "سورة الروم", 60, "مكية", 404),
            Surah(31, "لقمان", "Luqman", "سورة لقمان", 34, "مكية", 411),
            Surah(32, "السجدة", "As-Sajdah", "سورة السجدة", 30, "مكية", 415),
            Surah(33, "الأحزاب", "Al-Ahzab", "سورة الأحزاب", 73, "مدنية", 418),
            Surah(34, "سبأ", "Saba", "سورة سبأ", 54, "مكية", 428),
            Surah(35, "فاطر", "Fatir", "سورة فاطر", 45, "مكية", 434),
            Surah(36, "يس", "Yasin", "سورة يس", 83, "مكية", 440),
            Surah(37, "الصافات", "As-Saffat", "سورة الصافات", 182, "مكية", 445),
            Surah(38, "ص", "Sad", "سورة ص", 88, "مكية", 453),
            Surah(39, "الزمر", "Az-Zumar", "سورة الزمر", 75, "مكية", 458),
            Surah(40, "غافر", "Ghafir", "سورة غافر", 85, "مكية", 467),
            Surah(41, "فصلت", "Fussilat", "سورة فصلت", 54, "مكية", 477),
            Surah(42, "الشورى", "Ash-Shura", "سورة الشورى", 53, "مكية", 483),
            Surah(43, "الزخرف", "Az-Zukhruf", "سورة الزخرف", 89, "مكية", 489),
            Surah(44, "الدخان", "Ad-Dukhan", "سورة الدخان", 59, "مكية", 496),
            Surah(45, "الجاثية", "Al-Jathiyah", "سورة الجاثية", 37, "مكية", 499),
            Surah(46, "الأحقاف", "Al-Ahqaf", "سورة الأحقاف", 35, "مكية", 502),
            Surah(47, "محمد", "Muhammad", "سورة محمد", 38, "مدنية", 507),
            Surah(48, "الفتح", "Al-Fath", "سورة الفتح", 29, "مدنية", 511),
            Surah(49, "الحجرات", "Al-Hujurat", "سورة الحجرات", 18, "مدنية", 515),
            Surah(50, "ق", "Qaf", "سورة ق", 45, "مكية", 518),
            Surah(51, "الذاريات", "Adh-Dhariyat", "سورة الذاريات", 60, "مكية", 520),
            Surah(52, "الطور", "At-Tur", "سورة الطور", 49, "مكية", 523),
            Surah(53, "النجم", "An-Najm", "سورة النجم", 62, "مكية", 526),
            Surah(54, "القمر", "Al-Qamar", "سورة القمر", 55, "مكية", 528),
            Surah(55, "الرحمن", "Ar-Rahman", "سورة الرحمن", 78, "مدنية", 531),
            Surah(56, "الواقعة", "Al-Waqi'ah", "سورة الواقعة", 96, "مكية", 534),
            Surah(57, "الحديد", "Al-Hadid", "سورة الحديد", 29, "مدنية", 537),
            Surah(58, "المجادلة", "Al-Mujadila", "سورة المجادلة", 22, "مدنية", 542),
            Surah(59, "الحشر", "Al-Hashr", "سورة الحشر", 24, "مدنية", 545),
            Surah(60, "الممتحنة", "Al-Mumtahanah", "سورة الممتحنة", 13, "مدنية", 549),
            Surah(61, "الصف", "As-Saff", "سورة الصف", 14, "مدنية", 551),
            Surah(62, "الجمعة", "Al-Jumu'ah", "سورة الجمعة", 11, "مدنية", 553),
            Surah(63, "المنافقون", "Al-Munafiqun", "سورة المنافقون", 11, "مدنية", 554),
            Surah(64, "التغابن", "At-Taghabun", "سورة التغابن", 18, "مدنية", 556),
            Surah(65, "الطلاق", "At-Talaq", "سورة الطلاق", 12, "مدنية", 558),
            Surah(66, "التحريم", "At-Tahrim", "سورة التحريم", 12, "مدنية", 560),
            Surah(67, "الملك", "Al-Mulk", "سورة الملك", 30, "مكية", 562),
            Surah(68, "القلم", "Al-Qalam", "سورة القلم", 52, "مكية", 564),
            Surah(69, "الحاقة", "Al-Haaqqah", "سورة الحاقة", 52, "مكية", 566),
            Surah(70, "المعارج", "Al-Ma'arij", "سورة المعارج", 44, "مكية", 568),
            Surah(71, "نوح", "Nuh", "سورة نوح", 28, "مكية", 570),
            Surah(72, "الجن", "Al-Jinn", "سورة الجن", 28, "مكية", 572),
            Surah(73, "المزمل", "Al-Muzzammil", "سورة المزمل", 20, "مكية", 574),
            Surah(74, "المدثر", "Al-Muddaththir", "سورة المدثر", 56, "مكية", 575),
            Surah(75, "القيامة", "Al-Qiyamah", "سورة القيامة", 40, "مكية", 577),
            Surah(76, "الإنسان", "Al-Insan", "سورة الإنسان", 31, "مدنية", 578),
            Surah(77, "المرسلات", "Al-Mursalat", "سورة المرسلات", 50, "مكية", 580),
            Surah(78, "النبأ", "An-Naba", "سورة النبأ", 40, "مكية", 582),
            Surah(79, "النازعات", "An-Nazi'at", "سورة النازعات", 46, "مكية", 583),
            Surah(80, "عبس", "Abasa", "سورة عبس", 42, "مكية", 585),
            Surah(81, "التكوير", "At-Takwir", "سورة التكوير", 29, "مكية", 586),
            Surah(82, "الانفطار", "Al-Infitar", "سورة الانفطار", 19, "مكية", 587),
            Surah(83, "المطففين", "Al-Mutaffifin", "سورة المطففين", 36, "مكية", 587),
            Surah(84, "الانشقاق", "Al-Inshiqaq", "سورة الانشقاق", 25, "مكية", 589),
            Surah(85, "البروج", "Al-Buruj", "سورة البروج", 22, "مكية", 590),
            Surah(86, "الطارق", "At-Tariq", "سورة الطارق", 17, "مكية", 591),
            Surah(87, "الأعلى", "Al-A'la", "سورة الأعلى", 19, "مكية", 591),
            Surah(88, "الغاشية", "Al-Ghashiyah", "سورة الغاشية", 26, "مكية", 592),
            Surah(89, "الفجر", "Al-Fajr", "سورة الفجر", 30, "مكية", 593),
            Surah(90, "البلد", "Al-Balad", "سورة البلد", 20, "مكية", 594),
            Surah(91, "الشمس", "Ash-Shams", "سورة الشمس", 15, "مكية", 595),
            Surah(92, "الليل", "Al-Layl", "سورة الليل", 21, "مكية", 595),
            Surah(93, "الضحى", "Ad-Duha", "سورة الضحى", 11, "مكية", 596),
            Surah(94, "الشرح", "Ash-Sharh", "سورة الشرح", 8, "مكية", 596),
            Surah(95, "التين", "At-Tin", "سورة التين", 8, "مكية", 597),
            Surah(96, "العلق", "Al-Alaq", "سورة العلق", 19, "مكية", 597),
            Surah(97, "القدر", "Al-Qadr", "سورة القدر", 5, "مكية", 598),
            Surah(98, "البينة", "Al-Bayyinah", "سورة البينة", 8, "مدنية", 598),
            Surah(99, "الزلزلة", "Az-Zalzalah", "سورة الزلزلة", 8, "مدنية", 599),
            Surah(100, "العاديات", "Al-Adiyat", "سورة العاديات", 11, "مكية", 599),
            Surah(101, "القارعة", "Al-Qari'ah", "سورة القارعة", 11, "مكية", 600),
            Surah(102, "التكاثر", "At-Takathur", "سورة التكاثر", 8, "مكية", 600),
            Surah(103, "العصر", "Al-Asr", "سورة العصر", 3, "مكية", 601),
            Surah(104, "الهمزة", "Al-Humazah", "سورة الهمزة", 9, "مكية", 601),
            Surah(105, "الفيل", "Al-Fil", "سورة الفيل", 5, "مكية", 601),
            Surah(106, "قريش", "Quraysh", "سورة قريش", 4, "مكية", 602),
            Surah(107, "الماعون", "Al-Ma'un", "سورة الماعون", 7, "مكية", 602),
            Surah(108, "الكوثر", "Al-Kawthar", "سورة الكوثر", 3, "مكية", 602),
            Surah(109, "الكافرون", "Al-Kafirun", "سورة الكافرون", 6, "مكية", 603),
            Surah(110, "النصر", "An-Nasr", "سورة النصر", 3, "مدنية", 603),
            Surah(111, "المسد", "Al-Masad", "سورة المسد", 5, "مكية", 603),
            Surah(112, "الإخلاص", "Al-Ikhlas", "سورة الإخلاص", 4, "مكية", 604),
            Surah(113, "الفلق", "Al-Falaq", "سورة الفلق", 5, "مكية", 604),
            Surah(114, "الناس", "An-Nas", "سورة الناس", 6, "مكية", 604)
        )
    }
}
