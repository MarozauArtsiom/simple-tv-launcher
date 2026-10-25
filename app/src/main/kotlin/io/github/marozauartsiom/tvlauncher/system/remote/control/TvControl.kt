package io.github.marozauartsiom.tvlauncher.system.remote.control

/** Keys a phone or an assistant can press. [wire] is the name used on the wire; [keyCode] is Android's. */
enum class RemoteKey(val wire: String, val keyCode: Int) {
    Up("up", 19), Down("down", 20), Left("left", 21), Right("right", 22), Ok("ok", 23),
    Back("back", 4), Home("home", 3), PlayPause("play_pause", 85),
    VolumeUp("volume_up", 24), VolumeDown("volume_down", 25), Mute("mute", 164);

    val isDpad: Boolean get() = this in setOf(Up, Down, Left, Right, Ok)

    companion object {
        fun fromWire(s: String?): RemoteKey? = entries.firstOrNull { it.wire == s?.trim()?.lowercase() }
    }
}

data class ActionResult(val ok: Boolean, val message: String) {
    companion object {
        fun ok(message: String) = ActionResult(true, message)
        fun fail(message: String) = ActionResult(false, message)
    }
}

data class AppEntry(val label: String, val packageName: String)

/**
 * Everything the phone page and the AI tools can do to the TV. One implementation talks to Android
 * ([AndroidTvControl]); tests use fakes. Coordinates are fractions of the screen (0..1), so the caller never
 * needs the TV's resolution. Calls block and are made from the server's worker threads.
 */
interface TvControl {
    /** The accessibility helper is enabled (screen picture, taps, text). */
    val helperReady: Boolean

    /** Real key events through the TV's own adb work (they reach every app, YouTube included). */
    fun adbReady(): Boolean

    /** A JPEG of the screen, at most about one per second (the system's limit); null if there is none. */
    fun screenshot(width: Int = 960, quality: Int = 60): ByteArray?

    /** The readable elements on screen, one per line, with their positions as fractions. */
    fun readScreen(): String

    fun tap(x: Float, y: Float, long: Boolean = false): ActionResult
    fun tapText(text: String): ActionResult
    fun scroll(x: Float?, y: Float?, direction: String): ActionResult
    fun typeText(text: String, x: Float? = null, y: Float? = null): ActionResult
    fun enter(x: Float? = null, y: Float? = null): ActionResult
    fun key(key: RemoteKey, times: Int = 1): ActionResult

    fun listApps(): List<AppEntry>
    fun openApp(name: String): ActionResult
    fun openUrl(url: String): ActionResult
    fun webSearch(query: String): ActionResult
    fun youtubeSearch(query: String): ActionResult
    fun youtubePlay(video: String): ActionResult
    fun powerOff(): ActionResult
    fun sleepTimer(minutes: Int): ActionResult

    /** The package of the app on screen, if known. */
    fun foregroundApp(): String?
}

/** Pure helpers shared by the real control and tests. */
object TvLinks {
    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val ID_IN_URL = Regex("(?:v=|youtu\\.be/|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})")

    /** A YouTube video id from an id or any usual YouTube link; null if there is none. */
    fun youtubeId(video: String): String? {
        val v = video.trim()
        if (VIDEO_ID.matches(v)) return v
        return ID_IN_URL.find(v)?.groupValues?.get(1)
    }

    fun youtubeSearchUrl(query: String): String = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(query.trim(), "UTF-8")

    fun webSearchUrl(query: String): String = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(query.trim(), "UTF-8")

    /** The best app for a spoken name: exact label, then label prefix, then label or package containing it. */
    fun matchApp(apps: List<AppEntry>, name: String): List<AppEntry> {
        val q = name.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        apps.filter { it.label.lowercase() == q || it.packageName.lowercase() == q }.takeIf { it.isNotEmpty() }?.let { return it }
        apps.filter { it.label.lowercase().startsWith(q) }.takeIf { it.isNotEmpty() }?.let { return it }
        return apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }
}
