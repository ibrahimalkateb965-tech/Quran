package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A custom accessible Icon Button for blind users.
 * Requires double tap to activate, and single tap to announce its action.
 */
@Composable
fun BlindAccessibleIconButton(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier.size(48.dp),
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .blindAccessibleClickable(
                onClickLabel = onClickLabel,
                onClick = { if (enabled) onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * A custom accessible regular Button for blind users.
 * Requires double tap to activate, and single tap to announce its action.
 */
@Composable
fun BlindAccessibleButton(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.extraLarge,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    // Note: We use a Box wrapper because standard Button handles its own clicks.
    // To suppress standard clicks and use our custom double-tap logic, 
    // we use a Box styled to look like a Button, or we wrap a non-clickable Surface.
    
    // Using Material3 Button but intercepting pointer events is tricky.
    // Instead, we will build a visual equivalent or disable standard clicks.
    
    // Simplest approach: Use the standard Button for visuals, but override interactions via Box.
    Box(
        modifier = modifier
            .clip(shape) // standard button shape
            .blindAccessibleClickable(
                onClickLabel = onClickLabel,
                onClick = { if (enabled) onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Render the actual button but make it technically disabled (or ignore pointer events) 
        // so our Box modifier handles the touches. Wait, if it's disabled, colors change.
        
        androidx.compose.material3.Surface(
            color = colors.containerColor,
            contentColor = colors.contentColor,
            shape = shape,
            modifier = Modifier.matchParentSize()
        ) {}
        
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
