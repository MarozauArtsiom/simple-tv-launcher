package io.github.marozauartsiom.tvlauncher.data.apps

import android.content.ActivityNotFoundException
import android.content.Intent

sealed interface LaunchResult {
    data object Started : LaunchResult
    /** The app is gone, disabled or has no launchable activity: refresh the list and tell the user. */
    data object NotFound : LaunchResult
    data object Failed : LaunchResult
}

/** Starts apps. [start] is `Context.startActivity`, injected so tests don't need an activity. */
class AppLauncher(private val source: PackageSource, private val start: (Intent) -> Unit) {
    fun launch(packageName: String): LaunchResult {
        val intent = source.launchIntent(packageName) ?: return LaunchResult.NotFound
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            start(intent); LaunchResult.Started
        } catch (e: ActivityNotFoundException) {
            LaunchResult.NotFound
        } catch (e: SecurityException) {
            LaunchResult.Failed
        } catch (e: RuntimeException) {
            LaunchResult.Failed
        }
    }
}
