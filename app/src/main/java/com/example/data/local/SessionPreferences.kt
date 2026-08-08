package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionPreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback to normal SharedPreferences if Keystore is corrupted/unavailable
        context.applicationContext.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        )
    }

    fun saveSession(reciterId: String, surahId: Int, ayahIndex: Int) {
        prefs.edit {
            putString(KEY_LAST_RECITER_ID, reciterId)
            putInt(KEY_LAST_SURAH_ID, surahId)
            putInt(KEY_LAST_AYAH_INDEX, ayahIndex)
        }
    }

    fun getSession(): SessionState? {
        val reciterId = prefs.getString(KEY_LAST_RECITER_ID, null)
        val surahId = prefs.getInt(KEY_LAST_SURAH_ID, -1)
        val ayahIndex = prefs.getInt(KEY_LAST_AYAH_INDEX, -1)

        if (reciterId != null && surahId != -1 && ayahIndex != -1) {
            return SessionState(reciterId, surahId, ayahIndex)
        }
        return null
    }

    fun clearSession() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "mueen_session_prefs"
        private const val KEY_LAST_RECITER_ID = "last_reciter_id"
        private const val KEY_LAST_SURAH_ID = "last_surah_id"
        private const val KEY_LAST_AYAH_INDEX = "last_ayah_index"
        
        @Volatile
        private var INSTANCE: SessionPreferences? = null

        fun getInstance(context: Context): SessionPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionPreferences(context).also { INSTANCE = it }
            }
        }
    }
}

data class SessionState(
    val reciterId: String,
    val surahId: Int,
    val ayahIndex: Int
)
