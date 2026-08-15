package com.example.ui.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmAccentTerracottaBright

@Composable
fun AudioEqualizerBars(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .height(24.dp)
            .clearAndSetSemantics { } // زخرفي بحت — لا قيمة معلوماتية لقارئ الشاشة
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
                        color = if (index % 2 == 0) WarmAccentTerracotta else WarmAccentTerracottaBright,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
