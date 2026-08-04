package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.BlindAccessibleIconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Surah
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.DarkImmersiveBg
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.DarkImmersiveSurface
import com.example.ui.theme.DarkImmersiveBorder
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.components.blindAccessibleClickable

@Composable
fun SurahIndexSheet(
    surahs: List<Surah>,
    currentSurahId: Int?,
    onSelectSurah: (Int, Int) -> Unit, // surahId, ayahIndex
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSurahForAyahs by remember { mutableStateOf<Surah?>(null) }

    val filteredSurahs = remember(searchQuery, surahs) {
        if (searchQuery.isBlank()) surahs
        else {
            val q = searchQuery.trim()
            surahs.filter {
                it.nameArabic.contains(q) || it.id.toString() == q || it.nameEnglish.lowercase().contains(q.lowercase())
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("surah_index_sheet"),
        color = DarkImmersiveBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedSurahForAyahs != null) {
                    BlindAccessibleIconButton(
                        onClick = { selectedSurahForAyahs = null },
                        onClickLabel = "العودة لقائمة السور",
                        onSingleTap = { onAnnounce("العودة لقائمة السور") },
                        modifier = Modifier
                            .height(48.dp)
                            .width(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع لقائمة السور",
                            tint = AccessibleGold
                        )
                    }
                    Text(
                        text = "اختر الآية (${selectedSurahForAyahs!!.nameArabic})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AccessibleGold,
                        modifier = Modifier.semantics {
                            contentDescription = "قائمة آيات سورة ${selectedSurahForAyahs!!.nameArabic}"
                        }
                    )
                } else {
                    Text(
                        text = "فهرس السور (114 سورة)",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AccessibleGold,
                        modifier = Modifier.semantics {
                            contentDescription = "فهرس السور. يحتوي على مائة وأربعة عشر سورة"
                        }
                    )
                }

                BlindAccessibleIconButton(
                    onClick = onDismiss,
                    onClickLabel = "إغلاق النافذة",
                    onSingleTap = { onAnnounce("إغلاق نافذة السور") },
                    modifier = Modifier
                        .height(48.dp)
                        .width(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق نافذة السور",
                        tint = AccessibleGold
                    )
                }
            }

            if (selectedSurahForAyahs == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("surah_search_input")
                        .semantics { contentDescription = "مربع بحث عن سورة بالاسم أو الرقم" },
                    placeholder = {
                        Text("ابحث باسم السورة أو رقمها (مثال: الكهف أو 18)", color = Color.Gray, fontSize = 16.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AccessibleGold)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccessibleGold,
                        unfocusedBorderColor = DarkImmersiveBorder,
                        focusedTextColor = TextPrimaryWhite,
                        unfocusedTextColor = TextPrimaryWhite,
                        focusedContainerColor = DarkImmersiveCard,
                        unfocusedContainerColor = DarkImmersiveCard
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedSurahForAyahs != null) {
                    val count = selectedSurahForAyahs!!.ayahCount
                    items(count, key = { it }) { index ->
                        val ayahNumber = index + 1
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .blindAccessibleClickable(
                                    onClickLabel = "تشغيل من الآية $ayahNumber",
                                    onClick = { onSelectSurah(selectedSurahForAyahs!!.id, index) },
                                    onSingleTap = { onAnnounce("الآية $ayahNumber") }
                                )
                                .semantics {
                                    contentDescription = "الآية $ayahNumber. انقر مرتين للتشغيل من هذه الآية."
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkImmersiveCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "الآية $ayahNumber",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSurahs, key = { it.id }) { surah ->
                        val isSelected = surah.id == currentSurahId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .blindAccessibleClickable(
                                    onClickLabel = "عرض آيات سورة ${surah.nameArabic}",
                                    onClick = { selectedSurahForAyahs = surah },
                                    onSingleTap = { onAnnounce("سورة ${surah.nameArabic}، عدد آياتها ${surah.ayahCount}") }
                                )
                                .semantics {
                                    contentDescription = "سورة ${surah.nameArabic}. رقمها ${surah.id}. آياتها ${surah.ayahCount}. انقر مرتين لاختيار الآية."
                                }
                                .testTag("surah_item_${surah.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkImmersiveSurface else DarkImmersiveCard
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .width(44.dp)
                                            .background(
                                                if (isSelected) AccessibleGold else DarkImmersiveSurface,
                                                shape = RoundedCornerShape(22.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${surah.id}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) DarkImmersiveBg else AccessibleGold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = "سورة ${surah.nameArabic}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = if (isSelected) AccessibleGold else TextPrimaryWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${surah.revelationType} • ${surah.ayahCount} آية",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.LightGray
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Text(
                                        text = "مفتوحة الآن",
                                        color = AccessibleGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

