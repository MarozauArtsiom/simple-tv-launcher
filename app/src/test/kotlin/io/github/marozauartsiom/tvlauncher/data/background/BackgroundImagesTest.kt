package io.github.marozauartsiom.tvlauncher.data.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class BackgroundImagesTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun usb(): File = File(tmp.root, "usb").apply { mkdirs() }
    private fun store(): File = File(tmp.root, "store")

    /** A real picture file: [left] half red, the rest blue, so crops and rotations can be told apart. */
    private fun picture(file: File, width: Int, height: Int, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): File {
        file.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) for (y in 0 until height step maxOf(1, height / 8)) bitmap.setPixel(x, y, if (x < width / 2) Color.RED else Color.BLUE)
        for (x in 0 until width) for (y in 0 until height) bitmap.setPixel(x, y, if (x < width / 2) Color.RED else Color.BLUE)
        FileOutputStream(file).use { bitmap.compress(format, 90, it) }
        return file
    }

    private fun images(vararg roots: File) = BackgroundImages({ roots.toList() }, store())

    // ---- scanning ----

    @Test fun scanFindsPicturesOfTheSupportedTypes() = runBlocking {
        picture(File(usb(), "a.png"), 40, 20)
        picture(File(usb(), "b.JPG"), 40, 20, Bitmap.CompressFormat.JPEG)
        picture(File(usb(), "c.webp"), 40, 20, Bitmap.CompressFormat.WEBP_LOSSY)
        picture(File(usb(), "d.jpeg"), 40, 20, Bitmap.CompressFormat.JPEG)
        File(usb(), "notes.txt").writeText("x".repeat(2000))
        File(usb(), "movie.mp4").writeBytes(ByteArray(5000))
        assertEquals(listOf("a.png", "b.JPG", "c.webp", "d.jpeg"), images(usb()).scan().map { it.name })
    }

    @Test fun scanLooksIntoFoldersButNotTooDeepAndSkipsHiddenOnes() = runBlocking {
        picture(File(usb(), "Pictures/trip/a.png"), 40, 20)         // depth 2: found
        picture(File(usb(), "a/b/c/deep.png"), 40, 20)              // depth 3: too deep
        picture(File(usb(), ".Trash/hidden.png"), 40, 20)           // hidden folder
        picture(File(usb(), "._mac.png"), 40, 20)                   // hidden file
        picture(File(usb(), "Android/data/x.png"), 40, 20)          // app data
        assertEquals(listOf("a.png"), images(usb()).scan().map { it.name })
    }

    @Test fun scanIsSortedAndCapped() = runBlocking {
        repeat(BackgroundImages.MAX_IMAGES + 10) { picture(File(usb(), "p%03d.png".format(it)), 40, 20) }
        val found = images(usb()).scan()
        assertEquals(BackgroundImages.MAX_IMAGES, found.size)
        assertEquals("p000.png", found.first().name)
    }

    @Test fun scanCoversEveryVolumeAndSurvivesAMissingOne() = runBlocking {
        val second = File(tmp.root, "usb2").apply { mkdirs() }
        picture(File(usb(), "a.png"), 40, 20)
        picture(File(second, "b.png"), 40, 20)
        assertEquals(listOf("a.png", "b.png"), images(usb(), File(tmp.root, "gone"), second).scan().map { it.name })
    }

    @Test fun tinyFilesAreNotPictures() = runBlocking {
        File(usb(), "stub.png").writeBytes(ByteArray(10))
        assertTrue(images(usb()).scan().isEmpty())
    }

    // ---- decoding ----

    @Test fun anyPictureBecomesExactly960By540() {
        listOf(1920 to 1080, 4000 to 3000, 600 to 1200, 100 to 50, 960 to 540).forEach { (w, h) ->
            val bitmap = BackgroundDecoder.cover(picture(File(tmp.root, "t_${w}x$h.png"), w, h))
            assertNotNull("$w x $h", bitmap)
            assertEquals("$w x $h", 960, bitmap!!.width)
            assertEquals("$w x $h", 540, bitmap.height)
        }
    }

    @Test fun aTallPictureIsCroppedNotStretched() {
        // 2:1 red | blue, tall version: the left half stays red, the right half blue
        val bitmap = BackgroundDecoder.cover(picture(File(tmp.root, "tall.png"), 400, 800))!!
        assertEquals(Color.RED, bitmap.getPixel(100, 270))
        assertEquals(Color.BLUE, bitmap.getPixel(860, 270))
    }

    @Test fun thumbnailsAreSmall() {
        val thumb = BackgroundDecoder.thumbnail(picture(File(tmp.root, "t.png"), 1920, 1080))!!
        assertEquals(BackgroundDecoder.THUMB_WIDTH, thumb.width)
        assertEquals(BackgroundDecoder.THUMB_HEIGHT, thumb.height)
    }

    @Test fun aBrokenFileGivesNullNotACrash() {
        val broken = File(tmp.root, "broken.png").apply { writeBytes(ByteArray(4000) { 7 }) }
        assertNull(BackgroundDecoder.cover(broken))
        assertNull(BackgroundDecoder.cover(File(tmp.root, "missing.png")))
    }

    // ---- importing ----

    @Test fun importStoresA960By540CopyThatSurvivesTheDrive() = runBlocking {
        val source = picture(File(usb(), "big.png"), 3000, 2000)
        val images = images(usb())
        val name = images.import(source)!!
        source.delete() // the drive is pulled out
        val stored = images.fileFor(name)
        assertNotNull(stored)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(stored!!.path, bounds)
        assertEquals(960, bounds.outWidth)
        assertEquals(540, bounds.outHeight)
        assertTrue(stored.length() < 400_000)
    }

    @Test fun importKeepsOnlyTheLatestPicture() = runBlocking {
        val images = images(usb())
        val first = images.import(picture(File(usb(), "a.png"), 400, 300))!!
        Thread.sleep(5)
        val second = images.import(picture(File(usb(), "b.png"), 400, 300))!!
        assertFalse(first == second)
        assertNull(images.fileFor(first))
        assertNotNull(images.fileFor(second))
        assertEquals(1, store().listFiles()!!.size)
    }

    @Test fun importOfABrokenFileChangesNothing() = runBlocking {
        val images = images(usb())
        val good = images.import(picture(File(usb(), "a.png"), 400, 300))!!
        assertNull(images.import(File(usb(), "broken.png").apply { writeBytes(ByteArray(3000) { 1 }) }))
        assertNotNull(images.fileFor(good))
    }

    @Test fun clearRemovesTheStoredPicture() = runBlocking {
        val images = images(usb())
        val name = images.import(picture(File(usb(), "a.png"), 400, 300))!!
        images.clear()
        assertNull(images.fileFor(name))
    }

    @Test fun onlyOurOwnFileNamesAreResolved() {
        val images = images(usb())
        store().mkdirs()
        File(store(), "bg_1.webp").writeBytes(ByteArray(10))
        File(store(), "other.webp").writeBytes(ByteArray(10))
        assertNotNull(images.fileFor("bg_1.webp"))
        assertNull(images.fileFor("other.webp"))
        assertNull(images.fileFor("../bg_1.webp"))
        assertNull(images.fileFor("/etc/passwd"))
        assertNull(images.fileFor("bg_2.webp")) // not there
    }
}
