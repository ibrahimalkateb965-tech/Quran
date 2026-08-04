package com.example.accessibility

import com.example.data.model.Reciter
import com.example.data.repository.QuranRepository

/**
 * محلل الأوامر الصوتية المُحسّن.
 *
 * يفصل منطق تحليل النصوص عن إدارة الميكروفون، مما يسهّل اختبار الوحدة.
 * يعتمد على:
 * - خرائط مرادفات للأوامر.
 * - Regex لاستخراج رقم الآية.
 * - Regex لاستخراج اسم السورة.
 * - مطابقة أسماء القراء المختصرة.
 */
class VoiceCommandParser(private val quranRepository: QuranRepository) {

    fun parseCommand(rawText: String): VoiceCommandResult {
        val clean = normalizeArabicText(rawText)

        // Priority order matters: more specific patterns first.

        // Surah play commands: extract from raw text so repository normalization works.
        val surahName = extractSurahName(rawText)
        if (surahName != null) {
            val found = quranRepository.findSurahByName(surahName)
            return if (found != null) {
                VoiceCommandResult.PlaySurahByName(found.nameArabic)
            } else {
                VoiceCommandResult.UnknownCommand(rawText)
            }
        }

        // Ayah number command: extract from raw text.
        val ayahNumber = extractAyahNumber(rawText)
        if (ayahNumber != null && ayahNumber > 0) {
            return VoiceCommandResult.GoToAyahNumber(ayahNumber)
        }

        // Reciter names (check before generic commands to avoid "تشغيل" swallowing them).
        val reciterId = extractReciter(clean)
        if (reciterId != null) {
            return VoiceCommandResult.ChangeReciter(reciterId)
        }

        // Generic command matching via normalized synonym maps.
        return when {
            matchesAny(clean, PAUSE_SYNONYMS) -> VoiceCommandResult.Pause
            matchesAny(clean, CONTINUOUS_SYNONYMS) -> VoiceCommandResult.ToggleContinuousPlay
            matchesAny(clean, RESUME_SYNONYMS) -> VoiceCommandResult.Resume
            matchesAny(clean, NEXT_SYNONYMS) -> VoiceCommandResult.NextAyah
            matchesAny(clean, PREVIOUS_SYNONYMS) -> VoiceCommandResult.PreviousAyah
            matchesAny(clean, BOOKMARK_SYNONYMS) -> VoiceCommandResult.ToggleBookmark
            matchesAny(clean, REPEAT_ONCE_SYNONYMS) -> VoiceCommandResult.ReplayAyah
            matchesAny(clean, REPEAT_MODE_SYNONYMS) -> VoiceCommandResult.ToggleRepeatMode
            matchesAny(clean, SURAH_INDEX_SYNONYMS) -> VoiceCommandResult.ShowSurahIndex
            matchesAny(clean, HELP_SYNONYMS) -> VoiceCommandResult.ShowHelp
            else -> VoiceCommandResult.UnknownCommand(rawText)
        }
    }

    private fun extractSurahName(text: String): String? {
        // Match patterns like "سورة البقرة", "شغل سورة الكهف", "تشغيل سورة الناس"
        val regex = Regex("""(?:تشغيل|شغل|شغلي|افتح|اعرض)?\s*(?:سور[ةه]|صور[ةه])\s+(.+?)(?:\s+من|\s+آية|\s+الآية|\$)""")
        regex.find(text)?.groupValues?.get(1)?.trim()?.let { return it }

        // Fallback: "سورة البقرة" at end of text
        val simpleRegex = Regex("""(?:سور[ةه]|صور[ةه])\s+(.+)""")
        simpleRegex.find(text)?.groupValues?.get(1)?.trim()?.let { return it }

        return null
    }

    private fun extractAyahNumber(text: String): Int? {
        // Normalize first to handle "آية", "أيه", "إيه" variations uniformly.
        val normalized = normalizeArabicText(text)

        // Digits: "الآية 5", "ايه 12", "الانتقال للآية 255"
        val digitRegex = Regex("""(?:الايه|ايه|الآية)\s*(\d+)""")
        digitRegex.find(normalized)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // Spoken Arabic number words
        return parseArabicNumberWord(normalized)
    }

    private fun extractReciter(text: String): String? {
        return when {
            text.contains("الحصري") -> "husary"
            text.contains("العفاسي") || text.contains("مشاري") -> "afasy"
            text.contains("المنشاوي") -> "minshawi"
            text.contains("عبد الباسط") -> "abdulbasit"
            text.contains("ماهر") || text.contains("المعيقلي") -> "maher"
            text.contains("صوفي") -> "sufi"
            else -> null
        }
    }

    private fun parseArabicNumberWord(text: String): Int? {
        val wordMap = mapOf(
            "واحد" to 1, "الاولي" to 1, "الاول" to 1,
            "اثنان" to 2, "ثاني" to 2, "الثانيه" to 2,
            "ثلاثه" to 3, "الثالثه" to 3, "ثلاث" to 3,
            "اربعه" to 4, "الرابعه" to 4, "اربع" to 4,
            "خمسه" to 5, "الخامسه" to 5, "خمس" to 5,
            "سته" to 6, "السادسه" to 6, "ست" to 6,
            "سبعه" to 7, "السابعه" to 7, "سبع" to 7,
            "ثمانيه" to 8, "الثامنه" to 8, "ثمان" to 8,
            "تسعه" to 9, "التاسعه" to 9, "تسع" to 9,
            "عشره" to 10, "العاشره" to 10, "عشر" to 10
        )
        for ((word, value) in wordMap) {
            // Use word boundaries to avoid matching substrings like "ست" inside "استماع".
            val regex = Regex("""(?:^|\s)$word(?:\s|$)""")
            if (regex.containsMatchIn(text)) return value
        }
        return null
    }

    private fun matchesAny(text: String, synonyms: List<String>): Boolean {
        return synonyms.any { text.contains(normalizeArabicText(it)) }
    }

    private fun normalizeArabicText(text: String): String {
        return text.trim()
            .replace(Regex("[ًٌٍَُِّْـ]"), "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("ى", "ي")
            .lowercase()
    }

    companion object {
        private val PAUSE_SYNONYMS = listOf("توقف", "ايقاف", "اسكت", "اقطع", "قف", "وقف")
        private val RESUME_SYNONYMS = listOf("تشغيل", "استئناف", "واصل", "كمل", "شغل", "شغلي")
        private val NEXT_SYNONYMS = listOf("التالي", "بعده", "بعدها", "تقدم", "قدام", "التاليه")
        private val PREVIOUS_SYNONYMS = listOf("السابق", "قبلها", "قبله", "ارجع", "ورا", "السابقه")
        private val BOOKMARK_SYNONYMS = listOf("مرجعيه", "علامه", "حفظ الايه", "مفضله", "اشاره")
        private val REPEAT_ONCE_SYNONYMS = listOf("كرر الايه", "اعاده الايه", "اعد الايه", "كررها")
        private val REPEAT_MODE_SYNONYMS = listOf("تكرار", "تاكيد", "تركيز", "حفظ", "تكرار الايات")
        private val CONTINUOUS_SYNONYMS = listOf("استماع متواصل", "تشغيل متواصل", "تلقائي", "متواصل")
        private val SURAH_INDEX_SYNONYMS = listOf("قائمه", "قايمه", "فهرس", "السور", "الصور")
        private val HELP_SYNONYMS = listOf("تعليمات", "مساعده", "الاوامر", "دليل", "مساعدة")
    }
}
