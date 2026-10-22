package io.github.marozauartsiom.tvlauncher.data.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns an app's picture into a 320x180 px tile (xhdpi for the 160x90 dp card) off the main thread,
 * and keeps the results in an LRU cache measured in bytes. Apps with a TV banner use it as is. Apps
 * with only an icon get a generated tile: the icon on a surface tinted from the icon, with the app
 * name drawn by the UI under it.
 */
class BannerLoader(
    private val source: PackageSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    maxBytes: Int = 16 * 1024 * 1024,
) {
    private val cache = object : LruCache<String, BannerArt>(maxBytes) {
        override fun sizeOf(key: String, value: BannerArt) = value.bitmap.asAndroidBitmap().allocationByteCount
    }

    fun peek(app: TvApp): BannerArt? = cache.get(app.packageName)

    suspend fun load(app: TvApp): BannerArt {
        cache.get(app.packageName)?.let { return it }
        val art = withContext(dispatcher) { render(app) }
        cache.put(app.packageName, art)
        return art
    }

    fun clear() = cache.evictAll()

    private fun render(app: TvApp): BannerArt {
        val loaded = runCatching { source.loadArt(app) }.getOrNull()
        return when {
            loaded == null -> generated(null)
            loaded.isBanner -> BannerArt(loaded.drawable.toBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).asImageBitmap(), isGenerated = false)
            else -> generated(loaded.drawable)
        }
    }

    private fun generated(icon: Drawable?): BannerArt {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val iconBitmap = icon?.toBitmap(ICON, ICON, Bitmap.Config.ARGB_8888)
        canvas.drawColor(iconBitmap?.let(::tintFrom) ?: NEUTRAL)
        iconBitmap?.let { canvas.drawBitmap(it, (WIDTH - ICON) / 2f, ICON_TOP.toFloat(), null) }
        return BannerArt(bitmap.asImageBitmap(), isGenerated = true)
    }

    companion object {
        const val WIDTH = 320
        const val HEIGHT = 180
        const val ICON = 72
        const val ICON_TOP = 32
        private val NEUTRAL = Color.rgb(0x3B, 0x3F, 0x4A)

        /** A dark version of the icon's colour, so white text on it stays readable. */
        fun tintFrom(icon: Bitmap): Int {
            val palette = Palette.from(icon).maximumColorCount(8).generate()
            val base = palette.darkMutedSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: palette.dominantSwatch?.rgb ?: return NEUTRAL
            val hsl = FloatArray(3).also { ColorUtils.colorToHSL(base, it) }
            hsl[1] = hsl[1].coerceAtMost(0.55f)
            hsl[2] = hsl[2].coerceIn(0.18f, 0.30f)
            return ColorUtils.HSLToColor(hsl)
        }
    }
}
