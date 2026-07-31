package com.example.data.model

data class Ayah(
    val numberInSurah: Int,
    val globalNumber: Int,
    val textArabic: String,
    val textTranslation: String = "",
    val audioUrl: String = "",
    val surahId: Int,
    val page: Int = 1,
    val juz: Int = 1
)
