package com.example.data.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.aistudio.quranblind.domain.model.Bookmark
import com.aistudio.quranblind.store.BookmarkStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-time, lossless migration of the legacy Room `bookmarks` table into [BookmarkStore]
 * (ADR-005 D-5). Reads the SQLite file directly — Room is no longer a dependency.
 *
 * Invariants: the legacy database is deleted only after every row has been verified in
 * the store; on any failure the file and the migrated flag are left untouched so the
 * next launch retries. Store mutations run on [Dispatchers.Main] so they never race the
 * UI's own toggles.
 */
class LegacyRoomBookmarkMigrator(
    private val context: Context,
    private val store: BookmarkStore,
    private val dbName: String = DB_NAME,
) {

    /** @return rows migrated, 0 when nothing was needed, -1 on failure (left for retry). */
    suspend fun migrateIfNeeded(): Int {
        if (store.isLegacyMigrated()) return 0
        val file = context.getDatabasePath(dbName)
        if (!file.exists()) {
            withContext(Dispatchers.Main) { store.markLegacyMigrated() }
            return 0
        }
        return try {
            val rows = readLegacyRows(file.path)
            val verified = withContext(Dispatchers.Main) {
                store.importLegacy(rows)
                val distinct = rows.distinctBy { it.surahId to it.ayahNumber }.size
                rows.all { store.isBookmarked(it.surahId, it.ayahNumber) } &&
                    store.bookmarks.value.size >= distinct
            }
            if (!verified) {
                Log.w(TAG, "legacy bookmark verification failed, keeping $dbName")
                return -1
            }
            withContext(Dispatchers.Main) { store.markLegacyMigrated() }
            context.deleteDatabase(dbName)
            Log.i(TAG, "migrated ${rows.size} bookmarks from Room")
            rows.size
        } catch (e: Exception) {
            Log.w(TAG, "legacy bookmark migration failed, will retry on next launch", e)
            -1
        }
    }

    private fun readLegacyRows(path: String): List<Bookmark> =
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            if (!hasBookmarksTable(db)) return emptyList()
            val out = mutableListOf<Bookmark>()
            db.rawQuery(
                "SELECT surahId, surahNameAr, ayahNumber, timestamp, note FROM bookmarks",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    out += Bookmark(
                        surahId = cursor.getInt(0),
                        ayahNumber = cursor.getInt(2),
                        surahNameAr = cursor.getString(1),
                        createdAtEpochMillis = cursor.getLong(3),
                        note = if (cursor.isNull(4)) "" else cursor.getString(4),
                    )
                }
            }
            out
        }

    private fun hasBookmarksTable(db: SQLiteDatabase): Boolean =
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'bookmarks'",
            null
        ).use { it.count > 0 }

    companion object {
        const val DB_NAME = "quran_a11y_database"
        private const val TAG = "LegacyBookmarkMigrator"
    }
}
