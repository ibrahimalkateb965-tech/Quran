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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAccessibleButton(
                onClick = onOpenSurahIndex,
                onClickLabel = "فتح قائمة اختيار السورة",
                onSingleTap = { onSingleTapAnnounce("اختيار السورة") },
                testTag = "surah_index_button",
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "اختيار السورة"
            )
            Spacer(modifier = Modifier.width(24.dp))
            HeaderAccessibleButton(
                onClick = onOpenReciters,
                onClickLabel = "تغيير القارئ المفضل",
                onSingleTap = { onSingleTapAnnounce("اختيار القارئ") },
                testTag = "reciter_select_button",
                icon = Icons.Default.Person,
                contentDescription = "اختيار القارئ"
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Continuous Play Strip
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(
                    onClickLabel = "تفعيل أو تعطيل التشغيل المتواصل",
                    onClick = {
                        onToggleContinuousPlay()
                        onSingleTapAnnounce(if (!isContinuousPlayEnabled) "التشغيل المتواصل مفعل" else "التشغيل المتواصل معطل")
                    }
                )
                .semantics {
                    contentDescription = "زر التشغيل المتواصل. حالياً " + (if (isContinuousPlayEnabled) "مفعل" else "معطل") + ". انقر مرتين للتبديل."
                },
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (isContinuousPlayEnabled) AccessibleGold else DarkImmersiveCard
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.Text(
                    text = "الاستماع المتواصل",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = if (isContinuousPlayEnabled) DarkImmersiveBg else androidx.compose.ui.graphics.Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = if (isContinuousPlayEnabled) DarkImmersiveBg else AccessibleGold,
                    modifier = Modifier.size(28.dp)
                )
            }
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
