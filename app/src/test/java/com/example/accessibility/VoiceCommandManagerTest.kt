package com.example.accessibility

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class VoiceCommandManagerTest {

    private lateinit var voiceCommandManager: VoiceCommandManager
    private lateinit var parseCommandMethod: Method

    @Before
    fun setup() {
        // Mock the context and audio manager
        val context = mockk<Context>(relaxed = true)
        val audioManager = mockk<android.media.AudioManager>(relaxed = true)
        io.mockk.every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        voiceCommandManager = VoiceCommandManager(context)

        // Use reflection to access the private parseCommand method for unit testing
        parseCommandMethod = VoiceCommandManager::class.java.getDeclaredMethod("parseCommand", String::class.java)
        parseCommandMethod.isAccessible = true
    }

    private fun parse(text: String): VoiceCommandResult {
        return parseCommandMethod.invoke(voiceCommandManager, text) as VoiceCommandResult
    }

    @Test
    fun `test pause command variations`() {
        assertTrue(parse("توقف") is VoiceCommandResult.Pause)
        assertTrue(parse("إيقاف الصوت") is VoiceCommandResult.Pause)
        assertTrue(parse("اسكت لو سمحت") is VoiceCommandResult.Pause)
    }

    @Test
    fun `test resume command variations`() {
        assertTrue(parse("تشغيل") is VoiceCommandResult.Resume)
        assertTrue(parse("استئناف القراءة") is VoiceCommandResult.Resume)
        assertTrue(parse("واصل") is VoiceCommandResult.Resume)
    }

    @Test
    fun `test navigation commands`() {
        assertTrue(parse("التالي") is VoiceCommandResult.NextAyah)
        assertTrue(parse("الآية التي بعدها") is VoiceCommandResult.NextAyah)
        
        assertTrue(parse("السابق") is VoiceCommandResult.PreviousAyah)
        assertTrue(parse("ارجع للآية") is VoiceCommandResult.PreviousAyah)
    }

    @Test
    fun `test ayah number parsing with digits`() {
        val result = parse("آية 15")
        assertTrue(result is VoiceCommandResult.GoToAyahNumber)
        assertEquals(15, (result as VoiceCommandResult.GoToAyahNumber).ayahNumber)
    }

    @Test
    fun `test ayah number parsing with Arabic words`() {
        val result = parse("الآية الخامسة")
        assertTrue(result is VoiceCommandResult.GoToAyahNumber)
        assertEquals(5, (result as VoiceCommandResult.GoToAyahNumber).ayahNumber)
    }

    @Test
    fun `test play surah command`() {
        val result = parse("تشغيل سورة البقرة")
        assertTrue(result is VoiceCommandResult.PlaySurahByName)
        assertEquals("البقره", (result as VoiceCommandResult.PlaySurahByName).surahName)
    }

    @Test
    fun `test reciter change command`() {
        assertTrue(parse("صوت الحصري") is VoiceCommandResult.ChangeReciter)
        assertEquals("husary", (parse("صوت الحصري") as VoiceCommandResult.ChangeReciter).reciterId)
        
        assertTrue(parse("الشيخ العفاسي") is VoiceCommandResult.ChangeReciter)
        assertEquals("afasy", (parse("الشيخ العفاسي") as VoiceCommandResult.ChangeReciter).reciterId)
    }

    @Test
    fun `test unknown command`() {
        val result = parse("أخبرني بنكتة")
        assertTrue(result is VoiceCommandResult.UnknownCommand)
        assertEquals("أخبرني بنكتة", (result as VoiceCommandResult.UnknownCommand).originalText)
    }
}
