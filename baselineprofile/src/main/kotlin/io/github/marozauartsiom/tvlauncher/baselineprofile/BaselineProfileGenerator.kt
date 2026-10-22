package io.github.marozauartsiom.tvlauncher.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "io.github.marozauartsiom.tvlauncher"

/**
 * The journey the Baseline Profile is recorded from: cold start, walk along Favourites, down the All apps
 * grid, open Settings and a sub-page, and come back. It uses the D-pad only, like the owner does.
 * Run it on the TV (see `baselineprofile/build.gradle.kts`); the key counts assume the default Home layout
 * and may be adjusted after the first run on the real device.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun startupAndScroll() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        // The launcher is a TV app: it has a LEANBACK_LAUNCHER entry, not a LAUNCHER one.
        startActivityAndWait(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setClassName(PACKAGE, "$PACKAGE.LauncherActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        repeat(3) { device.pressDPadRight(); device.waitForIdle() }          // along Favourites
        repeat(6) { device.pressDPadDown(); device.waitForIdle() }            // down the All apps grid
        repeat(6) { device.pressDPadUp(); device.waitForIdle() }
        repeat(2) { device.pressDPadUp(); device.waitForIdle() }              // to the top bar
        repeat(6) { device.pressDPadRight(); device.waitForIdle() }           // to the settings gear
        device.pressDPadCenter(); device.waitForIdle()                        // open Settings
        repeat(4) { device.pressDPadDown(); device.waitForIdle() }
        device.pressDPadCenter(); device.waitForIdle()                        // a sub-page
        device.pressBack(); device.waitForIdle()
        device.pressBack(); device.waitForIdle()
    }
}
