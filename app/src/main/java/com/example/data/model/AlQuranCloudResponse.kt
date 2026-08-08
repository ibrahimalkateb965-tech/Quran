package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlQuranCloudResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: SurahApiResponseData
)

@JsonClass(generateAdapter = true)
data class SurahApiResponseData(
    @Json(name = "number") val number: Int,
    @Json(name = "ayahs") val ayahs: List<AyahApiResponse>
)

@JsonClass(generateAdapter = true)
data class AyahApiResponse(
    @Json(name = "number") val globalNumber: Int,
    @Json(name = "text") val text: String,
    @Json(name = "numberInSurah") val numberInSurah: Int,
    @Json(name = "audio") val audioUrl: String? = null,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "juz") val juz: Int = 1
)
