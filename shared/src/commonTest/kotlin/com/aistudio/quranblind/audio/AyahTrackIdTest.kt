package com.aistudio.quranblind.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AyahTrackIdTest {

    @Test
    fun encode_matchesTheAndroidMediaIdFormat() {
        // Must stay byte-identical to "${ayah.surahId}_${ayah.numberInSurah}" in QuranViewModel.
        assertEquals("2_255", AyahTrackId.encode(2, 255))
        assertEquals("1_1", AyahTrackId.encode(1, 1))
        assertEquals("114_6", AyahTrackId.encode(114, 6))
    }

    @Test
    fun decode_roundTripsEncode() {
        assertEquals(2 to 255, AyahTrackId.decode(AyahTrackId.encode(2, 255)))
        assertEquals(114 to 6, AyahTrackId.decode("114_6"))
    }

    @Test
    fun decode_rejectsMalformedIds() {
        assertNull(AyahTrackId.decode(""))
        assertNull(AyahTrackId.decode("2"))
        assertNull(AyahTrackId.decode("2_"))
        assertNull(AyahTrackId.decode("_255"))
        assertNull(AyahTrackId.decode("x_255"))
        assertNull(AyahTrackId.decode("2_y"))
        assertNull(AyahTrackId.decode("1_2_3"))
        assertNull(AyahTrackId.decode("2-255"))
    }
}
