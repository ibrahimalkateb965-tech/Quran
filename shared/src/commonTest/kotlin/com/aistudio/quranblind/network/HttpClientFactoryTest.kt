package com.aistudio.quranblind.network

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@Serializable
private data class ProbeResponse(val code: Int, val status: String)

private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")

private fun testConfig() = NetworkConfig(baseUrl = "https://example.com/", isDebug = false)

class HttpClientFactoryTest {

    @Test
    fun t1_okResponseDeserialisesThroughContentNegotiation() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"code":200,"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = JSON_HEADERS,
            )
        }
        val client = HttpClientFactory.createTestHttpClient(testConfig(), engine)
        try {
            val result: ProbeResponse = client.get("https://example.com/probe").body()
            assertEquals(ProbeResponse(200, "ok"), result)
        } finally {
            client.close()
        }
    }

    @Test
    fun t2_unknownJsonFieldDoesNotThrow() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"code":200,"status":"ok","mystery_field":"surprise"}""",
                status = HttpStatusCode.OK,
                headers = JSON_HEADERS,
            )
        }
        val client = HttpClientFactory.createTestHttpClient(testConfig(), engine)
        try {
            val result: ProbeResponse = client.get("https://example.com/probe").body()
            assertEquals(200, result.code)
        } finally {
            client.close()
        }
    }

    @Test
    fun t3_two500sThen200_succeedsWithExactlyThreeCalls() = runTest {
        var calls = 0
        val engine = MockEngine { _ ->
            calls++
            if (calls < 3) {
                respond("error", HttpStatusCode.InternalServerError)
            } else {
                respond(
                    content = """{"code":200,"status":"ok"}""",
                    status = HttpStatusCode.OK,
                    headers = JSON_HEADERS,
                )
            }
        }
        val client = HttpClientFactory.createTestHttpClient(
            testConfig(),
            engine,
            retryDelayMillis = { 0L },
            warn = { _, _ -> },
        )
        try {
            val result: ProbeResponse = client.get("https://example.com/probe").body()
            assertEquals(ProbeResponse(200, "ok"), result)
            assertEquals(3, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun t4_three500s_failsWithExactlyThreeCallsNotFour() = runTest {
        var calls = 0
        val engine = MockEngine { _ ->
            calls++
            respond("error", HttpStatusCode.InternalServerError)
        }
        val client = HttpClientFactory.createTestHttpClient(
            testConfig(),
            engine,
            retryDelayMillis = { 0L },
            warn = { _, _ -> },
        )
        try {
            assertFailsWith<ServerResponseException> {
                client.get("https://example.com/probe").body<ProbeResponse>()
            }
            assertEquals(3, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun t5_releaseConfig_installsLoggingPlugin() = runTest {
        // LogLevel itself leaves no observable trace on a MockEngine response, so this
        // test pins the install-site: both branches install the Logging plugin, and the
        // BODY/NONE selection is a pure if-expression on config.isDebug with no other
        // branches. Fleet Commander verifies the level mapping by code review.
        val debugEngine = MockEngine { respond("{}") }
        val releaseEngine = MockEngine { respond("{}") }
        val debug = HttpClientFactory.createTestHttpClient(
            testConfig().copy(isDebug = true),
            debugEngine,
        )
        val release = HttpClientFactory.createTestHttpClient(
            testConfig().copy(isDebug = false),
            releaseEngine,
        )
        try {
            assertNotNull(debug.pluginOrNull(Logging))
            assertNotNull(release.pluginOrNull(Logging))
        } finally {
            debug.close()
            release.close()
        }
    }
}
