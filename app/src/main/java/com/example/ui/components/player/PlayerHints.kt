package com.example.ui.components.player

/**
 * نصوص تلميحات الإيماءات لشاشة المشغل — مكيّفة حسب وضع TalkBack.
 * تفريع سلوك (نصوص) وليس تفريع تصميم — وفق .agents/DUAL_MODE_ARCHITECTURE.md
 */
data class PlayerGestureHints(
    val playPauseHint: String,
    val navigationHint: String
)

fun playerGestureHints(isTalkBackEnabled: Boolean): PlayerGestureHints =
    if (isTalkBackEnabled) {
        PlayerGestureHints(
            playPauseHint = "نقرتان: تشغيل/إيقاف",
            navigationHint = "سحب بإصبعين: آية آية"
        )
    } else {
        PlayerGestureHints(
            playPauseHint = "نقرتين: تشغيل/إيقاف",
            navigationHint = "سحب أفقي: آية آية"
        )
    }

fun buildPlayerStatusDescription(
    surahName: String?,
    ayahNumber: Int,
    ayahCount: Int?,
    isPlaying: Boolean,
    isRepeatModeActive: Boolean,
    isTalkBackEnabled: Boolean
): String = buildString {
    append("تطبيق القرآن الكريم للمكفوفين. ")
    if (surahName != null && ayahCount != null) {
        append("سورة $surahName، الآية $ayahNumber من أصل $ayahCount. ")
    }
    append(if (isPlaying) "جاري التشغيل. " else "متوقف مؤقتاً. ")
    if (isRepeatModeActive) append("وضع التكرار مفعّل. ")
    if (isTalkBackEnabled) {
        append("انقر مرتين للتشغيل أو الإيقاف. اسحب بإصبعين يميناً أو يساراً للتنقل بين الآيات.")
    } else {
        append("انقر مرتين للتشغيل أو الإيقاف. اسحب يميناً ويساراً للتنقل.")
    }
}
