package com.aistudio.quranblind.domain.text

private const val BARE_NOON_NEXT_LETTERS = "[يرملونصذثكجشقسدطزفتضظب]"
private val noonSukoonPattern = Regex("(ن)[\\u0652\\u06DF\\u06E0\\u06E1](?=\\s*$BARE_NOON_NEXT_LETTERS)")

fun sanitizeUthmanicText(text: String): String =
    text.replace(noonSukoonPattern, "$1")
        .replace('\u06DF', '\u06E0')
        .replace('\u06E4', '\u0653')
        .replace("\u0600", "")
        .replace("\u06DD", "")
        .replace("\uFEFF", "")
        .replace("\u200A", "")
        .replace("\u2060", "")
