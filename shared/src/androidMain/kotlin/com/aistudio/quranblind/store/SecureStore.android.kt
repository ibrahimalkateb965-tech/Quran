package com.aistudio.quranblind.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStoreAndroid {
    @Volatile internal var appContext: Context? = null
    fun init(context: Context) { appContext = context.applicationContext }
}

actual fun createSecureStore(name: String): SecureStore {
    val ctx = checkNotNull(SecureStoreAndroid.appContext) { "SecureStoreAndroid.init(context) must be called first" }
    return AndroidSecureStore(ctx, name)
}

internal class AndroidSecureStore(context: Context, name: String) : SecureStore {
    private val prefs: SharedPreferences = openPrefs(context.applicationContext, name)

    private fun openPrefs(context: Context, name: String): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                name,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.w("SecureStore", "Encrypted prefs unavailable for $name, using plain prefs", e)
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    override fun getString(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (e: Exception) {
            android.util.Log.w("SecureStore", "getString failed for $key", e)
            null
        }
    }

    override fun putString(key: String, value: String) {
        try {
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            android.util.Log.w("SecureStore", "putString failed for $key", e)
        }
    }

    override fun getInt(key: String, default: Int): Int {
        return try {
            prefs.getInt(key, default)
        } catch (e: Exception) {
            android.util.Log.w("SecureStore", "getInt failed for $key", e)
            default
        }
    }

    override fun putInt(key: String, value: Int) {
        try {
            prefs.edit().putInt(key, value).apply()
        } catch (e: Exception) {
            android.util.Log.w("SecureStore", "putInt failed for $key", e)
        }
    }

    override fun remove(key: String) {
        try {
            prefs.edit().remove(key).apply()
        } catch (e: Exception) {
            android.util.Log.w("SecureStore", "remove failed for $key", e)
        }
    }

    override fun clear() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.w("SecureStore", "clear failed", e)
        }
    }
}
