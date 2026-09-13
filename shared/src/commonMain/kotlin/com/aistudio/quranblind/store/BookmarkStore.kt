package com.aistudio.quranblind.store

import com.aistudio.quranblind.domain.model.Bookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Multiplatform bookmark persistence over [SecureStore] (ADR-005).
 *
 * The whole list is stored as one JSON array under [KEY_BOOKMARKS]. A missing or
 * unparsable value reads as an empty list and is left untouched until the first
 * successful mutation, so a bad write can never destroy what is already on disk.
 * [now] is injected so tests are deterministic.
 */
class BookmarkStore(private val store: SecureStore, private val now: () -> Long) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _bookmarks = MutableStateFlow(readPersisted())

    /** Newest first (createdAtEpochMillis descending, insertion order on ties). */
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean =
        _bookmarks.value.any { it.surahId == surahId && it.ayahNumber == ayahNumber }

    /** Removes the bookmark if present, otherwise adds it. Returns the new state. */
    fun toggle(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean {
        val current = _bookmarks.value
        val existing = current.firstOrNull { it.surahId == surahId && it.ayahNumber == ayahNumber }
        return if (existing != null) {
            persist(current - existing)
            false
        } else {
            persist(current + Bookmark(surahId, ayahNumber, surahNameAr, now()))
            true
        }
    }

    /** Idempotent merge by (surahId, ayahNumber); entries already present win. */
    fun importLegacy(items: List<Bookmark>) {
        val current = _bookmarks.value
        val known = current.map { it.surahId to it.ayahNumber }.toMutableSet()
        val added = items.filter { known.add(it.surahId to it.ayahNumber) }
        persist(current + added)
    }

    fun isLegacyMigrated(): Boolean = store.getString(KEY_LEGACY_MIGRATED) == MIGRATED_VALUE

    fun markLegacyMigrated() = store.putString(KEY_LEGACY_MIGRATED, MIGRATED_VALUE)

    private fun persist(list: List<Bookmark>) {
        val sorted = sortNewestFirst(list)
        store.putString(KEY_BOOKMARKS, json.encodeToString(sorted))
        _bookmarks.value = sorted
    }

    private fun readPersisted(): List<Bookmark> {
        val raw = store.getString(KEY_BOOKMARKS) ?: return emptyList()
        return try {
            sortNewestFirst(json.decodeFromString<List<Bookmark>>(raw))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sortNewestFirst(list: List<Bookmark>): List<Bookmark> =
        list.sortedByDescending { it.createdAtEpochMillis }

    companion object {
        const val STORE_NAME = "mueen_bookmarks"
        const val KEY_BOOKMARKS = "bookmarks_v1"
        const val KEY_LEGACY_MIGRATED = "legacy_room_migrated"
        private const val MIGRATED_VALUE = "1"
    }
}
