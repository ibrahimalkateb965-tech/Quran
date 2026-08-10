package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import com.example.accessibility.LocalTalkBackEnabled
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.accessibility.LocalPendingBlindAction

/**
 * A custom modifier that requires a physical double-tap to activate when TalkBack is OFF,
 * but still correctly responds to TalkBack's virtual click (which is triggered by the user's
 * double-tap gesture while TalkBack is ON).
 *
 * Reads TalkBack state reactively from [LocalTalkBackEnabled] so the UI adapts if TalkBack
 * is toggled while the app is running.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.blindAccessibleClickable(
    onClickLabel: String = "اختيار",
    onLongClickLabel: String? = null,
    role: Role? = null,
    onSingleTap: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val isTalkBackEnabled = LocalTalkBackEnabled.current

    if (isTalkBackEnabled) {
        // إذا كان TalkBack الخاص بالهاتف مفعلاً، نستخدم combinedClickable لدعم onClick و onLongClick
        this.combinedClickable(
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            role = role,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        // إذا كان TalkBack معطلاً، نستخدم المنطق المخصص لتطبيقنا (المساعد الداخلي).
        // ضغطة واحدة = نطق وتخزين الإجراء للـ Global Interceptor
        val pendingBlindAction = LocalPendingBlindAction.current
        val currentOnClick by rememberUpdatedState(onClick)
        
        this.combinedClickable(
            role = role,
            onClick = {
                // منع الصدى الشبحي (Ghost Echo Barrier):
                // إذا تم تنفيذ نقر مزدوج عالمي خلال الـ 500 ملي ثانية الماضية، نتجاهل هذه النقرة تماماً.
                if (System.currentTimeMillis() - GlobalBlindGestureState.lastDoubleTapTime < 500) {
                    return@combinedClickable
                }
                onSingleTap()
                pendingBlindAction.registerAction { currentOnClick() }
            },
            onDoubleClick = {
                currentOnClick()
                pendingBlindAction.clear()
            },
            onLongClick = onLongClick
        )
    }
}
