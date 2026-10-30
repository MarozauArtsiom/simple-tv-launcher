package io.github.marozauartsiom.tvlauncher.system.remote

import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalApiTest {
    private val secret = "0123456789abcdef0123456789abcdef"
    private val gate = AccessGate({ 0L }).apply { setOpen(setOf(ClientKind.Phone, ClientKind.Assistant)) }
    private val tv = FakeTvControl()
    private val inner = RemoteApi(
        gate, tv, McpServer(tv, "1", encodeBase64 = { "x" }), { "192.168.1.20:8765" },
        look = { RemoteLook("midnight", "ru", emptyMap()) },
        asset = { path -> "file:$path".toByteArray() },
        font = { byteArrayOf(9) },
    )
    private val api = ExternalApi(inner) { secret }

    private fun req(method: String, path: String, body: String = "", key: String? = null) = api.handle(
        HttpRequest(method, path, emptyMap(), mapOf("host" to "abc.lhr.life") + listOfNotNull(key?.let { "authorization" to "Bearer $it" }), body.toByteArray()),
    )

    private fun pairBody(kind: String) = """{"code":"${gate.code}","kind":"$kind"}"""

    @Test fun withoutTheSecretNothingExists() {
        listOf("/", "/pair", "/mcp", "/api/pair", "/s/", "/s/wrong/pair", "/s/${secret}x/pair", "/s/${secret.dropLast(1)}/pair", "/s${secret}/pair").forEach {
            assertEquals(it, 404, req("GET", it).status)
        }
        assertEquals(404, req("POST", "/api/pair", pairBody("assistant")).status)
    }

    @Test fun wrongSecretDoesNotCountAsAPairingAttempt() {
        repeat(20) { req("POST", "/s/wrong/api/pair", """{"code":"000000","kind":"assistant"}""") }
        assertEquals(200, req("POST", "/s/$secret/api/pair", pairBody("assistant")).status)
    }

    @Test fun thePairingPageAndItsFilesAreServed() {
        assertEquals("file:remote/pair.html", req("GET", "/s/$secret/pair").body.decodeToString())
        assertEquals("file:remote/pair.html", req("GET", "/s/$secret/").body.decodeToString())
        assertEquals("file:remote/pair.html", req("GET", "/s/$secret").body.decodeToString())
        assertEquals(200, req("GET", "/s/$secret/pair.js").status)
        assertEquals(200, req("GET", "/s/$secret/themes.css").status)
        assertEquals(200, req("GET", "/s/$secret/assets/bg/sky.svg").status)
        assertEquals(200, req("GET", "/s/$secret/api/look").status)
    }

    @Test fun thePhoneRemoteIsNotReachableFromTheInternet() {
        // Even a valid phone key, even with the secret.
        val phone = (gate.pair(gate.code, ClientKind.Phone, "p") as PairResult.Paired).key
        listOf("/api/key", "/api/tap", "/api/text", "/api/state", "/api/frame", "/remote.js", "/index.html").forEach {
            assertEquals(it, 404, req("POST", "/s/$secret$it", """{"key":"up"}""", key = phone).status)
            assertEquals(it, 404, req("GET", "/s/$secret$it", key = phone).status)
        }
        assertTrue(tv.calls.isEmpty())
    }

    @Test fun pairingFromTheInternetAlwaysMakesAnAssistantNeverAPhone() {
        val r = req("POST", "/s/$secret/api/pair", pairBody("phone"))
        assertEquals(200, r.status)
        val key = Json.parseToJsonElement(r.body.decodeToString()).jsonObject["key"]!!.jsonPrimitive.content
        assertNull(gate.authorize(key, ClientKind.Phone))
        assertEquals(ClientKind.Assistant, gate.clients().single().kind)
        // A body that isn't JSON is a wrong code, never a crash.
        assertEquals(403, req("POST", "/s/$secret/api/pair", "not json").status)
    }

    @Test fun mcpNeedsAnAssistantKeyAndTheSecret() {
        val ping = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
        assertEquals(401, req("POST", "/s/$secret/mcp", ping).status)
        val key = Json.parseToJsonElement(req("POST", "/s/$secret/api/pair", pairBody("assistant")).body.decodeToString()).jsonObject["key"]!!.jsonPrimitive.content
        assertEquals(200, req("POST", "/s/$secret/mcp", ping, key = key).status)
        assertEquals(404, req("POST", "/mcp", ping, key = key).status)
        assertEquals(405, req("GET", "/s/$secret/mcp", key = key).status)
    }

    @Test fun theKeyMayRideInThePathForConnectorsThatCantSendHeaders() {
        val ping = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
        val key = Json.parseToJsonElement(req("POST", "/s/$secret/api/pair", pairBody("assistant")).body.decodeToString()).jsonObject["key"]!!.jsonPrimitive.content
        assertEquals(200, req("POST", "/s/$secret/mcp/$key", ping).status)
        assertEquals(401, req("POST", "/s/$secret/mcp/wrongkey", ping).status)
        assertEquals(404, req("POST", "/mcp/$key", ping).status) // still needs the secret path
    }

    @Test fun aWipeEndsTheInternetSessionToo() {
        val key = Json.parseToJsonElement(req("POST", "/s/$secret/api/pair", pairBody("assistant")).body.decodeToString()).jsonObject["key"]!!.jsonPrimitive.content
        gate.wipe()
        gate.setOpen(setOf(ClientKind.Assistant))
        assertEquals(401, req("POST", "/s/$secret/mcp", """{"jsonrpc":"2.0","id":1,"method":"ping"}""", key = key).status)
    }

    @Test fun theBannerGivesTheTunnelAddress() {
        assertEquals("https://86e0f64868e2d7.lhr.life", TunnelBanner.parse("86e0f64868e2d7.lhr.life tunneled with tls termination, https://86e0f64868e2d7.lhr.life"))
        assertNull(TunnelBanner.parse("Welcome to localhost.run! More at https://admin.localhost.run/"))
        assertNull(TunnelBanner.parse("create an account and add your key, see https://localhost.run/docs/forever-free/"))
        assertNull(TunnelBanner.parse("x tunneled with tls termination, https://evil.example.com"))
    }
}
