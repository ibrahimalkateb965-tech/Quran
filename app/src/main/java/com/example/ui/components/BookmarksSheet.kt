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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary

@Composable
fun BookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    onSelectBookmark: (Int, Int) -> Unit, // surahId, ayahIndex
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    AccessibleBottomSheet(
        title = "الإشارات المرجعية",
        contentDescriptionText = "الإشارات المرجعية. لديك ${bookmarks.size} إشارة.",
        onDismiss = onDismiss,
        onAnnounce = onAnnounce,
        showCloseButton = false
    ) {
        if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "لا توجد إشارات مرجعية محفوظة.",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmTextPrimary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookmarks, key = { "${it.surahId}_${it.ayahNumber}" }) { bookmark ->
                    AccessibleListItemCard(
                        title = "سورة ${bookmark.surahNameAr}",
                        subtitle = "الآية ${bookmark.ayahNumber}",
                        onClickLabel = "الانتقال إلى سورة ${bookmark.surahNameAr} الآية ${bookmark.ayahNumber}",
                        contentDescriptionText = "سورة ${bookmark.surahNameAr}، الآية ${bookmark.ayahNumber}. انقر مرتين للانتقال إليها.",
                        onClick = { onSelectBookmark(bookmark.surahId, bookmark.ayahNumber - 1) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .width(44.dp)
                                    .background(WarmCardActive, shape = RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${bookmark.surahId}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmTextLight
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
