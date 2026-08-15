package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.size
import com.example.ui.components.BlindAccessibleIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Surah
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmAccentTerracottaBright
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

@Composable
fun SurahIndexSheet(
    surahs: List<Surah>,
    currentSurahId: Int?,
    currentAyahIndex: Int,
    onSelectSurah: (Int, Int) -> Unit, // surahId, ayahIndex
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    var selectedSurahForAyahs by remember { mutableStateOf<Surah?>(null) }
    val surahListState = rememberLazyListState()
    val ayahListState = rememberLazyListState()

    val currentSurah = selectedSurahForAyahs

    if (currentSurah != null) {
        BackHandler {
            selectedSurahForAyahs = null
            onAnnounce("تم العودة لقائمة السور")
        }
    }

    val titleContent: @Composable () -> Unit = {
        val annotatedTitle = if (currentSurah != null) {
            buildAnnotatedString {
                withStyle(SpanStyle(color = WarmTextPrimary)) {
                    append("سورة ${currentSurah.nameArabic} ")
                }
                withStyle(SpanStyle(color = WarmAccentTerracotta, fontSize = 20.sp)) {
                    append("( اختيار الآية )")
                }
            }
        } else {
            buildAnnotatedString {
                withStyle(SpanStyle(color = WarmTextPrimary)) {
                    append("اختيار السورة ")
                }
                withStyle(SpanStyle(color = WarmAccentTerracotta, fontSize = 20.sp)) {
                    append("(114 سورة)")
                }
            }
        }
        Text(
            text = annotatedTitle,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = if (currentSurah != null) "قائمة آيات سورة ${currentSurah.nameArabic}" else "اختيار السورة. يحتوي على مائة وأربعة عشر سورة"
            }
        )
    }

    AccessibleBottomSheet(
        title = if (currentSurah != null) "سورة ${currentSurah.nameArabic} ( اختيار الآية )" else "اختيار السورة (114 سورة)",
        contentDescriptionText = if (currentSurah != null) "قائمة آيات سورة ${currentSurah.nameArabic}" else "اختيار السورة. يحتوي على مائة وأربعة عشر سورة",
        onDismiss = onDismiss,
        onAnnounce = onAnnounce,
        headerContent = null,
        titleContent = titleContent,
        showCloseButton = false
    ) {
            LaunchedEffect(currentSurahId, currentSurah) {
                if (currentSurah == null && currentSurahId != null) {
                    val targetIndex = maxOf(0, currentSurahId - 1 - 3)
                    surahListState.scrollToItem(targetIndex)
                }
            }

            LaunchedEffect(currentSurah) {
                if (currentSurah != null) {
                    if (currentSurah.id == currentSurahId && currentAyahIndex > 0) {
                        ayahListState.scrollToItem(currentAyahIndex)
                    } else {
                        ayahListState.scrollToItem(0)
                    }
                }
            }

            if (currentSurah != null) {
                LazyColumn(
                    state = ayahListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val count = currentSurah.ayahCount
                    items(count, key = { it }) { index ->
                        val ayahNumber = index + 1
                        val isCurrentAyahSelected = currentSurah.id == currentSurahId && index == currentAyahIndex
                        AccessibleListItemCard(
                            title = "الآية $ayahNumber",
                            isSelected = isCurrentAyahSelected,
                            onClickLabel = "تشغيل من الآية $ayahNumber",
                            contentDescriptionText = "الآية $ayahNumber. ${if (isCurrentAyahSelected) "محددة حالياً. " else ""}انقر مرتين للتشغيل من هذه الآية.",
                            onClick = { onSelectSurah(currentSurah.id, index) },
                            cardHeight = 64.dp
                        )
                    }
                }
            } else {
                val displaySurahs = remember(surahs) {
                    surahs.sortedBy { it.id }
                }
                LazyColumn(
                    state = surahListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displaySurahs, key = { it.id }) { surah ->
                        val isSelected = surah.id == currentSurahId
                        AccessibleListItemCard(
                            title = "سورة ${surah.nameArabic}",
                            subtitle = "${surah.revelationType} • ${surah.ayahCount} آية",
                            isSelected = isSelected,
                            onClickLabel = "عرض آيات سورة ${surah.nameArabic}",
                            contentDescriptionText = "سورة ${surah.nameArabic}. رقمها ${surah.id}. آياتها ${surah.ayahCount}. انقر مرتين لاختيار الآية.",
                            onClick = { selectedSurahForAyahs = surah },
                            testTag = "surah_item_${surah.id}",
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(44.dp)
                                        .background(
                                            if (isSelected) WarmAccentTerracotta else WarmCardActive,
                                            shape = RoundedCornerShape(22.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${surah.id}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else WarmTextLight
                                    )
                                }
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Text(
                                        text = "مفتوحة الآن",
                                        color = WarmAccentTerracottaBright,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }
    }
}
