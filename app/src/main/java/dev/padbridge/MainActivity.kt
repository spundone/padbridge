package dev.padbridge

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.padbridge.ui.HidUi
import dev.padbridge.ui.PadBridgeApp
import dev.padbridge.ui.PadBridgeTheme

class MainActivity : ComponentActivity(), HidManager.Listener {

    private var version = 0
    private val hid = mutableStateOf(HidUi.capture(0))
    /** Screen requested by the notification or Quick Settings tile ("TRACKPAD", "KEYBOARD"). */
    val requestedDest = mutableStateOf<String?>(null)

    private val bondReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = onHidChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Settings.init(this)
        Haptics.init(this)

        requestedDest.value = intent?.getStringExtra(EXTRA_DEST)
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("demo", false) == true) HidManager.startDemo()
        setContent {
            PadBridgeTheme { PadBridgeApp(hid.value, this) }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        }
        registerReceiver(bondReceiver, filter)
        // First run: the setup screen asks for permissions with context instead of a cold prompt.
        if (Settings.setupDone) ensurePermissionsAndStart() else if (Perms.hasConnect(this)) startHid()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_DEST)?.let { requestedDest.value = it }
    }

    companion object {
        const val EXTRA_DEST = "dest"

        fun open(ctx: Context, dest: String) = Intent(ctx, MainActivity::class.java)
            .putExtra(EXTRA_DEST, dest)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private fun ensurePermissionsAndStart() {
        val missing = Perms.runtime().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1)
        else startHid()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Perms.hasConnect(this)) startHid() else HidManager.start(this) // reports NO_PERMISSION
    }

    fun startHid() {
        HidService.start(this)
        HidManager.start(this)
    }

    private var updateChecked = false

    override fun onResume() {
        super.onResume()
        HidManager.addListener(this)
        onHidChanged()
        if (Perms.hasConnect(this)) { HidService.start(this); HidManager.restart(this) }
        if (!updateChecked && Settings.setupDone) { updateChecked = true; Updater.autoCheck(this) }
        // A failure from before (e.g. network access was off, since fixed in Settings) shouldn't linger.
        if (Updates.state is UpdateState.Failed) Updates.state = UpdateState.Idle
        else if (Settings.setupDone) HidManager.start(this) // shows "Permission needed"
    }

    override fun onPause() {
        HidManager.removeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        try { unregisterReceiver(bondReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onHidChanged() {
        hid.value = HidUi.capture(++version)
        applyKeepScreenOn()
    }

    fun applyKeepScreenOn() {
        if (aod || (Settings.keepScreenOn && HidManager.isConnected)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var aod = false

    /** Any touch or key press: resets the idle timer for dimming to AOD. */
    override fun onUserInteraction() {
        super.onUserInteraction()
        Idle.touched()
    }

    /** AOD mode: screen stays on at the panel's lowest brightness while input keeps working. */
    fun setAod(on: Boolean) {
        if (on == aod) return
        aod = on
        window.attributes = window.attributes.apply {
            screenBrightness = if (on) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        applyKeepScreenOn()
    }

    /** Gamepad runs landscape (immersive is set separately by [setBarsHidden]). */
    private var gamepadMode = false

    fun setGamepadMode(on: Boolean) {
        if (on == gamepadMode) return
        gamepadMode = on
        requestedOrientation = if (on) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private var immersive = false

    /** Hide status and navigation bars (swipe from an edge shows them briefly). */
    fun setBarsHidden(on: Boolean) {
        if (on == immersive) return
        immersive = on
        val c = WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Full display mode: draw into the camera cutout too, so the pad covers every pixel. */
    fun setFullDisplay(on: Boolean) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = when {
                !on -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                android.os.Build.VERSION.SDK_INT >= 30 -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    /** 0 = follow the sensor, 1 = portrait, 2 = landscape. */
    fun setTrackpadOrientation(mode: Int) {
        requestedOrientation = when (mode) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            2 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    /** Set by the remote screen: gets volume key presses (up?, pressed?) and returns true to consume them. */
    var volumeKeyHandler: ((up: Boolean, pressed: Boolean) -> Boolean)? = null

    private fun volumeKey(code: Int, event: android.view.KeyEvent, pressed: Boolean): Boolean {
        val h = volumeKeyHandler ?: return false
        if (code != android.view.KeyEvent.KEYCODE_VOLUME_UP && code != android.view.KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (pressed && event.repeatCount > 0) return true // no auto-repeat for slides
        return h(code == android.view.KeyEvent.KEYCODE_VOLUME_UP, pressed)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean =
        volumeKey(keyCode, event, true) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean =
        volumeKey(keyCode, event, false) || super.onKeyUp(keyCode, event)

    fun versionName(): String = try {
        (packageManager.getPackageInfo(packageName, 0).versionName ?: "?")
            // Screenshot demo (debug only): show the version as users see it.
            .let { if (HidManager.demo) it.removeSuffix("-dev") else it }
    } catch (_: Exception) { "?" }
}
