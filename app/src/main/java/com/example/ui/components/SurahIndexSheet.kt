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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    currentAyahIndex: Int,
    onSelectSurah: (Int, Int) -> Unit, // surahId, ayahIndex
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    var selectedSurahForAyahs by remember { mutableStateOf<Surah?>(null) }
    val surahListState = rememberLazyListState()
    val ayahListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    AccessibleBottomSheet(
        title = if (selectedSurahForAyahs != null) "سورة ${selectedSurahForAyahs!!.nameArabic} ( اختيار الآية )" else "اختيار السورة (114 سورة)",
        contentDescriptionText = if (selectedSurahForAyahs != null) "قائمة آيات سورة ${selectedSurahForAyahs!!.nameArabic}" else "اختيار السورة. يحتوي على مائة وأربعة عشر سورة",
        onDismiss = onDismiss,
        onAnnounce = onAnnounce,
        navigationIcon = null,
        headerContent = null,
        showCloseButton = true,
        onBackPress = {
            keyboardController?.hide()
            if (selectedSurahForAyahs != null) {
                selectedSurahForAyahs = null
                onAnnounce("تم العودة لقائمة السور")
            } else {
                onDismiss()
            }
        }
    ) {
            LaunchedEffect(currentSurahId, selectedSurahForAyahs) {
                if (selectedSurahForAyahs == null && currentSurahId != null) {
                    // الفهرس هو (id - 1) لأن المعرفات تبدأ من 1.
                    // نطرح 3 إضافية لنجعل العنصر قريباً من منتصف الشاشة.
                    val targetIndex = maxOf(0, currentSurahId - 1 - 3)
                    surahListState.scrollToItem(targetIndex)
                }
            }

            LaunchedEffect(selectedSurahForAyahs) {
                if (selectedSurahForAyahs != null) {
                    if (selectedSurahForAyahs!!.id == currentSurahId && currentAyahIndex > 0) {
                        ayahListState.scrollToItem(currentAyahIndex)
                    } else {
                        ayahListState.scrollToItem(0)
                    }
                }
            }

            // تم نقل نظام اعتراض التراجع إلى onBackPress في AccessibleBottomSheet

            if (selectedSurahForAyahs != null) {
                LazyColumn(
                    state = ayahListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val count = selectedSurahForAyahs!!.ayahCount
                    items(count, key = { it }) { index ->
                        val ayahNumber = index + 1
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .blindAccessibleClickable(
                                    onClickLabel = "تشغيل من الآية $ayahNumber",
                                    onClick = { onSelectSurah(selectedSurahForAyahs!!.id, index) }
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .blindAccessibleClickable(
                                    onClickLabel = "عرض آيات سورة ${surah.nameArabic}",
                                    onClick = { 
                                        keyboardController?.hide()
                                        selectedSurahForAyahs = surah 
                                    }
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
