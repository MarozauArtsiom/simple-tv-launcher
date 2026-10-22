package io.github.marozauartsiom.tvlauncher.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.marozauartsiom.tvlauncher.data.background.BackgroundDecoder
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes background bitmaps once, off the main thread, at 960x540 (about 2 MB each), and keeps at most
 * two (the current one and the previous one during a crossfade). Theme art comes from resources, the
 * owner's picture from a file; a colour needs no bitmap.
 */
class BackgroundCache(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val bitmaps = LinkedHashMap<Any, ImageBitmap>()

    private fun keyOf(spec: BackgroundSpec): Any? = when (spec) {
        is ImageBackground -> spec.resId
        is FileBackground -> spec.path
        else -> null
    }

    @Synchronized
    fun peek(spec: BackgroundSpec): ImageBitmap? = keyOf(spec)?.let { bitmaps[it] }

    suspend fun load(spec: BackgroundSpec): ImageBitmap? {
        val key = keyOf(spec) ?: return null
        peek(spec)?.let { return it }
        val decoded = withContext(dispatcher) {
            when (spec) {
                is ImageBackground -> decode(spec.resId)
                is FileBackground -> BackgroundDecoder.cover(File(spec.path))?.asImageBitmap()
                else -> null
            }
        } ?: return null
        synchronized(this) {
            bitmaps[key] = decoded
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
