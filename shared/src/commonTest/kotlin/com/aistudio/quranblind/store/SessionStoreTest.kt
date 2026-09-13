package com.aistudio.quranblind.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionStoreTest {

    @Test
    fun saveThenLoadRoundTripsAllThreeFields() {
        val store = SessionStore(InMemorySecureStore())
        store.save(SessionState(reciterId = "husary", surahId = 36, ayahIndex = 5))
        assertEquals(SessionState(reciterId = "husary", surahId = 36, ayahIndex = 5), store.load())
    }

    @Test
    fun loadOnEmptyStoreReturnsNull() {
        val store = SessionStore(InMemorySecureStore())
        assertNull(store.load())
    }

    @Test
    fun loadWithOnlyTwoOfThreeKeysReturnsNull() {
        val backing = InMemorySecureStore()
        backing.putString(SessionStore.KEY_LAST_RECITER_ID, "husary")
        backing.putInt(SessionStore.KEY_LAST_SURAH_ID, 2)
        assertNull(SessionStore(backing).load())
    }

    @Test
    fun legacyHusaryIdMigrates() {
        val backing = InMemorySecureStore()
        backing.putString(SessionStore.KEY_LAST_RECITER_ID, "ar.husary")
        backing.putInt(SessionStore.KEY_LAST_SURAH_ID, 1)
        backing.putInt(SessionStore.KEY_LAST_AYAH_INDEX, 0)
        assertEquals(SessionState(reciterId = "husary", surahId = 1, ayahIndex = 0), SessionStore(backing).load())
    }

    @Test
    fun legacyMinshawiIdMigrates() {
        val backing = InMemorySecureStore()
        backing.putString(SessionStore.KEY_LAST_RECITER_ID, "ar.minshawi")
        backing.putInt(SessionStore.KEY_LAST_SURAH_ID, 1)
        backing.putInt(SessionStore.KEY_LAST_AYAH_INDEX, 0)
        assertEquals(SessionState(reciterId = "minshawi", surahId = 1, ayahIndex = 0), SessionStore(backing).load())
    }

    @Test
    fun legacyAbdulbasitIdMigrates() {
        val backing = InMemorySecureStore()
        backing.putString(SessionStore.KEY_LAST_RECITER_ID, "ar.abdulbasitmurattal")
        backing.putInt(SessionStore.KEY_LAST_SURAH_ID, 1)
        backing.putInt(SessionStore.KEY_LAST_AYAH_INDEX, 0)
        assertEquals(SessionState(reciterId = "abdulbasit", surahId = 1, ayahIndex = 0), SessionStore(backing).load())
    }

    @Test
    fun clearEmptiesTheStore() {
        val backing = InMemorySecureStore()
        val store = SessionStore(backing)
        store.save(SessionState(reciterId = "husary", surahId = 36, ayahIndex = 5))
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun putIntGetIntDefaultPath() {
        val backing = InMemorySecureStore()
        assertEquals(7, backing.getInt("missing", 7))
        backing.putInt("n", 42)
        assertEquals(42, backing.getInt("n", 7))
    }
}
