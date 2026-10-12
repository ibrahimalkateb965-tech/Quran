package com.example.data.repository

import android.content.Context
import com.aistudio.quranblind.data.AssetQuranJsonSource
import com.aistudio.quranblind.data.JsonQuranRepository
import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import com.example.domain.repository.QuranRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : QuranRepository {

    private val delegate by lazy {
        JsonQuranRepository(AssetQuranJsonSource(context))
    }

    override fun getAllSurahs(): List<Surah> = delegate.getAllSurahs()

    override fun getSurahById(id: Int): Surah? = delegate.getSurahById(id)

    override fun findSurahByName(query: String): Surah? = delegate.findSurahByName(query)

    override fun getAyahs(
        surahId: Int,
        reciterIdentifier: String
    ): Flow<List<Ayah>> = delegate.getAyahs(surahId, reciterIdentifier)

    override fun getPageAyahs(
        pageNumber: Int,
        reciterIdentifier: String
    ): Flow<List<Ayah>> = delegate.getPageAyahs(pageNumber, reciterIdentifier)

    override fun getPageForAyah(surahId: Int, ayahIndex: Int): Int =
        delegate.getPageForAyah(surahId, ayahIndex)
}

