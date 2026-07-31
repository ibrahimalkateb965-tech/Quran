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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    val uiState by viewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag > 60) {
                                    viewModel.playNextAyah()
                                } else if (totalDrag < -60) {
                                    viewModel.playPreviousAyah()
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
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
                        onOpenHelp = { viewModel.toggleHelpDialog(true) },
                        onToggleScreenOff = { viewModel.toggleScreenOffMode() },
                        isScreenOffMode = uiState.isScreenOffMode,
                        onSingleTapAnnounce = { viewModel.announce(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic TalkBack Live Announcement & Voice Feedback
                    val activeSurah = uiState.currentSurah
                    val ayahs = uiState.currentAyahs
                    val currentAyah = ayahs.getOrNull(uiState.currentAyahIndex)

                    val talkBackDescription = buildString {
                        append("تطبيق القرآن الكريم للمكفوفين. ")
                        if (activeSurah != null) {
                            append("سورة ${activeSurah.nameArabic}، الآية ${currentAyah?.numberInSurah ?: 1} من أصل ${activeSurah.ayahCount}. ")
                        }
                        if (uiState.isPlaying) append("جاري التشغيل. ") else append("متوقف مؤقتاً. ")
                        if (uiState.tarkizRepeatMode > 1) append("وضع التكرار مفعّل. ")
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
                        visible = uiState.isListeningVoice,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ListeningVoiceBanner()
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

                    // 3-Ayah List Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentIndex = uiState.currentAyahIndex
                        
                        // Previous Ayah Card
                        val prevAyah = ayahs.getOrNull(currentIndex - 1)
                        if (prevAyah != null) {
                            AyahCard(
                                ayah = prevAyah,
                                label = "الآية السابقة",
                                isCurrent = false,
                                onClick = { viewModel.playPreviousAyah() },
                                onSingleTap = { viewModel.announce("الآية ${prevAyah.numberInSurah} السابقة") },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        
                        // Current Ayah Card
                        val currAyah = ayahs.getOrNull(currentIndex)
                        if (currAyah != null) {
                            AyahCard(
                                ayah = currAyah,
                                label = "الآية الحالية",
                                isCurrent = true,
                                isPlaying = uiState.isPlaying,
                                onClick = { viewModel.togglePlayback() },
                                onSingleTap = { viewModel.announce("الآية ${currAyah.numberInSurah} الحالية") },
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                        
                        // Next Ayah Card
                        val nextAyah = ayahs.getOrNull(currentIndex + 1)
                        if (nextAyah != null) {
                            AyahCard(
                                ayah = nextAyah,
                                label = "الآية التالية",
                                isCurrent = false,
                                onClick = { viewModel.playNextAyah() },
                                onSingleTap = { viewModel.announce("الآية ${nextAyah.numberInSurah} التالية") },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
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

                    // Accessibility Voice Command Big Button
                    BigVoiceMicrophoneButton(
                        isListening = uiState.isListeningVoice,
                        onClick = { viewModel.startVoiceCommand() },
                        onSingleTapAnnounce = { viewModel.announce(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Playback & Control Panel
                    ControlPanel(
                        isPlaying = uiState.isPlaying,
                        isBookmarked = uiState.isCurrentAyahBookmarked,
                        tarkizRepeatMode = uiState.tarkizRepeatMode,
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
                            text = "القارئ: ${uiState.selectedReciter.nameArabic}",
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
                if (uiState.isScreenOffMode) {
                    ScreenOffSaverOverlay(
                        onWakeUp = { viewModel.toggleScreenOffMode() }
                    )
                }

                // Surah Index Dialog Sheet
                if (uiState.showSurahIndex) {
                    SurahIndexSheet(
                        surahs = uiState.surahs,
                        currentSurahId = uiState.currentSurah?.id,
                        onSelectSurah = { surahId, ayahIndex ->
                            viewModel.loadSurah(surahId, targetAyahIndex = ayahIndex, autoPlay = true)
                            viewModel.toggleSurahIndex(false)
                        },
                        onDismiss = { viewModel.toggleSurahIndex(false) },
                        onAnnounce = { viewModel.announce(it) }
                    )
                }

                // Reciter Selector Sheet
                if (uiState.showReciterDialog) {
                    ReciterSelectorSheet(
                        selectedReciter = uiState.selectedReciter,
                        onSelectReciter = { reciter ->
                            viewModel.selectReciter(reciter)
                            viewModel.toggleReciterDialog(false)
                        },
                        onDismiss = { viewModel.toggleReciterDialog(false) },
                        onAnnounce = { viewModel.announce(it) }
                    )
                }

                // Voice Guide Sheet
                if (uiState.showHelpDialog) {
                    VoiceCommandGuideSheet(
                        onSpeakGuide = {
                            viewModel.announce(
                                "دليل الأوامر الصوتية. قل: تشغيل سورة كذا، أو الآية كذا، أو توقف، أو تشغيل، أو التالي، أو تكرار، أو أسماء القراء، أو قائمة السور."
                            )
                        },
                        onDismiss = { viewModel.toggleHelpDialog(false) },
                        onAnnounce = { viewModel.announce(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AudioEqualizerBars(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(24.dp)
    ) {
        val barHeights = listOf(0.4f, 0.8f, 0.5f, 0.9f, 0.6f)
        val animTargetValues = listOf(0.9f, 0.3f, 0.95f, 0.4f, 0.85f)

        barHeights.forEachIndexed { index, baseScale ->
            val scale by infiniteTransition.animateFloat(
                initialValue = if (isPlaying) baseScale else 0.25f,
                targetValue = if (isPlaying) animTargetValues[index] else 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + index * 70, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((24 * scale).dp)
                    .background(
                        color = if (index % 2 == 0) AccessibleGold else AccessibleGoldVariant,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun GestureHintChip(label: String) {
    Surface(
        color = DarkImmersiveCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkImmersiveBorder)
    ) {
        Text(
            text = label,
            color = TextMutedZinc,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun HeaderBar(
    onOpenSurahIndex: () -> Unit,
    onOpenReciters: () -> Unit,
    onOpenHelp: () -> Unit,
    onToggleScreenOff: () -> Unit,
    isScreenOffMode: Boolean,
    onSingleTapAnnounce: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {


        // Action Buttons Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            BlindAccessibleIconButton(
                onClick = onOpenSurahIndex,
                onClickLabel = "فتح فهرس السور",
                onSingleTap = { onSingleTapAnnounce("فهرس السور") },
                modifier = Modifier
                    .size(48.dp)
                    .background(DarkImmersiveCard, CircleShape)
                    .border(1.dp, DarkImmersiveBorder, CircleShape)
                    .testTag("surah_index_button")
            ) {
                Icon(Icons.Default.List, contentDescription = "فهرس السور", tint = AccessibleGold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            BlindAccessibleIconButton(
                onClick = onOpenReciters,
                onClickLabel = "تغيير القارئ المفضل",
                onSingleTap = { onSingleTapAnnounce("اختيار القارئ") },
                modifier = Modifier
                    .size(48.dp)
                    .background(DarkImmersiveCard, CircleShape)
                    .border(1.dp, DarkImmersiveBorder, CircleShape)
                    .testTag("reciter_select_button")
            ) {
                Icon(Icons.Default.Person, contentDescription = "اختيار القارئ", tint = AccessibleGold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            BlindAccessibleIconButton(
                onClick = onOpenHelp,
                onClickLabel = "تعليمات الأوامر الصوتية",
                onSingleTap = { onSingleTapAnnounce("دليل المساعدة") },
                modifier = Modifier
                    .size(48.dp)
                    .background(DarkImmersiveCard, CircleShape)
                    .border(1.dp, DarkImmersiveBorder, CircleShape)
                    .testTag("help_guide_button")
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = "دليل المساعدة", tint = AccessibleGold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            BlindAccessibleIconButton(
                onClick = onToggleScreenOff,
                onClickLabel = "وضع إيقاف الشاشة لتوفير البطارية",
                onSingleTap = { onSingleTapAnnounce("وضع إيقاف الشاشة") },
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isScreenOffMode) AccessibleGold else DarkImmersiveCard, CircleShape)
                    .border(1.dp, if (isScreenOffMode) AccessibleGold else DarkImmersiveBorder, CircleShape)
                    .testTag("screen_off_toggle")
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "وضع إيقاف الشاشة",
                    tint = if (isScreenOffMode) DarkImmersiveBg else AccessibleGold
                )
            }
        }
    }
}

@Composable
fun ListeningVoiceBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        color = AccessibleGold,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = 4.dp)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = "جاري الاستماع الآن، تحدث بالأمر الصوتي"
            }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = DarkImmersiveBg, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "جاري الاستماع لطلبك الصوتي...",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkImmersiveBg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BigVoiceMicrophoneButton(
    isListening: Boolean,
    onClick: () -> Unit,
    onSingleTapAnnounce: (String) -> Unit
) {
    BlindAccessibleButton(
        onClick = onClick,
        onClickLabel = "زر الأمر الصوتي الرئيسي. اضغط للتحدث بأسماء السور أو الأوامر",
        onSingleTap = { onSingleTapAnnounce("استماع للأوامر الصوتية") },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, if (isListening) AccessibleGreenAccent else AccessibleGold, RoundedCornerShape(20.dp))
            .testTag("voice_mic_main_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isListening) AccessibleGreenAccent else AccessibleGold
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "استماع للأوامر الصوتية",
                tint = DarkImmersiveBg,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isListening) "جاري الاستماع..." else "تحدث بالأمر الصوتي (أو اضغط مطولاً)",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkImmersiveBg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ControlPanel(
    isPlaying: Boolean,
    isBookmarked: Boolean,
    tarkizRepeatMode: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSingleTapAnnounce: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(DarkImmersiveCard, RoundedCornerShape(24.dp))
            .border(1.dp, DarkImmersiveBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Repeat Tarkiz Button
        BlindAccessibleIconButton(
            onClick = onToggleRepeat,
            onClickLabel = "وضع تكرار الحفظ والتركيز. الحالي: ${if (tarkizRepeatMode == 1) "بدون تكرار" else "تكرار $tarkizRepeatMode مرات"}",
            onSingleTap = { onSingleTapAnnounce("وضع التكرار") },
            modifier = Modifier
                .size(54.dp)
                .testTag("repeat_tarkiz_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "وضع التكرار",
                    tint = if (tarkizRepeatMode > 1) AccessibleGreenAccent else TextPrimaryWhite,
                    modifier = Modifier.size(30.dp)
                )
                if (tarkizRepeatMode > 1 && tarkizRepeatMode != 99) {
                    Text(
                        text = "$tarkizRepeatMode",
                        color = AccessibleGreenAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        // Previous Ayah Button
        BlindAccessibleIconButton(
            onClick = onPrev,
            onClickLabel = "الانتقال للآية السابقة",
            onSingleTap = { onSingleTapAnnounce("الانتقال للآية السابقة") },
            modifier = Modifier
                .size(56.dp)
                .testTag("prev_ayah_button")
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "الآية السابقة",
                tint = AccessibleGold,
                modifier = Modifier.size(38.dp)
            )
        }

        // Main Play/Pause Big Center Button
        BlindAccessibleButton(
            onClick = onTogglePlay,
            onClickLabel = if (isPlaying) "إيقاف التلاوة مؤقتاً" else "تشغيل التلاوة",
            onSingleTap = { onSingleTapAnnounce(if (isPlaying) "إيقاف التلاوة مؤقتاً" else "تشغيل التلاوة") },
            modifier = Modifier
                .size(64.dp)
                .testTag("play_pause_center_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AccessibleGold),
            shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "إيقاف التلاوة" else "تشغيل التلاوة",
                tint = DarkImmersiveBg,
                modifier = Modifier.size(40.dp)
            )
        }

        // Next Ayah Button
        BlindAccessibleIconButton(
            onClick = onNext,
            onClickLabel = "الانتقال للآية التالية",
            onSingleTap = { onSingleTapAnnounce("الانتقال للآية التالية") },
            modifier = Modifier
                .size(56.dp)
                .testTag("next_ayah_button")
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "الآية التالية",
                tint = AccessibleGold,
                modifier = Modifier.size(38.dp)
            )
        }

        // Bookmark Toggle Button
        BlindAccessibleIconButton(
            onClick = onToggleBookmark,
            onClickLabel = if (isBookmarked) "إزالة الآية من المفضلة" else "حفظ الآية في المفضلة والإشارات المرجعية",
            onSingleTap = { onSingleTapAnnounce("تغيير حالة المفضلة") },
            modifier = Modifier
                .size(54.dp)
                .testTag("bookmark_toggle_button")
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isBookmarked) "إزالة من المفضلة" else "إضافة للمفضلة",
                tint = if (isBookmarked) AccessibleGold else TextPrimaryWhite,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun ScreenOffSaverOverlay(
    onWakeUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onWakeUp() }
            .semantics {
                contentDescription = "الشاشة مغلقة لتوفير البطارية. انقر مرتين لإلغاء القفل أو واصل التحكم بالإيماءات والصوت."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "وضع توفير البطارية للمكفوفين مفعّل",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "المس الشاشة مرتين للخروج من الوضع",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AyahCard(
    ayah: com.example.data.model.Ayah,
    label: String,
    isCurrent: Boolean,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onSingleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isCurrent) AccessibleGold else DarkImmersiveBorder
    val bgColor = if (isCurrent) DarkImmersiveSurface else DarkImmersiveCard
    val elevation = if (isCurrent) 8.dp else 2.dp
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { 
                contentDescription = "$label: رقم ${ayah.numberInSurah}، ${ayah.textArabic}"
            }
            .blindAccessibleClickable(
                onClickLabel = "تشغيل الآية",
                onClick = onClick,
                onSingleTap = onSingleTap
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$label - الآية ${ayah.numberInSurah}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) AccessibleGold else TextMutedZinc,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = ayah.textArabic,
                style = MaterialTheme.typography.headlineMedium,
                color = if (isCurrent) TextPrimaryWhite else TextMutedZinc,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            if (isCurrent && isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                AudioEqualizerBars(isPlaying = true)
            }
        }
    }
}
