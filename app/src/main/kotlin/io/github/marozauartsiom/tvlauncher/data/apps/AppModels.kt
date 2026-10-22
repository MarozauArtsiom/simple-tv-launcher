package io.github.marozauartsiom.tvlauncher.data.apps

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import kotlinx.serialization.Serializable

/** One launchable app. [isLeanback] is true when it has a TV launcher activity. */
@Immutable
@Serializable
data class TvApp(
    val packageName: String,
    val activityClass: String,
    val label: String,
    val isLeanback: Boolean,
) {
    val key: ItemKey get() = ItemKey.app(packageName)
}

/** Art for a tile: a 320x180 banner, or a generated one (icon on a tinted surface; the name is drawn by the UI). */
@Immutable
data class BannerArt(val bitmap: ImageBitmap, val isGenerated: Boolean)

/** What the platform gives us for an app's picture. */
class LoadedArt(val drawable: Drawable, val isBanner: Boolean)

/** Everything the repositories need from Android's package machinery, so tests can fake it. */
interface PackageSource {
    /** Launchable apps from both the TV and phone launcher categories (labels loaded). Not yet deduplicated. */
    fun queryLaunchable(): List<TvApp>

    /** Banner if the app has one, else its icon. */
    fun loadArt(app: TvApp): LoadedArt?

    /** TV launch intent, else the regular one, else null. */
    fun launchIntent(packageName: String): Intent?

    /** Calls [onChange] after any install, removal or update. Returns a function that unregisters. */
    fun registerChanges(onChange: () -> Unit): () -> Unit
}

/** Where tiles get their art. [BannerLoader] is the real one; tests and previews use fakes. */
interface ArtSource {
    /** The art if it is already in memory, so the first frame needs no wait. */
    fun peek(app: TvApp): BannerArt?

    suspend fun load(app: TvApp): BannerArt
}
