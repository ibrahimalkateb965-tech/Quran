package com.aistudio.quranblind.network

data class NetworkConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val isDebug: Boolean = false,
    val timeoutMillis: Long = 15_000,
    val maxRetries: Int = 3,
) {
    companion object {
        // Mirrors app/build.gradle.kts: buildConfigField BASE_URL
        const val DEFAULT_BASE_URL = "https://api.alquran.cloud/v1/"
    }
}
