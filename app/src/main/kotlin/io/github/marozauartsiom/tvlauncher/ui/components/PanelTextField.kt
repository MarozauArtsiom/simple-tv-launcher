package io.github.marozauartsiom.tvlauncher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * A text field for the remote. A plain text field does not open the keyboard on OK and Back would
 * leave the screen, so the field sits inside a focusable surface: OK on the surface starts typing
 * (and shows Gboard), and Back or Escape stop typing and put focus back on the surface. The
 * surface carries the focus ring (also while typing).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PanelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    theme: ThemeSpec,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    onImeAction: () -> Unit = {},
    surfaceFocus: FocusRequester = remember { FocusRequester() },
) {
    val c = theme.colors
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val progress = rememberFocusProgress()
    val shape = remember { RoundedCornerShape(12.dp) }
    val textStyle = MaterialTheme.typography.titleMedium.copy(color = c.panelFg)
    // The caret starts at the end of the text, where the owner wants to continue typing.
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))

    Surface(
        onClick = { runCatching { fieldFocus.requestFocus() }; keyboard?.show() },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .focusRequester(surfaceFocus)
            .then(progress.track())
            .drawBehind {
                val p = progress.value
                if (p > 0f) {
                    val w = 3.dp.toPx()
                    drawRoundRect(
                        color = c.itemFocusRing.copy(alpha = p),
                        topLeft = Offset(-w / 2f, -w / 2f),
                        size = Size(size.width + w, size.height + w),
                        cornerRadius = CornerRadius(12.dp.toPx() + w / 2f),
                        style = Stroke(w),
                    )
                }
            },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = c.itemBg,
            focusedContainerColor = c.itemBg,
            pressedContainerColor = c.itemBg,
            contentColor = c.panelFg,
            focusedContentColor = c.panelFg,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = field,
                onValueChange = { field = it; if (it.text != value) onValueChange(it.text) },
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(c.panelFg),
                keyboardOptions = KeyboardOptions(imeAction = imeAction, keyboardType = keyboardType),
                keyboardActions = KeyboardActions(onAny = {
                    keyboard?.hide()
                    runCatching { surfaceFocus.requestFocus() }
                    onImeAction()
                }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(fieldFocus)
                    .onPreviewKeyEvent { event ->
                        // Back/Escape end typing instead of leaving the screen.
                        val leave = event.key == Key.Back || event.key == Key.Escape
                        if (leave && event.type == KeyEventType.KeyUp) {
                            keyboard?.hide()
                            runCatching { surfaceFocus.requestFocus() }
                        }
                        leave
                    },
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = MaterialTheme.typography.titleMedium, color = c.panelMuted, maxLines = 1)
                    }
                    inner()
                },
            )
        }
    }
}
