package io.github.marozauartsiom.tvlauncher.ui.shortcuts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.SaveResult
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcuts
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelButton
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelTextField
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.home.ShortcutColors
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import kotlinx.coroutines.launch

/** What the page is editing: an existing shortcut ([id] set) or a new one. */
private data class Draft(val id: String?, val title: String, val address: String)

private const val AddId = "add"

/**
 * Settings → Website shortcuts: the list (an "Add shortcut" row, then the shortcuts), and the editor
 * (name, address, Save / Delete / Cancel). Back leaves the editor first, then the page.
 *
 * [onSave] stores the shortcut; [onDelete] removes it (and from Favourites).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutsPage(
    theme: ThemeSpec,
    shortcuts: List<Shortcut>,
    onSave: suspend (id: String?, title: String, address: String) -> SaveResult,
    onDelete: suspend (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf<Draft?>(null) }
    val memory = remember { FocusMemory() }
    var wasEditing by remember { mutableStateOf(false) }

    BackHandler(enabled = draft != null) { draft = null }

    // Back from the editor: focus returns to the row that was open (or to "Add").
    LaunchedEffect(draft == null) {
        if (draft == null && wasEditing) {
            withFrameNanos { }
            if (!memory.restore()) memory.request(AddId)
        }
        wasEditing = draft != null
    }

    val current = draft
    SidePanel(
        title = stringResource(if (current == null) R.string.item_shortcuts else R.string.shortcut_edit_title),
        theme = theme,
        modifier = modifier,
    ) {
        if (current == null) {
            ShortcutList(theme, shortcuts, memory, onAdd = { draft = Draft(null, "", "") }, onEdit = { draft = Draft(it.id, it.title, it.url) })
        } else {
            ShortcutEditor(
                theme = theme,
                initial = current,
                onSave = onSave,
                onDelete = onDelete,
                onClose = { draft = null },
            )
        }
    }
}

@Composable
private fun ShortcutList(
    theme: ThemeSpec,
    shortcuts: List<Shortcut>,
    memory: FocusMemory,
    onAdd: () -> Unit,
    onEdit: (Shortcut) -> Unit,
) {
    // Only the first time: coming back from the editor, focus returns to the row that was open.
    LaunchedEffect(Unit) { withFrameNanos { }; if (memory.last == null) memory.request(AddId) }
    PanelScroll {
        PanelItem(
            title = stringResource(R.string.shortcut_add),
            theme = theme,
            icon = Icons.Plus,
            onClick = onAdd,
            modifier = memory.track(AddId),
        )
        shortcuts.forEach { shortcut ->
            val id = "row:${shortcut.id}"
            PanelRow(theme, onClick = { onEdit(shortcut) }, modifier = memory.track(id), height = 56.dp) {
                ShortcutArt(shortcut)
                Column(Modifier.weight(1f)) {
                    Text(shortcut.title, style = MaterialTheme.typography.titleMedium, color = theme.colors.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(Shortcuts.displayAddress(shortcut.url), style = MaterialTheme.typography.bodyMedium, color = theme.colors.panelMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Chevron, null, tint = theme.colors.panelMuted, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            text = stringResource(R.string.shortcut_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = theme.colors.panelMuted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** The mini tile in the list: the same letter and colour the shortcut has on Home. */
@Composable
private fun ShortcutArt(shortcut: Shortcut) {
    Box(
        Modifier.size(72.dp, 40.dp).background(ShortcutColors.forUrl(shortcut.url), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            shortcut.title.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            color = Color(0xFFF4F5F8),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutEditor(
    theme: ThemeSpec,
    initial: Draft,
    onSave: suspend (String?, String, String) -> SaveResult,
    onDelete: suspend (String) -> Unit,
    onClose: () -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var address by remember { mutableStateOf(initial.address) }
    var invalid by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val memory = remember { FocusMemory() }

    LaunchedEffect(Unit) { withFrameNanos { }; memory.request("title") }

    fun save() {
        scope.launch {
            when (onSave(initial.id, title, address)) {
                is SaveResult.Saved -> onClose()
                SaveResult.Invalid -> invalid = true
            }
        }
    }

    PanelScroll(spacing = 6.dp) {
        Label(stringResource(R.string.shortcut_name), theme)
        PanelTextField(
            value = title,
            onValueChange = { title = it; invalid = false },
            theme = theme,
            surfaceFocus = memory.requester("title"),
            modifier = Modifier.onFocusTrack(memory, "title"),
        )
        Label(stringResource(R.string.shortcut_address), theme, Modifier.padding(top = 6.dp))
        PanelTextField(
            value = address,
            onValueChange = { address = it; invalid = false },
            theme = theme,
            icon = Icons.Link,
            placeholder = "example.com",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
            onImeAction = ::save,
            surfaceFocus = memory.requester("address"),
            modifier = Modifier.onFocusTrack(memory, "address"),
        )
        if (invalid) {
            InlineNote(theme, stringResource(R.string.shortcut_invalid))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanelButton(stringResource(R.string.save), theme, onClick = ::save)
            if (initial.id != null) {
                PanelButton(stringResource(R.string.delete), theme, onClick = { scope.launch { onDelete(initial.id); onClose() } })
            }
            PanelButton(stringResource(R.string.cancel), theme, onClick = onClose)
        }
    }
}

@Composable
private fun Label(text: String, theme: ThemeSpec, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = theme.colors.panelMuted, modifier = modifier.padding(start = 2.dp))
}

/** An inline problem line: a warning icon and the message in the normal text colour (state is never colour alone). */
@Composable
private fun InlineNote(theme: ThemeSpec, text: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Warning, null, tint = theme.colors.panelFg, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = theme.colors.panelFg)
    }
}

private fun Modifier.onFocusTrack(memory: FocusMemory, id: String): Modifier = this.then(memory.trackOnly(id))
