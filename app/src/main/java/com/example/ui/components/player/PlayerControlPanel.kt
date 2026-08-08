package com.example.ui.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BlindAccessibleButton
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.AccessibleGreenAccent
import com.example.ui.theme.DarkImmersiveBg
import com.example.ui.theme.DarkImmersiveBorder
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.TextPrimaryWhite

@Composable
fun ListeningVoiceBanner(isScreenOffMode: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isScreenOffMode) 1.0f else 1.08f,
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
    val currentOnTogglePlay by rememberUpdatedState(onTogglePlay)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrev by rememberUpdatedState(onPrev)
    val currentOnToggleBookmark by rememberUpdatedState(onToggleBookmark)
    val currentOnToggleRepeat by rememberUpdatedState(onToggleRepeat)
    val currentOnSingleTapAnnounce by rememberUpdatedState(onSingleTapAnnounce)

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
        val repeatText = if (tarkizRepeatMode == 1) "بدون تكرار" else "تكرار $tarkizRepeatMode مرات"
        ControlAccessibleButton(
            onClick = currentOnToggleRepeat,
            onClickLabel = "وضع تكرار الحفظ والتركيز. الحالي: $repeatText",
            onSingleTap = { currentOnSingleTapAnnounce("وضع التكرار") },
            testTag = "repeat_tarkiz_button",
            icon = Icons.Default.Repeat,
            contentDescription = "وضع التكرار",
            isActive = tarkizRepeatMode > 1,
            badgeText = if (tarkizRepeatMode > 1 && tarkizRepeatMode != 99) "$tarkizRepeatMode" else null,
            buttonSize = 54.dp,
            iconSize = 30.dp
        )

        // Previous Ayah Button
        ControlAccessibleButton(
            onClick = currentOnPrev,
            onClickLabel = "الانتقال للآية السابقة",
            onSingleTap = { currentOnSingleTapAnnounce("الانتقال للآية السابقة") },
            testTag = "prev_ayah_button",
            icon = Icons.Default.SkipPrevious,
            contentDescription = "الآية السابقة",
            isActive = true,
            buttonSize = 56.dp,
            iconSize = 38.dp
        )

        // Main Play/Pause Big Center Button
        BlindAccessibleButton(
            onClick = currentOnTogglePlay,
            onClickLabel = if (isPlaying) "إيقاف التلاوة مؤقتاً" else "تشغيل التلاوة",
            onSingleTap = { currentOnSingleTapAnnounce(if (isPlaying) "إيقاف التلاوة مؤقتاً" else "تشغيل التلاوة") },
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
        ControlAccessibleButton(
            onClick = currentOnNext,
            onClickLabel = "الانتقال للآية التالية",
            onSingleTap = { currentOnSingleTapAnnounce("الانتقال للآية التالية") },
            testTag = "next_ayah_button",
            icon = Icons.Default.SkipNext,
            contentDescription = "الآية التالية",
            isActive = true,
            buttonSize = 56.dp,
            iconSize = 38.dp
        )

        // Bookmark Toggle Button
        ControlAccessibleButton(
            onClick = currentOnToggleBookmark,
            onClickLabel = if (isBookmarked) "إزالة الآية من المفضلة" else "حفظ الآية في المفضلة والإشارات المرجعية",
            onSingleTap = { currentOnSingleTapAnnounce("تغيير حالة المفضلة") },
            testTag = "bookmark_toggle_button",
            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = if (isBookmarked) "إزالة من المفضلة" else "إضافة للمفضلة",
            isActive = isBookmarked,
            buttonSize = 54.dp,
            iconSize = 32.dp
        )
    }
}

@Composable
fun ControlAccessibleButton(
    onClick: () -> Unit,
    onClickLabel: String,
    onSingleTap: () -> Unit,
    testTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    badgeText: String? = null,
    buttonSize: Dp,
    iconSize: Dp
) {
    BlindAccessibleIconButton(
        onClick = onClick,
        onClickLabel = onClickLabel,
        onSingleTap = onSingleTap,
        modifier = Modifier
            .size(buttonSize)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) AccessibleGold else TextPrimaryWhite,
                modifier = Modifier.size(iconSize)
            )
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    color = AccessibleGreenAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
