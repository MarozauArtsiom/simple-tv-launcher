package io.github.marozauartsiom.tvlauncher.ui.home

import io.github.marozauartsiom.tvlauncher.data.apps.TvApp
import io.github.marozauartsiom.tvlauncher.data.apps.app
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeListsTest {
    private val apps = listOf(app("a", "Alpha"), app("b", "Beta"), app("c", "Gamma"), app("d", "Delta"))
    private fun build(settings: SettingsData) = HomeLists.build(apps, settings, locale = Locale.ENGLISH)

    @Test fun favouritesKeepTheOwnersOrder() {
        val c = build(SettingsData(favourites = listOf(ItemKey.app("c"), ItemKey.app("a"), ItemKey.app("d"))))
        assertEquals(listOf("Gamma", "Alpha", "Delta"), c.favourites.map { it.label })
    }

    @Test fun missingAndHiddenFavouritesAreSkipped() {
        val c = build(SettingsData(favourites = listOf(ItemKey.app("gone"), ItemKey.app("b"), ItemKey.app("a")), hidden = setOf("a")))
        assertEquals(listOf("Beta"), c.favourites.map { it.label })
        assertEquals(listOf("Beta", "Delta", "Gamma"), c.all.map { it.label })
    }

    @Test fun allAppsAreSortedByLaunchesThenName() {
        val c = build(SettingsData(launchCounts = mapOf("pkg:c" to 5, "pkg:b" to 2, "pkg:d" to 2)))
        assertEquals(listOf("Gamma", "Beta", "Delta", "Alpha"), c.all.map { it.label })
    }

    @Test fun favouritesAreAlsoInAllApps() {
        val c = build(SettingsData(favourites = listOf(ItemKey.app("a"))))
        assertTrue(c.all.any { it.label == "Alpha" })
    }

    @Test fun shortcutsAppearInBothListsLikeApps() {
        val shortcut = Shortcut("s1", "News", "https://example.org")
        val c = build(SettingsData(favourites = listOf(ItemKey.shortcut("s1")), shortcuts = listOf(shortcut)))
        assertEquals(listOf("News"), c.favourites.map { it.label })
        assertEquals(shortcut, c.favourites.single().shortcut)
        assertTrue(c.all.any { it.shortcut == shortcut })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeContentSourceTest {
    private val apps = listOf(app("a", "Alpha"), app("b", "Beta"))

    @Test fun launchCountsOnlyReorderOnReset() = runTest {
        val appsFlow = MutableStateFlow<List<TvApp>>(apps)
        val settings = MutableStateFlow(SettingsData())
        val resets = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val source = HomeContentSource(appsFlow, settings, resets, StandardTestDispatcher(testScheduler)) { Locale.ENGLISH }

        val collected = mutableListOf<List<String>>()
        val job = launch { source.content.collect { collected += it.all.map { t -> t.label } } }
        runCurrent()
        assertEquals(listOf("Alpha", "Beta"), collected.last())

        settings.value = SettingsData(launchCounts = mapOf("pkg:b" to 3)) // the owner launched Beta
        runCurrent()
        assertEquals("tiles must not shuffle while the owner is away", listOf("Alpha", "Beta"), collected.last())

        resets.emit(Unit) // Home key / screen on
        runCurrent()
        assertEquals(listOf("Beta", "Alpha"), collected.last())
        job.cancel()
    }

    @Test fun favouriteChangesApplyAtOnce() = runTest {
        val settings = MutableStateFlow(SettingsData())
        val source = HomeContentSource(MutableStateFlow(apps), settings, MutableSharedFlow(), StandardTestDispatcher(testScheduler)) { Locale.ENGLISH }
        assertTrue(source.content.first().favourites.isEmpty())
        settings.value = SettingsData(favourites = listOf(ItemKey.app("b")))
        assertEquals(listOf("Beta"), source.content.first().favourites.map { it.label })
    }
}
