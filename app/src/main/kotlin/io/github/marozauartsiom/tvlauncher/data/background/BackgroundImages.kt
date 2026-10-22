package io.github.marozauartsiom.tvlauncher.data.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An image found on a USB drive. */
data class PickedImage(val path: String, val name: String)

/** Decoding of pictures for backgrounds: always cropped to cover 960×540, whatever the source size. */
object BackgroundDecoder {
    const val WIDTH = 960
    const val HEIGHT = 540
    const val THUMB_WIDTH = 296
    const val THUMB_HEIGHT = 166

    /** Pictures with more pixels than this are refused (the decoder would need too much memory even sampled). */
    private const val MAX_PIXELS = 150_000_000L

    /** [file] scaled and centre-cropped to [width]×[height], rotated as its EXIF says, or null if it cannot be read. */
    fun cover(file: File, width: Int = WIDTH, height: Int = HEIGHT): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0 || w.toLong() * h > MAX_PIXELS) return null

        val rotation = rotationOf(file)
        val (srcW, srcH) = if (rotation % 180 != 0) h to w else w to h
        var sample = 1
        while (srcW / (sample * 2) >= width && srcH / (sample * 2) >= height) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: return null

        val matrix = Matrix().apply { if (rotation != 0) postRotate(rotation.toFloat()) }
        val upright = if (rotation != 0) Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true) else decoded
        val scale = maxOf(width / upright.width.toFloat(), height / upright.height.toFloat())
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val m = Matrix().apply {
            postScale(scale, scale)
            postTranslate((width - upright.width * scale) / 2f, (height - upright.height * scale) / 2f)
        }
        Canvas(out).drawBitmap(upright, m, Paint(Paint.FILTER_BITMAP_FLAG))
        if (upright !== decoded) upright.recycle()
        decoded.recycle()
        out
    }.getOrNull()

    fun thumbnail(file: File): Bitmap? = cover(file, THUMB_WIDTH, THUMB_HEIGHT)

    private fun rotationOf(file: File): Int = runCatching {
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)
}

/**
 * Finds pictures on removable storage and keeps the one the owner picked. [roots] are the mounted
 * removable volumes (resolved when asked, so a newly inserted drive is seen). The chosen picture is
 * copied into [storeDir] at 960×540, so the background survives pulling the drive out.
 */
class BackgroundImages(
    private val roots: () -> List<File>,
    private val storeDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Up to [MAX_IMAGES] pictures from the top three folder levels of each volume, sorted by name. */
    suspend fun scan(): List<PickedImage> = withContext(io) {
        val found = ArrayList<PickedImage>()
        for (root in roots()) walk(root, depth = 0, into = found)
        found.sortedBy { it.name.lowercase() }.take(MAX_IMAGES)
    }

    private fun walk(dir: File, depth: Int, into: MutableList<PickedImage>) {
        if (into.size >= MAX_IMAGES * 2) return
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (child in children) {
            if (child.name.startsWith(".")) continue
            if (child.isDirectory) {
                if (depth < MAX_DEPTH && child.name !in SKIPPED_DIRS) walk(child, depth + 1, into)
            } else if (child.isFile && child.extension.lowercase() in EXTENSIONS && child.length() in MIN_BYTES..MAX_BYTES) {
                into += PickedImage(child.path, child.name)
            }
        }
    }

    /** Copies [source] into app storage, cropped to [width]×[height], replacing the previous one; returns the stored file name. */
    suspend fun import(source: File, width: Int = BackgroundDecoder.WIDTH, height: Int = BackgroundDecoder.HEIGHT): String? = withContext(io) {
        val bitmap = BackgroundDecoder.cover(source, width, height) ?: return@withContext null
        try {
            storeDir.mkdirs()
            val name = "bg_${System.currentTimeMillis()}.webp"
            val target = File(storeDir, name)
            FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, it) }
            storeDir.listFiles()?.filter { it.name != name && NAME.matches(it.name) }?.forEach { it.delete() }
            name
        } catch (e: java.io.IOException) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    /** The stored picture for a saved name, if it is one of ours and still there. */
    fun fileFor(name: String): File? = name.takeIf { NAME.matches(it) }?.let { File(storeDir, it) }?.takeIf { it.isFile }

    /** Removes the stored picture (the owner went back to a theme or colour background). */
    suspend fun clear() = withContext(io) { storeDir.listFiles()?.filter { NAME.matches(it.name) }?.forEach { it.delete() } }

    companion object {
        const val MAX_IMAGES = 36
        private const val MAX_DEPTH = 2
        private const val MIN_BYTES = 64L
        private const val MAX_BYTES = 60L * 1024 * 1024
        private val EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        private val SKIPPED_DIRS = setOf("Android", "LOST.DIR", "System Volume Information", "\$RECYCLE.BIN")
        private val NAME = Regex("bg_\\d{1,16}\\.webp")
    }
}
