package com.aistudio.quranblind.data

/** Returns the full quran_uthmani_tanzil.json text; the platform decides where it lives. */
fun interface QuranJsonSource {
    fun readQuranJson(): String
}
