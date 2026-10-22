package io.github.marozauartsiom.tvlauncher.system

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcuts

/**
 * Opens a shortcut's address in the default browser (TV Bro). [start] is `Context.startActivity`.
 * Only http(s) addresses are opened, even if storage was edited by hand.
 */
class WebLauncher(private val start: (Intent) -> Unit) {
    sealed interface Result {
        data object Started : Result
        data object NoBrowser : Result
        data object Rejected : Result
    }

    fun open(url: String): Result {
        val safe = Shortcuts.normalizeUrl(url) ?: return Result.Rejected
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safe))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            start(intent); Result.Started
        } catch (e: ActivityNotFoundException) {
            Result.NoBrowser
        } catch (e: SecurityException) {
            Result.NoBrowser
        }
    }
}
