package dev.padbridge.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.padbridge.HidManager
import dev.padbridge.HidService
import dev.padbridge.HostOs
import dev.padbridge.MainActivity
import dev.padbridge.Perms
import dev.padbridge.Settings

private const val STEPS = 5

/**
 * First-run walkthrough: welcome, permissions, computer type, pairing, then where to start. Each step has one
 * obvious button; steps that are already done (permission granted, computer connected) move on by themselves.
 */
@Composable
fun SetupScreen(hid: HidUi, activity: MainActivity, onDone: (Dest?) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var permTick by remember { mutableIntStateOf(0) }
    val granted = remember(permTick, hid.version) { Perms.hasConnect(activity) && Perms.hasScan(activity) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permTick++
        if (Perms.hasConnect(activity)) activity.startHid()
    }
    val askAll = {
        launcher.launch((Perms.runtime().toList() + Perms.scan().toList()).distinct().toTypedArray())
    }
    // Granted: nothing left to do on this step.
    androidx.compose.runtime.LaunchedEffect(granted, step) {
        if (step == 1 && granted && permTick > 0) { kotlinx.coroutines.delay(450); step = 2 }
    }
    // Connected while pairing: show the check for a moment, then move on.
    androidx.compose.runtime.LaunchedEffect(hid.connected, step) {
        if (step == 3 && hid.connected) { kotlinx.coroutines.delay(1200); step = 4 }
    }
    val finish: (Dest?) -> Unit = { Settings.setupDone = true; onDone(it) }
    androidx.activity.compose.BackHandler(enabled = step > 0) { step-- }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        // Tablets: a readable column, not text stretched across the screen.
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearWavyProgressIndicator(progress = { (step + 1f) / STEPS }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                if (step < STEPS - 1) TextButton(onClick = { finish(null) }) { Text("Skip setup") }
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "setup",
            ) { s ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
                    when (s) {
                        0 -> Welcome()
                        1 -> PermissionsStep(activity, permTick, askAll)
                        2 -> ComputerStep { step = 3 }
                        3 -> PairStep(hid)
                        else -> DoneStep(hid, finish)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
                Spacer(Modifier.weight(1f))
                // Permissions not granted yet: the main button asks for them; "Not now" moves on without.
                if (step == 1 && !granted) TextButton(onClick = { step++ }) { Text("Not now") }
                if (step == 3 && !hid.connected) TextButton(onClick = { step++ }) { Text("Pair later") }
                if (step != 3 || hid.connected) Button(
                    onClick = {
                        when {
                            step == 1 && !granted -> askAll()
                            step < STEPS - 1 -> step++
                            else -> finish(Dest.TRACKPAD)
                        }
                    },
                    modifier = Modifier.height(ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.MediumContentPadding,
                ) {
                    Text(
                        when {
                            step == 0 -> "Get started"
                            step == 1 && !granted -> "Allow"
                            step == STEPS - 1 -> "Open trackpad"
                            else -> "Next"
                        },
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun Hero(shape: androidx.graphics.shapes.RoundedPolygon, icon: ImageVector) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = shape.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(112.dp),
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(48.dp)) } }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Title(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

private val Modes = listOf(
    Triple(Dest.TRACKPAD, Icons.Rounded.TouchApp, "Trackpad with gestures"),
    Triple(Dest.KEYBOARD, Icons.Rounded.Keyboard, "Keyboard with shortcuts"),
    Triple(Dest.REMOTE, Icons.Rounded.SettingsRemote, "Remote for TVs and slides"),
    Triple(Dest.GAMEPAD, Icons.Rounded.SportsEsports, "Gamepad"),
    Triple(Dest.AIR_MOUSE, Icons.Rounded.Mouse, "Air mouse"),
    Triple(Dest.TABLET, Icons.Rounded.Draw, "Drawing tablet with pressure"),
)

@Composable
private fun Welcome() {
    Hero(MaterialShapes.Cookie9Sided, Icons.Rounded.TouchApp)
    Title(
        "Your phone, as a trackpad and keyboard",
        "PadBridge connects over Bluetooth to Windows, macOS, Linux, TVs and tablets. Nothing to install on the other device.",
    )
    Modes.forEach { (_, icon, text) ->
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialShapes.Circle.toShape(), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Takes about a minute: one permission, your computer type, then pairing.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PermissionsStep(activity: MainActivity, tick: Int, ask: () -> Unit) {
    Hero(MaterialShapes.SoftBurst, Icons.Rounded.Bluetooth)
    Title(
        "One permission",
        "Nearby devices lets PadBridge act as a Bluetooth trackpad and keyboard. Nothing leaves the phone except " +
            "input to the computer you pick.",
    )
    val nearby = remember(tick) { Perms.hasConnect(activity) && Perms.hasScan(activity) }
    val notif = remember(tick) {
        android.os.Build.VERSION.SDK_INT < 33 ||
            activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    PermRow(Icons.Rounded.Bluetooth, "Nearby devices", "Required. Connect to computers and find new ones.", nearby, ask)
    Spacer(Modifier.height(12.dp))
    PermRow(Icons.Rounded.Notifications, "Notifications", "Optional. Shows the connection while PadBridge runs in the background.", notif, ask)
}

@Composable
private fun PermRow(icon: ImageVector, title: String, body: String, granted: Boolean, onGrant: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            if (granted) Icon(Icons.Rounded.Check, "Granted", tint = MaterialTheme.colorScheme.primary)
            else FilledTonalButton(onClick = onGrant) { Text("Allow") }
        }
    }
}

@Composable
private fun ComputerStep(onPicked: () -> Unit) {
    var os by remember { mutableStateOf(Settings.hostOs) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Hero(MaterialShapes.Clover4Leaf, Icons.Rounded.DesktopWindows)
    Title("What will you control?", "Sets key labels, shortcuts and what gestures do. You can change it any time.")
    val options = listOf(
        Triple(HostOs.WINDOWS, Icons.Rounded.DesktopWindows, "Windows 10 or 11"),
        Triple(HostOs.MAC, Icons.Rounded.LaptopMac, "MacBook, iMac, Mac mini, iPad"),
        Triple(HostOs.LINUX, Icons.Rounded.Terminal, "Linux, Chromebook, Android TV and tablets"),
    )
    options.forEach { (o, icon, sub) ->
        val selected = o == os
        Card(
            onClick = {
                os = o; Settings.hostOs = o; OsBus.version++
                // Picking is the answer: move on after a beat so the check shows.
                scope.launch { kotlinx.coroutines.delay(300); onPicked() }
            },
            shape = RoundedCornerShape(if (selected) 28.dp else 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.label, style = MaterialTheme.typography.titleLarge)
                    Text(sub, style = MaterialTheme.typography.bodyMedium)
                }
                if (selected) Icon(Icons.Rounded.Check, null)
            }
        }
    }
    Text(
        "Apple TV and Google TV work with any choice; the Remote tab has profiles for them.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun PairStep(hid: HidUi) {
    val ctx = LocalContext.current
    var nearby by remember { mutableStateOf(false) }
    if (nearby) NearbySheet(hid) { nearby = false }
    Hero(if (hid.connected) MaterialShapes.SoftBurst else MaterialShapes.Cookie12Sided, if (hid.connected) Icons.Rounded.Check else Icons.Rounded.Search)
    if (hid.connected) {
        Title("Connected", "${hid.hostName ?: "Your computer"} is ready. You can pair more devices later from the Connect tab.")
        return
    }
    // Already paired with a computer (setup run again): offer it first.
    val known = remember(hid.version) { HidManager.bondedHosts(false) }
    Title("Pair your computer", "Three steps, once. After that PadBridge reconnects by itself.")
    val os = Settings.hostOs
    val steps = listOf(
        when (os) {
            HostOs.WINDOWS -> "On the PC, open Settings › Bluetooth & devices and leave it open."
            HostOs.MAC -> "On the Mac, open System Settings › Bluetooth and leave it open."
            HostOs.LINUX -> "On the computer or TV, open its Bluetooth settings and leave them open."
        },
        "Tap Find nearby devices below and pick your computer.",
        "Accept the pairing prompt on both screens. The codes should match.",
    )
    steps.forEachIndexed { i, t ->
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("${i + 1}", style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(Modifier.width(12.dp))
            Text(t, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
        }
    }
    Spacer(Modifier.height(16.dp))
    val fix = rememberBluetoothFix(hid)
    val ready = hid.state == HidManager.State.READY || hid.state == HidManager.State.CONNECTING
    if (!ready && fix.action != null) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fix.problem, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = fix.run) { Text(fix.action) }
            }
        }
    }
    known.take(3).forEach { d ->
        FilledTonalButton(
            onClick = { HidManager.connect(d) }, enabled = ready,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) { Text("Connect to ${HidManager.name(d)}") }
    }
    Button(onClick = { nearby = true }, enabled = ready, modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
        Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(8.dp)); Text("Find nearby devices")
    }
    Spacer(Modifier.height(4.dp))
    TextButton(
        onClick = {
            try {
                HidManager.openPairingWindow()
                ctx.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300))
            } catch (_: Exception) {}
        },
        enabled = ready,
    ) { Icon(Icons.Rounded.Visibility, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Computer can't find it? Make this phone visible") }
    if (hid.state == HidManager.State.CONNECTING) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            androidx.compose.material3.LoadingIndicator(Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(hid.detail.ifEmpty { "Connecting…" }, style = MaterialTheme.typography.bodyMedium)
        }
    }
    var help by remember { mutableStateOf(false) }
    TextButton(onClick = { help = !help }) { Text(if (help) "Hide troubleshooting" else "Paired, but nothing moves?") }
    androidx.compose.animation.AnimatedVisibility(help) {
        Text(
            when (os) {
                HostOs.WINDOWS -> "Open Control Panel › Devices and Printers, right-click this phone › Properties › Services and " +
                    "tick “Drivers for keyboard, mice, etc. (HID)”. "
                else -> ""
            } + "Paired before PadBridge was installed (for example by Phone Link)? Remove the pairing on both sides and pair again.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
    // Keep the service alive while pairing.
    androidx.compose.runtime.LaunchedEffect(Unit) { if (Perms.hasConnect(ctx)) HidService.start(ctx) }
}

@Composable
private fun DoneStep(hid: HidUi, finish: (Dest?) -> Unit) {
    Hero(MaterialShapes.Sunny, Icons.Rounded.Check)
    Title(
        "You're set",
        if (hid.connected) "Connected to ${hid.hostName}. Where do you want to start?"
        else "Not connected yet: pick a computer on the Connect tab whenever you're ready. Where do you want to start?",
    )
    // Two columns of big tiles: one tap finishes setup and opens that mode.
    Modes.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (dest, icon, _) ->
                Card(
                    onClick = { finish(dest) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(dest.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Tip: two fingers scroll, pinch zooms, three and four fingers switch apps. Each mode's ⋮ menu has the " +
            "gesture guide and its own settings.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
