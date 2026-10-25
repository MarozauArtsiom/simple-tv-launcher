package io.github.marozauartsiom.tvlauncher.system.remote

import android.util.Base64
import io.github.marozauartsiom.tvlauncher.system.remote.control.ActionResult
import io.github.marozauartsiom.tvlauncher.system.remote.control.RemoteKey
import io.github.marozauartsiom.tvlauncher.system.remote.control.TvControl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The MCP endpoint (Streamable HTTP, JSON responses, no sessions): `initialize`, `ping`, `tools/list`,
 * `tools/call`. Each tool is a thin call into [TvControl]. Only paired assistants reach this (see [RemoteApi]).
 * [encodeBase64] is swapped in unit tests, which run without Android's Base64.
 */
class McpServer(
    private val control: TvControl,
    private val version: String,
    private val encodeBase64: (ByteArray) -> String = { Base64.encodeToString(it, Base64.NO_WRAP) },
) {
    /** Returns the JSON response body, or null for notifications (answered with 202 and no body). */
    fun handle(body: String): String? {
        val element = try { Json.parseToJsonElement(body) } catch (e: Exception) { return error(JsonNull, -32700, "parse error").toString() }
        return when (element) {
            is JsonArray -> element.mapNotNull { (it as? JsonObject)?.let(::single) }.takeIf { it.isNotEmpty() }?.let { JsonArray(it).toString() }
            is JsonObject -> single(element)?.toString()
            else -> error(JsonNull, -32600, "invalid request").toString()
        }
    }

    private fun single(req: JsonObject): JsonElement? {
        val id = req["id"] ?: return null // a notification: nothing to answer
        val method = req["method"]?.jsonPrimitive?.contentOrNull ?: return error(id, -32600, "invalid request")
        val params = req["params"] as? JsonObject ?: JsonObject(emptyMap())
        return when (method) {
            "initialize" -> result(id, buildJsonObject {
                put("protocolVersion", params["protocolVersion"]?.jsonPrimitive?.contentOrNull ?: PROTOCOL)
                putJsonObject("capabilities") { putJsonObject("tools") { put("listChanged", false) } }
                putJsonObject("serverInfo") { put("name", "simple-tv-launcher"); put("title", "Simple TV Launcher"); put("version", version) }
                put("instructions", INSTRUCTIONS)
            })
            "ping" -> result(id, JsonObject(emptyMap()))
            "tools/list" -> result(id, buildJsonObject { put("tools", JsonArray(TOOLS.map { it.json() })) })
            "tools/call" -> {
                val name = params["name"]?.jsonPrimitive?.contentOrNull ?: return error(id, -32602, "missing tool name")
                val args = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())
                if (TOOLS.none { it.name == name }) return error(id, -32602, "unknown tool: $name")
                result(id, call(name, args))
            }
            else -> error(id, -32601, "method not found: $method")
        }
    }

    private fun call(name: String, a: JsonObject): JsonObject {
        fun s(key: String) = a[key]?.jsonPrimitive?.contentOrNull
        fun f(key: String) = a[key]?.jsonPrimitive?.floatOrNull
        fun i(key: String) = a[key]?.jsonPrimitive?.intOrNull
        fun b(key: String) = a[key]?.jsonPrimitive?.booleanOrNull
        fun need(key: String) = s(key) ?: throw IllegalArgumentException("missing \"$key\"")
        return try {
            when (name) {
                "list_apps" -> text(control.listApps().joinToString("\n") { "${it.label} (${it.packageName})" }.ifEmpty { "no apps" })
                "open_app" -> outcome(control.openApp(need("name")))
                "youtube_search" -> outcome(control.youtubeSearch(need("query")))
                "youtube_play" -> outcome(control.youtubePlay(need("video")))
                "open_url" -> outcome(control.openUrl(need("url")))
                "web_search" -> outcome(control.webSearch(need("query")))
                "screenshot" -> control.screenshot(960, 60)?.let { jpg ->
                    buildJsonObject {
                        putJsonArray("content") {
                            add(buildJsonObject { put("type", "image"); put("data", encodeBase64(jpg)); put("mimeType", "image/jpeg") })
                            add(buildJsonObject { put("type", "text"); put("text", "The TV screen (960×540). Paid video shows as black: it is copy-protected.") })
                        }
                    }
                } ?: errorText("no screen picture: the TV helper is off")
                "read_screen" -> text(control.readScreen())
                "tap" -> outcome(control.tap(f("x") ?: throw IllegalArgumentException("missing \"x\""), f("y") ?: throw IllegalArgumentException("missing \"y\""), b("long") ?: false))
                "tap_text" -> outcome(control.tapText(need("text")))
                "scroll" -> outcome(control.scroll(f("x"), f("y"), need("direction")))
                "type_text" -> {
                    val typed = control.typeText(need("text"), f("x"), f("y"))
                    if (typed.ok && b("submit") == true) outcome(control.enter(f("x"), f("y")).let { ActionResult(it.ok, typed.message + "; " + it.message) }) else outcome(typed)
                }
                "press_key" -> {
                    val key = RemoteKey.fromWire(s("key")) ?: throw IllegalArgumentException("unknown key; use one of ${RemoteKey.entries.joinToString { it.wire }}")
                    outcome(control.key(key, i("times") ?: 1))
                }
                "power_off" -> outcome(control.powerOff())
                "sleep_timer" -> outcome(control.sleepTimer(i("minutes") ?: throw IllegalArgumentException("missing \"minutes\"")))
                "get_status" -> text(buildString {
                    append("App on screen: ").append(control.foregroundApp() ?: "unknown").append('\n')
                    append("Screen and taps: ").append(if (control.helperReady) "available" else "off (the TV helper is not enabled)").append('\n')
                    append("Keys in every app (adb): ").append(if (control.adbReady()) "yes" else "no — keys work in the launcher and most apps, not in YouTube").append('\n')
                })
                else -> errorText("unknown tool")
            }
        } catch (e: IllegalArgumentException) {
            errorText(e.message ?: "bad arguments")
        }
    }

    private fun outcome(r: ActionResult) = if (r.ok) text(r.message) else errorText(r.message)

    private fun text(t: String, isError: Boolean = false) = buildJsonObject {
        putJsonArray("content") { add(buildJsonObject { put("type", "text"); put("text", t) }) }
        if (isError) put("isError", true)
    }

    private fun errorText(t: String) = text(t, isError = true)

    private fun result(id: JsonElement, result: JsonObject) = buildJsonObject { put("jsonrpc", "2.0"); put("id", id); put("result", result) }

    private fun error(id: JsonElement, code: Int, message: String) = buildJsonObject {
        put("jsonrpc", "2.0"); put("id", id)
        putJsonObject("error") { put("code", code); put("message", message) }
    }

    private class Tool(val name: String, val description: String, val props: List<Prop> = emptyList()) {
        fun json() = buildJsonObject {
            put("name", name)
            put("description", description)
            putJsonObject("inputSchema") {
                put("type", "object")
                putJsonObject("properties") { props.forEach { p -> put(p.name, p.schema()) } }
                putJsonArray("required") { props.filter { it.required }.forEach { add(it.name) } }
            }
        }
    }

    private class Prop(val name: String, val type: String, val description: String, val required: Boolean = true, val enum: List<String>? = null) {
        fun schema() = buildJsonObject {
            put("type", type)
            put("description", description)
            if (enum != null) put("enum", buildJsonArray { enum.forEach { add(JsonPrimitive(it)) } })
        }
    }

    companion object {
        const val PROTOCOL = "2025-06-18"

        val INSTRUCTIONS = """
            You control a Google TV (Android 11) in the user's home through its launcher.
            - To see what is on screen, call read_screen (text and buttons with positions) or screenshot (a picture, about one per second).
            - Positions are fractions of the screen: x and y from 0 to 1 (0,0 is the top-left corner). read_screen prints them for each element.
            - tap presses the element at a position; tap_text presses the element with that text. scroll scrolls lists and web pages.
            - type_text fills the text field that is selected or under x,y (any language). Set submit=true to press Enter.
            - YouTube draws its own interface: use youtube_search and youtube_play, then press_key (up/down/left/right/ok/back); typing there takes Latin letters only.
            - press_key home goes to the launcher. open_app opens an app by name (see list_apps). open_url and web_search use the TV's browser.
            - Paid video (Netflix, Kinopub playback) appears black in screenshots: it is copy-protected.
            - Ask before power_off.
        """.trimIndent()

        private val TOOLS = listOf(
            Tool("list_apps", "List the apps installed on the TV (name and package)."),
            Tool("open_app", "Open an app by its name, e.g. \"YouTube\", \"Kinopub\", \"TV Bro\".", listOf(Prop("name", "string", "App name or package"))),
            Tool("youtube_search", "Open YouTube on the TV with search results for a query (any language).", listOf(Prop("query", "string", "What to search for"))),
            Tool("youtube_play", "Play a YouTube video on the TV.", listOf(Prop("video", "string", "Video id or YouTube link"))),
            Tool("open_url", "Open a web address in the TV's browser.", listOf(Prop("url", "string", "http(s) address"))),
            Tool("web_search", "Search the web (Google) in the TV's browser.", listOf(Prop("query", "string", "What to search for"))),
            Tool("screenshot", "Take a picture of the TV screen (JPEG, 960×540). At most about one new picture per second."),
            Tool("read_screen", "List the text, buttons and fields on screen with their positions (x, y as 0..1 fractions). Empty for apps that draw their own UI, like YouTube."),
            Tool("tap", "Press the element at a position on screen (like touching it).", listOf(
                Prop("x", "number", "0..1 from the left"), Prop("y", "number", "0..1 from the top"),
                Prop("long", "boolean", "Long press", required = false),
            )),
            Tool("tap_text", "Press the element whose text or label matches.", listOf(Prop("text", "string", "Text shown on the element"))),
            Tool("scroll", "Scroll the list or page under a position (default: the middle of the screen).", listOf(
                Prop("direction", "string", "Where the content should move to show more", enum = listOf("up", "down", "left", "right")),
                Prop("x", "number", "0..1", required = false), Prop("y", "number", "0..1", required = false),
            )),
            Tool("type_text", "Type into the selected text field (or the one at x,y). Replaces its text.", listOf(
                Prop("text", "string", "The text"),
                Prop("submit", "boolean", "Press Enter afterwards", required = false),
                Prop("x", "number", "0..1", required = false), Prop("y", "number", "0..1", required = false),
            )),
            Tool("press_key", "Press a remote control key.", listOf(
                Prop("key", "string", "The key", enum = RemoteKey.entries.map { it.wire }),
                Prop("times", "integer", "How many times (1-20)", required = false),
            )),
            Tool("power_off", "Turn the TV off (standby). Ask the user first. This ends the connection."),
            Tool("sleep_timer", "Turn the TV off after some minutes; 0 cancels the timer.", listOf(Prop("minutes", "integer", "0..240"))),
            Tool("get_status", "What app is on screen and which control methods work right now."),
        )
    }
}
