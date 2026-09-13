package com.example.service

import android.view.KeyEvent

/**
 * Decides whether an incoming media-button event is an unsolicited resume that must be swallowed.
 *
 * The problem this guards against: when a phone call ends, telephony injects an
 * ACTION_MEDIA_BUTTON into the app's [androidx.media3.session.MediaSessionService] and playback
 * starts without the user asking for it. That injected event cannot be told apart from a genuine
 * notification tap by identity alone -- media3 delivers notification actions, lock-screen actions
 * and system-injected buttons through the same synthetic
 * `MediaSession.ControllerInfo.createLegacyControllerInfo()`, all as ACTION_DOWN, so the controller
 * and package name are identical in both cases.
 *
 * The one signal that does separate them is timing: the ghost event lands within a moment of the
 * call ending. Suppressing only inside that window keeps notification, lock-screen, Bluetooth and
 * wired-headset controls fully functional the rest of the time.
 */
internal object MediaButtonPolicy {

    /** How long after a call ends an unsolicited play request is treated as a ghost event. */
    const val CALL_END_SUPPRESSION_WINDOW_MS = 2_000L

    /** Value meaning "no call has ended during this process' lifetime". */
    const val NO_CALL_RECORDED = Long.MAX_VALUE

    /**
     * Keys that would start playback on a paused player.
     *
     * Deliberately excludes NEXT/PREVIOUS/FAST_FORWARD/REWIND/STEP_*: those are transport controls
     * the user expects to work while paused and at the end of a track, and media3 maps the
     * notification's skip buttons onto them.
     */
    fun isPlayTriggerKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            keyCode == KeyEvent.KEYCODE_HEADSETHOOK

    /**
     * @param keyEvent the event carried by the ACTION_MEDIA_BUTTON intent, or null if absent.
     * @param playWhenReady the player's current intent to play.
     * @param millisSinceCallEnded elapsed time since the last call ended, 0 while a call is in
     *   progress, or [NO_CALL_RECORDED] if there has not been one.
     * @return true to consume the event and leave the player untouched.
     */
    fun shouldConsume(
        keyEvent: KeyEvent?,
        playWhenReady: Boolean,
        millisSinceCallEnded: Long
    ): Boolean {
        if (keyEvent == null) return false
        if (!isPlayTriggerKey(keyEvent.keyCode)) return false
        // Already playing: PLAY_PAUSE and HEADSETHOOK mean "pause" here, never an unsolicited start.
        if (playWhenReady) return false
        return millisSinceCallEnded in 0 until CALL_END_SUPPRESSION_WINDOW_MS
    }
}
