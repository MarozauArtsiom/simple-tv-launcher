package io.github.marozauartsiom.tvlauncher.system.remote

import io.github.marozauartsiom.tvlauncher.system.remote.control.AdbCommands
import io.github.marozauartsiom.tvlauncher.system.remote.control.AppEntry
import io.github.marozauartsiom.tvlauncher.system.remote.control.RemoteKey
import io.github.marozauartsiom.tvlauncher.system.remote.control.TvLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TvLinksTest {
    @Test fun youtubeIdFromIdsAndLinks() {
        assertEquals("dQw4w9WgXcQ", TvLinks.youtubeId("dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", TvLinks.youtubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1"))
        assertEquals("dQw4w9WgXcQ", TvLinks.youtubeId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", TvLinks.youtubeId("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(TvLinks.youtubeId("cats"))
    }

    @Test fun searchUrlsAreEncoded() {
        assertEquals("https://www.youtube.com/results?search_query=%D0%BA%D0%BE%D1%82%D1%8B+%26+dogs", TvLinks.youtubeSearchUrl(" коты & dogs "))
        assertEquals("https://www.google.com/search?q=a%2Bb", TvLinks.webSearchUrl("a+b"))
    }

    @Test fun matchAppPrefersExactThenPrefixThenContains() {
        val apps = listOf(AppEntry("YouTube", "com.google.android.youtube.tv"), AppEntry("YouTube Kids", "com.google.android.youtube.tvkids"), AppEntry("Kinopub", "com.kinopub"))
        assertEquals(listOf(apps[0]), TvLinks.matchApp(apps, "youtube"))
        assertEquals(listOf(apps[2]), TvLinks.matchApp(apps, "kino"))
        assertEquals(listOf(apps[1]), TvLinks.matchApp(apps, "kids"))
        assertEquals(emptyList<AppEntry>(), TvLinks.matchApp(apps, " "))
    }

    @Test fun keysFromTheWire() {
        assertEquals(RemoteKey.Ok, RemoteKey.fromWire(" OK "))
        assertEquals(RemoteKey.PlayPause, RemoteKey.fromWire("play_pause"))
        assertNull(RemoteKey.fromWire("power"))
        assertNull(RemoteKey.fromWire(null))
    }

    @Test fun adbCommandsCanOnlyBeFixedTemplates() {
        assertEquals("input keyevent 19", AdbCommands.key(19))
        assertThrows(IllegalArgumentException::class.java) { AdbCommands.key(0) }
        assertEquals("input text 'hello%sworld'", AdbCommands.text("hello world"))
        assertNull(AdbCommands.text("a'; reboot; '"))
        assertNull(AdbCommands.text("\$(reboot)"))
        assertNull(AdbCommands.text("привет"))
        assertNull(AdbCommands.text(""))
        assertNull(AdbCommands.text("a".repeat(201)))
    }
}
