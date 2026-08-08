package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Ayah

@Entity(tableName = "ayahs")
data class AyahEntity(
    @PrimaryKey
    val globalNumber: Int,
    val numberInSurah: Int,
    val textArabic: String,
    val textTranslation: String = "",
    val audioUrl: String = "",
    val surahId: Int,
    val page: Int = 1,
    val juz: Int = 1,
    val reciterIdentifier: String = ""
) {
    fun toDomainModel(): Ayah {
        return Ayah(
            numberInSurah = numberInSurah,
            globalNumber = globalNumber,
            textArabic = textArabic,
            textTranslation = textTranslation,
            audioUrl = audioUrl,
            surahId = surahId,
            page = page,
            juz = juz
        )
    }
}
