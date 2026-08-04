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
    role: Role? = null,
    onSingleTap: () -> Unit = {},
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val isTalkBackEnabled = LocalTalkBackEnabled.current

    if (isTalkBackEnabled) {
        // إذا كان TalkBack الخاص بالهاتف مفعلاً، نستخدم clickable القياسي.
        // نظام TalkBack سيحول الضغطة المزدوجة التخيلية إلى onClick مباشرة بشكل سليم.
        this.clickable(
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
    } else {
        // إذا كان TalkBack معطلاً، نستخدم المنطق المخصص لتطبيقنا (المساعد الداخلي).
        // ضغطة واحدة = نطق، ضغطتين = تفعيل.
        this.combinedClickable(
            role = role,
            onClick = onSingleTap,
            onDoubleClick = onClick
        )
    }
}
