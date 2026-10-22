package io.github.marozauartsiom.tvlauncher.data.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The body of an HTTP answer, and its status code. */
data class HttpResult(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299
}

/** The one place the launcher talks to the network. Fakes replace it in tests. */
interface HttpFetcher {
    /** GET [url]; null if there was no answer at all (offline, timeout). */
    suspend fun get(url: String): HttpResult?
}

/**
 * `HttpURLConnection`: no OkHttp or Retrofit, to keep the APK small. 10 s timeouts, https only, and the
 * body is cut at 512 KB (the answers are a few KB).
 */
class UrlConnectionFetcher(private val io: CoroutineDispatcher = Dispatchers.IO) : HttpFetcher {
    override suspend fun get(url: String): HttpResult? = withContext(io) {
        if (!url.startsWith("https://")) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SimpleTVLauncher/1.0")
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (out.size() < MAX_BYTES) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }.orEmpty()
            HttpResult(code, body)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_BYTES = 512 * 1024
    }
}
