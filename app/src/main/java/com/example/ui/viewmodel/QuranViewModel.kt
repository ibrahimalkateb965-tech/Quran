package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.example.security.TrialManager
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val surahs: List<Surah> = emptyList(),
    val currentSurah: Surah? = null,
    val currentAyahs: List<Ayah> = emptyList(),
    val currentAyahIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLoadingAudio: Boolean = false,
    val currentLoopCount: Int = 1
)

data class SettingsUiState(
    val selectedReciter: Reciter = Reciter.DEFAULT_RECITERS.first(),
    val tarkizRepeatMode: Int = 1,
    val isContinuousPlayEnabled: Boolean = false
)

data class BookmarkUiState(
    val isCurrentAyahBookmarked: Boolean = false
)

data class VoiceUiState(
    val isListeningVoice: Boolean = false
)

data class DialogUiState(
    val showSurahIndex: Boolean = false,
    val showReciterDialog: Boolean = false,
    val showHelpDialog: Boolean = false,
    val showBookmarksSheet: Boolean = false
)

data class ScreenModeUiState(
    val isScreenOffMode: Boolean = false
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuranRepository(application)
    val haptic = HapticFeedbackManager(application)
    val speechManager = SpeechManager(application)
    private val voiceManager = VoiceCommandManager(application)

    private var mediaController: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var playerListener: Player.Listener? = null
    private var isControllerReleased = false
    private var pendingAudioUrlToPlay: String? = null
    private var pendingAyahAnnouncement: String? = null

    private var isAwaitingNetworkRecovery = false
    private var networkRetryCount = 0
    private var networkRetryJob: Job? = null

    private val _playbackUiState = MutableStateFlow(PlaybackUiState(surahs = repository.getAllSurahs()))
    val playbackUiState: StateFlow<PlaybackUiState> = _playbackUiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    private val _bookmarkUiState = MutableStateFlow(BookmarkUiState())
    val bookmarkUiState: StateFlow<BookmarkUiState> = _bookmarkUiState.asStateFlow()

    private val _voiceUiState = MutableStateFlow(VoiceUiState())
    val voiceUiState: StateFlow<VoiceUiState> = _voiceUiState.asStateFlow()

    private val _dialogUiState = MutableStateFlow(DialogUiState())
    val dialogUiState: StateFlow<DialogUiState> = _dialogUiState.asStateFlow()

    private val _screenModeUiState = MutableStateFlow(ScreenModeUiState())
    val screenModeUiState: StateFlow<ScreenModeUiState> = _screenModeUiState.asStateFlow()

    private val _isTrialExpired = MutableStateFlow<Boolean?>(null)
    val isTrialExpired: StateFlow<Boolean?> = _isTrialExpired.asStateFlow()

    private val _announcementEvent = Channel<String>(Channel.BUFFERED)
    val announcementEvent = _announcementEvent.receiveAsFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _isTrialExpired.value = TrialManager.getInstance(application).isTrialExpired()
        }

        val sessionToken = SessionToken(application, ComponentName(application, QuranAudioService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            if (isControllerReleased) return@addListener
            mediaController = controllerFuture?.get()

            playerListener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackUiState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) {
                        pendingAyahAnnouncement?.let { msg ->
                            pendingAyahAnnouncement = null
                            viewModelScope.launch { delay(400); announce(msg) }
                        }
                        if (isAwaitingNetworkRecovery) {
                            isAwaitingNetworkRecovery = false
                            networkRetryCount = 0
                            haptic.vibrateNetworkRecovery()
                            announce("عاد الاتصال بالإنترنت، جاري مواصلة التلاوة")
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _playbackUiState.update { it.copy(isLoadingAudio = true) }
                        }
                        Player.STATE_READY -> {
                            _playbackUiState.update { it.copy(isLoadingAudio = false) }
                        }
                        Player.STATE_ENDED -> {
                            onAyahPlaybackEnded()
                        }
                        else -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    pendingAyahAnnouncement = null
                    if (isNetworkRelatedError(error)) {
                        handleNetworkPlaybackError()
                    } else {
                        announce("حدث خطأ في تشغيل الصوت")
                    }
                }
            }.also { listener ->
                mediaController?.addListener(listener)
            }
            
            // Play pending audio if any
            pendingAudioUrlToPlay?.let { url ->
                pendingAudioUrlToPlay = null
                playAudioUrl(url)
            }
        }, ContextCompat.getMainExecutor(application))

        val lastPos = repository.getLastPosition()
        if (lastPos != null) {
            loadSurah(lastPos.first, lastPos.second, autoPlay = false)
        } else {
            // Load default starting Surah (1. Al-Fatihah)
            loadSurah(1, autoPlay = false)
        }
    }

    fun loadSurah(surahId: Int, targetAyahIndex: Int = 0, autoPlay: Boolean = true) {
        val surah = repository.getSurahById(surahId) ?: return
        viewModelScope.launch {
            _playbackUiState.update {
                it.copy(
                    currentSurah = surah,
                    currentAyahIndex = targetAyahIndex,
                    isLoadingAudio = true,
                    currentLoopCount = 1
                )
            }
            repository.saveLastPosition(surahId, targetAyahIndex)
            val ayahs = repository.fetchAyahsForSurah(surahId, _settingsUiState.value.selectedReciter.serverIdentifier)
            val initialBookmarked = if (ayahs.isNotEmpty()) {
                repository.isBookmarked(surahId, ayahs[targetAyahIndex.coerceIn(0, ayahs.lastIndex)].numberInSurah)
            } else false

            _playbackUiState.update {
                it.copy(
                    currentAyahs = ayahs,
                    isLoadingAudio = false
                )
            }
            _bookmarkUiState.update {
                it.copy(isCurrentAyahBookmarked = initialBookmarked)
            }

            announce("${surah.translationArabic}. عدد آياتها ${surah.ayahCount}.")

            if (autoPlay && ayahs.isNotEmpty()) {
                playCurrentAyah()
            }
        }
    }

    private fun playAudioUrl(url: String) {
        mediaController?.let { controller ->
            controller.setMediaItem(MediaItem.fromUri(url), 0L)
            controller.prepare()
            controller.play()
        } ?: run {
            pendingAudioUrlToPlay = url
        }
    }

    private fun playCurrentAyah() {
        val ayahs = _playbackUiState.value.currentAyahs
        val index = _playbackUiState.value.currentAyahIndex
        if (index !in ayahs.indices) return

        val activeAyah = ayahs[index]
        if (activeAyah.audioUrl.isBlank()) return

        viewModelScope.launch {
            val bookmarked = repository.isBookmarked(activeAyah.surahId, activeAyah.numberInSurah)
            _bookmarkUiState.update { it.copy(isCurrentAyahBookmarked = bookmarked) }
        }

        val controller = mediaController
        if (controller != null) {
            controller.setMediaItem(MediaItem.fromUri(activeAyah.audioUrl), 0L)
            controller.prepare()
            controller.play()
        } else {
            pendingAudioUrlToPlay = activeAyah.audioUrl
        }
    }

    private fun onAyahPlaybackEnded() {
        val playbackState = _playbackUiState.value
        val repeatMode = _settingsUiState.value.tarkizRepeatMode
        val currentLoop = playbackState.currentLoopCount

        // Check if we need to repeat the current Ayah (Tarkiz/Hifz Mode)
        if (repeatMode > 1 && (repeatMode == 99 || currentLoop < repeatMode)) {
            _playbackUiState.update { it.copy(currentLoopCount = currentLoop + 1) }
            playCurrentAyah()
            return
        }

        // Otherwise reset loop count and proceed to next Ayah
        _playbackUiState.update { it.copy(currentLoopCount = 1) }

        if (playbackState.currentAyahIndex < playbackState.currentAyahs.lastIndex) {
            if (_settingsUiState.value.isContinuousPlayEnabled) {
                goToAyah(playbackState.currentAyahIndex + 1, autoPlay = true)
            }
        } else {
            // End of Surah reached
            haptic.vibrateRepeatOn()
            val nextSurahId = (playbackState.currentSurah?.id ?: 1) + 1
            if (nextSurahId <= 114) {
                announce("انتهت السورة. الانتقال للسورة التالية.")
                if (_settingsUiState.value.isContinuousPlayEnabled) {
                    loadSurah(nextSurahId, autoPlay = true)
                } else {
                    loadSurah(nextSurahId, autoPlay = false)
                }
            } else {
                announce("تم ختام القرآن الكريم.")
            }
        }
    }

    fun togglePlayback() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
            performAction("تم الإيقاف المؤقت", HapticType.DOUBLE_TAP)
        } else {
            if (mediaController?.playbackState == Player.STATE_ENDED) {
                playCurrentAyah()
            } else {
                mediaController?.play()
                performAction("جاري التشغيل", HapticType.DOUBLE_TAP)
            }
        }
    }

    fun replayCurrentAyah() {
        val state = _playbackUiState.value
        _playbackUiState.update { it.copy(currentLoopCount = state.currentLoopCount + 1) }
        performAction("تكرار الآية", HapticType.DOUBLE_TAP)
        playCurrentAyah()
    }

    fun toggleContinuousPlay() {
        val next = !_settingsUiState.value.isContinuousPlayEnabled
        _settingsUiState.update { it.copy(isContinuousPlayEnabled = next) }
        if (next) {
            performAction("وضع الاستماع المتواصل مفعّل", HapticType.CLICK)
        } else {
            performAction("تم إيقاف الاستماع المتواصل", HapticType.CLICK)
        }
    }

    fun navigateAyah(offset: Int) {
        val state = _playbackUiState.value
        val newIndex = state.currentAyahIndex + offset

        if (newIndex in state.currentAyahs.indices) {
            goToAyah(newIndex, autoPlay = true)
        } else if (newIndex >= state.currentAyahs.size) {
            val nextSurahId = (state.currentSurah?.id ?: 1) + 1
            if (nextSurahId <= 114) {
                loadSurah(nextSurahId, autoPlay = true)
            }
        } else if (newIndex < 0) {
            val prevSurahId = (state.currentSurah?.id ?: 1) - 1
            if (prevSurahId >= 1) {
                loadSurah(prevSurahId, autoPlay = true)
            }
        }
    }

    fun playNextAyah() = navigateAyah(1)
    fun playPreviousAyah() = navigateAyah(-1)

    fun goToAyah(index: Int, autoPlay: Boolean = true) {
        val state = _playbackUiState.value
        if (index !in state.currentAyahs.indices || index == state.currentAyahIndex) return

        _playbackUiState.update { it.copy(currentAyahIndex = index, currentLoopCount = 1) }
        val ayah = state.currentAyahs[index]
        repository.saveLastPosition(ayah.surahId, index)
        haptic.vibrateClick()

        if (autoPlay) {
            playCurrentAyah()
        }
    }

    fun toggleRepeatMode() {
        val modes = listOf(1, 3, 5, 10, 99)
        val currentIndex = modes.indexOf(_settingsUiState.value.tarkizRepeatMode)
        val newMode = modes[(currentIndex + 1) % modes.size]
        _settingsUiState.update { it.copy(tarkizRepeatMode = newMode) }
        _playbackUiState.update { it.copy(currentLoopCount = 1) }

        if (newMode > 1) {
            val modeText = if (newMode == 99) "تكرار لا نهائي" else "تكرار $newMode مرات"
            performAction("تم تفعيل وضع التركيز: $modeText", HapticType.REPEAT_ON)
        } else {
            performAction("تم إيقاف وضع التكرار", HapticType.REPEAT_OFF)
        }
    }

    fun toggleCurrentBookmark() {
        val playback = _playbackUiState.value
        val surah = playback.currentSurah ?: return
        val ayahs = playback.currentAyahs
        if (playback.currentAyahIndex !in ayahs.indices) return
        val activeAyah = ayahs[playback.currentAyahIndex]

        viewModelScope.launch {
            val isNowBookmarked = repository.toggleBookmark(
                surahId = surah.id,
                surahNameAr = surah.nameArabic,
                ayahNumber = activeAyah.numberInSurah
            )
            _bookmarkUiState.update { it.copy(isCurrentAyahBookmarked = isNowBookmarked) }
            
            if (isNowBookmarked) {
                performAction("تم إضافة سورة ${surah.nameArabic} الآية ${activeAyah.numberInSurah} للإشارات المرجعية", HapticType.BOOKMARK)
            } else {
                performAction("تم إزالة الآية من الإشارات المرجعية", HapticType.BOOKMARK)
            }
        }
    }

    fun selectReciter(reciter: Reciter) {
        _settingsUiState.update { it.copy(selectedReciter = reciter) }
        _dialogUiState.update { it.copy(showReciterDialog = false) }
        performAction("تم تغيير القارئ إلى ${reciter.nameArabic}", HapticType.CLICK)
        val surah = _playbackUiState.value.currentSurah
        if (surah != null) {
            loadSurah(surah.id, _playbackUiState.value.currentAyahIndex, autoPlay = _playbackUiState.value.isPlaying)
        }
    }

    fun startVoiceCommand() {
        haptic.vibrateVoiceListeningStarted()

        // Pause Quran audio while listening to avoid the microphone picking up the recitation.
        val wasPlaying = mediaController?.isPlaying == true
        if (wasPlaying) {
            mediaController?.pause()
        }

        voiceManager.startListening(
            onResult = { result ->
                handleVoiceCommandResult(result, wasPlaying)
            },
            onStatusChange = { isListening ->
                _voiceUiState.update { it.copy(isListeningVoice = isListening) }
            }
        )
    }

    private fun handleVoiceCommandResult(result: VoiceCommandResult, wasPlayingBeforeListening: Boolean) {
        when (result) {
            is VoiceCommandResult.PlaySurahByName -> {
                haptic.vibrateVoiceCommandSuccess()
                val surah = repository.findSurahByName(result.surahName)
                if (surah != null) {
                    announce("جاري تشغيل سورة ${surah.nameArabic}")
                    loadSurah(surah.id, autoPlay = true)
                } else {
                    haptic.vibrateVoiceCommandFailure()
                    announce("لم أجد سورة باسم ${result.surahName}")
                }
            }
            is VoiceCommandResult.GoToAyahNumber -> {
                haptic.vibrateVoiceCommandSuccess()
                val ayahs = _playbackUiState.value.currentAyahs
                val targetIndex = (result.ayahNumber - 1).coerceIn(0, (ayahs.size - 1).coerceAtLeast(0))
                if (ayahs.isNotEmpty()) {
                    goToAyah(targetIndex, autoPlay = true)
                    announce("الانتقال إلى الآية ${result.ayahNumber}")
                }
            }
            VoiceCommandResult.Pause -> {
                haptic.vibrateVoiceCommandSuccess()
                if (mediaController?.isPlaying == true) mediaController?.pause()
                announce("تم الإيقاف")
            }
            VoiceCommandResult.Resume -> {
                haptic.vibrateVoiceCommandSuccess()
                mediaController?.play()
                announce("تم التشغيل")
            }
            VoiceCommandResult.NextAyah -> {
                haptic.vibrateVoiceCommandSuccess()
                playNextAyah()
            }
            VoiceCommandResult.PreviousAyah -> {
                haptic.vibrateVoiceCommandSuccess()
                playPreviousAyah()
            }
            VoiceCommandResult.ToggleBookmark -> {
                haptic.vibrateVoiceCommandSuccess()
                toggleCurrentBookmark()
            }
            VoiceCommandResult.ToggleRepeatMode -> {
                haptic.vibrateVoiceCommandSuccess()
                toggleRepeatMode()
            }
            VoiceCommandResult.ToggleContinuousPlay -> {
                haptic.vibrateVoiceCommandSuccess()
                toggleContinuousPlay()
            }
            VoiceCommandResult.ReplayAyah -> {
                haptic.vibrateVoiceCommandSuccess()
                replayCurrentAyah()
            }
            is VoiceCommandResult.ChangeReciter -> {
                haptic.vibrateVoiceCommandSuccess()
                val found = Reciter.DEFAULT_RECITERS.find { it.id == result.reciterId }
                if (found != null) selectReciter(found)
            }
            VoiceCommandResult.ShowSurahIndex -> {
                haptic.vibrateVoiceCommandSuccess()
                _dialogUiState.update { it.copy(showSurahIndex = true) }
                announce("تم فتح قائمة السور")
            }
            VoiceCommandResult.ShowHelp -> {
                haptic.vibrateVoiceCommandSuccess()
                _dialogUiState.update { it.copy(showHelpDialog = true) }
                announce("تم فتح قائمة التعليمات والأوامر الصوتية")
            }
            is VoiceCommandResult.UnknownCommand -> {
                haptic.vibrateVoiceCommandFailure()
                announce("لم أتعرف على الأمر: ${result.originalText}")
            }
            is VoiceCommandResult.Error -> {
                haptic.vibrateVoiceCommandFailure()
                announce(result.message)
            }
        }

        // Resume Quran audio if it was playing before the voice command,
        // unless the user explicitly asked to pause.
        if (wasPlayingBeforeListening && result !is VoiceCommandResult.Pause) {
            mediaController?.play()
        }
    }

    fun toggleScreenOffMode() {
        haptic.vibrateLongPress()
        val next = !_screenModeUiState.value.isScreenOffMode
        _screenModeUiState.update { it.copy(isScreenOffMode = next) }
        if (next) {
            announce("تم تفعيل وضع إيقاف الشاشة لتوفير البطارية. الشاشة مغلقة الآن مع استمرار الإيماءات والصوت.")
        } else {
            announce("تم إلغاء وضع إيقاف الشاشة.")
        }
    }

    fun toggleSurahIndex(show: Boolean) {
        haptic.vibrateClick()
        _dialogUiState.update { it.copy(showSurahIndex = show) }
    }

    fun toggleBookmarksSheet(show: Boolean) {
        haptic.vibrateClick()
        _dialogUiState.update { it.copy(showBookmarksSheet = show) }
    }

    fun toggleReciterDialog(show: Boolean) {
        haptic.vibrateClick()
        _dialogUiState.update { it.copy(showReciterDialog = show) }
    }

    fun toggleHelpDialog(show: Boolean) {
        haptic.vibrateClick()
        _dialogUiState.update { it.copy(showHelpDialog = show) }
    }

    fun announce(text: String) {
        viewModelScope.launch {
            _announcementEvent.send(text)
        }
        speechManager.speak(text)
    }
    
    private fun performAction(msg: String, hapticType: HapticType = HapticType.CLICK) {
        when (hapticType) {
            HapticType.CLICK -> haptic.vibrateClick()
            HapticType.DOUBLE_TAP -> haptic.vibrateDoubleTap()
            HapticType.LONG_PRESS -> haptic.vibrateLongPress()
            HapticType.REPEAT_ON -> haptic.vibrateRepeatOn()
            HapticType.REPEAT_OFF -> haptic.vibrateRepeatOff()
            HapticType.BOOKMARK -> haptic.vibrateBookmark()
            HapticType.NETWORK_LOSS -> haptic.vibrateNetworkLoss()
            HapticType.NETWORK_RECOVERY -> haptic.vibrateNetworkRecovery()
            HapticType.VOICE_LISTENING_STARTED -> haptic.vibrateVoiceListeningStarted()
            HapticType.VOICE_COMMAND_SUCCESS -> haptic.vibrateVoiceCommandSuccess()
            HapticType.VOICE_COMMAND_FAILURE -> haptic.vibrateVoiceCommandFailure()
            HapticType.NONE -> {}
        }
        if (msg.isNotEmpty()) {
            announce(msg)
        }
    }

    private fun isNetworkRelatedError(error: PlaybackException): Boolean {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_TIMEOUT -> true
            else -> false
        }
    }

    private fun handleNetworkPlaybackError() {
        if (!isAwaitingNetworkRecovery) {
            isAwaitingNetworkRecovery = true
            networkRetryCount = 0
            performAction(
                "انقطع الاتصال بالإنترنت، جاري المحاولة مرة أخرى",
                HapticType.NETWORK_LOSS
            )
        }

        networkRetryJob?.cancel()
        networkRetryJob = viewModelScope.launch {
            if (networkRetryCount < MAX_NETWORK_RETRIES) {
                networkRetryCount++
                val backoffMs = MIN_RETRY_BACKOFF_MS * (1 shl (networkRetryCount - 1))
                delay(backoffMs.coerceAtMost(MAX_RETRY_BACKOFF_MS))
                mediaController?.prepare()
            } else {
                isAwaitingNetworkRecovery = false
                networkRetryCount = 0
                performAction(
                    "تعذّر الاتصال بالإنترنت بعد عدة محاولات. يرجى التحقق من الشبكة والمحاولة لاحقاً.",
                    HapticType.NETWORK_LOSS
                )
            }
        }
    }

    companion object {
        private const val MAX_NETWORK_RETRIES = 3
        private const val MIN_RETRY_BACKOFF_MS = 1_500L
        private const val MAX_RETRY_BACKOFF_MS = 10_000L
    }

    override fun onCleared() {
        isControllerReleased = true
        networkRetryJob?.cancel()
        playerListener?.let { mediaController?.removeListener(it) }
        playerListener = null
        
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        controllerFuture = null
        mediaController = null

        speechManager.shutdown()
        voiceManager.destroy()
        super.onCleared()
    }
}

enum class HapticType {
    CLICK, DOUBLE_TAP, LONG_PRESS, REPEAT_ON, REPEAT_OFF, BOOKMARK,
    NETWORK_LOSS, NETWORK_RECOVERY,
    VOICE_LISTENING_STARTED, VOICE_COMMAND_SUCCESS, VOICE_COMMAND_FAILURE,
    NONE
}
