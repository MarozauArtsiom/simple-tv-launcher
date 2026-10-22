package io.github.marozauartsiom.tvlauncher.data.apps

import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import java.text.Collator
import java.util.Locale

/** Pure list logic for the app lists: deduplication, hiding and ordering. */
object AppLists {
    /** One entry per package, preferring the TV activity, without ourselves, alphabetical. */
    fun dedupe(raw: List<TvApp>, ownPackage: String, collator: Collator = Collator.getInstance()): List<TvApp> =
        raw.filter { !it.packageName.startsWith(ownPackage) && it.packageName != ownPackage }
            .groupBy { it.packageName }
            .map { (_, entries) -> entries.firstOrNull { it.isLeanback } ?: entries.first() }
            .sortedWith(compareBy(collator) { it.label })

    fun visible(apps: List<TvApp>, hidden: Set<String>): List<TvApp> = apps.filter { it.packageName !in hidden }

    /** "All apps" order: most launched first, then alphabetical. Favourites are never reordered by this. */
    fun byUsage(apps: List<TvApp>, launchCounts: Map<String, Int>, locale: Locale = Locale.getDefault()): List<TvApp> {
        val collator = Collator.getInstance(locale)
        return apps.sortedWith(
            compareByDescending<TvApp> { launchCounts[ItemKey.app(it.packageName).value] ?: 0 }
                .thenComparing(compareBy(collator) { it.label }),
        )
    }
}
