package com.example.data.network

import com.example.data.model.AlQuranCloudResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface QuranApiService {
    @GET("surah/{surahId}/{reciterIdentifier}")
    suspend fun getSurahAyahs(
        @Path("surahId") surahId: Int,
        @Path("reciterIdentifier") reciterIdentifier: String
    ): Response<AlQuranCloudResponse>
}
