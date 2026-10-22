package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.background.PickedImage
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.PermissionNote
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * Settings → Profile photo: the picture in the round Settings button at the top right of Home.
 * "No photo" (a person icon) or a picture from a USB drive, same scan and permission note as the
 * background picker. OK on a picture keeps a small square copy; the drive can then be removed.
 */
@Composable
fun AvatarPickerPage(
    theme: ThemeSpec,
    hasPhoto: Boolean,
    pictures: PicturesState,
    adbCommand: String,
    memory: FocusMemory,
    thumbnail: suspend (PickedImage) -> ImageBitmap?,
    onChoose: (PickedImage?) -> Unit,
    onCheckAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request("none")
    }
    SidePanel(title = stringResource(R.string.item_avatar), theme = theme, modifier = modifier, width = 560.dp) {
        PanelScroll {
            PanelItem(
                title = stringResource(R.string.avatar_none),
                value = null,
                icon = Icons.Person,
                theme = theme,
                selected = !hasPhoto,
                onClick = { onChoose(null) },
                modifier = memory.track("none"),
            )
            Text(stringResource(R.string.bg_usb_section), style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(top = 8.dp))
            when (pictures) {
                PicturesState.Loading -> Unit
                PicturesState.NoAccess -> PermissionNote(
                    message = stringResource(R.string.usb_no_permission),
                    command = adbCommand,
                    buttonText = stringResource(R.string.check_again),
                    theme = theme,
                    onCheck = onCheckAccess,
                    buttonModifier = memory.track("check"),
                )
                is PicturesState.Loaded ->
                    if (pictures.images.isEmpty()) {
                        Text(stringResource(R.string.bg_usb_empty), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(4.dp)) {
                            pictures.images.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { image ->
                                        PictureThumb(
                                            image = image,
                                            theme = theme,
                                            thumbnail = thumbnail,
                                            onFocused = {},
                                            onClick = { onChoose(image) },
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
