package com.aistudio.quranblind.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val sharedModule = module {
    // Intentionally empty until the first shared implementation exists (ORDER-P0-004).
    // QuranRepository is NOT bound here: its implementation is still Android/Room-bound.
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedModule)
}
