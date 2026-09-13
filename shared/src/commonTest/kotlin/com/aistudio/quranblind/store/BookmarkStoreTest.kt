package com.aistudio.quranblind.store

import com.aistudio.quranblind.domain.model.Bookmark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookmarkStoreTest {

    private class FakeClock(var now: Long = 1_000L) {
        fun tick(): Long = ++now
    }

    private fun storeWith(secure: SecureStore = InMemorySecureStore(), clock: FakeClock = FakeClock()) =
        BookmarkStore(secure) { clock.tick() }

    @Test
    fun emptyStoreExposesEmptyListAndIsNotMigrated() {
        val store = storeWith()
        assertEquals(emptyList(), store.bookmarks.value)
        assertFalse(store.isBookmarked(1, 1))
        assertFalse(store.isLegacyMigrated())
    }

    @Test
    fun toggleAddsThenRemovesAndReturnsTheNewState() {
        val store = storeWith()
        assertTrue(store.toggle(2, "البقرة", 255))
        assertTrue(store.isBookmarked(2, 255))
        assertEquals("البقرة", store.bookmarks.value.single().surahNameAr)

        assertFalse(store.toggle(2, "البقرة", 255))
        assertFalse(store.isBookmarked(2, 255))
        assertEquals(emptyList(), store.bookmarks.value)
    }

    @Test
    fun bookmarksAreOrderedNewestFirst() {
        val store = storeWith()
        store.toggle(1, "الفاتحة", 1)
        store.toggle(2, "البقرة", 255)
        store.toggle(36, "يس", 1)
        assertEquals(listOf(36, 2, 1), store.bookmarks.value.map { it.surahId })
    }

    @Test
    fun persistedJsonRoundTripsIntoASecondStoreOverTheSameBackend() {
        val secure = InMemorySecureStore()
        val first = storeWith(secure)
        first.toggle(1, "الفاتحة", 1)
        first.toggle(2, "البقرة", 255)

        val second = storeWith(secure)
        assertEquals(first.bookmarks.value, second.bookmarks.value)
        assertTrue(second.isBookmarked(2, 255))
    }

    @Test
    fun importLegacyMergesByKeyIsIdempotentAndExistingEntriesWin() {
        val store = storeWith()
        store.toggle(2, "البقرة", 255) // createdAt = 1001
        val legacy = listOf(
            Bookmark(surahId = 2, ayahNumber = 255, surahNameAr = "legacy-name", createdAtEpochMillis = 5L),
            Bookmark(surahId = 18, ayahNumber = 10, surahNameAr = "الكهف", createdAtEpochMillis = 7L),
            Bookmark(surahId = 18, ayahNumber = 10, surahNameAr = "dup", createdAtEpochMillis = 8L),
        )

        store.importLegacy(legacy)
        store.importLegacy(legacy)

        val list = store.bookmarks.value
        assertEquals(2, list.size)
        assertEquals("البقرة", list.first { it.surahId == 2 }.surahNameAr)
        assertEquals(7L, list.first { it.surahId == 18 }.createdAtEpochMillis)
        assertEquals(listOf(2, 18), list.map { it.surahId })
    }

    @Test
    fun corruptPersistedValueReadsAsEmptyAndIsNotOverwrittenUntilAMutation() {
        val secure = InMemorySecureStore()
        secure.putString(BookmarkStore.KEY_BOOKMARKS, "{not json")
        val store = storeWith(secure)

        assertEquals(emptyList(), store.bookmarks.value)
        assertEquals("{not json", secure.getString(BookmarkStore.KEY_BOOKMARKS))

        store.toggle(1, "الفاتحة", 1)
        assertTrue(secure.getString(BookmarkStore.KEY_BOOKMARKS)!!.startsWith("["))
    }

    @Test
    fun legacyMigratedFlagRoundTrips() {
        val secure = InMemorySecureStore()
        val store = storeWith(secure)
        assertFalse(store.isLegacyMigrated())
        store.markLegacyMigrated()
        assertTrue(store.isLegacyMigrated())
        assertTrue(storeWith(secure).isLegacyMigrated())
    }
}
