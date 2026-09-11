package com.aistudio.quranblind.store

interface SecureStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
    fun clear()
}

/** Platform factory. [name] is the store namespace; Android maps it to the prefs file name. */
expect fun createSecureStore(name: String): SecureStore
