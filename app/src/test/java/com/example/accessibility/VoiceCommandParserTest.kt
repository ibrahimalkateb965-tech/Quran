package com.example.accessibility

import com.example.data.model.Surah
import com.example.data.repository.QuranRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceCommandParserTest {

    private lateinit var parser: VoiceCommandParser
    private lateinit var repository: QuranRepository

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.findSurahByName("البقرة") } returns Surah(
            2, "البقرة", "Al-Baqarah", "سورة البقرة", 286, "مدنية", 2
        )
        parser = VoiceCommandParser(repository)
    }

    private fun parse(text: String): VoiceCommandResult = parser.parseCommand(text)

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
        assertEquals("البقرة", (result as VoiceCommandResult.PlaySurahByName).surahName)
    }

    @Test
    fun `test continuous play command`() {
        assertTrue(parse("استماع متواصل") is VoiceCommandResult.ToggleContinuousPlay)
        assertTrue(parse("تشغيل متواصل") is VoiceCommandResult.ToggleContinuousPlay)
    }

    @Test
    fun `test replay ayah command`() {
        assertTrue(parse("كرر الآية") is VoiceCommandResult.ReplayAyah)
        assertTrue(parse("اعادة الاية") is VoiceCommandResult.ReplayAyah)
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
