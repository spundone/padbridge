package dev.padbridge.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.padbridge.HidManager
import dev.padbridge.HidManager.State
import dev.padbridge.HidService
import dev.padbridge.MainActivity
import dev.padbridge.Perms

/**
 * What's wrong with Bluetooth, in plain words, and the one action that fixes it. Every screen that shows a
 * problem uses this, so "Needs attention" always comes with a button that does the right thing.
 */
data class BluetoothFix(val problem: String, val action: String?, val run: () -> Unit)

@Composable
fun rememberBluetoothFix(hid: HidUi): BluetoothFix {
    val ctx = LocalContext.current
    val activity = ctx as? MainActivity
    var denials by remember { mutableIntStateOf(0) }
    fun appSettings() = try {
        ctx.startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${ctx.packageName}"))
        )
    } catch (_: Exception) {}
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (Perms.hasConnect(ctx)) { activity?.startHid() ?: run { HidService.start(ctx); HidManager.restart(ctx) } }
        else {
            denials++
            // Denied for good ("Don't ask again", or denied twice): Android won't show the prompt any more.
            val canAsk = activity?.shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT) ?: true
            if (!canAsk && denials > 0) appSettings()
        }
    }
    val enable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        HidService.start(ctx); HidManager.restart(ctx)
    }
    val restart = { HidService.start(ctx); HidManager.restart(ctx) }
    // Missing permission wins over whatever the state says (it can still read "Stopped" before the first start).
    val state = if (!Perms.hasConnect(ctx) && hid.state != State.UNSUPPORTED) State.NO_PERMISSION else hid.state
    return when (state) {
        State.NO_PERMISSION -> BluetoothFix(
            "PadBridge needs the Nearby devices permission to act as a Bluetooth trackpad and keyboard.",
            "Allow",
        ) { perms.launch((Perms.runtime().toList() + Perms.scan().toList()).distinct().toTypedArray()) }
        State.BLUETOOTH_OFF -> BluetoothFix("Bluetooth is off.", "Turn on") {
            try { enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
            catch (_: Exception) { ctx.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
        }
        State.IDLE -> BluetoothFix("PadBridge is stopped.", "Start", restart)
        State.UNSUPPORTED -> if (hid.needsBluetoothRestart) BluetoothFix(
            "Bluetooth needs a restart before PadBridge can use it. Turn it off and on again.", "Bluetooth settings",
        ) { ctx.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
        else BluetoothFix(hid.detail.ifEmpty { "This phone can't act as a Bluetooth keyboard." }, "Try again", restart)
        else -> BluetoothFix(hid.detail, null) {}
    }
}

/** Short status for pills and chips: says what's wrong instead of "Needs attention". */
fun shortStatus(hid: HidUi): String = when {
    hid.local -> hid.hostName ?: "This phone"
    else -> when (hid.state) {
        State.CONNECTED -> hid.hostName ?: "Connected"
        State.CONNECTING -> "Connecting"
        State.REGISTERING -> "Starting"
        State.READY -> "Not connected"
        State.NO_PERMISSION -> "Permission needed"
        State.BLUETOOTH_OFF -> "Bluetooth off"
        State.IDLE -> "Stopped"
        State.UNSUPPORTED -> "Bluetooth problem"
    }
}
