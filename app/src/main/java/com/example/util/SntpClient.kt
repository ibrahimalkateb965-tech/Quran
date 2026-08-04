package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Minimal SNTP client (RFC 4330 simplified) that queries a trusted NTP server.
 * Returns the Unix epoch time in milliseconds or null on failure.
 */
object SntpClient {

    private const val DEFAULT_NTP_HOST = "time.google.com"
    private const val NTP_PORT = 123
    private const val NTP_PACKET_SIZE = 48

    // NTP time starts at 1900-01-01; Unix time starts at 1970-01-01.
    // Difference = 70 years = 2208988800 seconds.
    private const val OFFSET_1900_TO_1970_SECONDS = 2208988800L

    /**
     * Queries [host] on port 123 and returns the current network time in milliseconds.
     * Runs on [Dispatchers.IO].
     */
    suspend fun requestTime(
        host: String = DEFAULT_NTP_HOST,
        timeoutMillis: Int = 8_000
    ): Long? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply { soTimeout = timeoutMillis }

            val address = InetAddress.getByName(host)
            val buffer = ByteArray(NTP_PACKET_SIZE)

            // Set LI=0, VN=4, Mode=3 (client)
            buffer[0] = 0x1B

            val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(request)

            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)

            // Transmit timestamp is at bytes 40-47 (64-bit NTP timestamp).
            val seconds = readSeconds(buffer, 40)
            val fraction = readFraction(buffer, 40)

            val transmitTimeMillis = ((seconds - OFFSET_1900_TO_1970_SECONDS) * 1000L) +
                    ((fraction * 1000L) / 0x1_0000_0000L)

            transmitTimeMillis
        } catch (e: Exception) {
            null
        } finally {
            socket?.close()
        }
    }

    private fun readSeconds(buffer: ByteArray, offset: Int): Long {
        var seconds: Long = 0
        for (i in 0..3) {
            seconds = (seconds shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        return seconds
    }

    private fun readFraction(buffer: ByteArray, offset: Int): Long {
        var fraction: Long = 0
        for (i in 4..7) {
            fraction = (fraction shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        return fraction
    }
}
