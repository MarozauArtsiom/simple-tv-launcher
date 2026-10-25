package io.github.marozauartsiom.tvlauncher.system.remote.http

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URLDecoder

/** One request. Header names are lower-case. */
class HttpRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: ByteArray,
) {
    fun header(name: String): String? = headers[name.lowercase()]

    /** The key from `Authorization: Bearer <key>`, or null. */
    val bearer: String? get() = header("authorization")?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)?.trim()

    val bodyText: String get() = body.toString(Charsets.UTF_8)
}

class HttpResponse(
    val status: Int,
    val contentType: String,
    val body: ByteArray,
    val headers: Map<String, String> = emptyMap(),
) {
    companion object {
        fun text(status: Int, text: String) = HttpResponse(status, "text/plain; charset=utf-8", text.toByteArray())
        fun json(status: Int, json: String) = HttpResponse(status, "application/json; charset=utf-8", json.toByteArray())
        fun empty(status: Int) = HttpResponse(status, "text/plain", ByteArray(0))
    }
}

/** A request the server answers with [status] and closes, without calling the handler. */
class HttpError(val status: Int, message: String) : Exception(message)

/**
 * Reads one HTTP/1.1 request with hard limits, so a client on the network can't make the TV allocate much:
 * request line 2 KB, all headers 8 KB (at most 50), body 64 KB, and only `Content-Length` bodies (no chunked).
 */
object HttpParser {
    const val MAX_LINE = 2 * 1024
    const val MAX_HEADERS_BYTES = 8 * 1024
    const val MAX_HEADERS = 50
    const val MAX_BODY = 64 * 1024

    fun parse(input: InputStream): HttpRequest? {
        val requestLine = readLine(input, MAX_LINE) ?: return null
        val parts = requestLine.split(' ')
        if (parts.size != 3 || !parts[2].startsWith("HTTP/1.")) throw HttpError(400, "bad request line")
        val method = parts[0]
        if (method !in setOf("GET", "POST", "HEAD", "OPTIONS", "DELETE")) throw HttpError(405, "method not allowed")
        val target = parts[1]
        if (!target.startsWith("/")) throw HttpError(400, "bad target")

        val headers = LinkedHashMap<String, String>()
        var headerBytes = 0
        while (true) {
            val line = readLine(input, MAX_LINE) ?: throw HttpError(400, "unexpected end of headers")
            if (line.isEmpty()) break
            headerBytes += line.length
            if (headerBytes > MAX_HEADERS_BYTES || headers.size >= MAX_HEADERS) throw HttpError(431, "headers too large")
            val colon = line.indexOf(':')
            if (colon <= 0) throw HttpError(400, "bad header")
            headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
        }
        if (headers["transfer-encoding"] != null) throw HttpError(501, "chunked bodies are not supported")
        val length = headers["content-length"]?.let { it.toIntOrNull() ?: throw HttpError(400, "bad content-length") } ?: 0
        if (length < 0 || length > MAX_BODY) throw HttpError(413, "body too large")
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n < 0) throw HttpError(400, "body shorter than content-length")
            read += n
        }

        val q = target.indexOf('?')
        val path = decode(if (q < 0) target else target.substring(0, q))
        if (path.contains("..") || path.contains('\\') || path.contains('\u0000')) throw HttpError(400, "bad path")
        val query = if (q < 0) emptyMap() else target.substring(q + 1).split('&').filter { it.isNotEmpty() }.associate {
            val kv = it.split('=', limit = 2)
            decode(kv[0]) to decode(kv.getOrElse(1) { "" })
        }
        return HttpRequest(method, path, query, headers, body)
    }

    private fun decode(s: String): String = try { URLDecoder.decode(s, "UTF-8") } catch (e: IllegalArgumentException) { throw HttpError(400, "bad encoding") }

    /** One CRLF- (or LF-) terminated line as ISO-8859-1; null at end of stream before any byte. */
    private fun readLine(input: InputStream, max: Int): String? {
        val out = ByteArrayOutputStream(64)
        while (true) {
            val b = input.read()
            if (b < 0) return if (out.size() == 0) null else throw HttpError(400, "unexpected end of line")
            if (b == '\n'.code) break
            if (b != '\r'.code) out.write(b)
            if (out.size() > max) throw HttpError(414, "line too long")
        }
        return out.toString(Charsets.ISO_8859_1.name())
    }

    fun write(response: HttpResponse, extraHeaders: Map<String, String> = emptyMap()): ByteArray {
        val reason = REASONS[response.status] ?: "Status"
        val sb = StringBuilder("HTTP/1.1 ${response.status} $reason\r\n")
        val all = linkedMapOf(
            "Content-Type" to response.contentType,
            "Content-Length" to response.body.size.toString(),
            "Cache-Control" to "no-store",
            "X-Content-Type-Options" to "nosniff",
            "Referrer-Policy" to "no-referrer",
            "Connection" to "close",
        ) + extraHeaders + response.headers
        all.forEach { (k, v) -> sb.append(k).append(": ").append(v).append("\r\n") }
        sb.append("\r\n")
        return sb.toString().toByteArray(Charsets.ISO_8859_1) + response.body
    }

    private val REASONS = mapOf(
        200 to "OK", 202 to "Accepted", 204 to "No Content", 400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden",
        404 to "Not Found", 405 to "Method Not Allowed", 409 to "Conflict", 413 to "Payload Too Large", 414 to "URI Too Long",
        429 to "Too Many Requests", 431 to "Request Header Fields Too Large", 500 to "Internal Server Error", 501 to "Not Implemented",
        503 to "Service Unavailable",
    )
}

/**
 * True for addresses that can only be in the home network: loopback, private IPv4 (10/8, 172.16/12, 192.168/16),
 * link-local, and IPv6 unique-local (fc00::/7). Anything else is refused before a request is read.
 */
fun isLocalAddress(address: InetAddress): Boolean = when {
    address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress -> true
    address is Inet6Address -> (address.address[0].toInt() and 0xFE) == 0xFC
    address is Inet4Address -> false
    else -> false
}
