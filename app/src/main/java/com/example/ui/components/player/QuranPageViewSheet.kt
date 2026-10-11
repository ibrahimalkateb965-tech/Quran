package com.example.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import com.aistudio.quranblind.domain.model.SurahData
import com.example.ui.components.AccessibleBottomSheet
import com.example.ui.components.BlindAccessibleButton
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.components.SurahIndexSheet
import com.example.ui.theme.UthmanTahaFont
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmAccentTerracottaBright
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmCardActiveBorder
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmEarthBg
import com.example.ui.theme.WarmTextAyah
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

@Composable
fun QuranPageViewSheet(
    pageNumber: Int,
    currentSurahId: Int?,
    currentAyahNumberInSurah: Int?,
    ayahs: List<Ayah>,
    onSelectAyah: (surahId: Int, ayahIndex: Int) -> Unit,
    onChangePage: (Int) -> Unit,
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit,
    onSelectAyahFromSurahPicker: ((surahId: Int, ayahIndex: Int) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val pageAyahs = remember(ayahs, pageNumber) {
        if (ayahs.isNotEmpty() && (ayahs.first().page == pageNumber || ayahs.first().page == 0)) {
            ayahs
        } else {
            emptyList()
        }
    }
    val juzNumber = pageAyahs.firstOrNull()?.juz ?: ayahs.firstOrNull()?.juz ?: 1
    var isImageViewMode by remember { mutableStateOf(false) }
    var showSurahPicker by remember { mutableStateOf(false) }

    LaunchedEffect(pageNumber, pageAyahs.isNotEmpty()) {
        if (pageAyahs.isNotEmpty()) {
            val juz = pageAyahs.first().juz
            onAnnounce("صفحة $pageNumber في المصحف الشريف. الجزء $juz")
        }
    }

    // Scroll to the active ayah when list loads
    LaunchedEffect(pageAyahs, currentSurahId, currentAyahNumberInSurah) {
        val targetIndex = pageAyahs.indexOfFirst {
            it.surahId == currentSurahId && it.numberInSurah == currentAyahNumberInSurah
        }
        if (targetIndex != -1) {
            val safeIndex = maxOf(0, targetIndex - 1)
            listState.animateScrollToItem(safeIndex)
        }
    }

    AccessibleBottomSheet(
        title = "صفحة $pageNumber",
        contentDescriptionText = "صفحة $pageNumber في المصحف الشريف. الجزء $juzNumber. تحتوي على ${pageAyahs.size} آيات",
        onDismiss = onDismiss,
        onAnnounce = onAnnounce,
        headerContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous page button (صفحة سابقة)
                    BlindAccessibleIconButton(
                        onClick = {
                            if (pageNumber > 1) {
                                onChangePage(pageNumber - 1)
                            }
                        },
                        onClickLabel = "الانتقال إلى الصفحة السابقة",
                        modifier = Modifier
                            .size(44.dp)
                            .background(WarmCardLight, CircleShape)
                            .semantics(mergeDescendants = true) {
                                this.contentDescription = if (pageNumber > 1) "الصفحة السابقة، صفحة ${pageNumber - 1}" else "بداية المصحف"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (pageNumber > 1) WarmAccentTerracotta else WarmTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Page and Juz Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "صفحة $pageNumber",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = WarmAccentTerracotta
                        )
                        Text(
                            text = "الجزء $juzNumber",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = WarmTextSecondary
                        )
                    }

                    // Next page button (صفحة تالية)
                    BlindAccessibleIconButton(
                        onClick = {
                            if (pageNumber < 604) {
                                onChangePage(pageNumber + 1)
                            }
                        },
                        onClickLabel = "الانتقال إلى الصفحة التالية",
                        modifier = Modifier
                            .size(44.dp)
                            .background(WarmCardLight, CircleShape)
                            .semantics(mergeDescendants = true) {
                                this.contentDescription = if (pageNumber < 604) "الصفحة التالية، صفحة ${pageNumber + 1}" else "نهاية المصحف"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = if (pageNumber < 604) WarmAccentTerracotta else WarmTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Dual-Mode Display Switcher: [ 📖 مصحف المدينة المصور | 📜 نص الآيات ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                        .background(WarmCardLight, RoundedCornerShape(24.dp))
                        .border(1.dp, WarmCardBorder, RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option B: Mushaf Image Mode (Default)
                    val imageTabActive = isImageViewMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (imageTabActive) WarmCardActive else Color.Transparent)
                            .border(
                                width = if (imageTabActive) 1.dp else 0.dp,
                                color = if (imageTabActive) WarmCardActiveBorder else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                isImageViewMode = true
                                onAnnounce("تم تفعيل وضع صورة مصحف المدينة")
                            }
                            .padding(vertical = 6.dp)
                            .testTag("mushaf_image_tab")
                            .semantics(mergeDescendants = true) {
                                this.role = Role.Tab
                                this.contentDescription = "عرض صورة صفحة مصحف المدينة المصورة"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📖 مصحف المدينة المصور",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (imageTabActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (imageTabActive) WarmAccentTerracottaBright else WarmTextSecondary
                        )
                    }

                    // Option A: Uthmani Text Mode
                    val textTabActive = !isImageViewMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (textTabActive) WarmCardActive else Color.Transparent)
                            .border(
                                width = if (textTabActive) 1.dp else 0.dp,
                                color = if (textTabActive) WarmCardActiveBorder else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                isImageViewMode = false
                                onAnnounce("تم تفعيل وضع النص العثماني للآيات")
                            }
                            .padding(vertical = 6.dp)
                            .testTag("uthmani_text_tab")
                            .semantics(mergeDescendants = true) {
                                this.role = Role.Tab
                                this.contentDescription = "عرض نص الآيات العثماني"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📜 نص الآيات",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (textTabActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (textTabActive) WarmAccentTerracottaBright else WarmTextSecondary
                        )
                    }
                }
            }
        },
        showCloseButton = false
    ) {
        if (pageAyahs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WarmAccentTerracotta)
            }
        } else if (isImageViewMode) {
            // Option B: High-res authentic Madinah Mushaf Page Image
            MushafPageImageView(
                pageNumber = pageNumber,
                ayahs = pageAyahs,
                currentSurahId = currentSurahId,
                currentAyahNumberInSurah = currentAyahNumberInSurah,
                onSelectAyah = onSelectAyah,
                onAnnounce = onAnnounce,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Option A: Scrollable Uthmani text layout with Ayah Number Search
            var ayahSearchQuery by remember(pageNumber) { mutableStateOf("") }
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(ayahSearchQuery) {
                if (listState.firstVisibleItemIndex > 0) {
                    listState.scrollToItem(0)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search & Surah Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search by Ayah Number Input Bar
                    OutlinedTextField(
                        value = ayahSearchQuery,
                        onValueChange = { input ->
                            val normalized = normalizeArabicDigits(input)
                            ayahSearchQuery = normalized
                            if (normalized.isNotEmpty()) {
                                val effectiveQuery = normalized.trimStart('0').ifEmpty { "0" }
                                val count = pageAyahs.count { it.numberInSurah.toString().contains(effectiveQuery) }
                                if (count == 0) {
                                    onAnnounce("لا توجد آية برقم $normalized في هذه الصفحة")
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                text = "البحث برقم الآية...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = WarmAccentTerracotta
                            )
                        },
                        trailingIcon = {
                            if (ayahSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        ayahSearchQuery = ""
                                        onAnnounce("تم مسح البحث، عرض جميع آيات الصفحة")
                                    },
                                    modifier = Modifier.semantics {
                                        this.contentDescription = "مسح البحث برقم الآية"
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = WarmTextSecondary
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                val queryNum = ayahSearchQuery.trim()
                                val effectiveQuery = queryNum.trimStart('0').ifEmpty { "0" }
                                val count = pageAyahs.count { it.numberInSurah.toString().contains(effectiveQuery) }
                                if (count == 0) {
                                    onAnnounce("لا توجد آية برقم $queryNum في هذه الصفحة")
                                } else {
                                    onAnnounce("تم العثور على $count آيات برقم $queryNum في هذه الصفحة")
                                }
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmAccentTerracotta,
                            unfocusedBorderColor = WarmCardBorder,
                            focusedContainerColor = WarmCardLight,
                            unfocusedContainerColor = WarmCardLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ayah_search_text_field")
                            .semantics {
                                this.contentDescription = "حقل البحث برقم الآية في صفحة $pageNumber"
                            }
                    )

                    // Surah Picker Button
                    BlindAccessibleButton(
                        onClick = { showSurahPicker = true },
                        onClickLabel = "اختيارُ السورةِ والانتقالُ لصفحاتِها",
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .widthIn(min = 60.dp)
                            .border(1.dp, WarmCardBorder, RoundedCornerShape(16.dp))
                            .testTag("page_sheet_surah_picker_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmCardLight,
                            contentColor = WarmAccentTerracotta
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "السور",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = WarmAccentTerracotta
                            )
                        }
                    }
                }

                val displayedAyahs = remember(pageAyahs, ayahSearchQuery) {
                    if (ayahSearchQuery.isBlank()) {
                        pageAyahs
                    } else {
                        val queryNum = ayahSearchQuery.trim()
                        val effectiveQuery = queryNum.trimStart('0').ifEmpty { "0" }
                        pageAyahs.filter { it.numberInSurah.toString().contains(effectiveQuery) }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("quran_page_view_list")
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (displayedAyahs.isEmpty()) {
                        item(key = "empty_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp)
                                    .testTag("ayah_search_empty_state"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = WarmTextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "لا توجد آية برقم «$ayahSearchQuery» في هذه الصفحة",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = WarmTextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "تأكد من رقم الآية المطلوب أو اضغط على مسح البحث",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    BlindAccessibleButton(
                                        onClick = {
                                            ayahSearchQuery = ""
                                            onAnnounce("تم مسح البحث، عرض جميع آيات الصفحة")
                                        },
                                        onClickLabel = "مسح البحث وعرض جميع آيات الصفحة",
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "مسح البحث",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(displayedAyahs, key = { _, ayah -> "${ayah.surahId}_${ayah.numberInSurah}" }) { index, ayah ->
                            val surah = remember(ayah.surahId) {
                                SurahData.SURAH_LIST.find { it.id == ayah.surahId }
                            }
                            val isFirstAyahInSurah = ayah.numberInSurah == 1
                            val isPreviousDifferentSurah = index > 0 && displayedAyahs[index - 1].surahId != ayah.surahId
                            val isSurahStart = isFirstAyahInSurah || isPreviousDifferentSurah

                            val isActive = ayah.surahId == currentSurahId && ayah.numberInSurah == currentAyahNumberInSurah

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Display Surah Header Banner if a Surah starts on this page
                                if (isSurahStart && surah != null) {
                                    SurahPageHeaderBanner(surah = surah)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Display Basmalah (except for Surat Al-Fatihah ID = 1 and Surat At-Tawbah ID = 9)
                                    if (ayah.surahId != 9 && ayah.surahId != 1 && isFirstAyahInSurah) {
                                        BasmalahBanner()
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                // Ayah Item Card
                                PageAyahItemCard(
                                    ayah = ayah,
                                    surahName = surah?.nameArabic ?: "",
                                    isActive = isActive,
                                    onClick = {
                                        onSelectAyah(ayah.surahId, ayah.numberInSurah - 1)
                                        onAnnounce("تم الانتقال إلى سورة ${surah?.nameArabic ?: ""} الآية ${ayah.numberInSurah}")
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSurahPicker) {
        SurahIndexSheet(
            surahs = SurahData.SURAH_LIST,
            currentSurahId = currentSurahId,
            currentAyahIndex = 0,
            onSelectSurah = { selectedSurahId, selectedAyahIndex ->
                showSurahPicker = false
                if (onSelectAyahFromSurahPicker != null) {
                    onSelectAyahFromSurahPicker(selectedSurahId, selectedAyahIndex)
                } else {
                    val targetSurah = SurahData.SURAH_LIST.find { it.id == selectedSurahId }
                    val targetPage = targetSurah?.startPage ?: 1
                    onChangePage(targetPage)
                    onSelectAyah(selectedSurahId, selectedAyahIndex)
                    onAnnounce("تم الانتقالُ إلى سورةِ ${targetSurah?.nameArabic ?: ""}، صفحة $targetPage")
                }
            },
            onDismiss = { showSurahPicker = false },
            onAnnounce = onAnnounce
        )
    }
}

@Composable
fun SurahPageHeaderBanner(
    surah: Surah
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCardLight),
        border = BorderStroke(1.5.dp, WarmAccentTerracotta)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "سورة ${surah.nameArabic}",
                fontFamily = UthmanTahaFont,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = WarmAccentTerracotta,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${surah.revelationType} • عدد آياتها ${surah.ayahCount}",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BasmalahBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            fontFamily = UthmanTahaFont,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 36.sp
            ),
            color = WarmTextAyah,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PageAyahItemCard(
    ayah: Ayah,
    surahName: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) WarmCardActive else WarmCardLight
    val borderColor = if (isActive) WarmCardActiveBorder else WarmCardBorder
    val elevation = if (isActive) 3.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.role = Role.Button
                this.contentDescription = buildString {
                    append("الآية ${ayah.numberInSurah} من سورة $surahName. ")
                    append(ayah.textArabic)
                    if (isActive) append(". الآية المحددة حالياً")
                }
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header within the item: Ayah number & Active indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isActive) WarmAccentTerracotta else WarmCardBorder.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "الآية ${ayah.numberInSurah}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isActive) WarmTextLight else WarmTextPrimary
                    )
                }

                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = WarmAccentTerracotta,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الآية الحالية",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = WarmAccentTerracotta
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ayah Quranic Text
            Text(
                text = "${ayah.textArabic}  ﴿${ayah.numberInSurah}﴾",
                fontFamily = UthmanTahaFont,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    lineHeight = 52.sp
                ),
                color = WarmTextAyah,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun normalizeArabicDigits(input: String): String {
    return input.map { ch ->
        when (ch) {
            in '٠'..'٩' -> ('0' + (ch - '٠'))
            in '۰'..'۹' -> ('0' + (ch - '۰'))
            else -> ch
        }
    }.filter { it in '0'..'9' }.joinToString("")
}
