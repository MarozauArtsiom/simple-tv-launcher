package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.LanguageChoice
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * Language: Belarusian first, then Russian, English, Polish, German, French, each by its own name,
 * and "Same as the TV" last with the language it resolves to. OK applies at once.
 * Focus starts on the saved choice, which carries the check mark.
 */
@Composable
fun LanguagePickerPage(
    theme: ThemeSpec,
    selected: LanguageChoice,
    /** The tag the launcher uses when it follows the TV (the TV's language if it is one of ours, else "en"). */
    tvTag: String,
    memory: FocusMemory,
    onChoose: (LanguageChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedId = idOf(selected)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request(selectedId)
    }
    SidePanel(title = stringResource(R.string.item_language), theme = theme, modifier = modifier) {
        PanelScroll {
            LanguageChoice.supportedTags.forEach { tag ->
                PanelItem(
                    title = languageName(tag),
                    theme = theme,
                    selected = selected == LanguageChoice.Fixed(tag),
                    onClick = { onChoose(LanguageChoice.Fixed(tag)) },
                    modifier = memory.track("lang:$tag"),
                )
            }
            PanelItem(
                title = stringResource(R.string.language_follow_tv),
                value = languageName(tvTag),
                theme = theme,
                selected = selected == LanguageChoice.Follow,
                onClick = { onChoose(LanguageChoice.Follow) },
                modifier = memory.track("lang:follow"),
            )
        }
    }
}

private fun idOf(choice: LanguageChoice) = when (choice) {
    LanguageChoice.Follow -> "lang:follow"
    is LanguageChoice.Fixed -> "lang:${choice.tag}"
}
