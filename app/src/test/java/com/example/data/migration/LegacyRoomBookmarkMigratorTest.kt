package com.example.data.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.aistudio.quranblind.store.BookmarkStore
import com.aistudio.quranblind.store.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LegacyRoomBookmarkMigratorTest {

    private class FakeSecureStore : SecureStore {
        private val map = mutableMapOf<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getInt(key: String, default: Int): Int = map[key]?.toIntOrNull() ?: default
        override fun putInt(key: String, value: Int) { map[key] = value.toString() }
        override fun remove(key: String) { map.remove(key) }
        override fun clear() = map.clear()
    }

    private val dbName = "test_legacy_quran_a11y_database"
    private lateinit var context: Context
    private lateinit var store: BookmarkStore
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        store = BookmarkStore(FakeSecureStore()) { 999_999L }
        dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
        Dispatchers.resetMain()
    }

    private fun createLegacyDatabase(rows: List<Triple<Int, Int, Long>>) {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL(
                "CREATE TABLE bookmarks(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "surahId INTEGER NOT NULL, surahNameAr TEXT NOT NULL, " +
                    "ayahNumber INTEGER NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL)"
            )
            rows.forEach { (surah, ayah, ts) ->
                db.execSQL(
                    "INSERT INTO bookmarks(surahId, surahNameAr, ayahNumber, timestamp, note) VALUES (?, ?, ?, ?, ?)",
                    arrayOf(surah, "name-$surah", ayah, ts, "")
                )
            }
        }
    }

    private fun migrator() = LegacyRoomBookmarkMigrator(context, store, dbName)

    @Test
    fun migratesEveryLegacyRowNewestFirstThenDeletesTheFileAndSetsTheFlag() = runTest {
        createLegacyDatabase(listOf(Triple(1, 1, 100L), Triple(2, 255, 300L), Triple(36, 1, 200L)))
        assertTrue(dbFile.exists())

        val migrated = migrator().migrateIfNeeded()

        assertEquals(3, migrated)
        assertEquals(listOf(2, 36, 1), store.bookmarks.value.map { it.surahId })
        assertEquals("name-2", store.bookmarks.value.first().surahNameAr)
        assertEquals(300L, store.bookmarks.value.first().createdAtEpochMillis)
        assertTrue(store.isBookmarked(36, 1))
        assertFalse(dbFile.exists())
        assertTrue(store.isLegacyMigrated())
    }

    @Test
    fun secondRunIsANoOp() = runTest {
        createLegacyDatabase(listOf(Triple(1, 1, 100L)))
        assertEquals(1, migrator().migrateIfNeeded())

        createLegacyDatabase(listOf(Triple(2, 2, 200L))) // a stray file must not be re-imported
        assertEquals(0, migrator().migrateIfNeeded())
        assertEquals(1, store.bookmarks.value.size)
    }

    @Test
    fun freshInstallWithoutADatabaseJustSetsTheFlag() = runTest {
        assertFalse(dbFile.exists())
        assertEquals(0, migrator().migrateIfNeeded())
        assertTrue(store.isLegacyMigrated())
        assertEquals(emptyList<Any>(), store.bookmarks.value)
    }

    @Test
    fun unreadableFileIsLeftInPlaceWithTheFlagUnset() = runTest {
        dbFile.writeBytes(ByteArray(64) { 0x41 })

        assertEquals(-1, migrator().migrateIfNeeded())
        assertTrue(dbFile.exists())
        assertFalse(store.isLegacyMigrated())
        assertEquals(emptyList<Any>(), store.bookmarks.value)
    }

    @Test
    fun databaseWithoutABookmarksTableCountsAsZeroRowsAndIsCleanedUp() = runTest {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { it.execSQL("CREATE TABLE other(x INTEGER)") }

        assertEquals(0, migrator().migrateIfNeeded())
        assertFalse(dbFile.exists())
        assertTrue(store.isLegacyMigrated())
    }
}
