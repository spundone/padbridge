package dev.padbridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.padbridge.HidManager
import dev.padbridge.Settings
import dev.padbridge.TabletView

/**
 * Drawing tablet: the pad's active area is the computer's whole screen (absolute position, hover, pressure).
 * Opened from the trackpad's menu.
 */
@Composable
fun TabletScreen(hid: HidUi, onBack: () -> Unit) {
    var ratio by remember { mutableFloatStateOf(Settings.tabletRatio) }
    var editing by remember { mutableStateOf(false) }
    var hover by remember { mutableStateOf(Settings.tabletFingerHover) }
    var sendAs by remember { mutableIntStateOf(Settings.tabletSendAs) }
    var enabled by remember { mutableStateOf(Settings.tablet) }
    var ratioMenu by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf<TabletView?>(null) }
    // Works once the descriptor has it (re-pair), or right away in "Control this phone".
    val ready = (enabled && HidManager.tabletRegistered) || hid.local

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to trackpad") }
            Text("Drawing tablet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            // Ratio: match the computer's display so circles stay round.
            androidx.compose.foundation.layout.Box {
                FilterChip(
                    selected = true, onClick = { ratioMenu = true },
                    label = { Text(if (ratio <= 0f) "Whole pad" else TabletView.ratioName(ratio)) },
                    leadingIcon = { Icon(Icons.Rounded.AspectRatio, null, Modifier.size(18.dp)) },
                )
                DropdownMenu(ratioMenu, { ratioMenu = false }, shape = RoundedCornerShape(16.dp), containerColor = glassMenuColor()) {
                    TabletView.RATIOS.forEach { (r, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            trailingIcon = if (kotlin.math.abs(r - ratio) < 0.01f) ({ Icon(Icons.Rounded.Check, null) }) else null,
                            onClick = { ratioMenu = false; ratio = r; Settings.tabletRatio = r; view?.layoutArea() },
                        )
                    }
                    DropdownMenuItem(text = { Text("Custom…") }, onClick = { ratioMenu = false; custom = true })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Whole pad (stretch)") },
                        trailingIcon = if (ratio <= 0f) ({ Icon(Icons.Rounded.Check, null) }) else null,
                        onClick = { ratioMenu = false; ratio = 0f; Settings.tabletRatio = 0f; view?.layoutArea() },
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            FilledTonalIconToggleButton(checked = editing, onCheckedChange = { editing = it; view?.editing = it }) {
                Icon(if (editing) Icons.Rounded.Check else Icons.Rounded.Edit, if (editing) "Done" else "Move or resize the area")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FilterChip(
                selected = !hover, onClick = { hover = false; Settings.tabletFingerHover = false },
                label = { Text("Finger draws") }, leadingIcon = { Icon(Icons.Rounded.Draw, null, Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = hover, onClick = { hover = true; Settings.tabletFingerHover = true },
                label = { Text("Finger hovers") }, leadingIcon = { Icon(Icons.Rounded.PanTool, null, Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = true,
                onClick = { sendAs = (sendAs + 1) % 3; Settings.tabletSendAs = sendAs },
                label = { Text(when (sendAs) { 1 -> "Pen"; 2 -> "Mouse"; else -> "Auto" }) },
                leadingIcon = { Icon(if (sendAs == 2) Icons.Rounded.Mouse else Icons.Rounded.Brush, null, Modifier.size(18.dp)) },
            )
        }

        if (!ready) Surface(
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (enabled) "Remove this phone on the computer and pair it again, so it sees the tablet."
                    else "The tablet is a new kind of device for the computer: turn it on, then pair again once.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!enabled) FilledTonalButton(
                    onClick = { enabled = true; Settings.tablet = true; HidManager.reRegister() },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Turn on drawing tablet") }
            }
        }

        AndroidView(
            factory = { ctx -> TabletView(ctx).also { view = it } },
            update = { v -> v.editing = editing; hid.version; v.invalidate() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Text(
            when {
                hover -> "S Pen hovers and presses with pressure. Finger: move to point, tap to click, double-tap and hold to drag, two-finger tap to right-click."
                else -> "S Pen hovers and presses with pressure; its button right-clicks. Finger: touch to draw."
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
    }

    if (custom) {
        var w by remember { mutableStateOf("16") }
        var h by remember { mutableStateOf("9") }
        AlertDialog(
            onDismissRequest = { custom = false },
            title = { Text("Display ratio") },
            text = {
                Column {
                    Text("Your display's width and height (pixels, or the ratio).", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(w, { w = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), label = { Text("Width") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Text("  :  ")
                        OutlinedTextField(h, { h = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), label = { Text("Height") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val r = (w.toFloatOrNull() ?: 0f) / (h.toFloatOrNull() ?: 0f)
                    if (r.isFinite() && r > 0.2f && r < 6f) { ratio = r; Settings.tabletRatio = r; view?.layoutArea() }
                    custom = false
                }) { Text("Use") }
            },
            dismissButton = { TextButton(onClick = { custom = false }) { Text("Cancel") } },
            containerColor = glassDialogColor(),
        )
    }
}
