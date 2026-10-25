package io.github.marozauartsiom.tvlauncher.system.remote.control

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import io.github.marozauartsiom.tvlauncher.data.apps.PackageSource
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The real [TvControl]. What works on the Xiaomi TV A Pro (Android 11), from the 2026-10-25 spike:
 * - screen picture: the accessibility helper's `takeScreenshot`, at most once per ~1.1 s;
 * - taps and scrolling: actions on the accessibility node under the point (injected touch gestures are refused
 *   by this firmware); real touch through adb (`input tap`) only where no node can be pressed;
 * - text: `ACTION_SET_TEXT` into the field (any language), else `input text` through adb (Latin);
 * - keys: Back/Home as global actions; D-pad and OK in-process when the launcher is in front, else by moving
 *   accessibility focus, else real key events through adb (the only way into YouTube); volume and play/pause
 *   through the audio manager.
 */
class AndroidTvControl(
    private val context: Context,
    private val helper: () -> AccessibilityService?,
    private val adb: LoopbackAdb,
    private val packages: PackageSource,
    private val launcherActivity: () -> Activity?,
    private val powerOffAction: () -> Boolean,
    private val sleepTimerAction: (minutes: Int) -> Unit,
) : TvControl {
    private val main = Handler(Looper.getMainLooper())
    private val shotExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "remote-shot").apply { isDaemon = true } }
    private val shotLock = Any()
    private var lastShot: ByteArray? = null
    private var lastShotAt = 0L
    @Volatile private var adbKnownGood: Boolean? = null
    @Volatile private var adbProbing = false

    /** Told when a background adb check finishes, so the settings page can say which keys work. */
    @Volatile var onAdbResult: (Boolean) -> Unit = {}

    override val helperReady: Boolean get() = helper() != null

    /**
     * Whether real key events work. Never blocks a request: the first call starts a check in the background
     * (it may wait for the owner to allow debugging on the TV) and answers false until it succeeds.
     */
    override fun adbReady(): Boolean {
        adbKnownGood?.let { return it }
        if (!adbProbing) {
            adbProbing = true
            Thread({
                val ok = adb.probe()
                adbKnownGood = ok
                adbProbing = false
                onAdbResult(ok)
            }, "remote-adb-check").apply { isDaemon = true }.start()
        }
        return false
    }

    /** Forgets the adb state and closes the connection (access turned off, or the owner wants a new check). */
    fun reset() { adbKnownGood = null; adb.close() }

    private val screenW get() = context.resources.displayMetrics.widthPixels
    private val screenH get() = context.resources.displayMetrics.heightPixels
    private fun px(x: Float) = (x.coerceIn(0f, 1f) * (screenW - 1)).toInt()
    private fun py(y: Float) = (y.coerceIn(0f, 1f) * (screenH - 1)).toInt()

    // ---------- screen ----------

    override fun screenshot(width: Int, quality: Int): ByteArray? = synchronized(shotLock) {
        val now = SystemClock.elapsedRealtime()
        if (lastShot != null && now - lastShotAt < SHOT_INTERVAL_MS) return lastShot
        val service = helper() ?: return null
        val latch = CountDownLatch(1)
        var jpg: ByteArray? = null
        try {
            service.takeScreenshot(Display.DEFAULT_DISPLAY, shotExecutor, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    try {
                        val hw = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                        if (hw != null) {
                            val soft = hw.copy(Bitmap.Config.ARGB_8888, false)
                            hw.recycle()
                            val w = width.coerceIn(320, 1280)
                            val scaled = Bitmap.createScaledBitmap(soft, w, soft.height * w / soft.width, true)
                            if (scaled !== soft) soft.recycle()
                            val out = ByteArrayOutputStream(80 * 1024)
                            scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 90), out)
                            scaled.recycle()
                            jpg = out.toByteArray()
                        }
                    } finally {
                        result.hardwareBuffer.close()
                        latch.countDown()
                    }
                }

                override fun onFailure(errorCode: Int) { latch.countDown() }
            })
            latch.await(3, TimeUnit.SECONDS)
        } catch (e: RuntimeException) {
            return lastShot
        }
        jpg?.let { lastShot = it; lastShotAt = SystemClock.elapsedRealtime() }
        return jpg ?: lastShot
    }

    override fun readScreen(): String {
        val roots = roots()
        val app = foregroundApp()?.let { "App on screen: $it\n" }.orEmpty()
        return app + NodeOps.outline(roots, screenW, screenH)
    }

    // ---------- touch ----------

    override fun tap(x: Float, y: Float, long: Boolean): ActionResult {
        val roots = roots()
        val target = if (long) NodeOps.longTapTarget(roots, px(x), py(y)) else NodeOps.tapTarget(roots, px(x), py(y))
        val action = if (long) AccessibilityNodeInfo.ACTION_LONG_CLICK else AccessibilityNodeInfo.ACTION_CLICK
        if (target != null && (target as A11yNode).info.performAction(action)) return ActionResult.ok("pressed ${describe(target)}")
        // Nothing the accessibility service can press there (custom-drawn apps): a real touch through adb.
        if (adbReady()) {
            val cmd = if (long) "input swipe ${px(x)} ${py(y)} ${px(x)} ${py(y)} 700" else "input tap ${px(x)} ${py(y)}"
            if (adb.run(cmd)) return ActionResult.ok("touched the screen at ${px(x)},${py(y)}")
        }
        NodeOps.ancestorOrSelf(NodeOps.nodeAt(roots, px(x), py(y))) { it.isFocusable }?.let {
            if ((it as A11yNode).info.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) return ActionResult.ok("focused ${describe(it)}")
        }
        return ActionResult.fail("nothing to press at that point")
    }

    override fun tapText(text: String): ActionResult {
        val target = NodeOps.findByText(roots(), text) ?: return ActionResult.fail("no element with the text \"$text\" on screen")
        val info = (target as A11yNode).info
        if (info.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return ActionResult.ok("pressed ${describe(target)}")
        val b = target.bounds
        return tap(b.centerX().toFloat() / screenW, b.centerY().toFloat() / screenH)
    }

    override fun scroll(x: Float?, y: Float?, direction: String): ActionResult {
        val dir = direction.trim().lowercase()
        val roots = roots()
        val target = NodeOps.scrollTarget(roots, px(x ?: 0.5f), py(y ?: 0.5f))
        if (target != null) {
            val info = (target as A11yNode).info
            val directional = when (dir) {
                "up" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP
                "down" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN
                "left" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT
                "right" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT
                else -> return ActionResult.fail("direction must be up, down, left or right")
            }
            val forward = dir == "down" || dir == "right"
            if (info.performAction(directional.id) || info.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                return ActionResult.ok("scrolled $dir")
            }
        }
        if (adbReady()) {
            val cx = px(x ?: 0.5f); val cy = py(y ?: 0.5f)
            val d = (screenH * 0.3f).toInt()
            val (x2, y2) = when (dir) { "down" -> cx to cy - d; "up" -> cx to cy + d; "right" -> cx - d to cy; else -> cx + d to cy }
            if (adb.run("input swipe $cx $cy ${x2.coerceIn(0, screenW - 1)} ${y2.coerceIn(0, screenH - 1)} 300")) return ActionResult.ok("swiped $dir")
        }
        // Last resort: the D-pad moves through lists in every TV app.
        return key(when (dir) { "up" -> RemoteKey.Up; "down" -> RemoteKey.Down; "left" -> RemoteKey.Left; else -> RemoteKey.Right }, 3)
    }

    // ---------- text ----------

    override fun typeText(text: String, x: Float?, y: Float?): ActionResult {
        val field = NodeOps.editTarget(roots(), x?.let(::px), y?.let(::py))
        if (field != null) {
            val info = (field as A11yNode).info
            info.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            if (info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return ActionResult.ok("typed into ${describe(field)}")
        }
        val cmd = AdbCommands.text(text)
        if (cmd != null && adbReady() && adb.run(cmd)) return ActionResult.ok("typed with the keyboard")
        return if (cmd == null && field == null) ActionResult.fail("no text field on screen that accepts this text (this app takes only Latin letters typed as keys)")
        else ActionResult.fail("no text field on screen; select one first")
    }

    override fun enter(x: Float?, y: Float?): ActionResult {
        val field = NodeOps.editTarget(roots(), x?.let(::px), y?.let(::py))
        if (field != null && (field as A11yNode).info.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) {
            return ActionResult.ok("pressed Enter in ${describe(field)}")
        }
        if (adbReady() && adb.run(AdbCommands.key(KeyEvent.KEYCODE_ENTER))) return ActionResult.ok("pressed Enter")
        return ActionResult.fail("no text field to submit")
    }

    // ---------- keys ----------

    override fun key(key: RemoteKey, times: Int): ActionResult {
        var last = ActionResult.fail("not pressed")
        repeat(times.coerceIn(1, 20)) { last = keyOnce(key); if (!last.ok) return last }
        return last
    }

    private fun keyOnce(key: RemoteKey): ActionResult {
        when (key) {
            RemoteKey.Back -> helper()?.let { if (it.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)) return ActionResult.ok("Back") }
            RemoteKey.Home -> helper()?.let { if (it.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)) return ActionResult.ok("Home") }
            RemoteKey.VolumeUp, RemoteKey.VolumeDown, RemoteKey.Mute -> {
                val audio = context.getSystemService(AudioManager::class.java)
                val direction = when (key) { RemoteKey.VolumeUp -> AudioManager.ADJUST_RAISE; RemoteKey.VolumeDown -> AudioManager.ADJUST_LOWER; else -> AudioManager.ADJUST_TOGGLE_MUTE }
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
                return ActionResult.ok(key.wire)
            }
            RemoteKey.PlayPause -> {
                val audio = context.getSystemService(AudioManager::class.java)
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key.keyCode))
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key.keyCode))
                return ActionResult.ok("play/pause")
            }
            else -> Unit
        }
        if (key.isDpad) {
            // Our own launcher: the key goes straight into its window, exactly like the remote.
            launcherActivity()?.let { activity ->
                // Injected into our own window through the input system, exactly like the remote's key
                // (an app may inject into its own windows); a direct dispatch if that is refused.
                val injected = runCatching { android.app.Instrumentation().sendKeyDownUpSync(key.keyCode) }.isSuccess
                if (!injected) main.post {
                    val now = SystemClock.uptimeMillis()
                    fun ev(action: Int) = KeyEvent(now, now, action, key.keyCode, 0, 0, android.view.KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, android.view.InputDevice.SOURCE_DPAD)
                    activity.dispatchKeyEvent(ev(KeyEvent.ACTION_DOWN))
                    activity.dispatchKeyEvent(ev(KeyEvent.ACTION_UP))
                }
                return ActionResult.ok(key.wire)
            }
            accessibilityDpad(key)?.let { return it }
        }
        if (adbReady() && adb.run(AdbCommands.key(key.keyCode))) return ActionResult.ok(key.wire)
        return ActionResult.fail("this app doesn't take ${key.wire} from the remote helper; turn on network debugging for full keys")
    }

    /** D-pad by moving the input focus between accessibility nodes; null when that doesn't work in this app. */
    private fun accessibilityDpad(key: RemoteKey): ActionResult? {
        val roots = roots()
        val focus = roots.firstNotNullOfOrNull { NodeOps.firstMatching(it) { n -> n.isFocused } } as A11yNode? ?: return null
        if (key == RemoteKey.Ok) {
            val clickable = NodeOps.ancestorOrSelf(focus) { it.isClickable } as A11yNode? ?: return null
            return if (clickable.info.performAction(AccessibilityNodeInfo.ACTION_CLICK)) ActionResult.ok("ok") else null
        }
        val dir = when (key) { RemoteKey.Up -> View.FOCUS_UP; RemoteKey.Down -> View.FOCUS_DOWN; RemoteKey.Left -> View.FOCUS_LEFT; else -> View.FOCUS_RIGHT }
        val next = focus.info.focusSearch(dir) ?: return null
        return if (next.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) ActionResult.ok(key.wire) else null
    }

    // ---------- apps and links ----------

    override fun listApps(): List<AppEntry> = packages.queryLaunchable()
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .map { AppEntry(it.label, it.packageName) }
        .sortedBy { it.label.lowercase() }

    override fun openApp(name: String): ActionResult {
        val matches = TvLinks.matchApp(listApps(), name)
        val app = when {
            matches.isEmpty() -> return ActionResult.fail("no app called \"$name\"; use list_apps")
            matches.size > 1 && matches.none { it.label.equals(name, ignoreCase = true) } ->
                return ActionResult.fail("several apps match \"$name\": ${matches.joinToString { it.label }}")
            else -> matches.firstOrNull { it.label.equals(name, ignoreCase = true) } ?: matches.first()
        }
        val intent = packages.launchIntent(app.packageName) ?: return ActionResult.fail("${app.label} can't be opened")
        return start(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED), "opened ${app.label}")
    }

    override fun openUrl(url: String): ActionResult {
        val u = url.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
        val uri = Uri.parse(u)
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return ActionResult.fail("only web addresses can be opened")
        TvLinks.youtubeId(u)?.takeIf { uri.host?.contains("youtu") == true }?.let { return youtubePlay(it) }
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Straight into a browser: without a default one Android asks "Open with", and some apps (Kinopub) claim every link.
        browserPackage()?.let { intent.setPackage(it) }
        return start(intent, "opened $u")
    }

    /** The TV's browser: TV Bro if installed, else another known TV browser, else null (Android decides). */
    private fun browserPackage(): String? = BROWSERS.firstOrNull { packages.launchIntent(it) != null }

    override fun webSearch(query: String): ActionResult =
        if (query.isBlank()) ActionResult.fail("empty search") else openUrl(TvLinks.webSearchUrl(query))

    override fun youtubeSearch(query: String): ActionResult {
        if (query.isBlank()) return ActionResult.fail("empty search")
        return youtube(TvLinks.youtubeSearchUrl(query), "searching YouTube for \"$query\"")
    }

    override fun youtubePlay(video: String): ActionResult {
        val id = TvLinks.youtubeId(video) ?: return ActionResult.fail("not a YouTube video id or link")
        return youtube("https://www.youtube.com/watch?v=$id", "playing https://youtu.be/$id")
    }

    private fun youtube(url: String, message: String): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val tv = Intent(intent).setPackage(YOUTUBE_TV)
        return if (context.packageManager.resolveActivity(tv, 0) != null) start(tv, message) else start(intent, message)
    }

    override fun powerOff(): ActionResult =
        if (powerOffAction()) ActionResult.ok("turning the TV off") else ActionResult.fail("the TV helper is off")

    override fun sleepTimer(minutes: Int): ActionResult {
        if (minutes !in 0..240) return ActionResult.fail("minutes must be 0..240")
        sleepTimerAction(minutes)
        return ActionResult.ok(if (minutes == 0) "sleep timer cancelled" else "the TV turns off in $minutes min")
    }

    override fun foregroundApp(): String? = runCatching { helper()?.rootInActiveWindow?.packageName?.toString() }.getOrNull()

    private fun start(intent: Intent, message: String): ActionResult = try {
        (helper() ?: context).startActivity(intent)
        ActionResult.ok(message)
    } catch (e: ActivityNotFoundException) {
        ActionResult.fail("no app on the TV opens this")
    } catch (e: SecurityException) {
        ActionResult.fail("not allowed: ${e.message}")
    }

    // ---------- accessibility nodes ----------

    private fun roots(): List<UiNode> {
        val service = helper() ?: return emptyList()
        val windows = runCatching { service.windows.mapNotNull { it.root } }.getOrDefault(emptyList())
        val roots = windows.ifEmpty { listOfNotNull(service.rootInActiveWindow) }
        return roots.map { A11yNode(it, null) }
    }

    private fun describe(n: UiNode): String {
        val label = n.text?.takeIf { it.isNotBlank() } ?: n.description?.takeIf { it.isNotBlank() }
        return label?.let { "\"${it.take(60)}\"" } ?: n.className.substringAfterLast('.')
    }

    /** [UiNode] over an `AccessibilityNodeInfo`; children are read on demand. */
    private class A11yNode(val info: AccessibilityNodeInfo, override val parent: UiNode?) : UiNode {
        override val className: String get() = info.className?.toString().orEmpty()
        override val text: String? get() = info.text?.toString()
        override val description: String? get() = info.contentDescription?.toString()
        override val bounds: Rect by lazy { Rect().also(info::getBoundsInScreen) }
        override val isVisible: Boolean get() = info.isVisibleToUser
        override val isClickable: Boolean get() = info.isClickable
        override val isLongClickable: Boolean get() = info.isLongClickable
        override val isScrollable: Boolean get() = info.isScrollable
        override val isEditable: Boolean get() = info.isEditable
        override val isFocusable: Boolean get() = info.isFocusable
        override val isFocused: Boolean get() = info.isFocused
        override val children: List<UiNode> by lazy { (0 until info.childCount).mapNotNull { info.getChild(it) }.map { A11yNode(it, this) } }
    }

    private companion object {
        const val SHOT_INTERVAL_MS = 1_100L
        const val YOUTUBE_TV = "com.google.android.youtube.tv"
        val BROWSERS = listOf("com.phlox.tvwebbrowser", "com.tcl.browser", "com.android.chrome", "org.mozilla.firefox", "com.opera.browser")
    }
}
