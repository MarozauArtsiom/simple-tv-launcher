package io.github.marozauartsiom.tvlauncher.system.remote

import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpRequest
import io.github.marozauartsiom.tvlauncher.system.remote.http.HttpResponse
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The routes reachable from the internet through the tunnel, for AI assistants only. It is a thin gate in front
 * of [RemoteApi]: nothing is answered unless the path starts with `/s/<secret>/`, where the secret is 128 random
 * bits made each time the switch is turned on. A request without it gets a plain 404 and touches nothing (no
 * pairing attempt is counted, so strangers can't lock the owner out). Behind the secret only a few things exist:
 * the pairing page and its files, `/api/look`, `/api/pair` (assistants only, never a phone) and `/mcp`. The phone
 * remote, the screen picture and every other route stay on the home network.
 *
 * The pairing page uses relative links, so it works under the prefix as it is.
 */
class ExternalApi(
    private val inner: RemoteApi,
    private val secret: () -> String,
) {
    fun handle(req: HttpRequest): HttpResponse {
        val s = secret()
        val prefix = "/s/$s"
        // Compare the whole secret in constant time; the rest of the path is only looked at after it matched.
        val given = req.path.removePrefix("/s/").substringBefore('/')
        val ok = req.path.startsWith("/s/") && MessageDigest.isEqual(given.toByteArray(), s.toByteArray()) &&
            (req.path.length == prefix.length || req.path[prefix.length] == '/')
        if (!ok) return HttpResponse.empty(404)
        var path = req.path.removePrefix(prefix).ifEmpty { "/" }
        // Connectors in the Claude app can't send an Authorization header, so the key may ride in the path:
        // /s/<secret>/mcp/<key>. It is turned into the usual Bearer header here. (The relay sees it either way.)
        var headers = req.headers - "host" - "origin"
        if (path.startsWith("/mcp/") && path.length > 5) {
            headers = headers + ("authorization" to "Bearer " + path.removePrefix("/mcp/"))
            path = "/mcp"
        }
        val methods = ALLOWED[path] ?: if (path.startsWith("/assets/bg/")) GET else return HttpResponse.empty(404)
        if (req.method !in methods) return HttpResponse.empty(405)
        val forwarded = HttpRequest(
            req.method,
            if (path == "/") "/pair" else path,
            req.query,
            headers,
            if (path == "/api/pair") forceAssistant(req.bodyText).toByteArray() else req.body,
        )
        return inner.handleExternal(forwarded)
    }

    /** Pairing from the internet is for assistants: the kind is set here whatever the client wrote. */
    private fun forceAssistant(body: String): String {
        val json = try { Json.parseToJsonElement(body) as? JsonObject } catch (e: Exception) { null } ?: JsonObject(emptyMap())
        return JsonObject(json + ("kind" to JsonPrimitive("assistant"))).toString()
    }

    private companion object {
        val GET = setOf("GET", "HEAD")
        val POST = setOf("POST")
        val ALLOWED: Map<String, Set<String>> = mapOf(
            "/" to GET, "/pair" to GET, "/pair.js" to GET, "/remote.css" to GET, "/themes.css" to GET,
            "/assets/landscape-placeholder.svg" to GET, "/assets/fonts/Nunito-Variable.ttf" to GET,
            "/api/look" to GET, "/api/pair" to POST, "/mcp" to POST,
        )
    }
}
