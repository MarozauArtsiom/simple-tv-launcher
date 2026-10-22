package io.github.marozauartsiom.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * "This needs a permission that only adb can grant": the message, the exact command in a code box,
 * and a button to check again. Shared by the background picker and the USB installer.
 */
@Composable
fun PermissionNote(
    message: String,
    command: String,
    buttonText: String,
    theme: ThemeSpec,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
) {
    val c = theme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth().background(c.itemBg, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Warning, null, tint = c.panelMuted, modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.panelFg)
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp),
                    color = c.panelFg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
        PanelButton(buttonText, theme, onCheck, buttonModifier)
    }
}
