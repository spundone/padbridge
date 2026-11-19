package dev.padbridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.padbridge.Root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Experimental root features: Bluetooth speaker (A2DP sink), device class, controller identity. */
@Composable
fun RootScreen(onBack: () -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(true) }
    var rooted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf(false) }
    var cod by remember { mutableStateOf("") }
    var did by remember { mutableStateOf("") }

    fun refresh() {
        speaker = Root.speakerEnabled(); cod = Root.currentClass(); did = Root.currentControllerId()
    }

    LaunchedEffect(Unit) {
        rooted = withContext(Dispatchers.IO) { Root.available() }
        withContext(Dispatchers.IO) { refresh() }
        checking = false
    }

    /** Apply property changes, restart Bluetooth, and show what actually happened. */
    fun apply(what: String, block: () -> List<Root.Result>) {
        busy = true
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                val results = block()
                val restart = Root.restartBluetooth()
                refresh()
                buildString {
                    appendLine(what)
                    results.forEach { appendLine((if (it.ok) "✓ " else "✗ ") + it.out) }
                    appendLine(if (restart.ok) "✓ Bluetooth restarted" else "✗ Bluetooth restart: ${restart.out}")
                }
            }
            log = out.trim()
            busy = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Root mode") },
                subtitle = { Text("Experimental · resets on reboot") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.padding(padding).fillMaxWidth(),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Warning, null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "These change Bluetooth system properties with root, then restart Bluetooth (connections drop " +
                                "for a few seconds). Nothing is permanent: rebooting the phone restores the defaults. " +
                                "Computers remember a device's class and ID when they pair, so remove and re-pair after changing them.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        checking -> { LoadingIndicator(Modifier.size(32.dp)); Spacer(Modifier.width(8.dp)); Text("Checking for root…") }
                        rooted -> { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Root access granted") }
                        else -> Text(
                            "No root access. These features need Magisk or KernelSU, and PadBridge allowed in the root manager.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (busy) { Spacer(Modifier.weight(1f)); LoadingIndicator(Modifier.size(32.dp)) }
                }
            }

            // ---------------------------------------------------------- speaker
            item { SectionHeader("Bluetooth speaker") }
            item {
                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    colors = segColors(),
                    leadingContent = { Icon(Icons.Rounded.Speaker, null) },
                    supportingContent = {
                        Column {
                            Text(
                                "Turns on Android's A2DP sink and AVRCP controller, so a computer can pick this phone as an " +
                                    "audio output and play through its speaker. Headphones keep working (source + sink coexist). " +
                                    "After enabling, pair or reconnect from the computer's sound settings."
                            )
                            Spacer(Modifier.padding(4.dp))
                            Text(
                                if (speaker) "Status: enabled" else "Status: off",
                                color = if (speaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = rooted && !busy && !speaker, onClick = {
                                    apply("Enable speaker") { Root.setSpeaker(true) }
                                }) { Text("Enable") }
                                OutlinedButton(enabled = rooted && !busy && speaker, onClick = {
                                    apply("Disable speaker") { Root.setSpeaker(false) }
                                }) { Text("Disable") }
                            }
                        }
                    },
                ) { Text("Receive audio (A2DP sink)") }
            }

            // ---------------------------------------------------------- class of device
            item { SectionHeader("Device class") }
            item {
                Text(
                    "Windows looks at the phone's Bluetooth class. As a phone it enables audio profiles and leaves HID off; " +
                        "as a keyboard + mouse it should load the trackpad and keyboard driver directly. Current: " +
                        (cod.ifEmpty { "phone default" }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
            }
            val classes = Root.DeviceClass.entries
            items(classes.size) { i ->
                val c = classes[i]
                val selected = if (c == Root.DeviceClass.DEFAULT) Root.DeviceClass.entries.none { it != Root.DeviceClass.DEFAULT && it.value == cod }
                else cod == c.value
                SegmentedListItem(
                    onClick = { if (rooted && !busy) apply("Device class: ${c.label}") { listOf(Root.setDeviceClass(c)) } },
                    enabled = rooted && !busy,
                    shapes = ListItemDefaults.segmentedShapes(i, classes.size),
                    colors = segColors(),
                    supportingContent = { Text(c.blurb) },
                    trailingContent = if (selected) ({ Icon(Icons.Rounded.Check, null) }) else null,
                ) { Text(c.label) }
            }

            // ---------------------------------------------------------- controller identity
            item { SectionHeader("Controller identity") }
            item {
                Text(
                    "Reports a game controller's vendor and product ID in the Bluetooth Device ID record, which is how " +
                        "Windows, Linux, SDL and Steam pick a controller driver. The gamepad's reports stay in PadBridge's " +
                        "generic layout, so a driver expecting the real controller's format may misread buttons; this is for " +
                        "experimenting. Current: " + (did.ifEmpty { "phone default" }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
            }
            val ids = Root.ControllerId.entries
            items(ids.size) { i ->
                val c = ids[i]
                val selected = if (c == Root.ControllerId.NONE) did.isEmpty() else did == "${c.vendor}:${c.product}"
                SegmentedListItem(
                    onClick = { if (rooted && !busy) apply("Controller identity: ${c.label}") { Root.setControllerId(c) } },
                    enabled = rooted && !busy,
                    shapes = ListItemDefaults.segmentedShapes(i, ids.size),
                    colors = segColors(),
                    leadingContent = { Icon(Icons.Rounded.Science, null) },
                    supportingContent = { Text(c.blurb) },
                    trailingContent = if (selected) ({ Icon(Icons.Rounded.Check, null) }) else null,
                ) { Text(c.label) }
            }

            if (log.isNotEmpty()) {
                item { SectionHeader("Last change") }
                item {
                    Text(
                        log, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp).padding(bottom = 24.dp),
                    )
                }
            }
        }
    }
}
