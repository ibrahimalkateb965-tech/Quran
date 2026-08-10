package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlQuranCloudResponse(
    @field:Json(name = "code") val code: Int,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "data") val data: SurahApiResponseData
)

@JsonClass(generateAdapter = true)
data class SurahApiResponseData(
    @field:Json(name = "number") val number: Int,
    @field:Json(name = "ayahs") val ayahs: List<AyahApiResponse>
)

@JsonClass(generateAdapter = true)
data class AyahApiResponse(
    @field:Json(name = "number") val globalNumber: Int,
    @field:Json(name = "text") val text: String,
    @field:Json(name = "numberInSurah") val numberInSurah: Int,
    @field:Json(name = "audio") val audioUrl: String? = null,
    @field:Json(name = "page") val page: Int = 1,
    @field:Json(name = "juz") val juz: Int = 1
)
