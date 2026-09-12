package com.aistudio.quranblind.data

import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.repository.QuranRepository
import kotlinx.coroutines.flow.first

// Swift cannot consume a Kotlin Flow directly; this suspend function is exported as an async method.
suspend fun loadAyahsOnce(
    repository: QuranRepository,
    surahId: Int,
    audioBaseUrl: String,
): List<Ayah> = repository.getAyahs(surahId, audioBaseUrl).first()
