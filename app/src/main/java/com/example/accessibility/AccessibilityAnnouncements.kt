package com.example.accessibility

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * ينشر إعلاناً لمستخدمي TalkBack دون الاعتماد على [View.announceForAccessibility]
 * الذي أصبح مهملاً في بعض إصدارات Android.
 *
 * يعمل هذا المساعد عبر إرسال [AccessibilityEvent.TYPE_ANNOUNCEMENT] مباشرةً
 * إلى [AccessibilityManager].
 */
fun announceForAccessibility(context: Context, text: String) {
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return

    if (!accessibilityManager.isEnabled) return

    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT).apply {
        this.text.add(text)
        className = View::class.java.name
        packageName = context.packageName
    }
    accessibilityManager.sendAccessibilityEvent(event)
}
