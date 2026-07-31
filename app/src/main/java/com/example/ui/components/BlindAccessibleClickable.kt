package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

/**
 * A custom modifier that requires a physical double-tap to activate when TalkBack is OFF,
 * but still correctly responds to TalkBack's virtual click (which is triggered by the user's
 * double-tap gesture while TalkBack is ON).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.blindAccessibleClickable(
    onClickLabel: String = "اختيار",
    role: Role? = null,
    onSingleTap: () -> Unit = {},
    onClick: () -> Unit
): Modifier {
    return this
        .combinedClickable(
            role = role,
            onClick = { 
                // When TalkBack is OFF, physical single tap lands here.
                onSingleTap() 
            },
            onDoubleClick = { 
                // When TalkBack is OFF, physical double tap lands here.
                onClick() 
            }
        )
        // Override the semantics so TalkBack's ACTION_CLICK maps to the actual action,
        // ignoring the empty or single-tap-only onClick above.
        .semantics(mergeDescendants = true) {
            onClick(label = onClickLabel, action = {
                onClick()
                true
            })
        }
}
