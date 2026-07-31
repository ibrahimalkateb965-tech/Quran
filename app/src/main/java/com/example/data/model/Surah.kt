package com.example.data.model

data class Surah(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val translationArabic: String,
    val ayahCount: Int,
    val revelationType: String, // "مكية" or "مدنية"
    val startPage: Int = 1
)
