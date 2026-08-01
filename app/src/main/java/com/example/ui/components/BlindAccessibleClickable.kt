package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import android.view.accessibility.AccessibilityManager
import android.content.Context
import androidx.compose.foundation.clickable

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
): Modifier = composed {
    val context = LocalContext.current
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val isTalkBackEnabled = am.isTouchExplorationEnabled

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
