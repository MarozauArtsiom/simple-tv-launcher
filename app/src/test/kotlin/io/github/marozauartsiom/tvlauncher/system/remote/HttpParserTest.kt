package io.github.marozauartsiom.tvlauncher.system.remote

import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpError
import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpParser
import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpResponse
import io.github.marozauartsiom.tvlauncher.system.remote.http.isLocalAddress
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HttpParserTest {
    private fun parse(raw: String) = HttpParser.parse(raw.byteInputStream(Charsets.ISO_8859_1))

    private fun status(raw: String): Int = try { parse(raw); fail("expected an error"); 0 } catch (e: HttpError) { e.status }

    @Test fun readsMethodPathQueryHeadersAndBody() {
        val r = parse("POST /api/key?a=1&b=x%20y HTTP/1.1\r\nHost: 192.168.1.2:8765\r\nAuthorization: Bearer abc\r\nContent-Length: 13\r\n\r\n{\"key\":\"up\"}x")!!
        assertEquals("POST", r.method)
        assertEquals("/api/key", r.path)
        assertEquals(mapOf("a" to "1", "b" to "x y"), r.query)
        assertEquals("192.168.1.2:8765", r.header("Host"))
        assertEquals("abc", r.bearer)
        assertEquals("{\"key\":\"up\"}x", r.bodyText)
    }

    @Test fun emptyStreamIsNoRequest() {
        assertNull(parse(""))
    }

    @Test fun refusesBadRequests() {
        assertEquals(400, status("GET /\r\n\r\n"))
        assertEquals(405, status("PUT / HTTP/1.1\r\n\r\n"))
        assertEquals(400, status("GET http://x/ HTTP/1.1\r\n\r\n"))
        assertEquals(400, status("GET /../secret HTTP/1.1\r\n\r\n"))
        assertEquals(400, status("GET /%2e%2e/secret HTTP/1.1\r\n\r\n"))
        assertEquals(501, status("POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n"))
        assertEquals(400, status("POST / HTTP/1.1\r\nContent-Length: 10\r\n\r\nshort"))
    }

    @Test fun enforcesLimits() {
        assertEquals(414, status("GET /" + "a".repeat(HttpParser.MAX_LINE) + " HTTP/1.1\r\n\r\n"))
        assertEquals(413, status("POST / HTTP/1.1\r\nContent-Length: ${HttpParser.MAX_BODY + 1}\r\n\r\n"))
        val many = (1..HttpParser.MAX_HEADERS + 1).joinToString("") { "X-$it: 1\r\n" }
        assertEquals(431, status("GET / HTTP/1.1\r\n$many\r\n"))
        val big = (1..10).joinToString("") { "X-$it: ${"a".repeat(1000)}\r\n" }
        assertEquals(431, status("GET / HTTP/1.1\r\n$big\r\n"))
    }

    @Test fun responsesCarrySafeHeaders() {
        val out = HttpParser.write(HttpResponse.text(200, "hi")).toString(Charsets.ISO_8859_1)
        assertTrue(out.startsWith("HTTP/1.1 200 OK\r\n"))
        listOf("Content-Length: 2", "Cache-Control: no-store", "X-Content-Type-Options: nosniff", "Connection: close").forEach { assertTrue(it, out.contains(it)) }
        assertTrue(out.endsWith("\r\n\r\nhi"))
    }

    @Test fun onlyHomeNetworkAddressesAreLocal() {
        listOf("127.0.0.1", "10.0.0.5", "172.16.3.4", "192.168.100.5", "169.254.1.1", "::1", "fe80::1", "fd12:3456::1").forEach {
            assertTrue(it, isLocalAddress(InetAddress.getByName(it)))
        }
        listOf("8.8.8.8", "172.32.0.1", "100.64.0.1", "2001:4860::8888").forEach {
            assertFalse(it, isLocalAddress(InetAddress.getByName(it)))
        }
    }
}
