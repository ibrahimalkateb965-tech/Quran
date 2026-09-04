package com.example.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

/**
 * Records when the device was last on a call, so [MediaButtonPolicy] can tell a telephony-injected
 * media button apart from a real one. Needs no runtime permission on any supported API level.
 *
 * API 31+ listens to [AudioManager] mode changes. Older releases fall back to the deprecated
 * [PhoneStateListener], which reports call state without READ_PHONE_STATE before API 31.
 */
internal class CallEndTracker(private val context: Context) {

    @Volatile
    private var inCall = false

    @Volatile
    private var callEndedAtElapsedMs = -1L

    /** Held as Any so the API 31 listener type is never referenced outside a version guard. */
    private var modeListener: Any? = null
    private var phoneStateListener: PhoneStateListener? = null

    /**
     * Elapsed milliseconds since the last call ended: 0 while a call is in progress, or
     * [MediaButtonPolicy.NO_CALL_RECORDED] if no call has ended yet.
     */
    fun millisSinceCallEnded(): Long {
        if (inCall) return 0L
        val endedAt = callEndedAtElapsedMs
        return if (endedAt < 0) {
            MediaButtonPolicy.NO_CALL_RECORDED
        } else {
            SystemClock.elapsedRealtime() - endedAt
        }
    }

    fun register() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && audioManager != null) {
            val listener = AudioManager.OnModeChangedListener { mode ->
                updateCallState(isCallMode(mode))
            }
            modeListener = listener
            audioManager.addOnModeChangedListener(context.mainExecutor, listener)
            updateCallState(isCallMode(audioManager.mode))
        } else {
            registerLegacy()
        }
    }

    fun unregister() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (modeListener as? AudioManager.OnModeChangedListener)?.let {
                audioManager?.removeOnModeChangedListener(it)
            }
        }
        modeListener = null

        @Suppress("DEPRECATION")
        phoneStateListener?.let { listener ->
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
    }

    @Suppress("DEPRECATION")
    private fun registerLegacy() {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                updateCallState(state != TelephonyManager.CALL_STATE_IDLE)
            }
        }
        try {
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            phoneStateListener = listener
        } catch (_: SecurityException) {
            // Without call state we simply never suppress; a dead guard beats a dead Play button.
        }
    }

    private fun updateCallState(nowInCall: Boolean) {
        val wasInCall = inCall
        inCall = nowInCall
        if (wasInCall && !nowInCall) {
            callEndedAtElapsedMs = SystemClock.elapsedRealtime()
        }
    }

    private fun isCallMode(mode: Int): Boolean =
        mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_CALL_SCREENING
}
