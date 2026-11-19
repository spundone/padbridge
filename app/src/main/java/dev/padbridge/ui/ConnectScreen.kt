@file:SuppressLint("MissingPermission")

package dev.padbridge.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.padbridge.HidManager
import dev.padbridge.HidManager.State
import dev.padbridge.HidService
import dev.padbridge.HostOs
import dev.padbridge.Settings

@Composable
fun ConnectScreen(
    hid: HidUi, osVersion: Int, onOsChanged: () -> Unit, onOpenSettings: () -> Unit,
    onOpen: (Dest) -> Unit = {},
) {
    val ctx = LocalContext.current
    val hosts = remember(hid.version) { HidManager.bondedHosts(Settings.showAllDevices) }
    val phoneName = remember(hid.version) {
        try { BluetoothAdapter.getDefaultAdapter()?.name } catch (_: SecurityException) { null } ?: "this phone"
    }

    var nearby by remember { mutableStateOf(false) }
    if (nearby) NearbySheet(hid) { nearby = false }

    LazyColumn(
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item { StatusHero(hid) }
        // The navigation bar already switches modes, so no shortcut tiles here. While connected, the
        // status card names the computer and has Disconnect, so the list shows only the others.
        val shown = if (hid.connected) hosts.filterNot { HidManager.sameHost(it, hid.hostAddress, hid.hostName) } else hosts

        if (shown.isNotEmpty() || !hid.connected) item { SectionHeader(if (hid.connected) "Other computers" else "Computers") }
        if (hosts.isEmpty()) {
            item {
                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    colors = segColors(),
                    leadingContent = { Icon(Icons.Outlined.Info, null) },
                    supportingContent = { Text("Pair a computer to see it here.") },
                ) { Text("No paired computers yet") }
            }
        }
        itemsIndexed(shown, key = { _, d -> d.address }) { i, d ->
            // The connected device can be a duplicate bond of the same PC, so match by name too.
            val connected = hid.connected && HidManager.sameHost(d, hid.hostAddress, hid.hostName)
            val connecting = !connected && hid.connectingAddress != null &&
                HidManager.sameHost(d, hid.connectingAddress, if (hid.state == State.CONNECTING) hid.hostName else null)
            SegmentedListItem(
                onClick = { if (!connected) HidManager.connect(d) },
                shapes = ListItemDefaults.segmentedShapes(i, shown.size),
                colors = if (connected) ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    supportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) else segColors(),
                leadingContent = {
                    Surface(
                        shape = if (connected) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
                        color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (connected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Computer, null) } }
                },
                supportingContent = {
                    Text(
                        when {
                            connected -> "Connected"
                            connecting -> "Connecting…"
                            d.address == Settings.lastHost -> "Last used"
                            else -> d.address
                        }
                    )
                },
                trailingContent = {
                    when {
                        connected -> FilledTonalIconButton(onClick = { HidManager.disconnect() }) {
                            Icon(Icons.Rounded.LinkOff, "Disconnect")
                        }
                        connecting -> ContainedLoadingIndicator(Modifier.size(40.dp))
                        else -> FilledTonalButton(onClick = { HidManager.connect(d) }) {
                            Text(if (hid.connected) "Switch" else "Connect")
                        }
                    }
                },
            ) { Text(HidManager.name(d), fontWeight = FontWeight.SemiBold) }
        }

        item {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { nearby = true },
                    modifier = Modifier.weight(1f).height(ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.MediumContentPadding,
                ) {
                    Icon(Icons.Rounded.Search, null, Modifier.size(ButtonDefaults.MediumIconSize))
                    Spacer(Modifier.width(ButtonDefaults.MediumIconSpacing))
                    Text("Find nearby computers", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { HidService.start(ctx); HidManager.restart(ctx) },
                    modifier = Modifier.size(ButtonDefaults.MediumContainerHeight),
                ) { Icon(Icons.Rounded.Refresh, "Refresh") }
            }
        }
        item {
            androidx.compose.material3.TextButton(
                onClick = {
                    try {
                        ctx.startActivity(
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                        )
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Couldn't make the phone discoverable: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
            ) {
                Icon(Icons.Rounded.Visibility, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Or make this phone visible and pair from the computer")
            }
        }
        item { PairingHelp(phoneName) }

        item { SectionHeader("Computer type") }
        item { OsSelector(osVersion, onOsChanged) }
        item {
            Text(
                "Sets keyboard labels, shortcuts and what gestures send.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        item { Gap(24) }
    }
}

@Composable
internal fun NearbySheet(hid: HidUi, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var permitted by remember { mutableStateOf(dev.padbridge.Perms.hasScan(ctx)) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        permitted = dev.padbridge.Perms.hasScan(ctx)
        if (permitted) HidManager.startDiscovery()
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (permitted) HidManager.startDiscovery() else launcher.launch(dev.padbridge.Perms.scan())
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { HidManager.stopDiscovery() } }

    // Computers first, then other named devices; skip nameless ones (usually phones' random addresses).
    val devices = remember(hid.version) {
        HidManager.found.toList()
            // Classic only: the HID Device profile can't use LE-only entries (Windows also advertises over LE).
            .filter { HidManager.isClassic(it) }
            .filter { HidManager.name(it) != it.address || HidManager.isComputer(it) }
            .sortedWith(compareBy({ !HidManager.isComputer(it) }, { HidManager.name(it).lowercase() }))
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Nearby computers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (dev.padbridge.Settings.hostOs) {
                                HostOs.WINDOWS -> "On the PC, open Settings › Bluetooth & devices and keep it open so the PC is visible."
                                HostOs.MAC -> "On the Mac, open System Settings › Bluetooth and keep it open."
                                HostOs.LINUX -> "Make the computer discoverable (e.g. open its Bluetooth settings)."
                            },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (hid.discovering) androidx.compose.material3.LoadingIndicator(Modifier.size(40.dp))
                    else androidx.compose.material3.FilledTonalIconButton(onClick = {
                        if (dev.padbridge.Perms.hasScan(ctx)) HidManager.startDiscovery() else launcher.launch(dev.padbridge.Perms.scan())
                    }) { Icon(Icons.Rounded.Refresh, "Scan again") }
                }
                Spacer(Modifier.height(12.dp))
                if (hid.state == State.CONNECTING) {
                    Text(hid.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                if (!permitted) {
                    Text("PadBridge needs the Nearby devices permission to look for computers.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            if (devices.isEmpty() && permitted) item {
                Text(
                    if (hid.discovering) "Looking…" else "Nothing found. Check the computer is visible, then scan again.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
            items(devices.size, key = { devices[it].address }) { i ->
                val d = devices[i]
                val computer = HidManager.isComputer(d)
                val bonded = try { d.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false }
                SegmentedListItem(
                    onClick = { HidManager.pairAndConnect(d) },
                    shapes = ListItemDefaults.segmentedShapes(i, devices.size),
                    colors = segColors(),
                    leadingContent = {
                        Icon(if (computer) Icons.Rounded.Computer else Icons.Rounded.Bluetooth, null)
                    },
                    supportingContent = { Text(if (bonded) "Paired · ${d.address}" else d.address) },
                    trailingContent = {
                        FilledTonalButton(onClick = { HidManager.pairAndConnect(d) }) { Text(if (bonded) "Connect" else "Pair") }
                    },
                ) { Text(HidManager.name(d), fontWeight = if (computer) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
    }
}

@Composable
fun OsSelector(osVersion: Int, onOsChanged: () -> Unit) {
    var os by remember(osVersion) { mutableStateOf(Settings.hostOs) }
    val icons = mapOf(
        HostOs.WINDOWS to Icons.Rounded.DesktopWindows,
        HostOs.MAC to Icons.Rounded.LaptopMac,
        HostOs.LINUX to Icons.Rounded.Terminal,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.fillMaxWidth()) {
        HostOs.entries.forEachIndexed { i, o ->
            ToggleButton(
                checked = os == o,
                onCheckedChange = {
                    os = o; Settings.hostOs = o; onOsChanged()
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    HostOs.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Icon(if (os == o) Icons.Rounded.Check else icons.getValue(o), null, Modifier.size(ToggleButtonDefaults.IconSize))
                Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                Text(o.label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StatusHero(hid: HidUi) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val busy = hid.state == State.CONNECTING || hid.state == State.REGISTERING
    val (container, content) = when (hid.state) {
        State.CONNECTED -> cs.primaryContainer to cs.onPrimaryContainer
        State.CONNECTING, State.REGISTERING -> cs.tertiaryContainer to cs.onTertiaryContainer
        State.READY -> cs.surfaceContainerHigh to cs.onSurface
        else -> cs.errorContainer to cs.onErrorContainer
    }
    val title = when (hid.state) {
        State.CONNECTED -> hid.hostName ?: "Connected"
        State.CONNECTING -> "Connecting…"
        State.READY -> "Ready to connect"
        State.REGISTERING -> "Starting…"
        State.BLUETOOTH_OFF -> "Bluetooth is off"
        State.NO_PERMISSION -> "Permission needed"
        State.UNSUPPORTED -> if (hid.needsBluetoothRestart) "Restart Bluetooth" else "Not supported"
        State.IDLE -> "Stopped"
    }
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(24.dp)) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (busy) {
                    ContainedLoadingIndicator(Modifier.size(72.dp))
                } else {
                    Surface(
                        shape = (if (hid.connected) MaterialShapes.SoftBurst else MaterialShapes.Cookie9Sided).toShape(),
                        // Solid tokens (not alpha over the card) so the shape reads in every palette.
                        color = when (hid.state) {
                            State.CONNECTED -> cs.primary
                            State.READY -> cs.secondaryContainer
                            else -> cs.surfaceContainerLowest
                        },
                        contentColor = when (hid.state) {
                            State.CONNECTED -> cs.onPrimary
                            State.READY -> cs.onSecondaryContainer
                            else -> content
                        },
                        modifier = Modifier.size(72.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (hid.state) {
                                    State.CONNECTED -> Icons.Rounded.Check
                                    State.READY -> Icons.Rounded.Bluetooth
                                    State.BLUETOOTH_OFF -> Icons.Rounded.BluetoothDisabled
                                    else -> Icons.Rounded.ErrorOutline
                                },
                                null, Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
            Gap(16)
            if (hid.connected) Text("Connected", style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            // The detail repeats the computer's name once connected; the eyebrow above says it instead.
            if (!hid.connected && hid.detail.isNotEmpty()) {
                Gap(4)
                Text(hid.detail, style = MaterialTheme.typography.bodyMedium)
            }
            val showRetry = hid.state in setOf(State.UNSUPPORTED, State.IDLE, State.BLUETOOTH_OFF, State.NO_PERMISSION)
            if (hid.connected || hid.state == State.CONNECTING || showRetry) {
                Gap(16)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hid.connected || hid.state == State.CONNECTING) {
                        OutlinedButton(onClick = { HidManager.disconnect() }) { Text("Disconnect") }
                    }
                    if (hid.needsBluetoothRestart || hid.state == State.BLUETOOTH_OFF) {
                        Button(onClick = {
                            ctx.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                        }) { Text("Open Bluetooth settings") }
                    } else if (showRetry) {
                        Button(onClick = { HidService.start(ctx); HidManager.restart(ctx) }) { Text("Retry") }
                    }
                }
            }
        }
    }
}


/** Pairing tips, folded away until asked for so the Connect screen stays short. */
@Composable
private fun PairingHelp(phoneName: String) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        androidx.compose.material3.TextButton(onClick = { open = !open }) {
            Icon(Icons.Outlined.Info, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (open) "Hide pairing help" else "How pairing works")
        }
        androidx.compose.animation.AnimatedVisibility(open) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Find nearby computers pairs from the phone, then connects the trackpad and keyboard. Keep the " +
                        "computer's Bluetooth settings open while it scans, and accept the prompt on both screens.",
                    "Or make this phone visible and add “$phoneName” from the computer's Bluetooth settings.",
                    "Windows: if it pairs but the trackpad doesn't work, open Control Panel › Devices and Printers, " +
                        "right-click “$phoneName” › Properties › Services and tick “Drivers for keyboard, mice, etc. (HID)”.",
                    "Paired before PadBridge was installed (for example via Phone Link)? Remove the pairing on both " +
                        "sides and pair again.",
                ).forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
