package com.aistudio.quranblind.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun createHttpClient(config: NetworkConfig = NetworkConfig()): HttpClient =
        createTestHttpClient(config, null)

    internal fun createTestHttpClient(
        config: NetworkConfig = NetworkConfig(),
        engine: HttpClientEngine?,
        retryDelayMillis: (attempt: Int) -> Long = { attempt -> 1000L * attempt },
        // Injectable so host unit tests do not hit android.util.Log (not mocked there).
        warn: (tag: String, message: String) -> Unit = PlatformLogger::warn,
    ): HttpClient {
        val block: HttpClientConfig<*>.() -> Unit = {
            // Mirrors NetworkModule: the OkHttp interceptor throws after the last failed
            // attempt instead of handing a 5xx response to the caller.
            expectSuccess = true

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = config.timeoutMillis
                connectTimeoutMillis = config.timeoutMillis
                socketTimeoutMillis = config.timeoutMillis
            }

            install(HttpRequestRetry) {
                // NetworkModule retries while tryCount < 3, i.e. 1 initial attempt
                // plus 2 retries = 3 total attempts. Ktor maxRetries counts retries
                // after the first attempt, so it must be maxRetries - 1.
                maxRetries = config.maxRetries - 1

                // Mirrors NetworkModule: any response with status >= 500 is retried.
                retryIf { _, response ->
                    response.status.value >= 500
                }

                // Mirrors NetworkModule: transport failures (IOException) are retried.
                // kotlinx.io.IOException is the multiplatform equivalent of the
                // JVM-only IO exception type, which does not exist on Kotlin/Native.
                retryOnExceptionIf { _, cause ->
                    cause is IOException
                }

                // Linear backoff 1000ms x attempt via suspending delay.
                // This replaces the blocking thread sleep used in NetworkModule,
                // which does not exist on Kotlin/Native and must never block
                // a thread inside a coroutine.
                delayMillis { attempt ->
                    warn(
                        "HttpClientFactory",
                        "Network failure. Retrying... (Attempt $attempt/${config.maxRetries})",
                    )
                    retryDelayMillis(attempt)
                }
            }

            install(Logging) {
                // Ktor 3.x removed Logger.DEFAULT; this anonymous logger preserves the
                // previous default behaviour (stdout) on every platform.
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            println("HttpClient: $message")
                        }
                    }
                level = if (config.isDebug) LogLevel.BODY else LogLevel.NONE
            }
        }
        return if (engine == null) HttpClient(block) else HttpClient(engine, block)
    }
}
