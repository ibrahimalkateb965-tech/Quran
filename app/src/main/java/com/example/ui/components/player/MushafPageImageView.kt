package com.example.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.SurahData
import com.example.ui.components.BlindAccessibleButton
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmAccentTerracottaBright
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmCardActiveBorder
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmEarthBg
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

/**
 * High-definition viewer for authentic Madinah Mushaf (King Fahd Complex) page images.
 * Provides full TalkBack accessibility, offline caching via Coil, and an interactive ayah jumper.
 */
@Composable
fun MushafPageImageView(
    pageNumber: Int,
    ayahs: List<Ayah>,
    currentSurahId: Int?,
    currentAyahNumberInSurah: Int?,
    onSelectAyah: (surahId: Int, ayahIndex: Int) -> Unit,
    onAnnounce: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useFallbackCdn by remember(pageNumber) { mutableStateOf(false) }

    // Official Quran.app BunnyCDN (width_1260) with GitHub raw fallback
    val formattedPage = "%03d".format(pageNumber)
    val primaryImageUrl = "https://files.quran.app/hafs/madani/width_1260/page$formattedPage.png"
    val fallbackImageUrl = "https://raw.githubusercontent.com/GovarJabbar/Quran-PNG/master/$formattedPage.png"
    val currentImageUrl = if (useFallbackCdn) fallbackImageUrl else primaryImageUrl

    val imageRequest = remember(currentImageUrl) {
        ImageRequest.Builder(context)
            .data(currentImageUrl)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val pageAyahsDescription = remember(ayahs, pageNumber) {
        if (ayahs.isNotEmpty()) {
            "صورة صفحة مصحف المدينة، صفحة $pageNumber، تحتوي على ${ayahs.size} آيات"
        } else {
            "صورة صفحة مصحف المدينة، صفحة $pageNumber"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("mushaf_page_image_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Mushaf Image Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFFDF7)) // Traditional creamy Mushaf paper tone
                .border(1.5.dp, WarmCardBorder, RoundedCornerShape(12.dp))
                .semantics(mergeDescendants = true) {
                    this.contentDescription = pageAyahsDescription
                },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mushaf_page_image"),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = WarmAccentTerracotta,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "جارٍ تحميل صورة صفحة $pageNumber...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextSecondary
                            )
                        }
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = WarmAccentTerracotta,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "تعذر تحميل صورة الصفحة عبر الإنترنت",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = WarmTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "تأكد من اتصال الإنترنت لتحميل صفحة المصحف وتخزينها محلياً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (!useFallbackCdn) {
                                BlindAccessibleButton(
                                    onClick = {
                                        useFallbackCdn = true
                                        onAnnounce("جارٍ محاولة التحميل من الخادم البديل")
                                    },
                                    onClickLabel = "إعادة المحاولة عبر الخادم البديل"
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(text = "إعادة المحاولة عبر الخادم البديل")
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // Quick Ayah Navigation Strip (الشريط السريع لآيات الصفحة)
        if (ayahs.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "آيات هذه الصفحة (${ayahs.size} آية) — اضغط للتلاوة:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = WarmTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                val hasMultipleSurahs = remember(ayahs) {
                    ayahs.map { it.surahId }.distinct().size > 1
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ayahs.forEach { ayah ->
                        val surah = remember(ayah.surahId) {
                            SurahData.SURAH_LIST.find { it.id == ayah.surahId }
                        }
                        val isActive = ayah.surahId == currentSurahId && ayah.numberInSurah == currentAyahNumberInSurah
                        val chipBg = if (isActive) WarmCardActive else WarmCardLight
                        val chipBorder = if (isActive) WarmCardActiveBorder else WarmCardBorder
                        val chipText = if (isActive) WarmAccentTerracottaBright else WarmTextPrimary
                        val chipLabel = if (hasMultipleSurahs && surah != null) {
                            "${surah.nameArabic} ${ayah.numberInSurah}"
                        } else {
                            "آية ${ayah.numberInSurah}"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    onSelectAyah(ayah.surahId, ayah.numberInSurah - 1)
                                    onAnnounce("تم الانتقال إلى سورة ${surah?.nameArabic ?: ""} الآية ${ayah.numberInSurah}")
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .semantics {
                                    this.role = Role.Button
                                    this.contentDescription = "الآية ${ayah.numberInSurah} من سورة ${surah?.nameArabic ?: ""}. ${ayah.textArabic}"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = chipText
                            )
                        }
                    }
                }
            }
        }
    }
}
