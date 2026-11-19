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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
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

/** First-run walkthrough: welcome, permissions, computer type, pairing, done. */
@Composable
fun SetupScreen(hid: HidUi, activity: MainActivity, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val canNext = when (step) {
        1 -> Perms.hasConnect(activity) || hid.version >= 0 // allowed to continue even if denied; app shows status
        else -> true
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearWavyProgressIndicator(progress = { (step + 1f) / STEPS }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                if (step < STEPS - 1) TextButton(onClick = { Settings.setupDone = true; onDone() }) { Text("Skip") }
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
                        1 -> PermissionsStep(activity)
                        2 -> ComputerStep()
                        3 -> PairStep(hid)
                        else -> DoneStep(hid)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = canNext,
                    onClick = {
                        if (step < STEPS - 1) step++ else { Settings.setupDone = true; onDone() }
                    },
                    modifier = Modifier.height(ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.MediumContentPadding,
                ) {
                    Text(
                        when (step) { 0 -> "Get started"; STEPS - 1 -> "Start using PadBridge"; 3 -> if (hid.connected) "Next" else "Later"; else -> "Next" },
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                    )
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
            modifier = Modifier.size(132.dp),
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(56.dp)) } }
    }
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun Title(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Welcome() {
    Hero(MaterialShapes.Cookie9Sided, Icons.Rounded.TouchApp)
    Title(
        "Your phone, as a trackpad",
        "PadBridge turns this phone into a Bluetooth trackpad, keyboard, TV remote, air mouse and gamepad for " +
            "Windows, macOS, Linux and TVs. No software is needed on the computer.",
    )
    val features = listOf(
        Icons.Rounded.TouchApp to "Multi-touch trackpad with gestures",
        Icons.Rounded.Keyboard to "Full keyboard and your phone's own keyboard",
        Icons.Rounded.SettingsRemote to "D-pad remote for TVs and slides",
        Icons.Rounded.SportsEsports to "Gamepad with haptics",
    )
    features.forEach { (icon, text) ->
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialShapes.Circle.toShape(), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PermissionsStep(activity: MainActivity) {
    var tick by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        tick++
        if (Perms.hasConnect(activity)) activity.startHid()
    }
    Hero(MaterialShapes.SoftBurst, Icons.Rounded.Bluetooth)
    Title(
        "Permissions",
        "Nearby devices lets PadBridge act as a Bluetooth trackpad and keyboard and find computers. Notifications show " +
            "the connection while it runs in the background. Nothing leaves the phone except input to the computer you pick.",
    )
    val nearby = remember(tick) { Perms.hasConnect(activity) && Perms.hasScan(activity) }
    val notif = remember(tick) {
        android.os.Build.VERSION.SDK_INT < 33 ||
            activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    PermRow(Icons.Rounded.Bluetooth, "Nearby devices", "Act as a Bluetooth trackpad/keyboard and find computers.", nearby) {
        launcher.launch((Perms.runtime().toList() + Perms.scan().toList()).distinct().toTypedArray())
    }
    Spacer(Modifier.height(12.dp))
    PermRow(Icons.Rounded.Notifications, "Notifications", "Shows the connection while PadBridge runs in the background.", notif) {
        if (android.os.Build.VERSION.SDK_INT >= 33) launcher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
    }
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
private fun ComputerStep() {
    var os by remember { mutableStateOf(Settings.hostOs) }
    Hero(MaterialShapes.Clover4Leaf, Icons.Rounded.DesktopWindows)
    Title("Your computer", "Sets keyboard labels, shortcuts and what gestures do. You can change it any time, per computer.")
    val options = listOf(
        Triple(HostOs.WINDOWS, Icons.Rounded.DesktopWindows, "Windows 10 or 11"),
        Triple(HostOs.MAC, Icons.Rounded.LaptopMac, "MacBook, iMac, Mac mini"),
        Triple(HostOs.LINUX, Icons.Rounded.Terminal, "GNOME, KDE and others"),
    )
    options.forEach { (o, icon, sub) ->
        val selected = o == os
        Card(
            onClick = { os = o; Settings.hostOs = o },
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
        "Controlling a TV (Android TV, Apple TV)? Pick any of these; the Remote tab has TV profiles.",
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
        // Done: no pairing steps or buttons left to show.
        Title("Connected", "${hid.hostName ?: "Your computer"} is ready. You can pair more computers later from the Connect tab.")
        return
    }
    Title(
        "Pair your computer",
        when (Settings.hostOs) {
            HostOs.WINDOWS -> "On the PC open Settings › Bluetooth & devices and keep it open, then find it from here. " +
                "If Windows pairs but the trackpad doesn't work, open Control Panel › Devices and Printers › this phone › " +
                "Properties › Services and tick “Drivers for keyboard, mice, etc. (HID)”."
            HostOs.MAC -> "On the Mac open System Settings › Bluetooth, then make this phone visible and click Connect next to it on the Mac."
            HostOs.LINUX -> "Open the computer's Bluetooth settings, then make this phone visible and pair from the computer."
        },
    )
    Button(onClick = { nearby = true }, modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
        Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(8.dp)); Text("Find nearby devices")
    }
    Spacer(Modifier.height(8.dp))
    FilledTonalButton(
        onClick = {
            try {
                HidManager.openPairingWindow()
                ctx.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300))
            } catch (_: Exception) {}
        },
        modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
    ) { Icon(Icons.Rounded.Visibility, null); Spacer(Modifier.width(8.dp)); Text("Make this phone visible") }
    Spacer(Modifier.height(16.dp))
    StatusPill(hid) {}
    if (hid.state == HidManager.State.NO_PERMISSION) {
        Text("Allow Nearby devices first (previous step).", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
    // Keep the service alive while pairing.
    androidx.compose.runtime.LaunchedEffect(Unit) { if (Perms.hasConnect(ctx)) HidService.start(ctx) }
}

@Composable
private fun DoneStep(hid: HidUi) {
    Hero(MaterialShapes.Sunny, Icons.Rounded.Check)
    Title(
        "You're set",
        "Tips: two fingers scroll, pinch zooms, three and four fingers switch apps and desktops. The trackpad's ⋮ menu " +
            "has the full gesture guide, and the moon gives a dim always-on look.",
    )
    Text(
        if (hid.connected) "Connected to ${hid.hostName}." else "Not connected yet. Pick a computer on the Connect tab whenever you're ready.",
        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start,
    )
}
