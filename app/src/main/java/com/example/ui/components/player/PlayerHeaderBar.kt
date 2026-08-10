package com.example.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    isContinuousPlayEnabled: Boolean,
    onToggleContinuousPlay: () -> Unit,
    onOpenSurahIndex: () -> Unit,
    onOpenReciters: () -> Unit,
    onSingleTapAnnounce: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Icons (Circular)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAccessibleButton(
                onClick = {
                    onToggleContinuousPlay()
                    onSingleTapAnnounce(if (!isContinuousPlayEnabled) "التشغيل المتواصل مفعل" else "التشغيل المتواصل معطل")
                },
                onClickLabel = "تفعيل أو تعطيل التشغيل المتواصل",
                onSingleTap = { 
                    onSingleTapAnnounce(if (isContinuousPlayEnabled) "الاستماع المتواصل مفعل" else "الاستماع المتواصل معطل") 
                },
                testTag = "continuous_play_button",
                icon = Icons.Default.Repeat,
                contentDescription = "الاستماع المتواصل. حالياً " + (if (isContinuousPlayEnabled) "مفعل" else "معطل"),
                isActive = isContinuousPlayEnabled
            )
            Spacer(modifier = Modifier.weight(1f))
            HeaderAccessibleButton(
                onClick = onOpenSurahIndex,
                onClickLabel = "فتح قائمة اختيار السورة",
                onSingleTap = { onSingleTapAnnounce("اختيار السورة") },
                testTag = "surah_index_button",
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "اختيار السورة"
            )
            Spacer(modifier = Modifier.weight(1f))
            HeaderAccessibleButton(
                onClick = onOpenReciters,
                onClickLabel = "تغيير القارئ المفضل",
                onSingleTap = { onSingleTapAnnounce("اختيار القارئ") },
                testTag = "reciter_select_button",
                icon = Icons.Default.Person,
                contentDescription = "اختيار القارئ"
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
            .size(62.dp)
            .background(if (isActive) AccessibleGold else DarkImmersiveCard, CircleShape)
            .border(1.dp, if (isActive) AccessibleGold else DarkImmersiveBorder, CircleShape)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) DarkImmersiveBg else AccessibleGold,
            modifier = Modifier.size(31.dp)
        )
    }
}
