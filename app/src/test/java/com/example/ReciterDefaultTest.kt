package com.example

import com.aistudio.quranblind.domain.model.Reciter
import com.example.ui.viewmodel.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * يثبّت القارئ الافتراضي على "محمود خليل الحصري مجود" ورابط خادمه الرسمي.
 *
 * سبب وجود هذا الاختبار: كان الرجوع الافتراضي يستخدم `DEFAULT_RECITERS.first()`
 * أي "ابراهيم الأخضر"، فيُستبدل القارئ المطلوب بقارئ آخر دون إشعار المستخدم.
 */
class ReciterDefaultTest {

    @Test
    fun defaultReciter_isHusaryMujawwad() {
        assertEquals("husary_mujawwad", Reciter.DEFAULT_RECITER.id)
        assertEquals("محمود خليل الحصري مجود", Reciter.DEFAULT_RECITER.nameArabic)
    }

    @Test
    fun husaryMujawwad_usesOfficialMp3QuranEndpoint() {
        val husaryMujawwad = Reciter.DEFAULT_RECITERS.first { it.id == "husary_mujawwad" }
        assertEquals(
            "https://verse.mp3quran.net/data/Husary_128kbps_Mujawwad/",
            husaryMujawwad.serverIdentifier
        )
    }

    @Test
    fun settingsUiState_defaultsToHusaryMujawwad_notFirstListEntry() {
        assertEquals("husary_mujawwad", SettingsUiState().selectedReciter.id)
    }

    @Test
    fun husaryVariants_haveDistinctEndpoints() {
        val ids = listOf("husary", "husary_mujawwad", "husary_muallim")
        val endpoints = ids.map { id ->
            Reciter.DEFAULT_RECITERS.first { it.id == id }.serverIdentifier
        }
        assertEquals(
            "Each Husary variant must resolve to its own server folder",
            ids.size,
            endpoints.toSet().size
        )
    }

    @Test
    fun everyReciterEndpoint_isAnAbsoluteDirectoryUrl() {
        Reciter.DEFAULT_RECITERS.forEach { reciter ->
            assertTrue(
                "${reciter.id} must expose an https directory URL ending with '/'",
                reciter.serverIdentifier.startsWith("https://") &&
                    reciter.serverIdentifier.endsWith("/")
            )
        }
    }
}
