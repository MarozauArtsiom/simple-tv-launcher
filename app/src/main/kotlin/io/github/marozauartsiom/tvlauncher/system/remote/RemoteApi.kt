package io.github.marozauartsiom.tvlauncher.system.remote

import io.github.marozauartsiom.tvlauncher.system.remote.control.RemoteKey
import io.github.marozauartsiom.tvlauncher.system.remote.control.TvControl
import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpRequest
import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** How the phone page should look: the TV's theme and language, and its strings in that language. */
data class RemoteLook(val theme: String, val lang: String, val strings: Map<String, String>)

/**
 * Every HTTP route of the remote. Pure routing over [AccessGate], [TvControl], [McpServer] and two providers,
 * so it is unit-tested without a socket.
 *
 * - Static files come from a fixed list (no paths from the request reach the file system).
 * - `/api/look` and `/api/pair` need no key; the phone's routes need a phone key, `/mcp` an assistant key.
 * - The `Host` header must be the TV's own address (stops DNS-rebinding pages), and a browser's `Origin`, when
 *   sent, must match it (stops other sites' scripts).
 */
class RemoteApi(
    private val gate: AccessGate,
    private val control: TvControl,
    private val mcp: McpServer,
    private val host: () -> String,
    private val look: () -> RemoteLook,
    private val asset: (String) -> ByteArray?,
    private val font: () -> ByteArray?,
    private val onUserAction: () -> Unit = {},
) {
    fun handle(req: HttpRequest): HttpResponse {
        val expectedHost = host()
        val reqHost = req.header("host")
        if (reqHost != null && reqHost != expectedHost) return HttpResponse.text(403, "wrong host")
        req.header("origin")?.let { if (it != "http://$expectedHost") return HttpResponse.text(403, "wrong origin") }

        STATIC[req.path]?.let { (file, type) ->
            if (req.method != "GET" && req.method != "HEAD") return HttpResponse.empty(405)
            val bytes = (if (file == FONT) font() else asset(file)) ?: return HttpResponse.empty(404)
            val headers = if (type.startsWith("text/html")) mapOf("Content-Security-Policy" to CSP) else emptyMap()
            return HttpResponse(200, type, bytes, headers)
        }
        if (req.path.startsWith("/assets/bg/")) {
            val name = req.path.removePrefix("/assets/bg/")
            if (!BG_NAME.matches(name)) return HttpResponse.empty(404)
            return asset("remote/assets/bg/$name")?.let { HttpResponse(200, "image/svg+xml", it) } ?: HttpResponse.empty(404)
        }

        return when (req.path) {
            "/api/look" -> lookJson()
            "/api/pair" -> if (req.method == "POST") pair(req) else HttpResponse.empty(405)
            "/mcp" -> mcp(req)
            else -> if (req.path.startsWith("/api/")) phone(req) else HttpResponse.text(404, "not found")
        }
    }

    private fun lookJson(): HttpResponse {
        val l = look()
        return HttpResponse.json(200, buildJsonObject {
            put("theme", l.theme); put("lang", l.lang)
            putJsonObject("strings") { l.strings.forEach { (k, v) -> put(k, v) } }
        }.toString())
    }

    private fun pair(req: HttpRequest): HttpResponse {
        val body = parse(req) ?: return HttpResponse.json(400, """{"error":"bad request"}""")
        val code = body.str("code").orEmpty()
        val kind = when (body.str("kind")) { "phone" -> ClientKind.Phone; "assistant" -> ClientKind.Assistant; else -> return HttpResponse.json(400, """{"error":"bad kind"}""") }
        val name = body.str("name") ?: if (kind == ClientKind.Phone) "Phone" else "AI assistant"
        return when (val r = gate.pair(code, kind, name)) {
            is PairResult.Paired -> { onUserAction(); HttpResponse.json(200, buildJsonObject { put("key", r.key); put("id", r.client.id) }.toString()) }
            is PairResult.WrongCode -> HttpResponse.json(403, buildJsonObject { put("error", "wrong"); put("left", r.attemptsLeft) }.toString())
            is PairResult.Locked -> HttpResponse.json(429, buildJsonObject { put("error", "locked"); put("seconds", r.secondsLeft) }.toString())
            PairResult.Closed -> HttpResponse.json(409, """{"error":"off"}""")
        }
    }

    private fun mcp(req: HttpRequest): HttpResponse {
        if (req.method != "POST") return HttpResponse.empty(405)
        gate.authorize(req.bearer, ClientKind.Assistant) ?: return HttpResponse(401, "application/json", """{"error":"pair this assistant on the TV first"}""".toByteArray(), mapOf("WWW-Authenticate" to "Bearer"))
        gate.touch(); onUserAction()
        val out = mcp.handle(req.bodyText) ?: return HttpResponse.empty(202)
        return HttpResponse.json(200, out)
    }

    private fun phone(req: HttpRequest): HttpResponse {
        val client = gate.authorize(req.bearer, ClientKind.Phone) ?: return HttpResponse.text(401, "not paired")
        if (req.method == "GET") return when (req.path) {
            "/api/state" -> lookJson()
            "/api/frame" -> control.screenshot(960, 60)?.let { HttpResponse(200, "image/jpeg", it) } ?: HttpResponse.empty(204)
            else -> HttpResponse.text(404, "not found")
        }
        if (req.method != "POST") return HttpResponse.empty(405)
        val b = parse(req) ?: JsonObject(emptyMap())
        gate.touch(); onUserAction()
        val result = when (req.path) {
            "/api/key" -> control.key(RemoteKey.fromWire(b.str("key")) ?: return HttpResponse.text(400, "unknown key"))
            "/api/tap" -> control.tap(b.num("x") ?: return bad(), b.num("y") ?: return bad(), b["long"]?.jsonPrimitive?.booleanOrNull ?: false)
            "/api/scroll" -> control.scroll(b.num("x"), b.num("y"), b.str("dir") ?: return bad())
            "/api/text" -> control.typeText((b.str("text") ?: return bad()).take(MAX_TEXT), b.num("x"), b.num("y"))
            "/api/enter" -> control.enter(b.num("x"), b.num("y"))
            "/api/disconnect" -> { gate.disconnect(client.id); return HttpResponse.json(200, "{}") }
            else -> return HttpResponse.text(404, "not found")
        }
        return HttpResponse.json(200, buildJsonObject { put("ok", result.ok); put("message", result.message) }.toString())
    }

    private fun bad() = HttpResponse.text(400, "bad request")

    private fun parse(req: HttpRequest): JsonObject? = try { Json.parseToJsonElement(req.bodyText) as? JsonObject } catch (e: Exception) { null }
    private fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.num(k: String) = this[k]?.jsonPrimitive?.floatOrNull

    companion object {
        private const val FONT = "<font>"
        private const val MAX_TEXT = 500
        private val BG_NAME = Regex("^[a-z0-9-]{1,40}\\.svg$")
        private const val CSP = "default-src 'self'; img-src 'self' blob: data:; style-src 'self'; script-src 'self'; connect-src 'self'; font-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"

        /** The only files the server hands out: request path → (asset path, content type). */
        val STATIC: Map<String, Pair<String, String>> = mapOf(
            "/" to ("remote/index.html" to "text/html; charset=utf-8"),
            "/pair" to ("remote/pair.html" to "text/html; charset=utf-8"),
            "/remote.js" to ("remote/remote.js" to "text/javascript; charset=utf-8"),
            "/pair.js" to ("remote/pair.js" to "text/javascript; charset=utf-8"),
            "/remote.css" to ("remote/remote.css" to "text/css; charset=utf-8"),
            "/themes.css" to ("remote/themes.css" to "text/css; charset=utf-8"),
            "/assets/landscape-placeholder.svg" to ("remote/assets/landscape-placeholder.svg" to "image/svg+xml"),
            "/assets/fonts/Nunito-Variable.ttf" to (FONT to "font/ttf"),
        )
    }
}
