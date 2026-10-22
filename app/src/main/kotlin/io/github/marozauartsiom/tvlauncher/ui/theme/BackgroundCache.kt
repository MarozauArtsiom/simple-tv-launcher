package io.github.marozauartsiom.tvlauncher.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes theme background bitmaps once, off the main thread, at 960x540 (about 2 MB each), and
 * keeps at most two (the current one and the previous one during a crossfade).
 */
class BackgroundCache(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val bitmaps = LinkedHashMap<Int, ImageBitmap>()

    @Synchronized
    fun peek(spec: BackgroundSpec): ImageBitmap? = (spec as? ImageBackground)?.let { bitmaps[it.resId] }

    suspend fun load(spec: BackgroundSpec): ImageBitmap? {
        val image = spec as? ImageBackground ?: return null
        peek(spec)?.let { return it }
        val decoded = withContext(dispatcher) { decode(image.resId) } ?: return null
        synchronized(this) {
            bitmaps[image.resId] = decoded
            while (bitmaps.size > 2) bitmaps.remove(bitmaps.keys.first())
        }
        return decoded
    }

    @Synchronized
    fun clear() = bitmaps.clear()

    private fun decode(resId: Int): ImageBitmap? = runCatching {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 2 // sources are 1920x1080; decode at 960x540
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        BitmapFactory.decodeResource(context.resources, resId, options)?.asImageBitmap()
    }.getOrNull()
}
