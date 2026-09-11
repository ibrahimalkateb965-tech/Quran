package com.aistudio.quranblind.store

data class SessionState(val reciterId: String, val surahId: Int, val ayahIndex: Int)

class SessionStore(private val store: SecureStore) {
    fun save(state: SessionState) {
        store.putString(KEY_LAST_RECITER_ID, state.reciterId)
        store.putInt(KEY_LAST_SURAH_ID, state.surahId)
        store.putInt(KEY_LAST_AYAH_INDEX, state.ayahIndex)
    }

    fun load(): SessionState? {
        var reciterId = store.getString(KEY_LAST_RECITER_ID)
        val surahId = store.getInt(KEY_LAST_SURAH_ID, -1)
        val ayahIndex = store.getInt(KEY_LAST_AYAH_INDEX, -1)

        // Migrate old reciter IDs to new ones
        if (reciterId != null) {
            reciterId = when (reciterId) {
                "ar.husary" -> "husary"
                "ar.minshawi" -> "minshawi"
                "ar.abdulbasitmurattal" -> "abdulbasit"
                else -> reciterId
            }
        }

        if (reciterId != null && surahId != -1 && ayahIndex != -1) {
            return SessionState(reciterId, surahId, ayahIndex)
        }
        return null
    }

    fun clear() = store.clear()

    companion object {
        const val STORE_NAME = "mueen_session_prefs"
        const val KEY_LAST_RECITER_ID = "last_reciter_id"
        const val KEY_LAST_SURAH_ID = "last_surah_id"
        const val KEY_LAST_AYAH_INDEX = "last_ayah_index"
    }
}
