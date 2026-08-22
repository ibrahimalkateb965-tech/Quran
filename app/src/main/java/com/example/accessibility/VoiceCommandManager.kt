package com.example.accessibility

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.domain.repository.QuranRepository

sealed class VoiceCommandResult {
    data class PlaySurahByName(val surahName: String) : VoiceCommandResult()
    data class GoToAyahNumber(val ayahNumber: Int) : VoiceCommandResult()
    object Pause : VoiceCommandResult()
    object Resume : VoiceCommandResult()
    object NextAyah : VoiceCommandResult()
    object PreviousAyah : VoiceCommandResult()
    object ToggleBookmark : VoiceCommandResult()
    object ToggleRepeatMode : VoiceCommandResult()
    object ToggleContinuousPlay : VoiceCommandResult()
    object ReplayAyah : VoiceCommandResult()
    data class ChangeReciter(val reciterId: String) : VoiceCommandResult()
    object ShowSurahIndex : VoiceCommandResult()
    object ShowHelp : VoiceCommandResult()
    data class UnknownCommand(val originalText: String) : VoiceCommandResult()
    data class Error(val message: String) : VoiceCommandResult()
}

class VoiceCommandManager(
    context: Context,
    quranRepository: QuranRepository? = null
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val parser = VoiceCommandParser(
        quranRepository ?: com.example.data.repository.QuranRepositoryImpl(
            appContext,
            com.example.data.local.QuranDatabase.getDatabase(appContext).bookmarkDao(),
            com.example.data.local.QuranDatabase.getDatabase(appContext).ayahDao()
        )
    )
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isDestroyed = false
    private var currentRequestId = 0
    private var audioFocusRequest: AudioFocusRequest? = null

    fun startListening(
        onResult: (VoiceCommandResult) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        if (isDestroyed) return

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onResult(VoiceCommandResult.Error("التعرف على الصوت غير مدعوم على هذا الجهاز"))
            return
        }

        // Prevent concurrent sessions and clean up the previous one.
        stopListeningInternal()

        currentRequestId++
        val requestId = currentRequestId

        requestAudioFocus()
        playStartTone()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (!isValidSession(requestId)) return
                        isListening = true
                        onStatusChange(true)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        if (!isValidSession(requestId)) return
                        isListening = false
                        onStatusChange(false)
                        abandonAudioFocus()
                    }

                    override fun onError(error: Int) {
                        if (!isValidSession(requestId)) return
                        isListening = false
                        onStatusChange(false)
                        abandonAudioFocus()
                        val errorMsg = resolveErrorMessage(error)
                        onResult(VoiceCommandResult.Error(errorMsg))
                    }

                    override fun onResults(results: Bundle?) {
                        if (!isValidSession(requestId)) return
                        isListening = false
                        onStatusChange(false)
                        abandonAudioFocus()

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) {
                            val parsed = parser.parseCommand(spokenText)
                            onResult(parsed)
                        } else {
                            onResult(VoiceCommandResult.Error("لم أستطع فهم الأمر"))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            isListening = false
            onStatusChange(false)
            abandonAudioFocus()
            onResult(VoiceCommandResult.Error("خدمة التعرف الصوتي غير متوفرة على جهازك: ${e.message}"))
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            cleanupAfterError(onStatusChange, onResult, "صلاحية الميكروفون غير ممنوحة للتطبيق")
        } catch (e: Exception) {
            cleanupAfterError(onStatusChange, onResult, "تعذر بدء التعرف الصوتي: ${e.message}")
        }
    }

    fun stopListening() {
        stopListeningInternal()
        abandonAudioFocus()
    }

    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        stopListeningInternal()
        abandonAudioFocus()
        toneGenerator.release()
    }

    private fun stopListeningInternal() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            isListening = false
        }
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        currentRequestId++
    }

    private fun cleanupAfterError(
        onStatusChange: (Boolean) -> Unit,
        onResult: (VoiceCommandResult) -> Unit,
        message: String
    ) {
        isListening = false
        onStatusChange(false)
        stopListeningInternal()
        abandonAudioFocus()
        onResult(VoiceCommandResult.Error(message))
    }

    private fun isValidSession(requestId: Int): Boolean {
        return !isDestroyed && requestId == currentRequestId
    }

    private fun playStartTone() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    private fun resolveErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "لم أستطع فهم الأمر الصوتي، يرجى المحاولة مرة أخرى"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم التحدث بأي أمر صوتی"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "صلاحية الميكروفون غير ممنوحة"
            SpeechRecognizer.ERROR_NETWORK -> "لا يوجد اتصال بالإنترنت للتعرف على الصوت"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهى وقت الاتصال بالإنترنت"
            SpeechRecognizer.ERROR_CLIENT -> "خطأ في خدمة جوجل الصوتية، قد يحتاج تطبيق جوجل للتحديث"
            SpeechRecognizer.ERROR_SERVER -> "خطأ في خادم جوجل للتعرف الصوتي"
            SpeechRecognizer.ERROR_AUDIO -> "مشكلة في تسجيل الصوت من الميكروفون"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة التعرف الصوتي مشغولة، جاري إعادة المحاولة"
            else -> "حدث خطأ في التعرف على الصوت (رمز الخطأ: $error)"
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attr)
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
