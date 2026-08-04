package com.example.accessibility

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal يحمل حالة TalkBack الحالية.
 *
 * يُستخدم داخل Modifiers المخصصة (مثل blindAccessibleClickable) لضبط السلوك
 * بدون الحاجة لتمرير القيمة يدوياً عبر كل مستوى من التركيب.
 */
val LocalTalkBackEnabled = compositionLocalOf { false }
