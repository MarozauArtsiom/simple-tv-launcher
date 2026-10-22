package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.background.PickedImage
import io.github.marozauartsiom.tvlauncher.data.prefs.BackgroundChoice
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.FocusProgress
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.PermissionNote
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.components.focusRing
import io.github.marozauartsiom.tvlauncher.ui.components.rememberFocusProgress
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundChoices
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundPick
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/** What the "pictures from USB" area has to show. */
sealed interface PicturesState {
    data object Loading : PicturesState
    /** All-files access is missing; the adb hint is shown instead. */
    data object NoAccess : PicturesState
    data class Loaded(val images: List<PickedImage>) : PicturesState
}

private const val Columns = 3

/**
 * Settings → Background. "From theme", six colours that suit the theme's text, and the pictures found
 * on a USB drive. Focusing any of them previews it on the whole screen ([onPreview]); OK keeps it
 * ([onChoose]); leaving the page (Back) ends the preview. [saved] carries the check mark.
 *
 * [thumbnail] decodes a small picture off the main thread; it is called only for rows on screen.
 */
@Composable
fun BackgroundPickerPage(
    theme: ThemeSpec,
    saved: BackgroundChoice,
    pictures: PicturesState,
    adbCommand: String,
    memory: FocusMemory,
    thumbnail: suspend (PickedImage) -> ImageBitmap?,
    onPreview: (BackgroundPick?) -> Unit,
    onChoose: (BackgroundPick) -> Unit,
    onCheckAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    val swatches = remember(theme.id) { BackgroundChoices.swatchesFor(theme) }
    val savedId = when (saved) {
        is BackgroundChoice.Solid -> swatches.firstOrNull { it == saved.argb }?.let { "color:$it" } ?: "theme"
        else -> "theme"
    }
    // Down from "From theme" (a wide row) would land on the swatch nearest its middle; go to the saved one (else the first).
    val firstSwatch = memory.requester("color:${(saved as? BackgroundChoice.Solid)?.argb?.takeIf { it in swatches } ?: swatches.first()}")
    DisposableEffect(Unit) { onDispose { onPreview(null) } }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request(savedId)
    }
    SidePanel(title = stringResource(R.string.item_background), theme = theme, modifier = modifier, width = 560.dp, dim = 0.15f) {
        PanelScroll {
            PanelItem(
                title = stringResource(R.string.value_from_theme),
                value = stringResource(theme.nameRes),
                icon = Icons.Image,
                theme = theme,
                selected = saved == BackgroundChoice.FromTheme,
                onClick = { onChoose(BackgroundPick.FromTheme) },
                modifier = memory.track("theme")
                    .focusProperties { down = firstSwatch }
                    .onFocusedPreview(BackgroundPick.FromTheme, onPreview),
            )
            Text(stringResource(R.string.bg_colors), style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                swatches.forEach { argb ->
                    Swatch(
                        argb = argb,
                        theme = theme,
                        isSaved = saved == BackgroundChoice.Solid(argb),
                        onFocused = { onPreview(BackgroundPick.Solid(argb)) },
                        onClick = { onChoose(BackgroundPick.Solid(argb)) },
                        modifier = memory.track("color:$argb").testTag("swatch:$argb"),
                    )
                }
            }
            when (pictures) {
                PicturesState.Loading -> Unit
                PicturesState.NoAccess -> {
                    Text(stringResource(R.string.bg_usb_section), style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(top = 8.dp))
                    PermissionNote(
                        message = stringResource(R.string.usb_no_permission),
                        command = adbCommand,
                        buttonText = stringResource(R.string.check_again),
                        theme = theme,
                        onCheck = onCheckAccess,
                        buttonModifier = memory.track("check"),
                    )
                }
                is PicturesState.Loaded -> {
                    Text(
                        text = stringResource(R.string.bg_usb_section) + if (pictures.images.isNotEmpty()) " · ${pictures.images.size}" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.panelMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (pictures.images.isEmpty()) {
                        Text(stringResource(R.string.bg_usb_empty), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            pictures.images.chunked(Columns).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { image ->
                                        PictureThumb(
                                            image = image,
                                            theme = theme,
                                            thumbnail = thumbnail,
                                            onFocused = { onPreview(BackgroundPick.Picture(image.path)) },
                                            onClick = { onChoose(BackgroundPick.Picture(image.path)) },
                                            modifier = memory.track("picture:${image.path}"),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Previews [pick] whenever the element gets focus. */
private fun Modifier.onFocusedPreview(pick: BackgroundPick, onPreview: (BackgroundPick?) -> Unit): Modifier =
    this.onFocusChanged { if (it.hasFocus) onPreview(pick) }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Swatch(
    argb: Int,
    theme: ThemeSpec,
    isSaved: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusProgress = rememberFocusProgress(),
) {
    LaunchedEffect(focus.focused) { if (focus.focused) onFocused() }
    val color = Color(argb)
    val description = stringResource(R.string.bg_colors)
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .zIndex(if (focus.focused) 1f else 0f)
            .then(focus.track())
            .focusRing(focus, theme.colors.itemFocusRing, 20.dp, growth = 0.12f)
            .semantics { contentDescription = description; selected = isSaved },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = color, focusedContainerColor = color),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (isSaved) {
                val mark = if (BackgroundChoices.hasLightText(theme)) Color.White else Color(0xFF0B0D12)
                Icon(Icons.Check, contentDescription = null, tint = mark, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PictureThumb(
    image: PickedImage,
    theme: ThemeSpec,
    thumbnail: suspend (PickedImage) -> ImageBitmap?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusProgress = rememberFocusProgress(),
) {
    val bitmap by produceState<ImageBitmap?>(null, image.path) { value = thumbnail(image) }
    val shape = remember { RoundedCornerShape(10.dp) }
    LaunchedEffect(focus.focused) { if (focus.focused) onFocused() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(148.dp, 83.dp)
            .zIndex(if (focus.focused) 1f else 0f)
            .then(focus.track())
            .focusRing(focus, theme.colors.itemFocusRing, 10.dp, growth = 0.06f)
            .semantics { contentDescription = image.name },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = theme.colors.itemBg, focusedContainerColor = theme.colors.itemBg),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(Modifier.size(148.dp, 83.dp).clip(shape)) {
            bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(148.dp, 83.dp)) }
        }
    }
}
