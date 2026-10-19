package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// لوحة ألوان دلالية واحدة — قيمها تتبدل حسب الوضع الفاتح/الداكن للنظام
@Immutable
data class QuranColors(
    val background: Color,        // خلفية جميع الشاشات
    val cardSurface: Color,       // بطاقة الآية الرئيسية + أزرار الرأس
    val cardFill: Color,          // البطاقات العادية غير المحددة
    val cardActive: Color,        // البطاقة المختارة / النشطة
    val cardBorder: Color,        // إطار البطاقات وكارت الآية
    val cardActiveBorder: Color,  // إطار البطاقات المختارة
    val textPrimary: Color,       // النصوص والعناوين الرئيسية
    val textSecondary: Color,     // النصوص الفرعية والمعلومات
    val textOnFill: Color,        // النصوص والأيقونات فوق cardActive / accentContainer / cardBorder
    val textAyah: Color,          // نص الآية القرآني
    val accent: Color,            // نص/أيقونة مميزة (اسم السورة، رقم الآية)
    val accentBright: Color,      // تمييز ثانوي ("مفتوحة الآن"، إطار الزر النشط)
    val accentContainer: Color,   // تعبئة الزر النشط — يحمل textOnFill
    val highlight: Color,         // الذهبي لعلامات التحديد
    val success: Color,
    val error: Color,
    val isDark: Boolean
)

// Light — neutral #F8F9FA؛ وضع أحادي عالي التباين (كتابة سوداء ناصعة حصراً دون أي ألوان بنية)
val LightQuranColors = QuranColors(
    background = Color(0xFFF8F9FA),
    cardSurface = Color(0xFFF8F9FA),
    cardFill = Color(0xFFF8F9FA),
    cardActive = Color(0xFFE5E5E5),
    cardBorder = Color(0xFF333333),
    cardActiveBorder = Color(0xFF000000),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF000000),
    textOnFill = Color(0xFFFFFFFF),
    textAyah = Color(0xFF000000),
    accent = Color(0xFF000000),
    accentBright = Color(0xFF000000),
    accentContainer = Color(0xFF000000),
    highlight = Color(0xFF000000),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC2626),
    isDark = false
)

// Dark — OLED true black؛ وضع أحادي عالي التباين (أبيض وأسود حصراً دون أي ألوان أخرى)
val DarkQuranColors = QuranColors(
    background = Color(0xFF000000),
    cardSurface = Color(0xFF000000),
    cardFill = Color(0xFF000000),
    cardActive = Color(0xFF222222),
    cardBorder = Color(0xFF555555),
    cardActiveBorder = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFFFFFFF),
    textOnFill = Color(0xFFFFFFFF),
    textAyah = Color(0xFFFFFFFF),
    accent = Color(0xFFFFFFFF),
    accentBright = Color(0xFFFFFFFF),
    accentContainer = Color(0xFF2A2A2A),
    highlight = Color(0xFFFFFFFF),
    success = Color(0xFFFFFFFF),
    error = Color(0xFFFFFFFF),
    isDark = true
)

// Legacy names — now theme-aware getters so existing call sites follow dark/light without edits.
// Migrate call sites to QuranTheme.colors.* and delete this block.
val WarmEarthBg: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.background
val WarmCardLight: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.cardFill
val WarmCardActive: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.cardActive
val WarmCardBorder: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.cardBorder
val WarmCardActiveBorder: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.cardActiveBorder
val WarmTextPrimary: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.textPrimary
val WarmTextSecondary: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.textSecondary
val WarmTextLight: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.textOnFill
val WarmTextAyah: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.textAyah
val WarmAccentTerracotta: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.accent
val WarmAccentTerracottaBright: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.accentBright
val WarmAccentGold: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.highlight

val DarkImmersiveBg: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.background
val DarkImmersiveCard: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.cardFill
val DarkImmersiveSurface: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.cardActive
val DarkImmersiveBorder: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.cardBorder

val AccessibleGold: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.accent
val AccessibleGoldVariant: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.accentBright
val AccessibleGreenAccent: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.success
val AccessibleRedAlert: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.error

val TextPrimaryWhite: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.textOnFill
val TextSecondaryGold: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.highlight
val TextMutedZinc: Color @Composable @ReadOnlyComposable get() = QuranTheme.colors.textSecondary
