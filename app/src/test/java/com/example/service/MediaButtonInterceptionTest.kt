package com.example.service

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives the production decision used by QuranAudioService.onMediaButtonEvent.
 *
 * The suppression only exists to stop playback resuming by itself when a phone call ends, so these
 * tests are split evenly between proving it fires there and proving it stays out of the way
 * everywhere else -- media3 routes notification, lock-screen and headset buttons through the very
 * same callback, and swallowing those leaves the user with dead controls.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaButtonInterceptionTest {

    private fun keyDown(keyCode: Int) = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
    private fun keyUp(keyCode: Int) = KeyEvent(KeyEvent.ACTION_UP, keyCode)

    private val insideWindow = MediaButtonPolicy.CALL_END_SUPPRESSION_WINDOW_MS - 1
    private val outsideWindow = MediaButtonPolicy.CALL_END_SUPPRESSION_WINDOW_MS + 1

    // --- The bug being fixed: unsolicited resume right after a call ends ---

    @Test
    fun `play keys are consumed just after a call ends while paused`() {
        for (keyCode in listOf(
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK
        )) {
            assertTrue(
                "keyCode $keyCode should be consumed inside the call-end window",
                MediaButtonPolicy.shouldConsume(keyDown(keyCode), playWhenReady = false, insideWindow)
            )
        }
    }

    @Test
    fun `play key is consumed while a call is still in progress`() {
        assertTrue(
            MediaButtonPolicy.shouldConsume(
                keyDown(KeyEvent.KEYCODE_MEDIA_PLAY),
                playWhenReady = false,
                millisSinceCallEnded = 0L
            )
        )
    }

    @Test
    fun `ACTION_UP is consumed as well as ACTION_DOWN`() {
        assertTrue(
            MediaButtonPolicy.shouldConsume(
                keyUp(KeyEvent.KEYCODE_MEDIA_PLAY),
                playWhenReady = false,
                insideWindow
            )
        )
    }

    // --- Regression guards: foreground controls must keep working ---

    @Test
    fun `notification Play works when no call has happened`() {
        assertFalse(
            "Play on a paused player must reach the session when no call is involved",
            MediaButtonPolicy.shouldConsume(
                keyDown(KeyEvent.KEYCODE_MEDIA_PLAY),
                playWhenReady = false,
                MediaButtonPolicy.NO_CALL_RECORDED
            )
        )
    }

    @Test
    fun `notification Play works once the call-end window has passed`() {
        assertFalse(
            MediaButtonPolicy.shouldConsume(
                keyDown(KeyEvent.KEYCODE_MEDIA_PLAY),
                playWhenReady = false,
                outsideWindow
            )
        )
    }

    @Test
    fun `pause is never consumed even inside the call-end window`() {
        for (keyCode in listOf(
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PAUSE
        )) {
            assertFalse(
                "keyCode $keyCode must reach the session while playing",
                MediaButtonPolicy.shouldConsume(keyDown(keyCode), playWhenReady = true, insideWindow)
            )
        }
    }

    @Test
    fun `transport keys are never consumed while paused`() {
        for (keyCode in listOf(
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD,
            KeyEvent.KEYCODE_MEDIA_STOP
        )) {
            assertFalse(
                "transport keyCode $keyCode must never be swallowed",
                MediaButtonPolicy.shouldConsume(keyDown(keyCode), playWhenReady = false, insideWindow)
            )
            assertFalse(
                "transport keyCode $keyCode is not a play trigger",
                MediaButtonPolicy.isPlayTriggerKey(keyCode)
            )
        }
    }

    @Test
    fun `next at end of surah reaches the session`() {
        // playWhenReady stays true at STATE_ENDED, and Next is the core "advance surah" gesture.
        assertFalse(
            MediaButtonPolicy.shouldConsume(
                keyDown(KeyEvent.KEYCODE_MEDIA_NEXT),
                playWhenReady = true,
                insideWindow
            )
        )
    }

    // --- Malformed input ---

    @Test
    fun `intent without a key event is passed through`() {
        assertFalse(
            MediaButtonPolicy.shouldConsume(null, playWhenReady = false, insideWindow)
        )
    }

    @Test
    fun `negative elapsed time is not treated as inside the window`() {
        assertFalse(
            MediaButtonPolicy.shouldConsume(
                keyDown(KeyEvent.KEYCODE_MEDIA_PLAY),
                playWhenReady = false,
                millisSinceCallEnded = -1L
            )
        )
    }
}
