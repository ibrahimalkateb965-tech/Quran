package com.example.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.LocalTalkBackEnabled
import com.example.data.model.Ayah
import com.example.ui.theme.UthmanTahaFont
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmCardActiveBorder
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmEarthBg
import com.example.ui.theme.WarmTextAyah

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AyahCard(
    ayah: Ayah,
    isCurrentProvider: () -> Boolean,
    isPlayingProvider: () -> Boolean = { false },
    isScreenOffModeProvider: () -> Boolean = { false },
    progressProvider: () -> Float = { 0f },
    onClick: () -> Unit,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCurrent = isCurrentProvider()
    val isPlaying = isPlayingProvider()
    val isScreenOffMode = isScreenOffModeProvider()
    
    val borderColor = if (isCurrent) WarmCardActiveBorder else WarmCardBorder
    val bgColor = WarmEarthBg
    val elevation = if (isCurrent) 4.dp else 1.dp
    val isTalkBackEnabled = LocalTalkBackEnabled.current
    
    val semanticsModifier = if (isTalkBackEnabled) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(ayah.numberInSurah) {
        scrollState.scrollTo(0)
    }

    val cleanText = remember(ayah.textArabic) {
        sanitizeUthmanicText(ayah.textArabic)
    }
    
    Card(
        modifier = modifier
            .fillMaxSize()
            .then(semanticsModifier)
            .then(
                if (isTalkBackEnabled) Modifier
                else Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onDoubleTap
                )
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cleanText,
                    fontFamily = UthmanTahaFont,
                    color = if (isCurrent) WarmTextAyah else WarmTextAyah.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 68.sp
                    ),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )
                
                if (isCurrent && isPlaying && !isScreenOffMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AudioEqualizerBars(isPlaying = true)
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = WarmCardLight),
        border = BorderStroke(1.dp, WarmCardBorder),
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
                color = WarmAccentTerracotta,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SurahNameCard(
    name: String,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCardLight),
        border = BorderStroke(1.dp, WarmCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "سورة $name",
                style = MaterialTheme.typography.titleLarge,
                color = WarmAccentTerracotta,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val bareNoonNextLetters = "[يرملونصذثكجشقسدطزفتضظب]"
private val noonSukoonPattern = Regex("(ن)[\\u0652\\u06DF\\u06E0\\u06E1](?=\\s*$bareNoonNextLetters)")

private fun sanitizeUthmanicText(text: String): String {
    return text.replace(noonSukoonPattern, "$1")
        .replace('\u06DF', '\u06E0')
        .replace('\u06E4', '\u0653')
        .replace("\u0600", "")
        .replace("\u06DD", "")
        .replace("\uFEFF", "")
        .replace("\u200A", "")
        .replace("\u2060", "")
}
