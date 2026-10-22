package io.github.marozauartsiom.tvlauncher.data.install

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsbInstallerTest {
    private fun apk(name: String, pkg: String, version: Long) = ApkInfo(ApkFile("/usb/$name", name, 5_000_000), pkg, name.removeSuffix(".apk"), "1.$version", version)

    private val files = mutableListOf<ApkInfo>()
    private val installed = mutableMapOf<String, Long>()
    private var storage = true
    private var canInstall = true
    private var commitOk = true
    private val committed = mutableListOf<String>()
    private val results = MutableSharedFlow<InstallResult>(extraBufferCapacity = 8)

    private val inspector = object : ApkInspector {
        override fun inspect(file: ApkFile) = files.firstOrNull { it.file == file }
        override fun installedVersion(packageName: String) = installed[packageName]
    }
    private val installer = object : ApkInstaller {
        override suspend fun commit(file: File, packageName: String): Boolean { committed += packageName; return commitOk }
    }

    /** A finder over fixed files, so the state machine is tested without a file system. */
    private fun TestScope.usbWith(vararg infos: ApkInfo): UsbInstaller {
        files += infos
        return UsbInstaller(
            scanner = { infos.map { it.file } },
            inspector = inspector, installer = installer,
            hasStorageAccess = { storage }, canInstallPackages = { canInstall }, results = results, scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
    }

    @Test fun withoutStorageAccessNothingIsRead() = runTest {
        storage = false
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        assertEquals(UsbState.NoStorageAccess, usb.state.value)
    }

    @Test fun emptyDriveIsEmpty() = runTest {
        val usb = usbWith()
        usb.refresh(); advanceUntilIdle()
        assertEquals(UsbState.Empty, usb.state.value)
    }

    @Test fun unreadableArchivesAreLeftOut() = runTest {
        val usb = UsbInstaller({ listOf(ApkFile("/usb/broken.apk", "broken.apk", 9000)) }, inspector, installer, { true }, { true }, results, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        usb.refresh(); advanceUntilIdle()
        assertEquals(UsbState.Empty, usb.state.value)
    }

    @Test fun labelsInstallUpdateAndInstalled() = runTest {
        installed["pkg.b"] = 1; installed["pkg.c"] = 3
        val usb = usbWith(apk("a.apk", "pkg.a", 1), apk("b.apk", "pkg.b", 2), apk("c.apk", "pkg.c", 3))
        usb.refresh(); advanceUntilIdle()
        val items = (usb.state.value as UsbState.Ready).items
        assertEquals(listOf(ApkAction.Install, ApkAction.Update, ApkAction.Installed), items.map { it.action })
    }

    @Test fun installCommitsAndWaitsForTheSystem() = runTest {
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        val item = (usb.state.value as UsbState.Ready).items.single()
        usb.install(item); advanceUntilIdle()
        assertEquals(listOf("pkg.a"), committed)
        assertEquals(UsbState.Installing(item), usb.state.value)
        results.emit(InstallResult.Success("pkg.a")); advanceUntilIdle()
        assertEquals(UsbState.Done(item), usb.state.value)
    }

    @Test fun cancelAndFailureAreTold() = runTest {
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        val item = (usb.state.value as UsbState.Ready).items.single()
        usb.install(item); advanceUntilIdle()
        results.emit(InstallResult.Cancelled); advanceUntilIdle()
        assertEquals(UsbState.Failed(item, cancelled = true), usb.state.value)
        usb.install(item); advanceUntilIdle()
        results.emit(InstallResult.Failure("x")); advanceUntilIdle()
        assertEquals(UsbState.Failed(item, cancelled = false), usb.state.value)
    }

    @Test fun aCommitThatCannotBeMadeFailsAtOnce() = runTest {
        commitOk = false
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        val item = (usb.state.value as UsbState.Ready).items.single()
        usb.install(item); advanceUntilIdle()
        assertEquals(UsbState.Failed(item, cancelled = false), usb.state.value)
    }

    @Test fun withoutUnknownAppsPermissionNothingIsCommitted() = runTest {
        canInstall = false
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        usb.install((usb.state.value as UsbState.Ready).items.single()); advanceUntilIdle()
        assertEquals(UsbState.NeedsUnknownApps, usb.state.value)
        assertTrue(committed.isEmpty())
    }

    @Test fun anAlreadyInstalledArchiveIsNotInstalledAgain() = runTest {
        installed["pkg.a"] = 1
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        usb.install((usb.state.value as UsbState.Ready).items.single()); advanceUntilIdle()
        assertTrue(committed.isEmpty())
        assertTrue(usb.state.value is UsbState.Ready)
    }

    @Test fun resultsWithoutAnInstallationInProgressAreIgnored() = runTest {
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        val before = usb.state.value
        results.emit(InstallResult.Success("pkg.zzz")); advanceUntilIdle()
        assertEquals(before, usb.state.value)
    }

    @Test fun refreshAfterDoneListsTheDriveAgain() = runTest {
        val usb = usbWith(apk("a.apk", "pkg.a", 1))
        usb.refresh(); advanceUntilIdle()
        val item = (usb.state.value as UsbState.Ready).items.single()
        usb.install(item); advanceUntilIdle()
        results.emit(InstallResult.Success("pkg.a")); advanceUntilIdle()
        installed["pkg.a"] = 1
        usb.refresh(); advanceUntilIdle()
        assertEquals(ApkAction.Installed, (usb.state.value as UsbState.Ready).items.single().action)
    }
}
