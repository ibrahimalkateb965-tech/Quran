package com.aistudio.quranblind.di

import com.aistudio.quranblind.audio.AudioEngine
import com.aistudio.quranblind.data.BundleQuranJsonSource
import com.aistudio.quranblind.data.JsonQuranRepository
import com.aistudio.quranblind.data.QuranJsonSource
import com.aistudio.quranblind.domain.repository.QuranRepository
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

val iosModule = module {
    single<QuranJsonSource> { BundleQuranJsonSource() }
    single<QuranRepository> { JsonQuranRepository(get()) }
    single { AudioEngine() }
}

fun initKoinIos() = initKoin { modules(iosModule) }

// Swift entry point: resolves the repository from the started Koin container.
fun iosQuranRepository(): QuranRepository = KoinPlatform.getKoin().get()

// Swift entry point: the single app-wide engine (one AVPlayer per process).
fun iosAudioEngine(): AudioEngine = KoinPlatform.getKoin().get()
