package io.github.marozauartsiom.tvlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayTest {
    @Test fun aPageOpenedFromTheTopBarClosesOnBack() {
        assertEquals(Overlay.None, Overlay.Settings(SettingsPage.Remote, direct = true).back())
    }

    @Test fun aPageOpenedFromSettingsGoesBackToTheList() {
        assertEquals(Overlay.Settings(SettingsPage.Root), Overlay.Settings(SettingsPage.Remote).back())
        assertEquals(Overlay.Settings(SettingsPage.Root), Overlay.Settings(SettingsPage.TopBar).back())
        assertEquals(Overlay.None, Overlay.Settings(SettingsPage.Root).back())
    }
}
