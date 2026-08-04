package com.example.ui.components.player

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.TextMutedZinc

@Composable
fun GestureHintChip(label: String) {
    Surface(
        color = DarkImmersiveCard,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = TextMutedZinc,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
