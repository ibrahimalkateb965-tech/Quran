package com.example.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * مدير النطق الداخلي للتطبيق.
 *
 * يراقب حالة TalkBack لحظياً وينشرها عبر [isTalkBackEnabledFlow].
 * عند تفعيل TalkBack، يتوقف TTS الداخلي ويجب على المستهلكين توجيه الإعلانات
 * إلى announceForAccessibility بدلاً منه.
 */
class SpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val accessibilityManager =
        appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isTalkBackEnabled = MutableStateFlow(isTalkBackEnabled())
    val isTalkBackEnabledFlow: StateFlow<Boolean> = _isTalkBackEnabled.asStateFlow()

    private val accessibilityStateChangeListener =
        AccessibilityManager.AccessibilityStateChangeListener { enabled ->
            updateTalkBackState(enabled && accessibilityManager?.isTouchExplorationEnabled == true)
        }

    private val touchExplorationStateChangeListener =
        AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
            updateTalkBackState(accessibilityManager?.isEnabled == true && enabled)
        }

    init {
        accessibilityManager?.addAccessibilityStateChangeListener(accessibilityStateChangeListener)
        accessibilityManager?.addTouchExplorationStateChangeListener(touchExplorationStateChangeListener)

        if (!_isTalkBackEnabled.value) {
            tts = TextToSpeech(appContext, this)
        }
    }

    private fun updateTalkBackState(enabled: Boolean) {
        if (_isTalkBackEnabled.value == enabled) return
        _isTalkBackEnabled.value = enabled

        if (enabled) {
            // عند تفعيل TalkBack نوقف TTS الداخلي فوراً.
            tts?.stop()
        } else if (tts == null) {
            tts = TextToSpeech(appContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true

            try {
                val currentVoice = tts?.voice ?: tts?.defaultVoice
                val isAlreadyArabic = currentVoice?.locale?.language?.startsWith("ar") == true

                if (!isAlreadyArabic) {
                    val result = tts?.setLanguage(Locale.Builder().setLanguage("ar").build())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("SpeechManager", "اللغة العربية غير مدعومة أو ملفاتها مفقودة في محرك الـ TTS الحالي")
                    }
                } else {
                    Log.d("SpeechManager", "صوت النظام الافتراضي عربي بالفعل، تم اعتماده كما هو للحفاظ على نبرة المستخدم.")
                }
            } catch (e: Exception) {
                tts?.setLanguage(Locale.Builder().setLanguage("ar").build())
            }
        } else {
            Log.e("SpeechManager", "فشل تهيئة محرك النطق TextToSpeech: $status")
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (_isTalkBackEnabled.value) return

        if (isInitialized) {
            tts?.speak(text, queueMode, null, "QuranA11yTTS")
        } else {
            if (tts == null) {
                tts = TextToSpeech(appContext, this)
            }
        }
    }

    fun isTalkBackEnabled(): Boolean {
        return accessibilityManager?.isEnabled == true &&
                accessibilityManager?.isTouchExplorationEnabled == true
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        accessibilityManager?.removeAccessibilityStateChangeListener(accessibilityStateChangeListener)
        accessibilityManager?.removeTouchExplorationStateChangeListener(touchExplorationStateChangeListener)
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
