package io.github.marozauartsiom.tvlauncher.ui

import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    private val vm = LauncherViewModel()

    @Test fun backClosesOneLevelAtATime() {
        vm.open(Overlay.Settings(SettingsPage.Theme))
        assertTrue(vm.onBack()); assertEquals(Overlay.Settings(SettingsPage.Root), vm.overlay.value)
        assertTrue(vm.onBack()); assertEquals(Overlay.None, vm.overlay.value)
    }

    @Test fun citySearchBacksIntoTheWeatherPanel() {
        vm.open(Overlay.WeatherSearch)
        vm.onBack(); assertEquals(Overlay.Weather, vm.overlay.value)
        vm.onBack(); assertEquals(Overlay.None, vm.overlay.value)
    }

    @Test fun everyOtherOverlayBacksStraightToHome() {
        listOf(Overlay.Weather, Overlay.Hdmi("HDMI2"), Overlay.AppMenu(ItemKey.app("a")), Overlay.MoveMode(ItemKey.app("a")), Overlay.IphoneHint).forEach {
            vm.open(it); assertTrue(vm.onBack()); assertEquals(Overlay.None, vm.overlay.value)
        }
    }

    @Test fun backAtTheTopAsksHomeToResetAndReportsNotConsumed() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<Unit>()
        val job = launch { vm.resets.toList(seen) }
        assertFalse(vm.onBack())
        assertEquals(1, seen.size)
        job.cancel()
    }

    @Test fun homeKeyClosesEverythingAndResets() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<Unit>()
        val job = launch { vm.resets.toList(seen) }
        vm.open(Overlay.Settings(SettingsPage.About))
        vm.onHomePressed()
        assertEquals(Overlay.None, vm.overlay.value)
        assertEquals(1, seen.size)
        job.cancel()
    }
}
