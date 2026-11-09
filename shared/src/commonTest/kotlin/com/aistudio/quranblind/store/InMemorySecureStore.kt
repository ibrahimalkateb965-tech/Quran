package com.aistudio.quranblind.store

class InMemorySecureStore : SecureStore {
    private val strings = mutableMapOf<String, String>()

    override fun getString(key: String): String? = strings[key]

    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    override fun getInt(key: String, default: Int): Int {
        return strings[key]?.toIntOrNull() ?: default
    }

    override fun putInt(key: String, value: Int) {
        strings[key] = value.toString()
    }

    override fun remove(key: String) {
        strings.remove(key)
    }

    override fun clear() {
        strings.clear()
    }
}
