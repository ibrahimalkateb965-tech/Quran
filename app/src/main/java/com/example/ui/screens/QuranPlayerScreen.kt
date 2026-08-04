package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Icon
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.components.BlindAccessibleButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.components.player.AudioEqualizerBars
import com.example.ui.components.player.AyahCard
import com.example.ui.components.player.AyahNumberCard
import com.example.ui.components.player.BigVoiceMicrophoneButton
import com.example.ui.components.player.ControlPanel
import com.example.ui.components.player.GestureHintChip
import com.example.ui.components.player.HeaderBar
import com.example.ui.components.player.ListeningVoiceBanner
import com.example.ui.components.player.ScreenOffSaverOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.LocalTalkBackEnabled
import com.example.accessibility.announceForAccessibility
import com.example.ui.components.ReciterSelectorSheet
import com.example.ui.components.SurahIndexSheet
import com.example.ui.components.VoiceCommandGuideSheet
import com.example.ui.components.blindAccessibleClickable
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.AccessibleGoldVariant
import com.example.ui.theme.AccessibleGreenAccent
import com.example.ui.theme.DarkImmersiveBg
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.DarkImmersiveSurface
import com.example.ui.theme.DarkImmersiveBorder
import com.example.ui.theme.TextMutedZinc
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.viewmodel.QuranViewModel

@Composable
fun QuranPlayerScreen(
    viewModel: QuranViewModel
) {
    val playbackUiState by viewModel.playbackUiState.collectAsState()
    val settingsUiState by viewModel.settingsUiState.collectAsState()
    val bookmarkUiState by viewModel.bookmarkUiState.collectAsState()
    val voiceUiState by viewModel.voiceUiState.collectAsState()
    val dialogUiState by viewModel.dialogUiState.collectAsState()
    val screenModeUiState by viewModel.screenModeUiState.collectAsState()
    val isTalkBackEnabled by viewModel.speechManager.isTalkBackEnabledFlow.collectAsState()
    val context = LocalContext.current

    // Observe announcement events
    LaunchedEffect(viewModel.announcementEvent, isTalkBackEnabled) {
        viewModel.announcementEvent.collect { message ->
            if (message.isNotBlank() && isTalkBackEnabled) {
                announceForAccessibility(context, message)
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTalkBackEnabled provides isTalkBackEnabled
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkImmersiveBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("quran_player_root")
                    // Ambient Subtle Gold Glow Canvas Effect
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AccessibleGold.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
                    // Full Screen Gestures for Blind Users
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                viewModel.togglePlayback()
                            },
                            onLongPress = {
                                viewModel.startVoiceCommand()
                            }
                        )
                    }
                    ) {
                // Main Accessible Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar with Status Badges and Quick Controls
                    HeaderBar(
                        onOpenSurahIndex = { viewModel.toggleSurahIndex(true) },
                        onOpenReciters = { viewModel.toggleReciterDialog(true) },
                        onToggleScreenOff = { viewModel.toggleScreenOffMode() },
                        isScreenOffMode = screenModeUiState.isScreenOffMode,
                        isContinuousPlayEnabled = settingsUiState.isContinuousPlayEnabled,
                        onToggleContinuousPlay = { viewModel.toggleContinuousPlay() },
                        onSingleTapAnnounce = { viewModel.announce(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic TalkBack Live Announcement & Voice Feedback
                    val activeSurah = playbackUiState.currentSurah
                    val ayahs = playbackUiState.currentAyahs
                    val currentAyah = ayahs.getOrNull(playbackUiState.currentAyahIndex)

                    val talkBackDescription = buildString {
                        append("تطبيق القرآن الكريم للمكفوفين. ")
                        if (activeSurah != null) {
                            append("سورة ${activeSurah.nameArabic}، الآية ${currentAyah?.numberInSurah ?: 1} من أصل ${activeSurah.ayahCount}. ")
                        }
                        if (playbackUiState.isPlaying) append("جاري التشغيل. ") else append("متوقف مؤقتاً. ")
                        if (settingsUiState.tarkizRepeatMode > 1) append("وضع التكرار مفعّل. ")
                        append("انقر مرتين للتشغيل أو الإيقاف. اضغط مطولاً للتحدث بالحدث الصوتي. اسحب يميناً ويساراً للتنقل.")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = talkBackDescription
                            }
                    )

                    // Active Voice Listening Pulse Indicator
                    AnimatedVisibility(
                        visible = voiceUiState.isListeningVoice,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ListeningVoiceBanner(isScreenOffMode = screenModeUiState.isScreenOffMode)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Surah Info Header
                    Text(
                        text = "سورة ${activeSurah?.nameArabic ?: ""} • ${activeSurah?.revelationType ?: ""} • ${activeSurah?.ayahCount ?: ""} آية",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccessibleGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .semantics { contentDescription = "سورة ${activeSurah?.nameArabic}، عدد الآيات ${activeSurah?.ayahCount}" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // HorizontalPager for Ayahs
                    val pagerState = rememberPagerState(
                        initialPage = playbackUiState.currentAyahIndex,
                        pageCount = { ayahs.size }
                    )
                    var isProgrammaticScroll by remember { mutableStateOf(false) }

                    // Sync ViewModel state to Pager (when audio auto-advances or commands change the Ayah)
                    LaunchedEffect(playbackUiState.currentAyahIndex) {
                        val target = playbackUiState.currentAyahIndex
                        if (target != pagerState.currentPage && target in ayahs.indices && !pagerState.isScrollInProgress) {
                            isProgrammaticScroll = true
                            try {
                                pagerState.animateScrollToPage(target)
                            } finally {
                                isProgrammaticScroll = false
                            }
                        }
                    }

                    // Sync Pager state to ViewModel (when user swipes)
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }
                            .distinctUntilChanged()
                            .collect { page ->
                                if (isProgrammaticScroll) return@collect
                                val currentIndex = viewModel.playbackUiState.value.currentAyahIndex
                                if (page != currentIndex && page in ayahs.indices) {
                                    viewModel.goToAyah(page, autoPlay = true)
                                }
                            }
                    }

                    val currentAyahIndex by remember(playbackUiState.currentAyahIndex) { derivedStateOf { playbackUiState.currentAyahIndex } }
                    val isPlaying by remember(playbackUiState.isPlaying) { derivedStateOf { playbackUiState.isPlaying } }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        val ayah = ayahs.getOrNull(page)
                        if (ayah != null) {
                            val isCurrentPage by remember(page, currentAyahIndex) {
                                derivedStateOf { page == currentAyahIndex }
                            }
                            val isPagePlaying by remember(isCurrentPage, isPlaying) {
                                derivedStateOf { isCurrentPage && isPlaying }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AyahCard(
                                    ayah = ayah,
                                    isCurrent = isCurrentPage,
                                    isPlaying = isPagePlaying,
                                    isScreenOffMode = screenModeUiState.isScreenOffMode,
                                    onClick = { },
                                    onDoubleTap = { viewModel.togglePlayback() },
                                    onSingleTap = { },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                AyahNumberCard(number = ayah.numberInSurah)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gesture Hint Quick Chips (Immersive UI Style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GestureHintChip(label = "نقرتين: تشغيل/إيقاف")
                        GestureHintChip(label = "ضغط مطول: أوامر صوتية")
                        GestureHintChip(label = "سحب أفقي: آية آية")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Spacer(modifier = Modifier.height(10.dp))

                    // Primary Playback & Control Panel
                    ControlPanel(
                        isPlaying = playbackUiState.isPlaying,
                        isBookmarked = bookmarkUiState.isCurrentAyahBookmarked,
                        tarkizRepeatMode = settingsUiState.tarkizRepeatMode,
                        onTogglePlay = { viewModel.togglePlayback() },
                        onNext = { viewModel.playNextAyah() },
                        onPrev = { viewModel.playPreviousAyah() },
                        onToggleBookmark = { viewModel.toggleCurrentBookmark() },
                        onToggleRepeat = { viewModel.toggleRepeatMode() },
                        onSingleTapAnnounce = { viewModel.announce(it) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom Reciter & Mode Bar Readout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "القارئ: ${settingsUiState.selectedReciter.nameArabic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedZinc
                        )
                        Text(
                            text = "الوضع: مكفوفين • قارئ الشاشة مفعّل",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccessibleGreenAccent
                        )
                    }
                }

                // Full Screen-Off Battery Saver Overlay (for blind users)
                if (screenModeUiState.isScreenOffMode) {
                    ScreenOffSaverOverlay(
                        onWakeUp = { viewModel.toggleScreenOffMode() }
                    )
                }

                // Surah Index Dialog Sheet
                if (dialogUiState.showSurahIndex) {
                    SurahIndexSheet(
                        surahs = playbackUiState.surahs,
                        currentSurahId = playbackUiState.currentSurah?.id,
                        onSelectSurah = { surahId, ayahIndex ->
                            viewModel.loadSurah(surahId, targetAyahIndex = ayahIndex, autoPlay = true)
                            viewModel.toggleSurahIndex(false)
                        },
                        onDismiss = { viewModel.toggleSurahIndex(false) },
                        onAnnounce = { viewModel.announce(it) }
                    )
                }

                // Reciter Selector Sheet
                if (dialogUiState.showReciterDialog) {
                    ReciterSelectorSheet(
                        selectedReciter = settingsUiState.selectedReciter,
                        onSelectReciter = { reciter ->
                            viewModel.selectReciter(reciter)
                            viewModel.toggleReciterDialog(false)
                        },
                        onDismiss = { viewModel.toggleReciterDialog(false) },
                        onAnnounce = { viewModel.announce(it) }
                    )
                }

            }
        }
    }
}
