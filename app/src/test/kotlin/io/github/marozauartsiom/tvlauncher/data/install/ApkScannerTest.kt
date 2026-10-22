package io.github.marozauartsiom.tvlauncher.data.install

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkScannerTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun usb(): File = File(tmp.root, "usb").apply { mkdirs() }
    private fun file(path: String, bytes: Int = 2048): File = File(usb(), path).apply { parentFile?.mkdirs(); writeBytes(ByteArray(bytes)) }
    private fun scan(): List<ApkFile> = runBlocking { ApkScanner({ listOf(usb()) }).scan() }

    @Test fun findsApkFilesSortedByNameIgnoringCase() {
        file("b.apk"); file("A.APK"); file("c.txt")
        assertEquals(listOf("A.APK", "b.apk"), scan().map { it.name })
    }

    @Test fun looksThreeFolderLevelsDown() {
        file("one/two/deep.apk"); file("one/two/three/too-deep.apk")
        assertEquals(listOf("deep.apk"), scan().map { it.name })
    }

    @Test fun skipsHiddenFilesSystemFoldersAndTinyFiles() {
        file(".hidden.apk"); file("Android/data.apk"); file("LOST.DIR/x.apk"); file("tiny.apk", bytes = 10); file("ok.apk")
        assertEquals(listOf("ok.apk"), scan().map { it.name })
    }

    @Test fun capsTheNumberOfFiles() {
        repeat(ApkScanner.MAX_FILES + 15) { file("app-%03d.apk".format(it)) }
        assertEquals(ApkScanner.MAX_FILES, scan().size)
    }

    @Test fun missingVolumeGivesNothing() {
        assertTrue(runBlocking { ApkScanner({ listOf(File(tmp.root, "nope")) }).scan() }.isEmpty())
    }

    @Test fun reportsSizeAndPath() {
        val f = file("x.apk", 4096)
        val found = scan().single()
        assertEquals(4096L, found.sizeBytes)
        assertEquals(f.path, found.path)
    }
}
