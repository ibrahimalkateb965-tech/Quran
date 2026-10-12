package com.aistudio.quranblind.di

import com.aistudio.quranblind.audio.AudioEngine
import com.aistudio.quranblind.data.BundleQuranJsonSource
import com.aistudio.quranblind.data.JsonQuranRepository
import com.aistudio.quranblind.data.QuranJsonSource
import com.aistudio.quranblind.domain.repository.QuranRepository
import com.aistudio.quranblind.store.BookmarkStore
import com.aistudio.quranblind.store.createSecureStore
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

val iosModule = module {
    single<QuranJsonSource> { BundleQuranJsonSource() }
    single<QuranRepository> { JsonQuranRepository(get()) }
    single { AudioEngine() }
    single { createSecureStore(BookmarkStore.STORE_NAME) }
    single { BookmarkStore(get()) { (NSDate().timeIntervalSince1970 * 1000.0).toLong() } }
}

fun initKoinIos() = initKoin { modules(iosModule) }

// Swift entry point: resolves the repository from the started Koin container.
fun iosQuranRepository(): QuranRepository = KoinPlatform.getKoin().get()

// Swift entry point: the single app-wide engine (one AVPlayer per process).
fun iosAudioEngine(): AudioEngine = KoinPlatform.getKoin().get()

// Swift entry point: multiplatform bookmark persistence.
fun iosBookmarkStore(): BookmarkStore = KoinPlatform.getKoin().get()

