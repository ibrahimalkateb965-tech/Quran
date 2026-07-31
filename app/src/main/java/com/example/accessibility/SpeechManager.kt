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
                // أولاً نأخذ الصوت الافتراضي للجهاز (الذي خصصه المستخدم في الإعدادات)
                val defaultVoice = tts?.defaultVoice
                
                // إذا كان الصوت الافتراضي يدعم العربية (لغة الهاتف عربية)، نستخدمه مباشرة كما هو
                if (defaultVoice != null && defaultVoice.locale.language.startsWith("ar")) {
                    tts?.voice = defaultVoice
                    Log.d("SpeechManager", "Using default system voice for Arabic: ${defaultVoice.name}")
                } else {
                    // إذا لغة الهاتف ليست عربية، نحاول البحث عن الصوت الذي يحتوي على كلمة 'male' 
                    // في الأصوات العربية المثبتة، أو نكتفي بضبط اللغة العربية
                    val arabicVoices = tts?.voices?.filter { it.locale.language.startsWith("ar") }
                    val maleVoice = arabicVoices?.firstOrNull { 
                        it.name.contains("male", ignoreCase = true) || it.name.endsWith("-local") 
                    }
                    
                    if (maleVoice != null) {
                        tts?.voice = maleVoice
                        Log.d("SpeechManager", "Using found male voice: ${maleVoice.name}")
                    } else {
                        tts?.setLanguage(Locale("ar"))
                    }
                }
            } catch (e: Exception) {
                tts?.setLanguage(Locale("ar"))
            }
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
