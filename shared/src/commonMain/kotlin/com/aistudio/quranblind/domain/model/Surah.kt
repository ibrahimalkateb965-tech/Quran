package com.aistudio.quranblind.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Surah(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val translationArabic: String,
    val ayahCount: Int,
    val revelationType: String,
    val startPage: Int = 1
)
