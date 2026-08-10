package com.example.ui.components.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayerHintsTest {

    @Test
    fun `talkback on - navigation hint describes two-finger swipe`() {
        val hints = playerGestureHints(isTalkBackEnabled = true)
        assertEquals("سحب بإصبعين: آية آية", hints.navigationHint)
        assertEquals("نقرتان: تشغيل/إيقاف", hints.playPauseHint)
    }

    @Test
    fun `talkback off - sighted hints unchanged`() {
        val hints = playerGestureHints(isTalkBackEnabled = false)
        assertEquals("سحب أفقي: آية آية", hints.navigationHint)
        assertEquals("نقرتين: تشغيل/إيقاف", hints.playPauseHint)
    }

    @Test
    fun `status description - talkback on uses two-finger instruction`() {
        val desc = buildPlayerStatusDescription(
            surahName = "الفاتحة", ayahNumber = 3, ayahCount = 7,
            isPlaying = true, isRepeatModeActive = false, isTalkBackEnabled = true
        )
        assertTrue(desc.contains("اسحب بإصبعين يميناً أو يساراً للتنقل بين الآيات"))
        assertTrue(desc.contains("سورة الفاتحة، الآية 3 من أصل 7"))
        assertTrue(desc.contains("جاري التشغيل"))
    }

    @Test
    fun `status description - talkback off keeps original instruction`() {
        val desc = buildPlayerStatusDescription(
            surahName = null, ayahNumber = 1, ayahCount = null,
            isPlaying = false, isRepeatModeActive = true, isTalkBackEnabled = false
        )
        assertTrue(desc.contains("اسحب يميناً ويساراً للتنقل"))
        assertFalse(desc.contains("بإصبعين"))
        assertFalse(desc.contains("سورة"))
        assertTrue(desc.contains("متوقف مؤقتاً"))
        assertTrue(desc.contains("وضع التكرار مفعّل"))
    }
}
