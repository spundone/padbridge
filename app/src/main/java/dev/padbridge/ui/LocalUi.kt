package dev.padbridge.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.padbridge.FloatingPadService
import dev.padbridge.Local
import dev.padbridge.LocalA11yService
import dev.padbridge.Settings

/**
 * "Control this phone" (GitHub build): the trackpad and keyboard drive this phone's own screen or its external
 * display, no Bluetooth. Shows what it needs (root or the accessibility service, and the overlay permission for the
 * cursor) with one-tap fixes, the display to control, and the floating trackpad.
 */
@Composable
fun LocalControlCard() {
    val ctx = LocalContext.current
    // Re-read permissions and services when coming back from the system settings pages.
    var resumed by remember { mutableIntStateOf(0) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) { resumed++; if (Local.enabled) Local.chooseBackend() }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    resumed
    val on = Local.enabled
    val backend = Local.backendLabel
    val a11y = LocalA11yService.instance != null
    val overlay = android.provider.Settings.canDrawOverlays(ctx)
    var useRoot by remember { mutableStateOf(Settings.localUseRoot) }
    var display by remember { mutableIntStateOf(Settings.localDisplay) }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PhonelinkSetup, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Control this phone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "No Bluetooth: the trackpad and keyboard drive this phone, or the external display it's " +
                            "showing (desktop mode, Samsung DeX, an extended display).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = on, onCheckedChange = { Local.setEnabled(ctx, it) })
            }

            if (on) {
                Text(
                    when {
                        backend != null -> "Controlling ${Local.displayName ?: "this phone"} · $backend"
                        !dev.padbridge.BuildConfig.A11Y_CONTROL ->
                            if (useRoot) "Waiting for root. Without root, this needs the PadBridge Control build"
                            else "Turn on root below, or install the PadBridge Control build"
                        useRoot -> "Waiting for root, or turn on the accessibility service below"
                        else -> "Turn on the accessibility service below"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (backend != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                if (!a11y && dev.padbridge.BuildConfig.A11Y_CONTROL) FilledTonalButton(onClick = {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Icon(Icons.Rounded.Accessibility, null, Modifier.size(18.dp))
                    Text("  Turn on \"PadBridge: control this phone\"")
                }
                // The standard APK has no accessibility service (Android blocks sideloading apps with one), so
                // without root this needs the Control build from the same release.
                if (!dev.padbridge.BuildConfig.A11Y_CONTROL && backend == null) {
                    Text(
                        "Without root, PadBridge drives this phone through an accessibility service. It's in a separate " +
                            "download, PadBridge-vX-control.apk on the releases page, because Android blocks installing " +
                            "apps with one from a browser or file manager. Install it with ADB or an app store like " +
                            "Obtainium; it updates over this app and keeps your settings.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("$REPO_URL/releases/latest")))
                    }) { Text("Open the releases page") }
                }
                if (!overlay && !a11y) OutlinedButton(onClick = {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${ctx.packageName}")))
                }) { Text("Allow the pointer to show (display over other apps)") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Use root when available", style = MaterialTheme.typography.bodyLarge)
                        Text("Real mouse and keyboard: hover, right click, every shortcut",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = useRoot, onCheckedChange = {
                        useRoot = it; Settings.localUseRoot = it
                        if (!it) dev.padbridge.RootInput.stop()
                        Local.chooseBackend()
                    })
                }

                val choices = remember(resumed) { Local.displayChoices(ctx) }
                if (choices.size > 1) {
                    Text("Display", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = display == -1, onClick = {
                                display = -1; Settings.localDisplay = -1; Local.pickDisplay(); Local.chooseBackend()
                            }, label = { Text("Automatic") })
                        }
                        items(choices) { (id, name) ->
                            FilterChip(selected = display == id, onClick = {
                                display = id; Settings.localDisplay = id; Local.pickDisplay(); Local.chooseBackend()
                            }, label = { Text(name) }, leadingIcon = { Icon(Icons.Rounded.Tag, null, Modifier.size(16.dp)) })
                        }
                    }
                }
            }

            OutlinedButton(onClick = {
                if (FloatingPadService.running) FloatingPadService.stop(ctx) else FloatingPadService.start(ctx)
                resumed++
            }) {
                Icon(Icons.Rounded.Layers, null, Modifier.size(18.dp))
                Text(if (FloatingPadService.running) "  Close floating trackpad" else "  Floating trackpad over other apps")
            }
            var alpha by remember { androidx.compose.runtime.mutableFloatStateOf(Settings.floatAlpha) }
            Text("Floating trackpad opacity · ${(alpha * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            androidx.compose.material3.Slider(
                value = alpha, valueRange = 0.25f..1f,
                onValueChange = { alpha = it; FloatingPadService.applyOpacity(it) },
            )
        }
    }
}
