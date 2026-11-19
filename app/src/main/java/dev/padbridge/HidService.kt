package dev.padbridge

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

object Perms {
    fun runtime(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            list += Manifest.permission.BLUETOOTH_CONNECT
            list += Manifest.permission.BLUETOOTH_ADVERTISE
        }
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        return list.toTypedArray()
    }

    /** What scanning for nearby computers needs on this Android version. */
    fun scan(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasScan(ctx: Context) = scan().all { ctx.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    fun hasConnect(ctx: Context) = Build.VERSION.SDK_INT < 31 ||
        ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
}

/**
 * Foreground service that keeps the HID registration alive while the app is in the background,
 * so switching apps or locking the screen does not drop the host.
 */
class HidService : Service(), HidManager.Listener {
    companion object {
        private const val CHANNEL = "hid"
        private const val NOTIF_ID = 1
        const val ACTION_STOP = "dev.padbridge.STOP"

        fun start(ctx: Context) {
            if (!Perms.hasConnect(ctx)) return
            ctx.startForegroundService(Intent(ctx, HidService::class.java))
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                BluetoothAdapter.STATE_ON -> HidManager.restart(this@HidService)
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> HidManager.onBluetoothOff()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Connection", NotificationManager.IMPORTANCE_LOW))
        registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        HidManager.addListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            HidManager.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        HidManager.start(this)
        return START_STICKY
    }

    override fun onHidChanged() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
        PadTile.refresh(this)
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(this, 0, MainActivity.open(this, "TRACKPAD"), flags)
        val keyboard = PendingIntent.getActivity(this, 2, MainActivity.open(this, "KEYBOARD"), flags)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, HidService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val text = when (HidManager.state) {
            HidManager.State.CONNECTED -> HidManager.detail
            HidManager.State.CONNECTING -> HidManager.detail
            else -> "Waiting for a computer"
        }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_mono)
            .setContentTitle("PadBridge")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Trackpad", open).build())
            .addAction(Notification.Action.Builder(null, "Keyboard", keyboard).build())
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
    }

    override fun onDestroy() {
        HidManager.removeListener(this)
        try { unregisterReceiver(btReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
