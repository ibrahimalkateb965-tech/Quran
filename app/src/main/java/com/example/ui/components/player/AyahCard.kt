package com.example.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.DarkImmersiveBorder
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.DarkImmersiveSurface
import com.example.ui.theme.TextMutedZinc
import com.example.ui.theme.TextPrimaryWhite


import com.example.ui.components.blindAccessibleClickable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AyahCard(
    ayah: Ayah,
    isCurrentProvider: () -> Boolean,
    isPlayingProvider: () -> Boolean = { false },
    isScreenOffModeProvider: () -> Boolean = { false },
    onClick: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onSingleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCurrent = isCurrentProvider()
    val isPlaying = isPlayingProvider()
    val isScreenOffMode = isScreenOffModeProvider()
    
    val borderColor = if (isCurrent) AccessibleGold else DarkImmersiveBorder
    val bgColor = if (isCurrent) DarkImmersiveSurface else DarkImmersiveCard
    val elevation = if (isCurrent) 8.dp else 2.dp
    
    Card(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { 
                contentDescription = ","
            }
            .blindAccessibleClickable(
                onClickLabel = "", // إفراغ النص لمنع نطق TalkBack الافتراضي بالإنجليزية
                onLongClickLabel = "",
                onClick = onClick,
                onSingleTap = onSingleTap,
                onLongClick = onDoubleTap
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = ayah.textArabic,
                style = MaterialTheme.typography.headlineLarge,
                color = if (isCurrent) TextPrimaryWhite else TextMutedZinc,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )
            
            if (isCurrent && isPlaying && !isScreenOffMode) {
                Spacer(modifier = Modifier.height(12.dp))
                AudioEqualizerBars(isPlaying = true)
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AyahNumberCard(
    number: Int,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkImmersiveCard),
        border = BorderStroke(1.dp, DarkImmersiveBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "الآية $number",
                style = MaterialTheme.typography.titleLarge,
                color = AccessibleGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
