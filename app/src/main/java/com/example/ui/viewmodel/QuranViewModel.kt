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
    val currentLoopCount: Int = 1,
    val continuousPlayStartIndex: Int? = null,
    val playbackProgress: Float = 0f
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

enum class StartupStep { RECITERS, SURAHS, COMPLETED }

data class DialogUiState(
    val startupStep: StartupStep = StartupStep.RECITERS,
    val showSurahIndex: Boolean = false,
    val showReciterDialog: Boolean = true,
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
    private val sessionPrefs = com.example.data.local.SessionPreferences.getInstance(application)

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

    private var progressJob: kotlinx.coroutines.Job? = null

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val currentPos = controller.currentPosition
                    val dur = controller.duration
                    val prog = if (dur > 0L) (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                    _playbackUiState.update { it.copy(playbackProgress = prog) }
                }
                delay(60L)
            }
        }
    }

    private fun stopProgressTracking(resetProgress: Boolean = false) {
        progressJob?.cancel()
        progressJob = null
        if (resetProgress) {
            _playbackUiState.update { it.copy(playbackProgress = 0f) }
        }
    }

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
                        startProgressTracking()
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
                    } else {
                        stopProgressTracking()
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

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        val mediaId = mediaItem?.mediaId ?: return
                        val parts = mediaId.split("_")
                        if (parts.size == 2) {
                            val surahId = parts[0].toIntOrNull()
                            val ayahNumber = parts[1].toIntOrNull()
                            if (surahId != null && ayahNumber != null) {
                                val currentAyahs = _playbackUiState.value.currentAyahs
                                val newIndex = currentAyahs.indexOfFirst { it.surahId == surahId && it.numberInSurah == ayahNumber }
                                if (newIndex != -1 && newIndex != _playbackUiState.value.currentAyahIndex) {
                                    _playbackUiState.update { it.copy(currentAyahIndex = newIndex, currentLoopCount = 1, playbackProgress = 0f) }
                                    viewModelScope.launch {
                                        val bookmarked = repository.isBookmarked(surahId, ayahNumber)
                                        _bookmarkUiState.update { it.copy(isCurrentAyahBookmarked = bookmarked) }
                                    }
                                }
                            }
                        }
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

        val savedSession = sessionPrefs.getSession()
        if (savedSession != null) {
            val reciter = Reciter.DEFAULT_RECITERS.find { it.serverIdentifier == savedSession.reciterId } ?: Reciter.DEFAULT_RECITERS.first()
            _settingsUiState.update { it.copy(selectedReciter = reciter) }
            closeStartupDialogs()
            loadSurah(savedSession.surahId, savedSession.ayahIndex, autoPlay = true)
            
            if (speechManager.isTalkBackEnabled()) {
                announce("استئناف التلاوة.")
            } else {
                announce("مرحباً بعودتك، تم استئناف التلاوة من آخر توقف.")
            }
        } else {
            // First launch fallback: Al-Fatihah, Ayah 1
            val defaultReciter = Reciter.DEFAULT_RECITERS.first()
            _settingsUiState.update { it.copy(selectedReciter = defaultReciter) }
            closeStartupDialogs()
            loadSurah(surahId = 1, targetAyahIndex = 0, autoPlay = false)

            if (speechManager.isTalkBackEnabled()) {
                announce(
                    "مرحباً بك. تم اختيار سورة الفاتحة كبداية. " +
                    "قارئ الشاشة يعمل الآن. لتجربة أفضل مع المساعد الصوتي الخاص بالتطبيق، " +
                    "يُفضّل إيقاف قارئ الشاشة بالضغط المطول على زري رفع وخفض الصوت معاً."
                )
            } else {
                announce("مرحباً بك. تم اختيار سورة الفاتحة والقارئ الافتراضي كبداية.")
            }
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
                    currentLoopCount = 1,
                    continuousPlayStartIndex = targetAyahIndex
                )
            }
            
            announce("${surah.translationArabic}. عدد آياتها ${surah.ayahCount}.")
            
            sessionPrefs.saveSession(
                reciterId = _settingsUiState.value.selectedReciter.serverIdentifier,
                surahId = surahId,
                ayahIndex = targetAyahIndex
            )

            repository.getAyahs(surahId, _settingsUiState.value.selectedReciter.serverIdentifier).collect { ayahs ->
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

                if (autoPlay && ayahs.isNotEmpty()) {
                    playCurrentAyah()
                }
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
            val isContinuous = _settingsUiState.value.isContinuousPlayEnabled
            val repeatMode = _settingsUiState.value.tarkizRepeatMode
            
            if (isContinuous && repeatMode <= 1) {
                val mediaItems = ayahs.drop(index).map { ayah ->
                    androidx.media3.common.MediaItem.Builder()
                        .setUri(ayah.audioUrl)
                        .setMediaId("${ayah.surahId}_${ayah.numberInSurah}")
                        .build()
                }
                controller.setMediaItems(mediaItems, 0, 0L)
            } else {
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri(activeAyah.audioUrl)
                    .setMediaId("${activeAyah.surahId}_${activeAyah.numberInSurah}")
                    .build()
                controller.setMediaItem(mediaItem, 0L)
            }
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

        // Check for end of Surah
        val isLastAyahInSurah = playbackState.currentAyahIndex >= playbackState.currentAyahs.lastIndex
        if (isLastAyahInSurah) {
            _playbackUiState.update { it.copy(isPlaying = false, currentLoopCount = 1) }
            announce("انتهت سورة ${playbackState.currentSurah?.nameArabic}")
            return
        }

        // Otherwise reset loop count and proceed to next Ayah
        _playbackUiState.update { it.copy(currentLoopCount = 1) }

        if (_settingsUiState.value.isContinuousPlayEnabled) {
            goToAyah(playbackState.currentAyahIndex + 1, autoPlay = true, isManual = false)
        } else {
            mediaController?.pause()
            _playbackUiState.update { it.copy(isPlaying = false) }
        }
    }

    fun togglePlayback() {
        if (mediaController?.playWhenReady == true && mediaController?.isPlaying == true) {
            mediaController?.pause()
            performAction("تم الإيقاف المؤقت", HapticType.DOUBLE_TAP)
        } else {
            val ayahs = _playbackUiState.value.currentAyahs
            val index = _playbackUiState.value.currentAyahIndex
            val activeAyah = ayahs.getOrNull(index)
            val currentLoadedUri = mediaController?.currentMediaItem?.localConfiguration?.uri?.toString()

            if (activeAyah != null && (currentLoadedUri == null || currentLoadedUri != activeAyah.audioUrl || mediaController?.playbackState == Player.STATE_ENDED || mediaController?.playbackState == Player.STATE_IDLE)) {
                playCurrentAyah()
                performAction("جاري التشغيل", HapticType.DOUBLE_TAP)
            } else {
                mediaController?.play()
                performAction("جاري التشغيل", HapticType.DOUBLE_TAP)
            }
        }
    }

    fun pausePlayback() {
        mediaController?.pause()
    }


    fun replayCurrentAyah() {
        val state = _playbackUiState.value
        _playbackUiState.update { it.copy(currentLoopCount = state.currentLoopCount + 1) }
        performAction("", HapticType.DOUBLE_TAP)
        playCurrentAyah()
    }

    fun toggleContinuousPlay(forceSpeak: Boolean = false) {
        val next = !_settingsUiState.value.isContinuousPlayEnabled
        _settingsUiState.update { it.copy(isContinuousPlayEnabled = next) }
        if (next) {
            _playbackUiState.update { it.copy(continuousPlayStartIndex = it.currentAyahIndex) }
            performAction("وضع الاستماع المتواصل مفعّل", HapticType.CLICK, forceSpeak = forceSpeak)
            mediaController?.let { controller ->
                if (controller.playbackState != Player.STATE_IDLE && controller.playbackState != Player.STATE_ENDED) {
                    val ayahs = _playbackUiState.value.currentAyahs
                    val index = _playbackUiState.value.currentAyahIndex
                    val repeatMode = _settingsUiState.value.tarkizRepeatMode
                    if (repeatMode <= 1 && index + 1 < ayahs.size) {
                        val mediaItemsToAdd = ayahs.drop(index + 1).map { ayah ->
                            MediaItem.Builder()
                                .setUri(ayah.audioUrl)
                                .setMediaId("${ayah.surahId}_${ayah.numberInSurah}")
                                .build()
                        }
                        val currentItemIndex = controller.currentMediaItemIndex
                        if (currentItemIndex != -1) {
                            if (controller.mediaItemCount > currentItemIndex + 1) {
                                controller.removeMediaItems(currentItemIndex + 1, controller.mediaItemCount)
                            }
                            controller.addMediaItems(currentItemIndex + 1, mediaItemsToAdd)
                        }
                    }
                }
            }
        } else {
            performAction("تم إيقاف الاستماع المتواصل", HapticType.CLICK, forceSpeak = forceSpeak)
            mediaController?.let { controller ->
                val currentItemIndex = controller.currentMediaItemIndex
                if (currentItemIndex != -1 && controller.mediaItemCount > currentItemIndex + 1) {
                    controller.removeMediaItems(currentItemIndex + 1, controller.mediaItemCount)
                }
            }
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

    fun goToAyah(index: Int, autoPlay: Boolean = true, isManual: Boolean = true) {
        val state = _playbackUiState.value
        if (index !in state.currentAyahs.indices || index == state.currentAyahIndex) return

        _playbackUiState.update {
            it.copy(
                currentAyahIndex = index,
                currentLoopCount = 1,
                continuousPlayStartIndex = if (isManual) index else it.continuousPlayStartIndex,
                playbackProgress = 0f
            )
        }
        val ayah = state.currentAyahs[index]
        
        sessionPrefs.saveSession(
            reciterId = _settingsUiState.value.selectedReciter.serverIdentifier,
            surahId = state.currentSurah?.id ?: 1,
            ayahIndex = index
        )
        
        performAction("", HapticType.CLICK)

        if (autoPlay) {
            playCurrentAyah()
        }
    }

    fun toggleRepeatMode(forceSpeak: Boolean = false) {
        val modes = listOf(1, 3, 5, 10, 99)
        val currentIndex = modes.indexOf(_settingsUiState.value.tarkizRepeatMode)
        val newMode = modes[(currentIndex + 1) % modes.size]
        _settingsUiState.update { it.copy(tarkizRepeatMode = newMode) }
        _playbackUiState.update { it.copy(currentLoopCount = 1) }

        if (newMode > 1) {
            val modeText = if (newMode == 99) "تكرار لا نهائي" else "تكرار $newMode مرات"
            performAction("تم تفعيل وضع التركيز: $modeText", HapticType.REPEAT_ON, forceSpeak = forceSpeak)
        } else {
            performAction("تم إيقاف وضع التكرار", HapticType.REPEAT_OFF, forceSpeak = forceSpeak)
        }
    }

    fun toggleCurrentBookmark(forceSpeak: Boolean = false) {
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
                performAction("تم إضافة سورة ${surah.nameArabic} الآية ${activeAyah.numberInSurah} للإشارات المرجعية", HapticType.BOOKMARK, forceSpeak = forceSpeak)
            } else {
                performAction("تم إزالة الآية من الإشارات المرجعية", HapticType.BOOKMARK, forceSpeak = forceSpeak)
            }
        }
    }

    fun selectReciter(reciter: Reciter) {
        _settingsUiState.update { it.copy(selectedReciter = reciter) }
        
        val currentSurahId = _playbackUiState.value.currentSurah?.id
        if (currentSurahId != null) {
            sessionPrefs.saveSession(
                reciterId = reciter.serverIdentifier,
                surahId = currentSurahId,
                ayahIndex = _playbackUiState.value.currentAyahIndex
            )
        }
        
        val currentStep = _dialogUiState.value.startupStep
        if (currentStep == StartupStep.RECITERS) {
            _dialogUiState.update { 
                it.copy(
                    startupStep = StartupStep.SURAHS,
                    showReciterDialog = false,
                    showSurahIndex = true
                ) 
            }
            performAction("تم اختيار القارئ ${reciter.nameArabic}. الرجاء اختيار السورة", HapticType.CLICK)
        } else {
            _dialogUiState.update { it.copy(showReciterDialog = false) }
            performAction("تم تغيير القارئ إلى ${reciter.nameArabic}", HapticType.CLICK)
            val surah = _playbackUiState.value.currentSurah
            if (surah != null) {
                mediaController?.stop()
                mediaController?.clearMediaItems()
                loadSurah(surah.id, _playbackUiState.value.currentAyahIndex, autoPlay = true)
            }
        }
    }

    fun startVoiceCommand() {
        performAction("", HapticType.VOICE_LISTENING_STARTED)

        // Pause Quran audio while listening to avoid the microphone picking up the recitation.
        val wasPlaying = mediaController?.isPlaying == true
        if (wasPlaying) {
            mediaController?.pause()
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
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
                    announce("جاري تشغيل سورة ${surah.nameArabic}", forceSpeak = true)
                    loadSurah(surah.id, autoPlay = true)
                } else {
                    haptic.vibrateVoiceCommandFailure()
                    announce("لم أجد سورة باسم ${result.surahName}", forceSpeak = true)
                }
            }
            is VoiceCommandResult.GoToAyahNumber -> {
                haptic.vibrateVoiceCommandSuccess()
                val ayahs = _playbackUiState.value.currentAyahs
                val targetIndex = (result.ayahNumber - 1).coerceIn(0, (ayahs.size - 1).coerceAtLeast(0))
                if (ayahs.isNotEmpty()) {
                    goToAyah(targetIndex, autoPlay = true)
                    announce("الانتقال إلى الآية ${result.ayahNumber}", forceSpeak = true)
                }
            }
            VoiceCommandResult.Pause -> {
                haptic.vibrateVoiceCommandSuccess()
                if (mediaController?.isPlaying == true) mediaController?.pause()
                announce("تم الإيقاف", forceSpeak = true)
            }
            VoiceCommandResult.Resume -> {
                haptic.vibrateVoiceCommandSuccess()
                mediaController?.play()
                announce("تم التشغيل", forceSpeak = true)
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
                toggleCurrentBookmark(forceSpeak = true)
            }
            VoiceCommandResult.ToggleRepeatMode -> {
                haptic.vibrateVoiceCommandSuccess()
                toggleRepeatMode(forceSpeak = true)
            }
            VoiceCommandResult.ToggleContinuousPlay -> {
                haptic.vibrateVoiceCommandSuccess()
                toggleContinuousPlay(forceSpeak = true)
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
                announce("تم فتح قائمة السور", forceSpeak = true)
            }
            VoiceCommandResult.ShowHelp -> {
                haptic.vibrateVoiceCommandSuccess()
                _dialogUiState.update { it.copy(showHelpDialog = true) }
                announce("تم فتح قائمة التعليمات والأوامر الصوتية", forceSpeak = true)
            }
            is VoiceCommandResult.UnknownCommand -> {
                haptic.vibrateVoiceCommandFailure()
                announce("لم أتعرف على الأمر: ${result.originalText}", forceSpeak = true)
            }
            is VoiceCommandResult.Error -> {
                haptic.vibrateVoiceCommandFailure()
                announce(result.message, forceSpeak = true)
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

    private fun closeStartupDialogs() {
        _dialogUiState.update { 
            it.copy(
                startupStep = StartupStep.COMPLETED,
                showReciterDialog = false,
                showSurahIndex = false
            ) 
        }
    }

    fun toggleSurahIndex(show: Boolean) {
        performAction("", HapticType.CLICK)
        if (show) {
            if (mediaController?.isPlaying == true) {
                mediaController?.pause()
            }
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
        }
        _dialogUiState.update { it.copy(showSurahIndex = show) }
    }

    fun toggleBookmarksSheet(show: Boolean) {
        performAction("", HapticType.CLICK)
        if (show) {
            if (mediaController?.isPlaying == true) {
                mediaController?.pause()
            }
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
        }
        _dialogUiState.update { it.copy(showBookmarksSheet = show) }
    }

    fun toggleReciterDialog(show: Boolean) {
        performAction("", HapticType.CLICK)
        if (show) {
            if (mediaController?.isPlaying == true) {
                mediaController?.pause()
            }
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
        }
        _dialogUiState.update { it.copy(showReciterDialog = show) }
    }

    fun toggleHelpDialog(show: Boolean) {
        performAction("", HapticType.CLICK)
        if (show) {
            if (mediaController?.isPlaying == true) {
                mediaController?.pause()
            }
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
        }
        _dialogUiState.update { it.copy(showHelpDialog = show) }
    }

    fun announce(text: String, forceSpeak: Boolean = false) {
        viewModelScope.launch {
            _announcementEvent.send(text)
        }
        if (forceSpeak || speechManager.isTalkBackEnabled()) {
            speechManager.speak(text)
        }
    }
    
    private fun performAction(msg: String, hapticType: HapticType = HapticType.CLICK, forceSpeak: Boolean = false) {
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
            announce(msg, forceSpeak)
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
        stopProgressTracking(resetProgress = true)
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
