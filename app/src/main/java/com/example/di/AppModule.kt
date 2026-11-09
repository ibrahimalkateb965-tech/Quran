package com.example.di

import android.content.Context
import com.example.accessibility.HapticFeedbackManager
import com.example.accessibility.SpeechManager
import com.example.data.local.AyahDao
import com.example.data.local.BookmarkDao
import com.example.data.local.QuranDatabase
import com.aistudio.quranblind.store.SessionStore
import com.aistudio.quranblind.store.createSecureStore
import com.example.data.repository.QuranRepositoryImpl
import com.example.domain.repository.QuranRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideQuranDatabase(@ApplicationContext context: Context): QuranDatabase {
        return QuranDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAyahDao(database: QuranDatabase): AyahDao {
        return database.ayahDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: QuranDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideSessionStore(): SessionStore {
        return SessionStore(createSecureStore(SessionStore.STORE_NAME))
    }

    @Provides
    @Singleton
    fun provideHapticFeedbackManager(@ApplicationContext context: Context): HapticFeedbackManager {
        return HapticFeedbackManager(context)
    }

    @Provides
    @Singleton
    fun provideSpeechManager(@ApplicationContext context: Context): SpeechManager {
        return SpeechManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuranRepository(
        impl: QuranRepositoryImpl
    ): QuranRepository
}
