package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.aistudio.quranblind.audio.AudioEngine
import com.aistudio.quranblind.audio.AudioEngineEvent
import com.aistudio.quranblind.audio.AudioTrack
import com.aistudio.quranblind.audio.AyahTrackId
import com.aistudio.quranblind.audio.PlaybackStatus
import com.example.service.QuranAudioService
import com.example.accessibility.HapticFeedbackManager
import com.example.accessibility.SpeechManager
import com.aistudio.quranblind.domain.model.Bookmark
import com.aistudio.quranblind.store.BookmarkStore
import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Reciter
import com.aistudio.quranblind.domain.model.Surah
import com.example.domain.repository.QuranRepository
import com.aistudio.quranblind.store.SessionState
import com.aistudio.quranblind.store.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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
    val selectedReciter: Reciter = Reciter.DEFAULT_RECITER,
    val tarkizRepeatMode: Int = 1,
    val isContinuousPlayEnabled: Boolean = false
)

data class BookmarkUiState(
    val isCurrentAyahBookmarked: Boolean = false
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

@HiltViewModel
class QuranViewModel @Inject constructor(
    application: Application,
    private val repository: QuranRepository,
    val haptic: HapticFeedbackManager,
    val speechManager: SpeechManager,
    private val sessionStore: SessionStore,
    private val bookmarkStore: BookmarkStore
) : AndroidViewModel(application) {

    private var mediaController: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var audioEngine: AudioEngine? = null
    private var eventsJob: Job? = null
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

    private val _dialogUiState = MutableStateFlow(DialogUiState())
    val dialogUiState: StateFlow<DialogUiState> = _dialogUiState.asStateFlow()

    private val _screenModeUiState = MutableStateFlow(ScreenModeUiState())
    val screenModeUiState: StateFlow<ScreenModeUiState> = _screenModeUiState.asStateFlow()

    private val _announcementEvent = Channel<String>(Channel.BUFFERED)
    val announcementEvent = _announcementEvent.receiveAsFlow()

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkStore.bookmarks

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var progressJob: kotlinx.coroutines.Job? = null

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                if (_screenModeUiState.value.isScreenOffMode) {
                    delay(500L)
                    continue
                }
                val engine = audioEngine
                if (engine != null && engine.isPlaying) {
                    val currentPos = engine.positionMs
                    val dur = engine.durationMs
                    val prog = if (dur > 0L) (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                    _playbackProgress.value = prog
                    _playbackUiState.update { it.copy(playbackProgress = prog) }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTracking(resetProgress: Boolean = false) {
        progressJob?.cancel()
        progressJob = null
        if (resetProgress) {
            _playbackProgress.value = 0f
            _playbackUiState.update { it.copy(playbackProgress = 0f) }
        }
    }

    private fun handleAudioEngineEvent(event: AudioEngineEvent) {
        when (event) {
            is AudioEngineEvent.IsPlayingChanged -> {
                _playbackUiState.update { it.copy(isPlaying = event.isPlaying) }
                if (event.isPlaying) {
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

            is AudioEngineEvent.StatusChanged -> {
                when (event.status) {
                    PlaybackStatus.BUFFERING -> {
                        _playbackUiState.update { it.copy(isLoadingAudio = true) }
                    }
                    PlaybackStatus.READY -> {
                        _playbackUiState.update { it.copy(isLoadingAudio = false) }
                    }
                    PlaybackStatus.ENDED -> {
                        onAyahPlaybackEnded()
                    }
                    PlaybackStatus.IDLE -> {}
                }
            }

            is AudioEngineEvent.TrackChanged -> {
                if (!event.automatic) return
                val (surahId, ayahNumber) = event.trackId?.let { AyahTrackId.decode(it) } ?: return
                val currentAyahs = _playbackUiState.value.currentAyahs
                val newIndex = currentAyahs.indexOfFirst { it.surahId == surahId && it.numberInSurah == ayahNumber }
                if (newIndex != -1 && newIndex != _playbackUiState.value.currentAyahIndex) {
                    _playbackUiState.update { it.copy(currentAyahIndex = newIndex, currentLoopCount = 1, playbackProgress = 0f) }
                }
            }

            is AudioEngineEvent.PlaybackFailed -> {
                pendingAyahAnnouncement = null
                if (event.isNetworkRelated) {
                    handleNetworkPlaybackError()
                } else {
                    announce("حدث خطأ في تشغيل الصوت")
                }
            }
        }
    }

    init {
        // ADR-005: the bookmark flag is derived from the store flow, so a late legacy
        // migration or a toggle is reflected without any point-in-time reads.
        combine(bookmarkStore.bookmarks, _playbackUiState) { list, playback ->
            val ayah = playback.currentAyahs.getOrNull(playback.currentAyahIndex)
            ayah != null && list.any { it.surahId == ayah.surahId && it.ayahNumber == ayah.numberInSurah }
        }.onEach { bookmarked ->
            _bookmarkUiState.update { it.copy(isCurrentAyahBookmarked = bookmarked) }
        }.launchIn(viewModelScope)
        val sessionToken = SessionToken(application, ComponentName(application, QuranAudioService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            if (isControllerReleased) return@addListener
            val controller = controllerFuture?.get() ?: return@addListener
            mediaController = controller
            val engine = AudioEngine(controller)
            audioEngine = engine
            eventsJob = engine.events.onEach { event -> handleAudioEngineEvent(event) }.launchIn(viewModelScope)

            // Play pending audio if any
            pendingAudioUrlToPlay?.let { url ->
                pendingAudioUrlToPlay = null
                playAudioUrl(url)
            }
        }, ContextCompat.getMainExecutor(application))

        val savedSession = sessionStore.load()
        if (savedSession != null) {
            val reciter = Reciter.DEFAULT_RECITERS.find { it.serverIdentifier == savedSession.reciterId } ?: Reciter.DEFAULT_RECITER
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
            val defaultReciter = Reciter.DEFAULT_RECITER
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
            
            sessionStore.save(
                SessionState(
                    reciterId = _settingsUiState.value.selectedReciter.serverIdentifier,
                    surahId = surahId,
                    ayahIndex = targetAyahIndex
                )
            )

            repository.getAyahs(surahId, _settingsUiState.value.selectedReciter.serverIdentifier).collect { ayahs ->
                _playbackUiState.update {
                    it.copy(
                        currentAyahs = ayahs,
                        isLoadingAudio = false
                    )
                }

                if (autoPlay && ayahs.isNotEmpty()) {
                    playCurrentAyah()
                }
            }
        }
    }

    private fun playAudioUrl(url: String) {
        audioEngine?.let { engine ->
            engine.setQueue(listOf(AudioTrack(id = "", url = url)), 0)
            engine.prepare()
            engine.play()
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

        val engine = audioEngine
        if (engine != null) {
            val isContinuous = _settingsUiState.value.isContinuousPlayEnabled
            val repeatMode = _settingsUiState.value.tarkizRepeatMode

            if (isContinuous && repeatMode <= 1) {
                val tracks = ayahs.drop(index).map { ayah ->
                    AudioTrack(id = AyahTrackId.encode(ayah.surahId, ayah.numberInSurah), url = ayah.audioUrl)
                }
                engine.setQueue(tracks, 0)
            } else {
                val track = AudioTrack(
                    id = AyahTrackId.encode(activeAyah.surahId, activeAyah.numberInSurah),
                    url = activeAyah.audioUrl
                )
                engine.setQueue(listOf(track), 0)
            }
            engine.prepare()
            engine.play()
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
            audioEngine?.pause()
            _playbackUiState.update { it.copy(isPlaying = false) }
        }
    }

    fun togglePlayback() {
        if (audioEngine?.playWhenReady == true && audioEngine?.isPlaying == true) {
            audioEngine?.pause()
            performAction("تم الإيقاف المؤقت", HapticType.DOUBLE_TAP)
        } else {
            val ayahs = _playbackUiState.value.currentAyahs
            val index = _playbackUiState.value.currentAyahIndex
            val activeAyah = ayahs.getOrNull(index)
            val currentLoadedUri = audioEngine?.currentTrack?.url

            if (activeAyah != null && (currentLoadedUri == null || currentLoadedUri != activeAyah.audioUrl || audioEngine?.status == PlaybackStatus.ENDED || audioEngine?.status == PlaybackStatus.IDLE)) {
                playCurrentAyah()
                performAction("جاري التشغيل", HapticType.DOUBLE_TAP)
            } else {
                audioEngine?.play()
                performAction("جاري التشغيل", HapticType.DOUBLE_TAP)
            }
        }
    }

    fun pausePlayback() {
        audioEngine?.pause()
        _playbackUiState.update { it.copy(isPlaying = false) }
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
            audioEngine?.let { engine ->
                if (engine.status != PlaybackStatus.IDLE && engine.status != PlaybackStatus.ENDED) {
                    val ayahs = _playbackUiState.value.currentAyahs
                    val index = _playbackUiState.value.currentAyahIndex
                    val repeatMode = _settingsUiState.value.tarkizRepeatMode
                    if (repeatMode <= 1 && index + 1 < ayahs.size && engine.currentIndex != -1) {
                        val tracksToAdd = ayahs.drop(index + 1).map { ayah ->
                            AudioTrack(id = AyahTrackId.encode(ayah.surahId, ayah.numberInSurah), url = ayah.audioUrl)
                        }
                        engine.replaceUpcoming(tracksToAdd)
                    }
                }
            }
        } else {
            performAction("تم إيقاف الاستماع المتواصل", HapticType.CLICK, forceSpeak = forceSpeak)
            audioEngine?.let { engine ->
                if (engine.currentIndex != -1) {
                    engine.clearUpcoming()
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
        
        sessionStore.save(
            SessionState(
                reciterId = _settingsUiState.value.selectedReciter.serverIdentifier,
                surahId = state.currentSurah?.id ?: 1,
                ayahIndex = index
            )
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
            val isNowBookmarked = bookmarkStore.toggle(
                surahId = surah.id,
                surahNameAr = surah.nameArabic,
                ayahNumber = activeAyah.numberInSurah
            )
            
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
            sessionStore.save(
                SessionState(
                    reciterId = reciter.serverIdentifier,
                    surahId = currentSurahId,
                    ayahIndex = _playbackUiState.value.currentAyahIndex
                )
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
                audioEngine?.stop()
                audioEngine?.clearQueue()
                loadSurah(surah.id, _playbackUiState.value.currentAyahIndex, autoPlay = true)
            }
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
            if (audioEngine?.isPlaying == true) {
                audioEngine?.pause()
            }
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
        }
        _dialogUiState.update { it.copy(showSurahIndex = show) }
    }

    fun toggleBookmarksSheet(show: Boolean) {
        performAction("", HapticType.CLICK)
        if (show) {
            if (audioEngine?.isPlaying == true) {
                audioEngine?.pause()
            }
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
        }
        _dialogUiState.update { it.copy(showBookmarksSheet = show) }
    }

    fun toggleReciterDialog(show: Boolean) {
        performAction("", HapticType.CLICK)
        if (show) {
            if (audioEngine?.isPlaying == true) {
                audioEngine?.pause()
            }
            _playbackUiState.update { it.copy(isLoadingAudio = false) }
        }
        _dialogUiState.update { it.copy(showReciterDialog = show) }
    }

    fun toggleHelpDialog(show: Boolean) {
        performAction("", HapticType.CLICK)
        if (show) {
            if (audioEngine?.isPlaying == true) {
                audioEngine?.pause()
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
            HapticType.NONE -> {}
        }
        if (msg.isNotEmpty()) {
            announce(msg, forceSpeak)
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
                audioEngine?.prepare()
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
        eventsJob?.cancel()
        eventsJob = null
        audioEngine?.release()
        audioEngine = null

        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        controllerFuture = null
        mediaController = null

        speechManager.shutdown()
        super.onCleared()
    }
}

enum class HapticType {
    CLICK, DOUBLE_TAP, LONG_PRESS, REPEAT_ON, REPEAT_OFF, BOOKMARK,
    NETWORK_LOSS, NETWORK_RECOVERY,
    NONE
}
