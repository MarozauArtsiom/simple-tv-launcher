package io.github.marozauartsiom.tvlauncher.system.remote

import io.github.marozauartsiom.tvlauncher.system.remote.control.ActionResult
import io.github.marozauartsiom.tvlauncher.system.remote.control.AppEntry
import io.github.marozauartsiom.tvlauncher.system.remote.control.RemoteKey
import io.github.marozauartsiom.tvlauncher.system.remote.control.TvControl

/** Records every call; answers with simple fixed results. */
class FakeTvControl : TvControl {
    val calls = mutableListOf<String>()
    var frame: ByteArray? = byteArrayOf(1, 2, 3)
    override var helperReady = true
    var adb = false

    private fun ok(call: String): ActionResult { calls += call; return ActionResult.ok("done: $call") }

    override fun adbReady() = adb
    override fun screenshot(width: Int, quality: Int) = frame.also { calls += "screenshot" }
    override fun readScreen() = "\"Search\" at 0.500, 0.100 [button]\n"
    override fun tap(x: Float, y: Float, long: Boolean) = ok("tap $x $y $long")
    override fun tapText(text: String) = if (text == "missing") ActionResult.fail("not found") else ok("tapText $text")
    override fun scroll(x: Float?, y: Float?, direction: String) = ok("scroll $x $y $direction")
    override fun typeText(text: String, x: Float?, y: Float?) = ok("type $text")
    override fun enter(x: Float?, y: Float?) = ok("enter")
    override fun key(key: RemoteKey, times: Int) = ok("key ${key.wire} $times")
    override fun listApps() = listOf(AppEntry("Kinopub", "com.kinopub"))
    override fun openApp(name: String) = ok("open $name")
    override fun openUrl(url: String) = ok("url $url")
    override fun webSearch(query: String) = ok("web $query")
    override fun youtubeSearch(query: String) = ok("yt $query")
    override fun youtubePlay(video: String) = ok("play $video")
    override fun powerOff() = ok("off")
    override fun sleepTimer(minutes: Int) = ok("sleep $minutes")
    override fun foregroundApp() = "com.kinopub"
}
