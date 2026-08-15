package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Earth Palette (النمط الدافئ البيج الترابي)
val WarmEarthBg = Color(0xFFB38A5F)           // الخلفية الأساسية لجميع الشاشات
val WarmCardLight = Color(0xFFC59E75)         // البطاقات العادية غير المحددة
val WarmCardActive = Color(0xFF201610)        // البطاقة المختارة / النشطة
val WarmCardBorder = Color(0xFF8A653F)        // إطار البطاقات وكارت الآية
val WarmTextPrimary = Color(0xFF1A120B)       // النصوص والعناوين الرئيسية
val WarmTextSecondary = Color(0xFF4E3929)     // النصوص الفرعية والمعلومات
val WarmTextLight = Color(0xFFF5EBE1)         // النصوص والأيقونات فوق البطاقات الداكنة
val WarmTextAyah = Color(0xFF120C07)          // نص الآية القرآني (أسود فحمي مائل للبني)
val WarmAccentTerracotta = Color(0xFF7C261E)  // الأحمر الطوبي الداكن (اسم السورة، رقم الآية، التمييز)
val WarmAccentTerracottaBright = Color(0xFFC85A48) // الأحمر الطوبي الدافئ ("مفتوحة الآن")
val WarmAccentGold = Color(0xFFE5A93C)        // الذهبي الدافئ لعلامات التحديد والتمييز

// Legacy Aliases for seamless integration
val DarkImmersiveBg = WarmEarthBg
val DarkImmersiveCard = WarmCardLight
val DarkImmersiveSurface = WarmCardActive
val DarkImmersiveBorder = WarmCardBorder

val AccessibleGold = WarmAccentTerracotta
val AccessibleGoldVariant = WarmAccentTerracottaBright
val AccessibleGreenAccent = Color(0xFF22C55E)
val AccessibleRedAlert = Color(0xFFEF4444)

val TextPrimaryWhite = WarmTextLight
val TextSecondaryGold = WarmAccentGold
val TextMutedZinc = WarmTextSecondary

