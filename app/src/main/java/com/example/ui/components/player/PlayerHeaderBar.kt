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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAccessibleButton(
                onClick = onToggleContinuousPlay,
                onClickLabel = if (isContinuousPlayEnabled) "إيقاف الاستماع المتواصل" else "تفعيل الاستماع المتواصل",
                testTag = "continuous_play_button",
                text = "الاستماع\nالمتواصل",
                contentDescription = "الاستماع المتواصل. حالياً " + (if (isContinuousPlayEnabled) "مفعل" else "معطل"),
                isActive = isContinuousPlayEnabled
            )
            Spacer(modifier = Modifier.weight(1f))
            HeaderAccessibleButton(
                onClick = onOpenSurahIndex,
                onClickLabel = "فتح قائمة السور",
                testTag = "surah_index_button",
                text = "اختيار\nالسورة",
                contentDescription = "اختيار السورة"
            )
            Spacer(modifier = Modifier.weight(1f))
            HeaderAccessibleButton(
                onClick = onOpenReciters,
                onClickLabel = "تغيير القارئ",
                testTag = "reciter_select_button",
                text = "اختيار\nالقارئ",
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
    text: String,
    contentDescription: String,
    isActive: Boolean = false
) {
    val bgColor = if (isActive) WarmAccentTerracotta else WarmCardLight
    val borderColor = if (isActive) WarmAccentTerracottaBright else WarmCardBorder
    val textColor = if (isActive) WarmTextLight else WarmAccentTerracotta

    BlindAccessibleIconButton(
        onClick = onClick,
        onClickLabel = onClickLabel,
        modifier = Modifier
            .size(76.dp)
            .background(bgColor, CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

