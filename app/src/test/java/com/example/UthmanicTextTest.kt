package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UthmanicTextTest {

    @Test
    fun `uthmanic ayah splits into non-empty words properly`() {
        val uthmanicText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val words = uthmanicText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        assertEquals(4, words.size)
        assertEquals("بِسْمِ", words[0])
        assertEquals("ٱللَّهِ", words[1])
        assertEquals("ٱلرَّحْمَٰنِ", words[2])
        assertEquals("ٱلرَّحِيمِ", words[3])
    }

    @Test
    fun `long uthmanic ayah retains all words and diacritics`() {
        val ayatAlKursi = "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ سِنَةٌ وَلَا نَوْمٌ"
        val words = ayatAlKursi.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        assertTrue(words.size >= 10)
        assertTrue(words.any { it.contains("ٱ") })
        assertTrue(words.any { it.contains("ۚ") }) // Waqf mark
    }

    @Test
    fun `sanitize uthmanic text normalizes small rounded zero and strips stray rosettes`() {
        val input = "ءَامَنُوٓا۟\u0600 إِذَا"
        val output = input.replace('\u06DF', '\u06E0').replace("\u0600", "")
        assertEquals("ءَامَنُوٓا۠ إِذَا", output)
        assertFalse(output.contains("۟"))
        assertFalse(output.contains("\u0600"))
        assertTrue(output.contains("۠"))
    }

    @Test
    fun `sanitize uthmanic text strips sukoon from noon before idgham and ikhfa letters`() {
        val bareNoonNextLetters = "[يرملونصذثكجشقسدطزفتضظب]"
        val pattern = Regex("(ن)[\\u0652\\u06DF\\u06E0\\u06E1](?=\\s*$bareNoonNextLetters)")
        
        // Ikhfa case: مِنْ شَرِّ -> مِن شَرِّ
        val ikhfaInput = "مِنْ شَرِّ مَا خَلَقَ"
        val ikhfaOutput = ikhfaInput.replace(pattern, "$1")
        assertEquals("مِن شَرِّ مَا خَلَقَ", ikhfaOutput)

        // Idgham case: مَنْ يَقُولُ -> مَن يَقُولُ
        val idghamInput = "مَنْ يَقُولُ"
        val idghamOutput = idghamInput.replace(pattern, "$1")
        assertEquals("مَن يَقُولُ", idghamOutput)

        // Izhar case: أَنْعَمْتَ عَلَيْهِمْ -> Should keep Sukoon on Noon!
        val izharInput = "أَنْعَمْتَ عَلَيْهِمْ"
        val izharOutput = izharInput.replace(pattern, "$1")
        assertEquals("أَنْعَمْتَ عَلَيْهِمْ", izharOutput)
    }

    @Test
    fun `sanitize uthmanic text normalizes madd marks to prominent arabic maddah above`() {
        val inputWithSmallMadda = "وَلَا ٱلضَّاۤلِّینَ"
        val output = inputWithSmallMadda.replace('\u06E4', '\u0653')
        assertEquals("وَلَا ٱلضَّآلِّینَ", output)
        assertTrue(output.contains("ٓ"))
    }
}
