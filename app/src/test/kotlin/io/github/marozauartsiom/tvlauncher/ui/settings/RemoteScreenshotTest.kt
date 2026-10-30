package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.data.prefs.TopButton
import io.github.marozauartsiom.tvlauncher.system.remote.ClientKind
import io.github.marozauartsiom.tvlauncher.system.remote.RemoteClient
import io.github.marozauartsiom.tvlauncher.system.remote.RemoteState
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.home.FakeArt
import io.github.marozauartsiom.tvlauncher.ui.home.HomeActions
import io.github.marozauartsiom.tvlauncher.ui.home.HomeFixtures
import io.github.marozauartsiom.tvlauncher.ui.home.HomeScreen
import io.github.marozauartsiom.tvlauncher.ui.remote.RemoteBadge
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundCache
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** Phone remote & AI access (`design/mockups/settings-remote.html`), the Top bar page, the Remote chip and the badge. */
class RemoteScreenshotTest : TvScreenshotTest() {
    private val cache by lazy {
        BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined).also { c ->
            runBlocking { Themes.all.forEach { c.load(it.background) } }
        }
    }
    private val art = FakeArt()

    private val on = RemoteState(
        phoneOn = true, aiOn = true, address = "192.168.100.5:8765", code = "102670", helperReady = true, fullKeys = true,
        clients = listOf(
            RemoteClient("a1b2c3", ClientKind.Phone, "Phone", 0),
            RemoteClient("d4e5f6", ClientKind.Assistant, "Claude", 0),
        ),
    )

    @Composable
    private fun OverHome(theme: ThemeSpec, hidden: Set<TopButton> = emptySet(), remoteOn: Boolean = true, aiConnected: Boolean = false, panel: (@Composable () -> Unit)? = null) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, cache)
            HomeScreen(
                content = HomeFixtures.content, theme = theme,
                topBar = HomeFixtures.topBar.copy(showRemote = true, remoteOn = remoteOn, remoteAiConnected = aiConnected, hidden = hidden),
                clock = { HomeFixtures.clock }, art = art, resets = emptyFlow(), actions = HomeActions(), overlayOpen = panel != null,
            )
            panel?.invoke()
        }
    }

    @Test fun remotePageInEveryTheme() {
        captureThemes("remote-page") { theme ->
            OverHome(theme) {
                RemoteAccessPage(theme, on, "adb shell …", FocusMemory(), {}, {}, {}, {}, {})
            }
        }
    }

    @Test fun remotePageStates() {
        val themes = listOf(Themes.byKey("midnight"))
        captureThemes("remote-page-off", themes) { theme ->
            OverHome(theme, remoteOn = false) { RemoteAccessPage(theme, RemoteState(helperReady = true), "", FocusMemory(), {}, {}, {}, {}, {}) }
        }
    }

    @Test fun remotePageNeedsTheHelper() {
        captureThemes("remote-page-helper", listOf(Themes.byKey("paper"))) { theme ->
            OverHome(theme, remoteOn = false) {
                RemoteAccessPage(theme, RemoteState(helperReady = false), "adb shell settings put secure enabled_accessibility_services …", FocusMemory(), {}, {}, {}, {}, {})
            }
        }
    }

    private val internet = on.copy(
        phoneOn = false, externalOn = true,
        externalUrl = "https://86e0f64868e2d7.lhr.life/s/0123456789abcdef0123456789abcdef",
        clients = listOf(RemoteClient("d4e5f6", ClientKind.Assistant, "Claude", 0)),
    )

    @Test fun remotePageFromTheInternet() {
        captureThemes("remote-page-internet", listOf("midnight", "paper").map(Themes::byKey)) { theme ->
            OverHome(theme) { RemoteAccessPage(theme, internet, "", FocusMemory(), {}, {}, {}, {}, {}) }
        }
    }

    @Test fun remotePageFromTheInternetInLongLanguages() {
        captureLocales("remote-page-internet-locales", tags = listOf("be", "fr", "de")) {
            val theme = Themes.default
            OverHome(theme) { RemoteAccessPage(theme, internet, "", FocusMemory(), {}, {}, {}, {}, {}) }
        }
    }

    @Test fun remotePageWhileTheTunnelConnects() {
        captureThemes("remote-page-connecting", listOf(Themes.byKey("midnight"))) { theme ->
            OverHome(theme) { RemoteAccessPage(theme, internet.copy(externalUrl = null, clients = emptyList()), "", FocusMemory(), {}, {}, {}, {}, {}) }
        }
    }

    @Test fun remotePageInLongLanguages() {
        captureLocales("remote-page-locales", tags = listOf("be", "fr", "de")) {
            val theme = Themes.default
            OverHome(theme) { RemoteAccessPage(theme, on, "", FocusMemory(), {}, {}, {}, {}, {}) }
        }
    }

    @Test fun topBarPageWithHiddenButtons() {
        val hidden = setOf(TopButton.Inputs, TopButton.Iphone)
        captureThemes("top-bar-page", listOf("midnight", "paper", "valley").map(Themes::byKey)) { theme ->
            OverHome(theme, hidden = hidden) { TopBarPage(theme, hidden, FocusMemory(), { _, _ -> }) }
        }
    }

    @Test fun topBarPageInLongLanguages() {
        captureLocales("top-bar-page-locales", tags = listOf("be", "fr", "de")) {
            val theme = Themes.default
            OverHome(theme) { TopBarPage(theme, emptySet(), FocusMemory(), { _, _ -> }) }
        }
    }

    @Test fun homeWithTheRemoteButton() {
        captureThemes("home-remote-chip", listOf("midnight", "paper", "valley").map(Themes::byKey)) { theme -> OverHome(theme) }
    }

    @Test fun homeWhileAnAssistantIsConnected() {
        // The Remote chip is Home's indicator: "AI is controlling" with the sparkle icon and the dot.
        captureThemes("home-remote-ai", listOf("midnight", "halloween-day").map(Themes::byKey)) { theme -> OverHome(theme, hidden = setOf(TopButton.Inputs), aiConnected = true) }
    }

    @Test fun homeWithHiddenButtons() {
        // Inputs and the weather hidden: the bar keeps Timer, Remote, iPhone and the avatar.
        captureThemes("home-top-bar-hidden", listOf(Themes.byKey("midnight"))) { theme -> OverHome(theme, hidden = setOf(TopButton.Inputs, TopButton.Weather), remoteOn = false) }
    }

    @Test fun badgeInEveryTheme() {
        captureThemes("remote-badge") { theme ->
            Box(Modifier.fillMaxSize()) {
                ThemedBackground(theme, cache)
                RemoteBadge(theme, phone = true, ai = true, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
            }
        }
    }
}
