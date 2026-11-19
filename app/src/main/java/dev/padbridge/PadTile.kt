package dev.padbridge

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile that jumps straight to the trackpad, like DeX's
 * "Use your phone as a touchpad" entry in the notification panel.
 */
class PadTile : TileService() {
    companion object {
        fun refresh(ctx: Context) = try {
            requestListeningState(ctx, ComponentName(ctx, PadTile::class.java))
        } catch (_: Exception) {}
    }

    override fun onStartListening() {
        super.onStartListening()
        val t = qsTile ?: return
        val connected = HidManager.isConnected
        t.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = "Trackpad"
        if (Build.VERSION.SDK_INT >= 29) {
            t.subtitle = if (connected) HidManager.host?.let { HidManager.name(it) } ?: "Connected" else "PadBridge"
        }
        t.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = MainActivity.open(this, "TRACKPAD")
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
