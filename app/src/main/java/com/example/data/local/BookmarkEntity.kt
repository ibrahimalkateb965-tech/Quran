package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val surahId: Int,
    val surahNameAr: String,
    val ayahNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
