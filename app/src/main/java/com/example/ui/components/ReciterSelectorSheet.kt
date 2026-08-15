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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Reciter
import com.example.ui.theme.WarmAccentGold
import com.example.ui.theme.WarmAccentTerracottaBright
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

@Composable
fun ReciterSelectorSheet(
    selectedReciter: Reciter,
    onSelectReciter: (Reciter) -> Unit,
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    AccessibleBottomSheet(
        title = "اختيار القارئ",
        contentDescriptionText = "اختيار القارئ",
        onDismiss = onDismiss,
        onAnnounce = onAnnounce,
        showCloseButton = false
    ) {
        val listState = rememberLazyListState()
        val index = remember(selectedReciter.id) {
            Reciter.DEFAULT_RECITERS.indexOfFirst { it.id == selectedReciter.id }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it > 0 }
            if (index >= 0) {
                listState.scrollToItem(index)
            }
        }

        LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(Reciter.DEFAULT_RECITERS, key = { it.id }) { reciter ->
                    val isSelected = reciter.id == selectedReciter.id
                    AccessibleListItemCard(
                        title = reciter.nameArabic,
                        subtitle = reciter.nameEnglish,
                        titleColor = if (isSelected) WarmAccentTerracottaBright else WarmTextPrimary,
                        subtitleColor = if (isSelected) WarmTextLight.copy(alpha = 0.75f) else WarmTextSecondary,
                        isSelected = isSelected,
                        onClickLabel = "اختيار القارئ ${reciter.nameArabic}",
                        contentDescriptionText = "${reciter.nameArabic}. ${if (isSelected) "مختار حالياً" else "انقر مرتين لاختياره"}",
                        onClick = { onSelectReciter(reciter) },
                        testTag = "reciter_item_${reciter.id}",
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "محدد",
                                    tint = WarmAccentGold,
                                    modifier = Modifier
                                        .height(32.dp)
                                        .width(32.dp)
                                )
                            }
                        }
                    )
            }
        }
    }
}
