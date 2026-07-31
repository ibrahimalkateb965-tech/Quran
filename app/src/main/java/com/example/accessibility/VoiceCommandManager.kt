package com.example.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

sealed class VoiceCommandResult {
    data class PlaySurahByName(val surahName: String) : VoiceCommandResult()
    data class GoToAyahNumber(val ayahNumber: Int) : VoiceCommandResult()
    object Pause : VoiceCommandResult()
    object Resume : VoiceCommandResult()
    object NextAyah : VoiceCommandResult()
    object PreviousAyah : VoiceCommandResult()
    object ToggleBookmark : VoiceCommandResult()
    object ToggleRepeatMode : VoiceCommandResult()
    data class ChangeReciter(val reciterId: String) : VoiceCommandResult()
    object ShowSurahIndex : VoiceCommandResult()
    object ShowHelp : VoiceCommandResult()
    data class UnknownCommand(val originalText: String) : VoiceCommandResult()
    data class Error(val message: String) : VoiceCommandResult()
}

class VoiceCommandManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening(
        onResult: (VoiceCommandResult) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult(VoiceCommandResult.Error("التعرف على الصوت غير مدعوم على هذا الجهاز"))
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    onStatusChange(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    onStatusChange(false)
                }

                override fun onError(error: Int) {
                    isListening = false
                    onStatusChange(false)
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "لم أستطع فهم الأمر الصوتي، يرجى المحاولة مرة أخرى"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم التحدث بأي أمر صوتی"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "صلاحية الميكروفون غير ممنوحة"
                        SpeechRecognizer.ERROR_NETWORK -> "لا يوجد اتصال بالإنترنت للتعرف على الصوت"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهى وقت الاتصال بالإنترنت"
                        SpeechRecognizer.ERROR_CLIENT -> "خطأ في خدمة جوجل الصوتية، قد يحتاج تطبيق جوجل للتحديث"
                        SpeechRecognizer.ERROR_SERVER -> "خطأ في خادم جوجل للتعرف الصوتي"
                        SpeechRecognizer.ERROR_AUDIO -> "مشكلة في تسجيل الصوت من الميكروفون"
                        else -> "حدث خطأ في التعرف على الصوت (رمز الخطأ: $error)"
                    }
                    onResult(VoiceCommandResult.Error(errorMsg))
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    onStatusChange(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        val parsed = parseCommand(spokenText)
                        onResult(parsed)
                    } else {
                        onResult(VoiceCommandResult.Error("لم أستطع فهم الأمر"))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            isListening = false
            onStatusChange(false)
            onResult(VoiceCommandResult.Error("صلاحية الميكروفون غير ممنوحة للتطبيق"))
        } catch (e: Exception) {
            isListening = false
            onStatusChange(false)
            onResult(VoiceCommandResult.Error("تعذر بدء التعرف الصوتي: ${e.message}"))
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun parseCommand(text: String): VoiceCommandResult {
        // Normalization to handle speech-to-text variations
        val clean = text.trim()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("ى", "ي")

        if (clean.contains("توقف") || clean.contains("ايقاف") || clean.contains("اسكت") || clean.contains("اقطع")) {
            return VoiceCommandResult.Pause
        }
        if ((clean.contains("تشغيل") && !clean.contains("سوره") && !clean.contains("صوره")) || clean.contains("استيناف") || clean.contains("واصل")) {
            return VoiceCommandResult.Resume
        }
        if (clean.contains("تالي") || clean.contains("بعده") || clean.contains("تقدم") || clean.contains("قدام")) {
            return VoiceCommandResult.NextAyah
        }
        if (clean.contains("سابق") || clean.contains("قبلها") || clean.contains("ارجع") || clean.contains("ورا")) {
            return VoiceCommandResult.PreviousAyah
        }
        if (clean.contains("مرجعيه") || clean.contains("علامه") || clean.contains("حفظ الايه") || clean.contains("مفضله")) {
            return VoiceCommandResult.ToggleBookmark
        }
        if (clean.contains("تكرار") || clean.contains("تاكيد") || clean.contains("تركيز") || clean.contains("حفظ")) {
            return VoiceCommandResult.ToggleRepeatMode
        }
        if (clean.contains("قائمه") || clean.contains("قايمه") || clean.contains("فهرس") || 
            (clean.contains("سور") && !clean.contains("سوره")) || 
            (clean.contains("صور") && !clean.contains("صوره"))) {
            return VoiceCommandResult.ShowSurahIndex
        }
        if (clean.contains("تعليمات") || clean.contains("مساعده") || clean.contains("الاوامر") || clean.contains("دليل")) {
            return VoiceCommandResult.ShowHelp
        }
        if (clean.contains("الحصري")) {
            return VoiceCommandResult.ChangeReciter("husary")
        }
        if (clean.contains("العفاسي")) {
            return VoiceCommandResult.ChangeReciter("afasy")
        }
        if (clean.contains("المنشاوي")) {
            return VoiceCommandResult.ChangeReciter("minshawi")
        }
        if (clean.contains("عبد الباسط")) {
            return VoiceCommandResult.ChangeReciter("abdulbasit")
        }

        // Check Ayah number command like "الآية 5" or "آية 12"
        if (clean.contains("ايه")) {
            val digits = clean.filter { it.isDigit() }
            if (digits.isNotEmpty()) {
                val num = digits.toIntOrNull()
                if (num != null && num > 0) {
                    return VoiceCommandResult.GoToAyahNumber(num)
                }
            }
            // Parse spoken Arabic number words
            val numFromWords = parseArabicNumberWord(clean)
            if (numFromWords != null) {
                return VoiceCommandResult.GoToAyahNumber(numFromWords)
            }
        }

        // Surah play command like "تشغيل سورة البقرة" or "سورة الكهف"
        if (clean.contains("سوره") || clean.contains("صوره") || clean.contains("تشغيل")) {
            val surahPart = clean.substringAfter("سوره").substringAfter("صوره").substringAfter("تشغيل").trim()
            if (surahPart.isNotBlank()) {
                return VoiceCommandResult.PlaySurahByName(surahPart)
            }
        }

        return VoiceCommandResult.UnknownCommand(clean)
    }

    private fun parseArabicNumberWord(text: String): Int? {
        val wordMap = mapOf(
            "واحد" to 1, "الاولي" to 1, "الاول" to 1,
            "اثنان" to 2, "ثاني" to 2, "الثانيه" to 2,
            "ثلاثه" to 3, "الثالثه" to 3, "ثلاث" to 3,
            "اربعه" to 4, "الرابعه" to 4, "اربع" to 4,
            "خمسه" to 5, "الخامسه" to 5, "خمس" to 5,
            "سته" to 6, "السادسه" to 6, "ست" to 6,
            "سبعه" to 7, "السابعه" to 7, "سبع" to 7,
            "ثمانيه" to 8, "الثامنه" to 8, "ثمان" to 8,
            "تسعه" to 9, "التاسعه" to 9, "تسع" to 9,
            "عشره" to 10, "العاشره" to 10, "عشر" to 10
        )
        for ((word, value) in wordMap) {
            if (text.contains(word)) return value
        }
        return null
    }
}
