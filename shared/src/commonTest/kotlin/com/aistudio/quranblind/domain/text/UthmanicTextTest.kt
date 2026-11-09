package com.aistudio.quranblind.domain.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Byte-identity port of app/src/test/java/com/example/UthmanicTextTest.kt.
// Every fixture is written as escaped code points so no editor bidi
// reordering or font substitution can alter it silently.
class UthmanicTextTest {

    @Test
    fun t1SmallRoundedZeroBecomesRingAndRosetteStripped() {
        val input = "\u0621\u064E\u0627\u0645\u064E\u0646\u064F\u0648\u0653\u0627\u06DF\u0600 \u0625\u0650\u0630\u064E\u0627"
        val expected = "\u0621\u064E\u0627\u0645\u064E\u0646\u064F\u0648\u0653\u0627\u06E0 \u0625\u0650\u0630\u064E\u0627"
        val output = sanitizeUthmanicText(input)
        assertEquals(expected, output)
        assertFalse(output.contains("\u06DF"))
        assertFalse(output.contains("\u0600"))
        assertTrue(output.contains("\u06E0"))
    }

    @Test
    fun t2IkhfaNoonSukoonStrippedBeforeIkhfaLetter() {
        val input = "\u0645\u0650\u0646\u0652 \u0634\u064E\u0631\u0651\u0650 \u0645\u064E\u0627 \u062E\u064E\u0644\u064E\u0642\u064E"
        val expected = "\u0645\u0650\u0646 \u0634\u064E\u0631\u0651\u0650 \u0645\u064E\u0627 \u062E\u064E\u0644\u064E\u0642\u064E"
        assertEquals(expected, sanitizeUthmanicText(input))
    }

    @Test
    fun t2bIdghamNoonSukoonStrippedBeforeIdghamLetter() {
        val input = "\u0645\u064E\u0646\u0652 \u064A\u064E\u0642\u064F\u0648\u0644\u064F"
        val expected = "\u0645\u064E\u0646 \u064A\u064E\u0642\u064F\u0648\u0644\u064F"
        assertEquals(expected, sanitizeUthmanicText(input))
    }

    @Test
    fun t2cIzharNoonSukoonSurvivesBeforeIzharLetter() {
        val input = "\u0623\u064E\u0646\u0652\u0639\u064E\u0645\u0652\u062A\u064E \u0639\u064E\u0644\u064E\u064A\u0652\u0647\u0650\u0645\u0652"
        assertEquals(input, sanitizeUthmanicText(input))
    }

    @Test
    fun t3SmallMaddaBecomesMaddahAbove() {
        val input = "\u0648\u064E\u0644\u064E\u0627 \u0671\u0644\u0636\u0651\u064E\u0627\u06E4\u0644\u0651\u0650\u06CC\u0646\u064E"
        val expected = "\u0648\u064E\u0644\u064E\u0627 \u0671\u0644\u0636\u0651\u064E\u0627\u0653\u0644\u0651\u0650\u06CC\u0646\u064E"
        val output = sanitizeUthmanicText(input)
        assertEquals(expected, output)
        assertTrue(output.contains("\u0653"))
    }

    @Test
    fun t4NbspBreaksLookaheadSoSukoonSurvives() {
        // On the JVM \s does not match U+00A0 without UNICODE_CHARACTER_CLASS,
        // so the noon-sukoon lookahead fails and the sukoon is kept.
        val input = "\u0645\u0650\u0646\u0652\u00A0\u0634\u064E\u0631\u0651\u0650"
        assertEquals(input, sanitizeUthmanicText(input))
    }

    @Test
    fun t5BasmalaUnchangedAndSplitsIntoFourWords() {
        val input = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064E\u0647\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650"
        assertEquals(input, sanitizeUthmanicText(input))
        val words = input.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        assertEquals(4, words.size)
        assertEquals("\u0628\u0650\u0633\u0652\u0645\u0650", words[0])
        assertEquals("\u0671\u0644\u0644\u0651\u064E\u0647\u0650", words[1])
        assertEquals("\u0671\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650", words[2])
        assertEquals("\u0671\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650", words[3])
    }

    @Test
    fun t6SanitizeIsIdempotentForEveryFixture() {
        val fixtures = listOf(
            "\u0621\u064E\u0627\u0645\u064E\u0646\u064F\u0648\u0653\u0627\u06DF\u0600 \u0625\u0650\u0630\u064E\u0627",
            "\u0645\u0650\u0646\u0652 \u0634\u064E\u0631\u0651\u0650 \u0645\u064E\u0627 \u062E\u064E\u0644\u064E\u0642\u064E",
            "\u0645\u064E\u0646\u0652 \u064A\u064E\u0642\u064F\u0648\u0644\u064F",
            "\u0623\u064E\u0646\u0652\u0639\u064E\u0645\u0652\u062A\u064E \u0639\u064E\u0644\u064E\u064A\u0652\u0647\u0650\u0645\u0652",
            "\u0648\u064E\u0644\u064E\u0627 \u0671\u0644\u0636\u0651\u064E\u0627\u06E4\u0644\u0651\u0650\u06CC\u0646\u064E",
            "\u0645\u0650\u0646\u0652\u00A0\u0634\u064E\u0631\u0651\u0650",
            "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064E\u0647\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650"
        )
        for (fixture in fixtures) {
            val once = sanitizeUthmanicText(fixture)
            assertEquals(once, sanitizeUthmanicText(once), "Not idempotent")
        }
    }
}
