package io.github.marozauartsiom.tvlauncher.system.remote

import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteApiTest {
    private val host = "192.168.1.20:8765"
    private val gate = AccessGate({ 0L }).apply { setOpen(setOf(ClientKind.Phone, ClientKind.Assistant)) }
    private val tv = FakeTvControl()
    private var actions = 0
    private val api = RemoteApi(
        gate, tv, McpServer(tv, "1") { "x" }, { host },
        look = { RemoteLook("midnight", "ru", mapOf("web_ok" to "ОК")) },
        asset = { path -> if (path.startsWith("remote/")) "file:$path".toByteArray() else null },
        font = { byteArrayOf(9) },
        onUserAction = { actions++ },
    )

    private fun req(method: String, path: String, body: String = "", key: String? = null, headers: Map<String, String> = mapOf("host" to host)) =
        api.handle(HttpRequest(method, path, emptyMap(), headers + listOfNotNull(key?.let { "authorization" to "Bearer $it" }), body.toByteArray()))

    private fun pair(kind: String, code: String = gate.code) = req("POST", "/api/pair", """{"code":"$code","kind":"$kind"}""")

    private fun key(kind: String) = Json.parseToJsonElement(pair(kind).body.decodeToString()).jsonObject["key"]!!.jsonPrimitive.content

    @Test fun staticFilesComeOnlyFromTheList() {
        val page = req("GET", "/")
        assertEquals(200, page.status)
        assertEquals("file:remote/index.html", page.body.decodeToString())
        assertTrue(page.headers["Content-Security-Policy"]!!.contains("script-src 'self'"))
        assertEquals(200, req("GET", "/assets/fonts/Nunito-Variable.ttf").status)
        assertEquals("file:remote/assets/bg/sky.svg", req("GET", "/assets/bg/sky.svg").body.decodeToString())
        assertEquals(404, req("GET", "/assets/bg/Sky.SVG").status)
        assertEquals(404, req("GET", "/index.html").status)
        assertEquals(405, req("POST", "/").status)
    }

    @Test fun hostAndOriginMustBeTheTv() {
        assertEquals(403, req("GET", "/", headers = mapOf("host" to "evil.example:8765")).status)
        assertEquals(403, req("GET", "/api/look", headers = mapOf("host" to host, "origin" to "http://evil.example")).status)
        assertEquals(200, req("GET", "/api/look", headers = mapOf("host" to host, "origin" to "http://$host")).status)
    }

    @Test fun pairingAnswersWithStatusCodes() {
        assertEquals(403, pair("phone", "000000").status)
        assertEquals(400, pair("toaster").status)
        assertEquals(400, req("POST", "/api/pair", "not json").status)
        assertEquals(200, pair("phone").status)
        gate.setOpen(setOf(ClientKind.Phone))
        assertEquals(409, pair("assistant").status)
        repeat(5) { pair("phone", "000000") }
        assertEquals(429, pair("phone").status)
    }

    @Test fun phoneRoutesNeedAPhoneKey() {
        assertEquals(401, req("POST", "/api/key", """{"key":"up"}""").status)
        val assistant = key("assistant")
        assertEquals(401, req("POST", "/api/key", """{"key":"up"}""", key = assistant).status)
        val phone = key("phone")
        assertEquals(200, req("POST", "/api/key", """{"key":"up"}""", key = phone).status)
        assertEquals("key up 1", tv.calls.last())
        assertEquals(400, req("POST", "/api/key", """{"key":"power"}""", key = phone).status)
        assertEquals(200, req("POST", "/api/tap", """{"x":0.1,"y":0.2,"long":true}""", key = phone).status)
        assertEquals("tap 0.1 0.2 true", tv.calls.last())
        assertEquals(400, req("POST", "/api/tap", """{"x":0.1}""", key = phone).status)
        req("POST", "/api/text", """{"text":"${"a".repeat(600)}"}""", key = phone)
        assertEquals("type " + "a".repeat(500), tv.calls.last())
        assertEquals("image/jpeg", req("GET", "/api/frame", key = phone).contentType)
        tv.frame = null
        assertEquals(204, req("GET", "/api/frame", key = phone).status)
        assertTrue(actions > 0)
    }

    @Test fun phoneCanDisconnectItself() {
        val phone = key("phone")
        assertEquals(200, req("POST", "/api/disconnect", key = phone).status)
        assertEquals(401, req("GET", "/api/state", key = phone).status)
    }

    @Test fun mcpNeedsAnAssistantKey() {
        val init = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
        val denied = req("POST", "/mcp", init)
        assertEquals(401, denied.status)
        assertEquals("Bearer", denied.headers["WWW-Authenticate"])
        assertEquals(401, req("POST", "/mcp", init, key = key("phone")).status)
        val assistant = key("assistant")
        assertEquals(200, req("POST", "/mcp", init, key = assistant).status)
        assertEquals(202, req("POST", "/mcp", """{"jsonrpc":"2.0","method":"notifications/initialized"}""", key = assistant).status)
        assertEquals(405, req("GET", "/mcp", key = assistant).status)
    }

    @Test fun wipeEndsEverySession() {
        val phone = key("phone")
        val assistant = key("assistant")
        gate.wipe()
        gate.setOpen(setOf(ClientKind.Phone, ClientKind.Assistant))
        assertEquals(401, req("GET", "/api/state", key = phone).status)
        assertEquals(401, req("POST", "/mcp", """{"jsonrpc":"2.0","id":1,"method":"ping"}""", key = assistant).status)
    }
}
