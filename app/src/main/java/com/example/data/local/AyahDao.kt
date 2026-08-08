package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AyahDao {
    @Query("SELECT * FROM ayahs WHERE surahId = :surahId AND reciterIdentifier = :reciterIdentifier ORDER BY numberInSurah ASC")
    fun getAyahsForSurah(surahId: Int, reciterIdentifier: String): List<AyahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("SELECT COUNT(*) FROM ayahs WHERE surahId = :surahId AND reciterIdentifier = :reciterIdentifier")
    fun getAyahCountForSurah(surahId: Int, reciterIdentifier: String): Int
}
