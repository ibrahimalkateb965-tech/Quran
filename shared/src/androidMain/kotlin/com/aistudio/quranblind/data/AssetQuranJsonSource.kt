package com.aistudio.quranblind.data

import android.content.Context

class AssetQuranJsonSource(
    private val context: Context,
    private val assetPath: String = "quran/quran_uthmani_tanzil.json"
) : QuranJsonSource {
    override fun readQuranJson(): String {
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }
}
