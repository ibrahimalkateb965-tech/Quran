package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardActiveBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

@Composable
fun AccessibleListItemCard(
    title: String,
    onClickLabel: String,
    contentDescriptionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
    isSelected: Boolean = false,
    cardHeight: Dp = 72.dp,
    testTag: String? = null,
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    val resolvedTitleColor = titleColor ?: if (isSelected) WarmTextPrimary else WarmTextPrimary
    val resolvedSubtitleColor = subtitleColor ?: if (isSelected) WarmTextSecondary else WarmTextSecondary
    val containerColor = if (isSelected) WarmCardActive else WarmCardLight
    val borderColor = if (isSelected) WarmCardActiveBorder else WarmCardBorder
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .blindAccessibleClickable(
                onClickLabel = onClickLabel,
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDescriptionText
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = resolvedTitleColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = resolvedSubtitleColor
                        )
                    }
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
