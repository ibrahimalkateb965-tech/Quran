package com.example.di

import android.content.Context
import com.example.accessibility.HapticFeedbackManager
import com.example.accessibility.SpeechManager
import com.aistudio.quranblind.store.BookmarkStore
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
    fun provideSessionStore(): SessionStore {
        return SessionStore(createSecureStore(SessionStore.STORE_NAME))
    }

    @Provides
    @Singleton
    fun provideBookmarkStore(): BookmarkStore {
        return BookmarkStore(createSecureStore(BookmarkStore.STORE_NAME), System::currentTimeMillis)
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
