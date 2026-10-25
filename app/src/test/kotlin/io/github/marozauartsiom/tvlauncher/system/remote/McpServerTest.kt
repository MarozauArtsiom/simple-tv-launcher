package io.github.marozauartsiom.tvlauncher.system.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpServerTest {
    private val tv = FakeTvControl()
    private val mcp = McpServer(tv, "1.2.3") { "B64(${it.size})" }

    private fun rpc(method: String, params: String = "{}"): JsonObject =
        Json.parseToJsonElement(mcp.handle("""{"jsonrpc":"2.0","id":7,"method":"$method","params":$params}""")!!).jsonObject

    private fun call(tool: String, args: String = "{}") = rpc("tools/call", """{"name":"$tool","arguments":$args}""")["result"]!!.jsonObject

    private fun JsonObject.text() = this["content"]!!.jsonArray.first().jsonObject["text"]!!.jsonPrimitive.content
    private fun JsonObject.isError() = this["isError"]?.jsonPrimitive?.boolean == true

    @Test fun initializeEchoesTheClientsVersionAndNamesTheServer() {
        val r = rpc("initialize", """{"protocolVersion":"2025-03-26"}""")
        assertEquals(7, r["id"]!!.jsonPrimitive.int)
        val result = r["result"]!!.jsonObject
        assertEquals("2025-03-26", result["protocolVersion"]!!.jsonPrimitive.content)
        assertEquals("1.2.3", result["serverInfo"]!!.jsonObject["version"]!!.jsonPrimitive.content)
        assertTrue(result["capabilities"]!!.jsonObject.containsKey("tools"))
    }

    @Test fun notificationsGetNoAnswer() {
        assertNull(mcp.handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""))
    }

    @Test fun listsEveryToolWithASchema() {
        val tools = rpc("tools/list")["result"]!!.jsonObject["tools"]!!.jsonArray.map { it.jsonObject }
        val names = tools.map { it["name"]!!.jsonPrimitive.content }.toSet()
        assertEquals(
            setOf("list_apps", "open_app", "youtube_search", "youtube_play", "open_url", "web_search", "screenshot", "read_screen", "tap", "tap_text", "scroll", "type_text", "press_key", "power_off", "sleep_timer", "get_status"),
            names,
        )
        tools.forEach { assertEquals("object", it["inputSchema"]!!.jsonObject["type"]!!.jsonPrimitive.content) }
    }

    @Test fun toolsCallTheTv() {
        assertEquals("done: open kinopub", call("open_app", """{"name":"kinopub"}""").text())
        assertEquals("done: yt коты", call("youtube_search", """{"query":"коты"}""").text())
        assertEquals("done: key right 2", call("press_key", """{"key":"right","times":2}""").text())
        assertEquals("done: type погода; done: enter", call("type_text", """{"text":"погода","submit":true}""").text())
        assertEquals("done: tap 0.5 0.25 false", call("tap", """{"x":0.5,"y":0.25}""").text())
        assertTrue(call("list_apps").text().contains("Kinopub (com.kinopub)"))
    }

    @Test fun screenshotIsAnImage() {
        val content = call("screenshot")["content"]!!.jsonArray.first().jsonObject
        assertEquals("image", content["type"]!!.jsonPrimitive.content)
        assertEquals("B64(3)", content["data"]!!.jsonPrimitive.content)
        tv.frame = null
        assertTrue(call("screenshot").isError())
    }

    @Test fun badArgumentsAndFailuresAreToolErrors() {
        assertTrue(call("open_app").isError())
        assertTrue(call("press_key", """{"key":"power"}""").isError())
        assertTrue(call("tap_text", """{"text":"missing"}""").isError())
        assertTrue(tv.calls.none { it.startsWith("open") || it.startsWith("key") })
    }

    @Test fun protocolErrors() {
        assertEquals(-32602, rpc("tools/call", """{"name":"rm_rf"}""")["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
        assertEquals(-32601, rpc("resources/list")["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
        assertEquals(-32700, Json.parseToJsonElement(mcp.handle("{oops")!!).jsonObject["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
    }

    @Test fun batchesAnswerEachRequest() {
        val out = Json.parseToJsonElement(mcp.handle("""[{"jsonrpc":"2.0","id":1,"method":"ping"},{"jsonrpc":"2.0","method":"notifications/x"}]""")!!)
        assertEquals(1, (out as JsonArray).size)
    }
}
