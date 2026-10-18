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

// Light — neutral #F8F9FA
val LightQuranColors = QuranColors(
    background = Color(0xFFF8F9FA),
    cardSurface = Color(0xFFF8F9FA),
    cardFill = Color(0xFFF8F9FA),
    cardActive = Color(0xFFA27448),
    cardBorder = Color(0xFF8A653F),
    cardActiveBorder = Color(0xFF6B4A2D),
    textPrimary = Color(0xFF1A120B),
    textSecondary = Color(0xFF4E3929),
    textOnFill = Color(0xFFF5EBE1),
    textAyah = Color(0xFF120C07),
    accent = Color(0xFF7C261E),
    accentBright = Color(0xFFC85A48),
    accentContainer = Color(0xFF7C261E),
    highlight = Color(0xFFE5A93C),
    success = Color(0xFF22C55E),
    error = Color(0xFFEF4444),
    isDark = false
)

// Dark — OLED true black؛ كل زوج نص/خلفية ≥ 4.5:1 والإطارات ≥ 3:1 على الأسود
val DarkQuranColors = QuranColors(
    background = Color(0xFF000000),
    cardSurface = Color(0xFF000000),
    cardFill = Color(0xFF000000),
    cardActive = Color(0xFF4A2E1B),
    cardBorder = Color(0xFF6E5A48),
    cardActiveBorder = Color(0xFFD9A877),
    textPrimary = Color(0xFFF2EDE8),
    textSecondary = Color(0xFFBFB3A6),
    textOnFill = Color(0xFFF5EBE1),
    textAyah = Color(0xFFEDE6DC),
    accent = Color(0xFFE8836F),
    accentBright = Color(0xFFF4A592),
    accentContainer = Color(0xFF8E3426),
    highlight = Color(0xFFF2C063),
    success = Color(0xFF4ADE80),
    error = Color(0xFFF87171),
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
