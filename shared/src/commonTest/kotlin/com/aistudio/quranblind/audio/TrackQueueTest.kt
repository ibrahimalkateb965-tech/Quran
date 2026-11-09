package com.aistudio.quranblind.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackQueueTest {

    private fun track(n: Int) = AudioTrack(id = "1_$n", url = "https://example.test/$n.mp3")
    private fun tracks(vararg n: Int) = n.map(::track)

    @Test
    fun emptyQueue_hasNoCurrentAndIndexMinusOne() {
        val q = TrackQueue()
        assertEquals(0, q.size)
        assertEquals(-1, q.index)
        assertNull(q.current)
        assertFalse(q.advance())
    }

    @Test
    fun set_positionsOnStartIndex() {
        val q = TrackQueue()
        q.set(tracks(1, 2, 3), startIndex = 1)
        assertEquals(3, q.size)
        assertEquals(1, q.index)
        assertEquals(track(2), q.current)
    }

    @Test
    fun set_clampsStartIndexIntoRange() {
        val q = TrackQueue()
        q.set(tracks(1, 2), startIndex = 9)
        assertEquals(1, q.index)
        q.set(tracks(1, 2), startIndex = -4)
        assertEquals(0, q.index)
        q.set(emptyList(), startIndex = 0)
        assertEquals(-1, q.index)
        assertNull(q.current)
    }

    @Test
    fun advance_movesForwardUntilTheEnd() {
        val q = TrackQueue()
        q.set(tracks(1, 2, 3), startIndex = 0)
        assertTrue(q.advance())
        assertEquals(track(2), q.current)
        assertTrue(q.advance())
        assertEquals(track(3), q.current)
        assertFalse(q.advance())
        // A refused advance leaves the position untouched.
        assertEquals(2, q.index)
        assertEquals(track(3), q.current)
    }

    @Test
    fun clearUpcoming_dropsEverythingAfterCurrent() {
        val q = TrackQueue()
        q.set(tracks(1, 2, 3, 4), startIndex = 1)
        q.clearUpcoming()
        assertEquals(2, q.size)
        assertEquals(1, q.index)
        assertEquals(track(2), q.current)
        assertFalse(q.advance())
    }

    @Test
    fun replaceUpcoming_swapsTheTailAndKeepsCurrent() {
        val q = TrackQueue()
        q.set(tracks(1, 2, 3), startIndex = 0)
        q.replaceUpcoming(tracks(7, 8))
        assertEquals(3, q.size)
        assertEquals(track(1), q.current)
        assertTrue(q.advance())
        assertEquals(track(7), q.current)
        assertTrue(q.advance())
        assertEquals(track(8), q.current)
        assertFalse(q.advance())
    }

    @Test
    fun replaceUpcoming_onEmptyQueueStartsAtZero() {
        val q = TrackQueue()
        q.replaceUpcoming(tracks(5, 6))
        assertEquals(2, q.size)
        assertEquals(0, q.index)
        assertEquals(track(5), q.current)
    }

    @Test
    fun clear_resetsEverything() {
        val q = TrackQueue()
        q.set(tracks(1, 2), startIndex = 1)
        q.clear()
        assertEquals(0, q.size)
        assertEquals(-1, q.index)
        assertNull(q.current)
    }
}
