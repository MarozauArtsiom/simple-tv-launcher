package io.github.marozauartsiom.tvlauncher.ui.home

import io.github.marozauartsiom.tvlauncher.data.apps.AppLists
import io.github.marozauartsiom.tvlauncher.data.apps.TvApp
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.Dispatchers

/** Pure list logic for Home. */
object HomeLists {
    fun build(
        apps: List<TvApp>,
        settings: SettingsData,
        launchCounts: Map<String, Int> = settings.launchCounts,
        locale: Locale = Locale.getDefault(),
    ): HomeContent {
        val visibleApps = AppLists.visible(apps, settings.hidden)
        val byKey = LinkedHashMap<ItemKey, HomeTile>()
        visibleApps.forEach { byKey[it.key] = HomeTile(it.key, it.label, app = it) }
        settings.shortcuts.forEach { byKey[ItemKey.shortcut(it.id)] = HomeTile(ItemKey.shortcut(it.id), it.title, shortcut = it) }

        // Favourites that are no longer installed or are hidden are skipped, but stay in storage.
        val favourites = settings.favourites.mapNotNull { byKey[it] }
        val collator = Collator.getInstance(locale)
        val all = byKey.values.sortedWith(
            compareByDescending<HomeTile> { launchCounts[it.key.value] ?: 0 }.thenComparing(compareBy(collator) { it.label }),
        )
        val hidden = apps.filter { it.packageName in settings.hidden }
            .map { HomeTile(it.key, it.label, app = it) }
            .sortedWith(compareBy(collator) { it.label })
        return HomeContent(favourites, all, hidden)
    }
}

/**
 * Combines installed apps and settings into [HomeContent]. Launch counts are only re-applied when
 * a reset arrives (Home key, screen on, Back at the top), so tiles do not shuffle under the focus
 * when the user comes back from an app.
 */
class HomeContentSource(
    private val apps: Flow<List<TvApp>>,
    private val settings: Flow<SettingsData>,
    private val resets: Flow<Unit>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val locale: () -> Locale = { Locale.getDefault() },
) {
    val content: Flow<HomeContent> = flow {
        var frozenCounts: Map<String, Int>? = null
        var appliedReset = -1
        val resetCount = resets.runningFold(0) { n, _ -> n + 1 }
        emitAll(
            combine(apps, settings, resetCount) { a, s, n ->
                val counts = frozenCounts.takeIf { n == appliedReset } ?: s.launchCounts
                frozenCounts = counts
                appliedReset = n
                HomeLists.build(a, s, counts, locale())
            },
        )
    }
        .flowOn(dispatcher)
        .distinctUntilChanged()
}
