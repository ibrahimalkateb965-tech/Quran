package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.example.service.QuranAudioService
import com.example.accessibility.HapticFeedbackManager
import com.example.accessibility.SpeechManager
import com.example.accessibility.VoiceCommandManager
import com.example.accessibility.VoiceCommandResult
import com.example.data.local.BookmarkEntity
import com.example.data.model.Ayah
import com.example.data.model.Reciter
import com.example.data.model.Surah
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val currentSurah: Surah? = null,
    val currentAyahs: List<Ayah> = emptyList(),
    val currentAyahIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLoadingAudio: Boolean = false,
    val selectedReciter: Reciter = Reciter.DEFAULT_RECITERS.first(),
    val tarkizRepeatMode: Int = 1, // 1 = play once, 3 = loop 3x, 5 = loop 5x, 10 = loop 10x, 99 = infinite
    val currentLoopCount: Int = 1,
    val isCurrentAyahBookmarked: Boolean = false,
    val isListeningVoice: Boolean = false,
    val voiceFeedbackText: String = "",
    val isScreenOffMode: Boolean = false,
    val showSurahIndex: Boolean = false,
    val showReciterDialog: Boolean = false,
    val showHelpDialog: Boolean = false,
    val announcementMessage: String = ""
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuranRepository(application)
    val haptic = HapticFeedbackManager(application)
    val speechManager = SpeechManager(application)
    private val voiceManager = VoiceCommandManager(application)

    private var mediaController: MediaController? = null
    private lateinit var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>

    private val _uiState = MutableStateFlow(QuranUiState(surahs = repository.getAllSurahs()))
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val sessionToken = SessionToken(application, ComponentName(application, QuranAudioService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _uiState.update { it.copy(isLoadingAudio = true) }
                        }
                        Player.STATE_READY -> {
                            _uiState.update { it.copy(isLoadingAudio = false) }
                        }
                        Player.STATE_ENDED -> {
                            onAyahPlaybackEnded()
                        }
                        else -> {}
                    }
                }
            })
        }, ContextCompat.getMainExecutor(application))

        // Load default starting Surah (1. Al-Fatihah)
        loadSurah(1, autoPlay = false)
    }

    fun loadSurah(surahId: Int, targetAyahIndex: Int = 0, autoPlay: Boolean = true) {
        val surah = repository.getSurahById(surahId) ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentSurah = surah,
                    currentAyahIndex = targetAyahIndex,
                    isLoadingAudio = true,
                    currentLoopCount = 1
                )
            }
            val ayahs = repository.fetchAyahsForSurah(surahId, _uiState.value.selectedReciter.serverIdentifier)
            val initialBookmarked = if (ayahs.isNotEmpty()) {
                repository.isBookmarked(surahId, ayahs[targetAyahIndex.coerceIn(0, ayahs.lastIndex)].numberInSurah)
            } else false

            _uiState.update {
                it.copy(
                    currentAyahs = ayahs,
                    isLoadingAudio = false,
                    isCurrentAyahBookmarked = initialBookmarked
                )
            }

            announce("${surah.translationArabic}. عدد آياتها ${surah.ayahCount}.")

            if (autoPlay && ayahs.isNotEmpty()) {
                playCurrentAyah()
            }
        }
    }

    private fun playCurrentAyah() {
        val ayahs = _uiState.value.currentAyahs
        val index = _uiState.value.currentAyahIndex
        if (index !in ayahs.indices) return

        val activeAyah = ayahs[index]
        viewModelScope.launch {
            val bookmarked = repository.isBookmarked(activeAyah.surahId, activeAyah.numberInSurah)
            _uiState.update { it.copy(isCurrentAyahBookmarked = bookmarked) }
        }

        if (activeAyah.audioUrl.isNotBlank()) {
            mediaController?.stop()
            mediaController?.setMediaItem(MediaItem.fromUri(activeAyah.audioUrl))
            mediaController?.prepare()
            mediaController?.play()
        }
    }

    private fun onAyahPlaybackEnded() {
        val state = _uiState.value
        val repeatMode = state.tarkizRepeatMode
        val currentLoop = state.currentLoopCount

        // Check if we need to repeat the current Ayah (Tarkiz/Hifz Mode)
        if (repeatMode > 1 && (repeatMode == 99 || currentLoop < repeatMode)) {
            _uiState.update { it.copy(currentLoopCount = currentLoop + 1) }
            playCurrentAyah()
            return
        }

        // Otherwise reset loop count and proceed to next Ayah
        _uiState.update { it.copy(currentLoopCount = 1) }

        if (state.currentAyahIndex < state.currentAyahs.lastIndex) {
            val nextIndex = state.currentAyahIndex + 1
            _uiState.update { it.copy(currentAyahIndex = nextIndex) }
            haptic.vibrateClick()
            playCurrentAyah()
        } else {
            // End of Surah reached
            haptic.vibrateRepeatOn()
            val nextSurahId = (state.currentSurah?.id ?: 1) + 1
            if (nextSurahId <= 114) {
                announce("انتهت السورة. الانتقال للسورة التالية.")
                loadSurah(nextSurahId, autoPlay = true)
            } else {
                announce("تم ختام القرآن الكريم.")
            }
        }
    }

    fun togglePlayback() {
        haptic.vibrateDoubleTap()
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
            announce("تم الإيقاف المؤقت")
        } else {
            if (mediaController?.playbackState == Player.STATE_ENDED) {
                playCurrentAyah()
            } else {
                mediaController?.play()
                announce("جاري التشغيل")
            }
        }
    }

    fun playNextAyah() {
        haptic.vibrateClick()
        val state = _uiState.value
        if (state.currentAyahIndex < state.currentAyahs.lastIndex) {
            val newIndex = state.currentAyahIndex + 1
            _uiState.update { it.copy(currentAyahIndex = newIndex, currentLoopCount = 1) }
            val ayah = state.currentAyahs[newIndex]
            announce("الآية ${ayah.numberInSurah}")
            playCurrentAyah()
        } else {
            val nextSurahId = (state.currentSurah?.id ?: 1) + 1
            if (nextSurahId <= 114) {
                loadSurah(nextSurahId, autoPlay = true)
            }
        }
    }

    fun playPreviousAyah() {
        haptic.vibrateClick()
        val state = _uiState.value
        if (state.currentAyahIndex > 0) {
            val newIndex = state.currentAyahIndex - 1
            _uiState.update { it.copy(currentAyahIndex = newIndex, currentLoopCount = 1) }
            val ayah = state.currentAyahs[newIndex]
            announce("الآية ${ayah.numberInSurah}")
            playCurrentAyah()
        } else {
            val prevSurahId = (state.currentSurah?.id ?: 1) - 1
            if (prevSurahId >= 1) {
                loadSurah(prevSurahId, autoPlay = true)
            }
        }
    }

    fun toggleRepeatMode() {
        val modes = listOf(1, 3, 5, 10, 99)
        val currentIndex = modes.indexOf(_uiState.value.tarkizRepeatMode)
        val newMode = modes[(currentIndex + 1) % modes.size]
        _uiState.update { it.copy(tarkizRepeatMode = newMode, currentLoopCount = 1) }

        if (newMode > 1) {
            haptic.vibrateRepeatOn()
            val modeText = if (newMode == 99) "تكرار لا نهائي" else "تكرار $newMode مرات"
            announce("تم تفعيل وضع التركيز: $modeText")
        } else {
            haptic.vibrateRepeatOff()
            announce("تم إيقاف وضع التكرار")
        }
    }

    fun toggleCurrentBookmark() {
        val state = _uiState.value
        val surah = state.currentSurah ?: return
        val ayahs = state.currentAyahs
        if (state.currentAyahIndex !in ayahs.indices) return
        val activeAyah = ayahs[state.currentAyahIndex]

        viewModelScope.launch {
            val isNowBookmarked = repository.toggleBookmark(
                surahId = surah.id,
                surahNameAr = surah.nameArabic,
                ayahNumber = activeAyah.numberInSurah
            )
            _uiState.update { it.copy(isCurrentAyahBookmarked = isNowBookmarked) }
            haptic.vibrateBookmark()

            if (isNowBookmarked) {
                announce("تم إضافة سورة ${surah.nameArabic} الآية ${activeAyah.numberInSurah} للإشارات المرجعية")
            } else {
                announce("تم إزالة الآية من الإشارات المرجعية")
            }
        }
    }

    fun selectReciter(reciter: Reciter) {
        haptic.vibrateClick()
        _uiState.update { it.copy(selectedReciter = reciter, showReciterDialog = false) }
        announce("تم تغيير القارئ إلى ${reciter.nameArabic}")
        val surah = _uiState.value.currentSurah
        if (surah != null) {
            loadSurah(surah.id, _uiState.value.currentAyahIndex, autoPlay = _uiState.value.isPlaying)
        }
    }

    fun startVoiceCommand() {
        haptic.vibrateLongPress()
        // Removed `announce(...)` to prevent the device speaker from triggering the microphone and immediately ending recognition.
        // Google's SpeechRecognizer will automatically play a standard beep sound indicating it's ready.
        voiceManager.startListening(
            onResult = { result ->
                handleVoiceCommandResult(result)
            },
            onStatusChange = { isListening ->
                _uiState.update { it.copy(isListeningVoice = isListening) }
            }
        )
    }

    private fun handleVoiceCommandResult(result: VoiceCommandResult) {
        when (result) {
            is VoiceCommandResult.PlaySurahByName -> {
                val surah = repository.findSurahByName(result.surahName)
                if (surah != null) {
                    announce("جاري تشغيل سورة ${surah.nameArabic}")
                    loadSurah(surah.id, autoPlay = true)
                } else {
                    announce("لم أجد سورة باسم ${result.surahName}")
                }
            }
            is VoiceCommandResult.GoToAyahNumber -> {
                val ayahs = _uiState.value.currentAyahs
                val targetIndex = (result.ayahNumber - 1).coerceIn(0, (ayahs.size - 1).coerceAtLeast(0))
                if (ayahs.isNotEmpty()) {
                    _uiState.update { it.copy(currentAyahIndex = targetIndex, currentLoopCount = 1) }
                    announce("الانتقال إلى الآية ${result.ayahNumber}")
                    playCurrentAyah()
                }
            }
            VoiceCommandResult.Pause -> {
                if (mediaController?.isPlaying == true) mediaController?.pause()
                announce("تم الإيقاف")
            }
            VoiceCommandResult.Resume -> {
                mediaController?.play()
                announce("تم التشغيل")
            }
            VoiceCommandResult.NextAyah -> playNextAyah()
            VoiceCommandResult.PreviousAyah -> playPreviousAyah()
            VoiceCommandResult.ToggleBookmark -> toggleCurrentBookmark()
            VoiceCommandResult.ToggleRepeatMode -> toggleRepeatMode()
            is VoiceCommandResult.ChangeReciter -> {
                val found = Reciter.DEFAULT_RECITERS.find { it.id == result.reciterId }
                if (found != null) selectReciter(found)
            }
            VoiceCommandResult.ShowSurahIndex -> {
                _uiState.update { it.copy(showSurahIndex = true) }
                announce("تم فتح قائمة السور")
            }
            VoiceCommandResult.ShowHelp -> {
                _uiState.update { it.copy(showHelpDialog = true) }
                announce("تم فتح قائمة التعليمات والأوامر الصوتية")
            }
            is VoiceCommandResult.UnknownCommand -> {
                announce("لم أتعرف على الأمر: ${result.originalText}")
            }
            is VoiceCommandResult.Error -> {
                announce(result.message)
            }
        }
    }

    fun toggleScreenOffMode() {
        haptic.vibrateLongPress()
        val next = !_uiState.value.isScreenOffMode
        _uiState.update { it.copy(isScreenOffMode = next) }
        if (next) {
            announce("تم تفعيل وضع إيقاف الشاشة لتوفير البطارية. الشاشة مغلقة الآن مع استمرار الإيماءات والصوت.")
        } else {
            announce("تم إلغاء وضع إيقاف الشاشة.")
        }
    }

    fun toggleSurahIndex(show: Boolean) {
        haptic.vibrateClick()
        _uiState.update { it.copy(showSurahIndex = show) }
    }

    fun toggleReciterDialog(show: Boolean) {
        haptic.vibrateClick()
        _uiState.update { it.copy(showReciterDialog = show) }
    }

    fun toggleHelpDialog(show: Boolean) {
        haptic.vibrateClick()
        _uiState.update { it.copy(showHelpDialog = show) }
    }

    fun announce(text: String) {
        _uiState.update { it.copy(announcementMessage = text) }
        speechManager.speak(text)
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        mediaController?.release()
        
        speechManager.shutdown()
        voiceManager.stopListening()
        super.onCleared()
    }
}
