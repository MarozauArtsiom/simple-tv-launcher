package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.data.install.ApkAction
import io.github.marozauartsiom.tvlauncher.data.install.ApkFile
import io.github.marozauartsiom.tvlauncher.data.install.ApkInfo
import io.github.marozauartsiom.tvlauncher.data.install.ApkItem
import io.github.marozauartsiom.tvlauncher.data.install.UsbState
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Test

private fun item(name: String, label: String, version: String, mb: Long, action: ApkAction) = ApkItem(
    ApkInfo(ApkFile("/storage/usb/$name", name, mb * 1_000_000), "pkg.${name.removeSuffix(".apk")}", label, version, 1),
    action,
)

private val tvBro = item("tvbro.apk", "TV Bro", "2.1.6", 38, ApkAction.Install)
private val airplay = item("airplay.apk", "AirPlay Server", "0.0.30", 14, ApkAction.Install)
private val simpleTv = item("launcher.apk", "Simple TV", "1.1.0", 9, ApkAction.Update)
private val list = UsbState.Ready(listOf(tvBro, airplay, simpleTv))

private const val STORAGE = "adb shell appops set --uid io.github.marozauartsiom.tvlauncher MANAGE_EXTERNAL_STORAGE allow"
private const val UNKNOWN = "adb shell appops set --uid io.github.marozauartsiom.tvlauncher REQUEST_INSTALL_PACKAGES allow"

@OptIn(ExperimentalTestApi::class)
class UsbInstallerPageTest : TvScreenshotTest() {
    private var state by mutableStateOf<UsbState>(list)
    private val memory = FocusMemory()
    private val installed = mutableListOf<String>()
    private val opened = mutableListOf<String>()
    private var checks = 0
    private var dones = 0

    private fun show(initial: UsbState) {
        state = initial
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                UsbInstallerPage(
                    Themes.default, state, STORAGE, UNKNOWN, memory,
                    onInstall = { installed += it.info.label }, onCheckAgain = { checks++ },
                    onOpen = { opened += it.info.label }, onDone = { dones++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    @Test fun listShowsCountVersionSizeAndAction() {
        show(list)
        composeRule.onNodeWithText("USB · 3 .apk files").assertExists()
        composeRule.onNodeWithText("2.1.6 · 38 MB").assertExists()
        composeRule.onNodeWithText("Update").assertExists()
        composeRule.onNodeWithText("TV Bro").assertIsFocused()
    }

    @Test fun singularCount() {
        show(UsbState.Ready(listOf(tvBro)))
        composeRule.onNodeWithText("USB · 1 .apk file").assertExists()
    }

    @Test fun okInstallsTheFocusedArchive() {
        show(list)
        press(Key.DirectionDown)
        press(Key.Enter)
        assertEquals(listOf("AirPlay Server"), installed)
    }

    @Test fun anInstalledArchiveReadsInstalled() {
        show(UsbState.Ready(listOf(item("a.apk", "Zed", "1.0", 5, ApkAction.Installed))))
        composeRule.onNodeWithText("Installed").assertExists()
    }

    @Test fun noStorageAccessShowsTheAdbCommandAndFocusesCheckAgain() {
        show(UsbState.NoStorageAccess)
        composeRule.onNodeWithText(STORAGE).assertExists()
        composeRule.onNodeWithText("Check again").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, checks)
    }

    @Test fun unknownAppsShowsItsOwnCommand() {
        show(UsbState.NeedsUnknownApps)
        composeRule.onNodeWithText(UNKNOWN).assertExists()
        composeRule.onNodeWithText("Check again").assertIsFocused()
    }

    @Test fun emptyDriveSaysSo() {
        show(UsbState.Empty)
        composeRule.onNodeWithText("No .apk files on the USB drive").assertExists()
    }

    @Test fun installingAsksToConfirmInTheSystemDialog() {
        show(UsbState.Installing(tvBro))
        composeRule.onNodeWithText("Installing…").assertExists()
        composeRule.onNodeWithText("Confirm the installation in the system dialog.").assertExists()
    }

    @Test fun doneOffersOpenFirstThenDone() {
        show(UsbState.Done(tvBro))
        composeRule.onNodeWithText("Open").assertIsFocused()
        press(Key.Enter)
        assertEquals(listOf("TV Bro"), opened)
        press(Key.DirectionRight)
        press(Key.Enter)
        assertEquals(1, dones)
    }

    @Test fun failureAndCancellationHaveTheirOwnText() {
        show(UsbState.Failed(tvBro, cancelled = false))
        composeRule.onNodeWithText("Installation failed").assertExists()
        composeRule.onNodeWithText("Check again").assertIsFocused()
        state = UsbState.Failed(tvBro, cancelled = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Installation cancelled").assertExists()
    }

    @Test fun focusMovesToTheListWhenTheDriveIsRead() {
        show(UsbState.Loading)
        state = list
        composeRule.waitForIdle()
        composeRule.onNodeWithText("TV Bro").assertIsFocused()
    }
}

class UsbInstallerScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }
    private val themes = listOf("midnight", "sky", "valley").map(Themes::byKey)

    private fun shot(name: String, state: UsbState) = captureThemes("settings-usb-$name", themes) { theme ->
        backdrop.Over(theme) { UsbInstallerPage(theme, state, STORAGE, UNKNOWN, FocusMemory(), {}, {}, {}, {}) }
    }

    @Test fun list() = shot("list", list)
    @Test fun noPermission() = shot("nopermission", UsbState.NoStorageAccess)
    @Test fun installing() = shot("installing", UsbState.Installing(tvBro))
    @Test fun done() = shot("done", UsbState.Done(tvBro))
    @Test fun unknownApps() = shot("unknownapps", UsbState.NeedsUnknownApps)
    @Test fun empty() = shot("empty", UsbState.Empty)
    @Test fun failed() = shot("failed", UsbState.Failed(tvBro, cancelled = true))

    @Test fun listInTheLongestLanguages() {
        captureLocales("settings-usb-locale", listOf("be", "fr")) { _ ->
            backdrop.Over(Themes.default) { UsbInstallerPage(Themes.default, list, STORAGE, UNKNOWN, FocusMemory(), {}, {}, {}, {}) }
        }
    }
}
