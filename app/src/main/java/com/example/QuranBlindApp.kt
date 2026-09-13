package com.example

import android.app.Application
import com.aistudio.quranblind.store.BookmarkStore
import com.aistudio.quranblind.store.SecureStoreAndroid
import com.example.data.migration.LegacyRoomBookmarkMigrator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class QuranBlindApp : Application() {

    /**
     * Resolved through an entry point (not field injection) because Hilt injects the
     * Application before [onCreate] runs, i.e. before [SecureStoreAndroid.init] — and
     * [BookmarkStore] needs the initialised store. The instance is the same singleton
     * the ViewModel receives, so the migrated list is visible to the UI immediately.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BookmarkMigrationEntryPoint {
        fun bookmarkStore(): BookmarkStore
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SecureStoreAndroid.init(this)
        val bookmarkStore = EntryPointAccessors
            .fromApplication(this, BookmarkMigrationEntryPoint::class.java)
            .bookmarkStore()
        appScope.launch {
            LegacyRoomBookmarkMigrator(this@QuranBlindApp, bookmarkStore).migrateIfNeeded()
        }
    }
}
