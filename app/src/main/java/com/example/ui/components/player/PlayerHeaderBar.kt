package com.example.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.DarkImmersiveBg
import com.example.ui.theme.DarkImmersiveBorder
import com.example.ui.theme.DarkImmersiveCard

@Composable
fun HeaderBar(
    onOpenSurahIndex: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenReciters: () -> Unit,
    onToggleScreenOff: () -> Unit,
    isScreenOffMode: Boolean,
    isContinuousPlayEnabled: Boolean,
    onToggleContinuousPlay: () -> Unit,
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
            HeaderAccessibleButton(
                onClick = onOpenSurahIndex,
                onClickLabel = "فتح فهرس السور",
                onSingleTap = { onSingleTapAnnounce("فهرس السور") },
                testTag = "surah_index_button",
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "فهرس السور"
            )
            Spacer(modifier = Modifier.width(6.dp))
            HeaderAccessibleButton(
                onClick = onOpenBookmarks,
                onClickLabel = "الإشارات المرجعية",
                onSingleTap = { onSingleTapAnnounce("الإشارات المرجعية") },
                testTag = "bookmarks_button",
                icon = Icons.Default.Bookmark,
                contentDescription = "الإشارات المرجعية"
            )
            Spacer(modifier = Modifier.width(6.dp))
            HeaderAccessibleButton(
                onClick = onOpenReciters,
                onClickLabel = "تغيير القارئ المفضل",
                onSingleTap = { onSingleTapAnnounce("اختيار القارئ") },
                testTag = "reciter_select_button",
                icon = Icons.Default.Person,
                contentDescription = "اختيار القارئ"
            )
            Spacer(modifier = Modifier.width(6.dp))
            HeaderAccessibleButton(
                onClick = onToggleContinuousPlay,
                onClickLabel = "زر التشغيل المتواصل",
                onSingleTap = { onSingleTapAnnounce("التشغيل المتواصل") },
                testTag = "continuous_play_toggle",
                icon = Icons.Default.Repeat,
                contentDescription = "التشغيل المتواصل",
                isActive = isContinuousPlayEnabled
            )
            Spacer(modifier = Modifier.width(6.dp))

            HeaderAccessibleButton(
                onClick = onToggleScreenOff,
                onClickLabel = "وضع إيقاف الشاشة لتوفير البطارية",
                onSingleTap = { onSingleTapAnnounce("وضع إيقاف الشاشة") },
                testTag = "screen_off_toggle",
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = "وضع إيقاف الشاشة",
                isActive = isScreenOffMode
            )
        }
    }
}

@Composable
fun HeaderAccessibleButton(
    onClick: () -> Unit,
    onClickLabel: String,
    onSingleTap: () -> Unit,
    testTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean = false
) {
    BlindAccessibleIconButton(
        onClick = onClick,
        onClickLabel = onClickLabel,
        onSingleTap = onSingleTap,
        modifier = Modifier
            .size(48.dp)
            .background(if (isActive) AccessibleGold else DarkImmersiveCard, CircleShape)
            .border(1.dp, if (isActive) AccessibleGold else DarkImmersiveBorder, CircleShape)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) DarkImmersiveBg else AccessibleGold
        )
    }
}
