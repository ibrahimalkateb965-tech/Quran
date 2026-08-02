package com.example.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityManager
import java.util.Locale

/**
 * مدير النطق الداخلي للتطبيق.
 */
class SpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val accessibilityManager =
        appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        if (!isTalkBackEnabled()) {
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
                    // السر هنا: إذا كان الصوت الافتراضي عربياً، لا تستدعي setLanguage أبداً!
                    // استدعاء setLanguage يلغي اختيار المستخدم للصوت (مثلاً الذكري) ويرجعه للصوت الأنثوي الافتراضي للمحرك.
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
        if (isTalkBackEnabled()) return

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
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
