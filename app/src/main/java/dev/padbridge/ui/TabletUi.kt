package dev.padbridge.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LineWeight
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.AutoFixNormal
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
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

/** One express key: a tap sends its shortcut, or (for Pan) it's held while pressed. */
private data class ExpressKey(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val hold: Int = 0,
    val action: () -> Unit = {})

/** Shortcut buttons for drawing apps: undo, redo, brush size, eraser, pan (holds Space), zoom. */
@Composable
private fun ExpressKeys(vertical: Boolean, view: TabletView?, modifier: Modifier) {
    val mac = Settings.hostOs == dev.padbridge.HostOs.MAC
    val mod = if (mac) dev.padbridge.Keys.L_GUI else dev.padbridge.Keys.L_CTRL
    val keys = listOf(
        ExpressKey("Undo", Icons.AutoMirrored.Rounded.Undo) { view?.undoCombo()?.let { HidManager.combo(it) } },
        ExpressKey("Redo", Icons.AutoMirrored.Rounded.Redo) { view?.redoCombo()?.let { HidManager.combo(it) } },
        ExpressKey("Smaller", Icons.Rounded.RemoveCircleOutline) { HidManager.tap(dev.padbridge.Keys.LBRACKET) },
        ExpressKey("Bigger", Icons.Rounded.AddCircleOutline) { HidManager.tap(dev.padbridge.Keys.RBRACKET) },
        ExpressKey("Eraser", Icons.Rounded.AutoFixNormal) { HidManager.tap(dev.padbridge.Keys.letter('e')) },
        ExpressKey("Pan", Icons.Rounded.PanTool, hold = dev.padbridge.Keys.SPACE),
        ExpressKey("Zoom −", Icons.Rounded.ZoomOut) { HidManager.combo(dev.padbridge.Combo(mod, dev.padbridge.Keys.MINUS)) },
        ExpressKey("Zoom +", Icons.Rounded.ZoomIn) { HidManager.combo(dev.padbridge.Combo(mod, dev.padbridge.Keys.EQUAL)) },
    )
    val item: @Composable (ExpressKey, Modifier) -> Unit = { k, m ->
        // (vertical: labels under the icons)
        var pressed by remember { mutableStateOf(false) }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (pressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = m.expressPress(k) { pressed = it },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                // Under the area (narrow keys) the icons stand alone; their names are still read out.
                Icon(k.icon, if (vertical) null else k.label, Modifier.size(22.dp))
                if (vertical) Text(k.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
    if (vertical) Column(modifier.width(76.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEach { item(it, Modifier.fillMaxWidth().weight(1f)) }
    } else Row(modifier.height(52.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEach { item(it, Modifier.weight(1f).fillMaxHeight()) }
    }
}

/** Tap = the key's action; hold keys (Pan) stay down while the finger does. */
private fun Modifier.expressPress(k: ExpressKey, onPressed: (Boolean) -> Unit): Modifier =
    pointerInput(k.label) {
        detectTapGestures(onPress = {
            onPressed(true); dev.padbridge.Haptics.click()
            if (k.hold != 0) HidManager.setKeyboardState(0, listOf(k.hold)) else k.action()
            tryAwaitRelease()
            if (k.hold != 0) HidManager.setKeyboardState(0, emptyList())
            onPressed(false)
        })
    }

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
    var curve by remember { mutableIntStateOf(Settings.tabletCurve) }
    var keysOn by remember { mutableStateOf(Settings.tabletKeys) }
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
            IconButton(onClick = { keysOn = !keysOn; Settings.tabletKeys = keysOn }) {
                Icon(Icons.Rounded.Keyboard, if (keysOn) "Hide express keys" else "Show express keys")
            }
            FilledTonalIconToggleButton(checked = editing, onCheckedChange = { editing = it; view?.editing = it }) {
                Icon(if (editing) Icons.Rounded.Check else Icons.Rounded.Edit, if (editing) "Done" else "Move or resize the area")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp).horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
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
            // Pen pressure curve, like a tablet driver's: soft reaches full pressure with a lighter touch.
            FilterChip(
                selected = true,
                onClick = { curve = (curve + 1) % 3; Settings.tabletCurve = curve },
                label = { Text(when (curve) { 0 -> "Soft"; 2 -> "Firm"; else -> "Linear" }) },
                leadingIcon = { Icon(Icons.Rounded.LineWeight, null, Modifier.size(18.dp)) },
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

        val pad: @Composable (Modifier) -> Unit = { m ->
            AndroidView(
                factory = { ctx -> TabletView(ctx).also { view = it } },
                update = { v -> v.editing = editing; hid.version; v.invalidate() },
                modifier = m,
            )
        }
        // Express keys beside the area (left when sideways, under it upright), like a Wacom's.
        if (keysOn && landscape) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                ExpressKeys(vertical = true, view = view, modifier = Modifier.fillMaxHeight().padding(end = 8.dp))
                pad(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            pad(Modifier.weight(1f).fillMaxWidth())
            if (keysOn) ExpressKeys(vertical = false, view = view, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        Text(
            when {
                hover -> "S Pen hovers and draws with pressure; your hand can rest on the glass. Finger: move to point, " +
                    "tap to click, double-tap and hold to drag."
                else -> "S Pen hovers and draws with pressure; your hand can rest on the glass. Finger: draw. Two " +
                    "fingers: pan, pinch to zoom, tap to undo; three-finger tap redoes."
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
