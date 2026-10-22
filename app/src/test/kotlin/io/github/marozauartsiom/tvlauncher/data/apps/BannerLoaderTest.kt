package io.github.marozauartsiom.tvlauncher.data.apps

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class BannerLoaderTest {
    private class ArtSource(private val art: Map<String, LoadedArt?>) : PackageSource {
        var loads = 0
        override fun queryLaunchable() = emptyList<TvApp>()
        override fun loadArt(app: TvApp): LoadedArt? { loads++; return art[app.packageName] }
        override fun launchIntent(packageName: String): Intent? = null
        override fun registerChanges(onChange: () -> Unit): () -> Unit = {}
    }

    private val red = ColorDrawable(Color.rgb(200, 30, 30))

    @Test fun aBannerIsScaledToTheTileSizeAndNotMarkedGenerated() = runBlocking {
        val loader = BannerLoader(ArtSource(mapOf("a" to LoadedArt(red, isBanner = true))), Dispatchers.Unconfined)
        val art = loader.load(app("a"))
        assertFalse(art.isGenerated)
        assertEquals(320, art.bitmap.width); assertEquals(180, art.bitmap.height)
    }

    @Test fun anIconBecomesAGeneratedTileTintedFromTheIcon() = runBlocking {
        val loader = BannerLoader(ArtSource(mapOf("a" to LoadedArt(red, isBanner = false))), Dispatchers.Unconfined)
        val art = loader.load(app("a"))
        assertTrue(art.isGenerated)
        val corner = art.bitmap.asAndroidBitmap().getPixel(4, 4)
        assertTrue("tint is dark enough for white text", Color.red(corner) < 130)
        assertTrue("tint keeps the icon's hue", Color.red(corner) > Color.blue(corner))
    }

    @Test fun missingArtGivesTheNeutralTile() = runBlocking {
        val art = BannerLoader(ArtSource(emptyMap()), Dispatchers.Unconfined).load(app("zzz"))
        assertTrue(art.isGenerated)
        assertEquals(Color.rgb(0x3B, 0x3F, 0x4A), art.bitmap.asAndroidBitmap().getPixel(4, 4))
    }

    @Test fun theSecondLoadComesFromTheCache() = runBlocking {
        val source = ArtSource(mapOf("a" to LoadedArt(red, true)))
        val loader = BannerLoader(source, Dispatchers.Unconfined)
        val first = loader.load(app("a"))
        assertSame(first, loader.load(app("a")))
        assertEquals(1, source.loads)
    }

    @Test fun theCacheEvictsLeastRecentlyUsedBySize() = runBlocking {
        val tile = 320 * 180 * 4
        val source = ArtSource(mapOf("a" to LoadedArt(red, true), "b" to LoadedArt(red, true), "c" to LoadedArt(red, true)))
        val loader = BannerLoader(source, Dispatchers.Unconfined, maxBytes = tile * 2 + 1000)
        loader.load(app("a")); loader.load(app("b")); loader.peek(app("a")); loader.load(app("c"))
        assertNotNull(loader.peek(app("a"))); assertNull(loader.peek(app("b"))); assertNotNull(loader.peek(app("c")))
        loader.clear(); assertNull(loader.peek(app("a")))
    }

    @Test fun aCrashingSourceStillGivesATile() = runBlocking {
        val broken = object : PackageSource by ArtSource(emptyMap()) {
            override fun loadArt(app: TvApp): LoadedArt? = error("boom")
        }
        assertTrue(BannerLoader(broken, Dispatchers.Unconfined).load(app("a")).isGenerated)
    }
}
