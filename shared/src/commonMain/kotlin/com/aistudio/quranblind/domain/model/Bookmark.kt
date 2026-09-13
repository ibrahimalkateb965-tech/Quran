package com.aistudio.quranblind.domain.model

@kotlinx.serialization.Serializable
data class Bookmark(
    val surahId: Int,
    val ayahNumber: Int,
    val surahNameAr: String,
    val createdAtEpochMillis: Long,
    val note: String = "",
)
