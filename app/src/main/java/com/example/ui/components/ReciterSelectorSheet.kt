package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Reciter
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.DarkImmersiveBg
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.DarkImmersiveSurface
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.components.blindAccessibleClickable

@Composable
fun ReciterSelectorSheet(
    selectedReciter: Reciter,
    onSelectReciter: (Reciter) -> Unit,
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    AccessibleBottomSheet(
        title = "اختيار القارئ",
        contentDescriptionText = "قائمة اختيار القارئ المفضل",
        onDismiss = onDismiss,
        onAnnounce = onAnnounce,
        showCloseButton = false
    ) {
        LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(Reciter.DEFAULT_RECITERS, key = { it.id }) { reciter ->
                    val isSelected = reciter.id == selectedReciter.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .blindAccessibleClickable(
                                onClickLabel = "اختيار القارئ",
                                onClick = { onSelectReciter(reciter) },
                                onSingleTap = { onAnnounce("القارئ ${reciter.nameArabic}") }
                            )
                            .semantics {
                                contentDescription = "${reciter.nameArabic}. ${if (isSelected) "مختار حالياً" else "انقر مرتين لاختياره"}"
                            }
                            .testTag("reciter_item_${reciter.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) DarkImmersiveSurface else DarkImmersiveCard
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = reciter.nameArabic,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (isSelected) AccessibleGold else TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = reciter.nameEnglish,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryWhite.copy(alpha = 0.7f)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "محدد",
                                    tint = AccessibleGold,
                                    modifier = Modifier
                                        .height(32.dp)
                                        .width(32.dp)
                                )
                            }
                        }
                    }
            }
        }
    }
}
