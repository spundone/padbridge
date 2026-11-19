package dev.padbridge

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play build: Google Play in-app updates (flexible). Play downloads in the background while the app keeps
 * working, then a restart applies it. PadBridge itself never touches the network.
 */
object Updater {
    private const val TAG = "PadBridge"
    const val SUPPORTED = true
    const val SOURCE = "Google Play"
    private const val REQUEST = 4711

    private var manager: AppUpdateManager? = null
    private var version = ""

    private fun manager(a: Activity) = manager ?: AppUpdateManagerFactory.create(a.applicationContext).also {
        manager = it
        it.registerListener(listener)
    }

    private val listener = InstallStateUpdatedListener { s ->
        when (s.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = s.totalBytesToDownload()
                Updates.state = UpdateState.Downloading(version, if (total > 0) s.bytesDownloaded().toFloat() / total else -1f)
            }
            InstallStatus.DOWNLOADED -> Updates.state = UpdateState.Ready(version)
            InstallStatus.FAILED -> Updates.state = UpdateState.Failed("Google Play couldn't download the update.")
            InstallStatus.CANCELED -> Updates.state = UpdateState.Idle
            else -> {}
        }
    }

    fun autoCheck(activity: Activity) {
        if (!Settings.autoUpdateCheck) return
        check(activity, manual = false)
    }

    fun check(activity: Activity, manual: Boolean) {
        if (manual) Updates.state = UpdateState.Checking
        manager(activity).appUpdateInfo
            .addOnSuccessListener { info ->
                version = info.availableVersionCode().toString()
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> Updates.state = UpdateState.Ready(version)
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        if (!manual && Settings.skippedUpdate == version) Updates.state = UpdateState.Idle
                        else Updates.state = UpdateState.Available(version)
                    }
                    else -> Updates.state = if (manual) UpdateState.UpToDate else UpdateState.Idle
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "update check", e)
                Updates.state = if (manual) UpdateState.Failed("Google Play isn't available on this phone.") else UpdateState.Idle
            }
    }

    fun start(activity: Activity) {
        val m = manager(activity)
        if (Updates.state is UpdateState.Ready) { m.completeUpdate(); return }
        m.appUpdateInfo.addOnSuccessListener { info ->
            try {
                @Suppress("DEPRECATION")
                m.startUpdateFlowForResult(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE), REQUEST)
            } catch (e: Exception) {
                Log.w(TAG, "update flow", e)
                Updates.state = UpdateState.Failed("Couldn't open the Google Play update.")
            }
        }
    }

    fun skip(v: String) {
        Settings.skippedUpdate = v
        Updates.state = UpdateState.Idle
    }
}
