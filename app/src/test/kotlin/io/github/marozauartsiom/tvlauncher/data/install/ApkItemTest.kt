package io.github.marozauartsiom.tvlauncher.data.install

import org.junit.Assert.assertEquals
import org.junit.Test

class ApkItemTest {
    private fun info(version: Long) = ApkInfo(ApkFile("/usb/a.apk", "a.apk", 1000), "pkg.a", "A", "1.0", version)

    @Test fun notInstalledMeansInstall() = assertEquals(ApkAction.Install, ApkItem.of(info(5), null).action)
    @Test fun newerArchiveMeansUpdate() = assertEquals(ApkAction.Update, ApkItem.of(info(6), 5).action)
    @Test fun sameVersionIsAlreadyInstalled() = assertEquals(ApkAction.Installed, ApkItem.of(info(5), 5).action)
    @Test fun olderArchiveIsNotOfferedAsADowngrade() = assertEquals(ApkAction.Installed, ApkItem.of(info(4), 5).action)
}
