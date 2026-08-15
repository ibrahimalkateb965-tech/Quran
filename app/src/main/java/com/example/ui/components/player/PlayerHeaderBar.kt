package com.example.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmAccentTerracottaBright
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmTextLight

@Composable
fun HeaderBar(
    isContinuousPlayEnabled: Boolean,
    onToggleContinuousPlay: () -> Unit,
    onOpenSurahIndex: () -> Unit,
    onOpenReciters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAccessibleButton(
                onClick = onToggleContinuousPlay,
                onClickLabel = if (isContinuousPlayEnabled) "إيقاف الاستماع المتواصل" else "تفعيل الاستماع المتواصل",
                testTag = "continuous_play_button",
                icon = Icons.Default.Repeat,
                contentDescription = "الاستماع المتواصل. حالياً " + (if (isContinuousPlayEnabled) "مفعل" else "معطل"),
                isActive = isContinuousPlayEnabled
            )
            Spacer(modifier = Modifier.weight(1f))
            HeaderAccessibleButton(
                onClick = onOpenSurahIndex,
                onClickLabel = "فتح قائمة السور",
                testTag = "surah_index_button",
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "اختيار السورة"
            )
            Spacer(modifier = Modifier.weight(1f))
            HeaderAccessibleButton(
                onClick = onOpenReciters,
                onClickLabel = "تغيير القارئ",
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
    testTag: String,
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false
) {
    val bgColor = if (isActive) WarmAccentTerracotta else WarmCardLight
    val borderColor = if (isActive) WarmAccentTerracottaBright else WarmCardBorder
    val iconTint = if (isActive) WarmTextLight else WarmAccentTerracotta

    BlindAccessibleIconButton(
        onClick = onClick,
        onClickLabel = onClickLabel,
        modifier = Modifier
            .size(62.dp)
            .background(bgColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(31.dp)
        )
    }
}
