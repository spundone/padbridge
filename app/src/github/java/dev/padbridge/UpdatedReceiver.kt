package dev.padbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After an in-app update Android closes PadBridge; this puts it one tap away again. */
class UpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.areNotificationsEnabled()) return
        nm.createNotificationChannel(NotificationChannel("updates", "Updates", NotificationManager.IMPORTANCE_DEFAULT))
        val version = try { c.packageManager.getPackageInfo(c.packageName, 0).versionName } catch (_: Exception) { null }
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        nm.notify(
            2, Notification.Builder(c, "updates")
                .setSmallIcon(R.drawable.ic_mono)
                .setContentTitle("PadBridge updated${version?.let { " to $it" } ?: ""}")
                .setContentText("Tap to open and reconnect.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
    }
}
