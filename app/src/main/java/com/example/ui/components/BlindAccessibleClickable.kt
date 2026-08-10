package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

import androidx.compose.ui.composed
import com.example.accessibility.LocalTalkBackEnabled

/**
 * A custom modifier that adapts its behavior based on TalkBack state.
 *
 * Reads TalkBack state reactively from [LocalTalkBackEnabled] so the UI adapts if TalkBack
 * is toggled while the app is running.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.blindAccessibleClickable(
    onClickLabel: String = "اختيار",
    onLongClickLabel: String? = null,
    role: Role? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val isTalkBackEnabled = LocalTalkBackEnabled.current

    if (isTalkBackEnabled) {
        // إذا كان TalkBack الخاص بالهاتف مفعلاً
        this.combinedClickable(
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            role = role,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        // وضع المبصرين: نقرة قياسية عادية
        this.combinedClickable(
            role = role,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}
