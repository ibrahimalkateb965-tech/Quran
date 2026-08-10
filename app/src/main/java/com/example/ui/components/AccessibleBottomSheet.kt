package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.DarkImmersiveBg

@Composable
fun AccessibleBottomSheet(
    title: String,
    contentDescriptionText: String,
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit,
    dismissLabel: String = "إغلاق النافذة",
    headerContent: (@Composable () -> Unit)? = null,
    showCloseButton: Boolean = true,
    content: @Composable () -> Unit
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnAnnounce by rememberUpdatedState(onAnnounce)

    BackHandler {
        currentOnDismiss()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                paneTitle = title
                customActions = listOf(
                    CustomAccessibilityAction(label = dismissLabel) {
                        currentOnDismiss()
                        true
                    }
                )
            },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = AccessibleGold,
                        modifier = Modifier.semantics {
                            contentDescription = contentDescriptionText
                            heading()
                        }
                    )
                }

                if (showCloseButton) {
                    BlindAccessibleIconButton(
                        onClick = currentOnDismiss,
                        onClickLabel = dismissLabel,
                        modifier = Modifier
                            .height(48.dp)
                            .width(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = dismissLabel,
                            tint = AccessibleGold
                        )
                    }
                }
            }
            
            if (headerContent != null) {
                headerContent()
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
        }
    }
}
