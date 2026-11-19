@file:SuppressLint("MissingPermission")

package dev.padbridge

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Owns the Android HID Device profile: registration, the single active host connection,
 * and all report traffic. Reports are built and sent on one background thread so touch
 * handling never blocks on Binder calls.
 */
object HidManager {
    private const val TAG = "PadBridge"

    enum class State { IDLE, BLUETOOTH_OFF, NO_PERMISSION, UNSUPPORTED, REGISTERING, READY, CONNECTING, CONNECTED }

    fun interface Listener { fun onHidChanged() }

    @Volatile var state = State.IDLE; private set
    @Volatile var detail: String = ""; private set
    @Volatile var host: BluetoothDevice? = null; private set
    @Volatile var connectingTo: BluetoothDevice? = null; private set
    /** Keyboard LED bits from the host (bit 0 Num, bit 1 Caps, bit 2 Scroll). */
    @Volatile var leds = 0; private set
    @Volatile var hiResWheel = false; private set
    /** Android's Bluetooth service accepted registration but is stuck; only a Bluetooth off/on clears it. */
    @Volatile var needsBluetoothRestart = false; private set

    private val listeners = CopyOnWriteArraySet<Listener>()
    fun addListener(l: Listener) = listeners.add(l)
    fun removeListener(l: Listener) = listeners.remove(l)

    private lateinit var ctx: Context
    private var adapter: BluetoothAdapter? = null
    @Volatile private var hid: BluetoothHidDevice? = null
    private var registered = false
    private var started = false
    private var pendingConnect: BluetoothDevice? = null
    private var retryCount = 0

    private val ioThread = HandlerThread("hid-io").apply { start() }
    private val io = Handler(ioThread.looper)
    private val ioExecutor = Executor { io.post(it) }
    private val main = Handler(Looper.getMainLooper())

    private fun notifyChanged() = main.post { listeners.forEach { it.onHidChanged() } }

    private fun setState(s: State, d: String = "") {
        if (demo && s != State.CONNECTED) return // screenshot demo: stay "connected"
        state = s; detail = d
        Log.i(TAG, "state=$s $d")
        notifyChanged()
    }

    val isConnected get() = state == State.CONNECTED && host != null

    // ------------------------------------------------------------------ lifecycle

    fun start(context: Context) {
        ctx = context.applicationContext
        Settings.init(ctx)
        if (started) { notifyChanged(); return }
        needsBluetoothRestart = false
        val a = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        adapter = a
        if (a == null) { setState(State.UNSUPPORTED, "This device has no Bluetooth adapter."); return }
        if (!Perms.hasConnect(ctx)) { setState(State.NO_PERMISSION, "Allow Nearby devices permission."); return }
        if (!a.isEnabled) { setState(State.BLUETOOTH_OFF, "Turn on Bluetooth."); return }
        started = true
        setState(State.REGISTERING, "Starting HID profile…")
        val ok = try {
            a.getProfileProxy(ctx, proxyListener, BluetoothProfile.HID_DEVICE)
        } catch (e: SecurityException) { false }
        if (!ok) {
            started = false
            setState(State.UNSUPPORTED, "This phone's Bluetooth stack does not expose the HID Device profile.")
            return
        }
        // Some ROMs ship the profile disabled: the proxy then never connects.
        main.postDelayed({
            if (hid == null && state == State.REGISTERING) setState(
                State.UNSUPPORTED,
                "The HID Device profile did not respond. Some ROMs disable it (bluetooth.profile.hid.device.enabled=false)."
            )
        }, 6000)
    }

    fun stop() {
        io.post {
            releaseAllInternal()
            val h = hid
            try {
                host?.let { h?.disconnect(it) }
                if (registered) h?.unregisterApp()
            } catch (_: Exception) {}
            try { if (h != null) adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, h) } catch (_: Exception) {}
            hid = null; registered = false; started = false; host = null; connectingTo = null
            setState(State.IDLE, "Stopped")
        }
    }

    /** Called when Bluetooth turns back on or permissions are granted. */
    fun restart(context: Context) {
        if (state == State.UNSUPPORTED && hid != null && !registered) {
            registerAttempts = 0
            register()
            return
        }
        if (state == State.BLUETOOTH_OFF || state == State.NO_PERMISSION || state == State.IDLE ||
            (state == State.UNSUPPORTED && hid == null)
        ) {
            started = false
            start(context)
        }
    }

    fun onBluetoothOff() {
        hid = null; registered = false; started = false; host = null; connectingTo = null
        setState(State.BLUETOOTH_OFF, "Turn on Bluetooth.")
    }

    private val proxyListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            hid = proxy as BluetoothHidDevice
            Log.i(TAG, "HID Device proxy connected")
            registerAttempts = 0
            register()
        }

        override fun onServiceDisconnected(profile: Int) {
            hid = null; registered = false; host = null
            setState(State.REGISTERING, "HID service disconnected, retrying…")
            started = false
            main.postDelayed({ if (hid == null) start(ctx) }, 2000)
        }
    }

    /** Physical pad size in 0.01 inch for the Precision Touchpad descriptor: phone width x ~65% of its height. */
    private fun padSize(): Pair<Int, Int> {
        val dm = ctx.resources.displayMetrics
        val shortPx = minOf(dm.widthPixels, dm.heightPixels); val longPx = maxOf(dm.widthPixels, dm.heightPixels)
        val w = (shortPx / dm.xdpi * 100).toInt().coerceIn(252, 2000)        // >= 64 mm (Windows minimum)
        val h = (longPx * 0.65f / dm.ydpi * 100).toInt().coerceIn(126, 2000) // >= 32 mm
        return w to h
    }

    /** Descriptor registered now: v2 includes the Precision Touchpad when native touchpad mode is on. */
    @Volatile var ptpRegistered = false; private set
    /** Descriptor registered now includes the N-key rollover keyboard. */
    @Volatile var nkroRegistered = false; private set

    private fun sdp(): BluetoothHidDeviceAppSdpSettings {
        val (w, h) = padSize()
        ptpRegistered = Settings.nativeTouchpad
        nkroRegistered = Settings.nkro
        val descriptor = HidDescriptor.current(w, h)
        Log.i(TAG, "descriptor v${HidDescriptor.VERSION}, ${descriptor.size} bytes: " +
            descriptor.joinToString("") { "%02x".format(it) })
        return BluetoothHidDeviceAppSdpSettings(
            "PadBridge",
            "Trackpad, keyboard and gamepad",
            "PadBridge",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            descriptor,
        )
    }

    /**
     * Re-register with the current descriptor (after turning native touchpad mode on/off). Hosts cache the
     * old descriptor, so the user must remove and re-pair the computer afterwards.
     */
    fun reRegister() = main.post {
        val h = hid ?: return@post
        releaseAll()
        try { host?.let { h.disconnect(it) } } catch (_: Exception) {}
        // onAppStatusChanged(registered=false) re-registers with sdp() (registerAttempts == 0).
        try { if (registered) h.unregisterApp() else register() } catch (e: Exception) { Log.w(TAG, "reRegister", e) }
    }

    // Same QoS as Google's WearMouse: best effort, low latency.
    private val qosOut = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT, 800, 9, 0, 11250,
        BluetoothHidDeviceAppQosSettings.MAX,
    )

    private fun register() {
        val h = hid ?: return
        val ok = try { h.registerApp(sdp(), null, qosOut, ioExecutor, callback) } catch (e: SecurityException) {
            Log.w(TAG, "registerApp", e)
            setState(State.REGISTERING, "Bluetooth refused the permission. Turn Bluetooth off and on (or restart the phone) if this persists.")
            scheduleReRegister()
            return
        } catch (e: Exception) {
            Log.w(TAG, "registerApp", e); false
        }
        if (ok) {
            Log.i(TAG, "registerApp accepted, waiting for onAppStatusChanged")
            setState(State.REGISTERING, "Registering trackpad and keyboard with Bluetooth…")
            main.removeCallbacks(registerWatchdog)
            main.postDelayed(registerWatchdog, 5000)
        }
        if (!ok) {
            // Another app may hold the HID Device role; it is exclusive.
            setState(State.REGISTERING, "Another app is using Bluetooth HID. Retrying…")
            scheduleReRegister()
        }
    }

    private var registerAttempts = 0

    /**
     * Some stacks accept registerApp() but never call back, usually because a stale registration from
     * a previous install or crashed process is still held. Clear it and retry, then give up visibly.
     */
    private val registerWatchdog = Runnable {
        if (registered || hid == null) return@Runnable
        registerAttempts++
        // One clean retry. If the stack still doesn't answer, unregistering won't help (it reports
        // "application not yet registered"); only restarting Bluetooth clears it.
        if (registerAttempts <= 1) {
            Log.w(TAG, "no onAppStatusChanged after registerApp; clearing stale registration (attempt $registerAttempts)")
            setState(State.REGISTERING, "Bluetooth didn't confirm the registration, retrying…")
            try { hid?.unregisterApp() } catch (e: Exception) { Log.w(TAG, "unregisterApp", e) }
            main.postDelayed({ if (!registered) register() }, 1500)
        } else {
            Log.e(TAG, "registration never confirmed; Bluetooth service needs a restart")
            needsBluetoothRestart = true
            setState(
                State.UNSUPPORTED,
                "Android's Bluetooth service is stuck. Turn Bluetooth off and on; PadBridge reconnects by itself."
            )
        }
    }

    private fun scheduleReRegister() {
        retryCount++
        val delay = (1000L shl retryCount.coerceAtMost(4))
        main.postDelayed({ if (!registered && hid != null) register() }, delay)
    }

    // ------------------------------------------------------------------ connections

    @Volatile private var connectStartedAt = 0L
    /** The last connect attempt was refused by the computer right away (see [refusalHint]). */
    @Volatile var hostRefused = false; private set

    /**
     * TVs often bond fine but hang up on the HID channel within a second: many only accept keyboards and mice that
     * were added from their own accessory menu, and that menu lists only devices whose Bluetooth class is a
     * keyboard, mouse or remote, which hides a phone.
     */
    private fun tvRefusalHint(d: BluetoothDevice): String =
        "${name(d)} paired, but it hangs up when PadBridge opens the keyboard and mouse connection. Many TVs only " +
            "accept keyboards added from their own menu: remove the pairing on both sides, make this phone visible, " +
            "then add it from the TV's Bluetooth or accessory settings. If the TV doesn't list the phone there, it " +
            "only accepts real keyboards, mice and remotes" +
            (if (BuildConfig.ROOT_FEATURES) " (root mode can make the phone present as one)." else ".")

    /**
     * Android Auto is projected from the phone and takes input from the car's own screen and controls, so a car
     * never sees Bluetooth keyboards for it; cars with Android Automotive built in accept one only if the
     * carmaker left Bluetooth keyboard support on.
     */
    private fun carRefusalHint(d: BluetoothDevice): String =
        "${name(d)} doesn't accept Bluetooth keyboards or mice. Android Auto runs on the phone and only takes input " +
            "from the car's screen and controls; cars with Google built in accept a keyboard only if the carmaker allows it."

    private fun refusalHint(): String = when (Settings.hostOs) {
        HostOs.WINDOWS ->
            "Windows has HID turned off for this phone. On the PC open Control Panel › Devices and Printers, " +
                "right-click the phone › Properties › Services, tick “Drivers for keyboard, mice, etc. (HID)”, " +
                "then tap Connect again. No such option? Remove the phone on the PC and pair again with PadBridge open."
        else -> "Remove the phone on the computer and pair again with PadBridge open, so it sees the trackpad and keyboard."
    }

    // ------------------------------------------------------------------ screenshot demo (debug builds only)

    /** Neutral, made-up hosts for README / store screenshots, so no real device names or addresses show. */
    private val demoHosts = linkedMapOf(
        "02:00:00:00:00:01" to "MacBook Pro",
        "02:00:00:00:00:02" to "Work PC",
        "02:00:00:00:00:03" to "Linux desktop",
    )
    @Volatile var demo = false; private set

    /** Debug builds: pretend to be connected to "MacBook Pro" (`adb shell am start … --ez demo true`). */
    fun startDemo() {
        if (!BuildConfig.DEBUG) return
        demo = true
        Settings.hostOs = HostOs.MAC
        main.postDelayed({
            val a = adapter ?: return@postDelayed
            host = a.getRemoteDevice(demoHosts.keys.first()); connectingTo = null
            setState(State.CONNECTED, "Connected to MacBook Pro")
        }, 2500)
    }

    // ------------------------------------------------------------------ phone-initiated pairing

    /**
     * Windows sees the phone's Bluetooth class ("phone"), so pairing started from the PC often ends up
     * without the HID connection. Starting from the phone works: scan, bond, then connect HID ourselves.
     */
    @Volatile var discovering = false; private set
    val found = java.util.concurrent.CopyOnWriteArrayList<BluetoothDevice>()
    @Volatile private var pairingTarget: BluetoothDevice? = null
    private var discoveryReceiverOn = false

    @Suppress("DEPRECATION")
    private fun deviceFrom(i: android.content.Intent): BluetoothDevice? =
        if (android.os.Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    private val discoveryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: Context, i: android.content.Intent) {
            when (i.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val d = deviceFrom(i) ?: return
                    if (found.none { it.address == d.address }) { found += d; notifyChanged() }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> notifyChanged()
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> { discovering = true; notifyChanged() }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> { discovering = false; notifyChanged() }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val d = deviceFrom(i) ?: return
                    val bond = i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    if (d.address != pairingTarget?.address) {
                        // Paired from the other side (a TV's or phone's Bluetooth settings) while the user is
                        // pairing in PadBridge: remember it, and connect if the host doesn't open HID itself.
                        if (bond == BluetoothDevice.BOND_BONDED && android.os.SystemClock.uptimeMillis() < pairingWindowUntil) {
                            Settings.addKnownHost(d.address)
                            main.postDelayed({ if (host == null && connectingTo == null) connect(d) }, 4000)
                        }
                        notifyChanged(); return
                    }
                    when (bond) {
                        BluetoothDevice.BOND_BONDING ->
                            setState(State.CONNECTING, "Pairing with ${name(d)}: confirm the same code on both screens.")
                        BluetoothDevice.BOND_BONDED -> {
                            pairingTarget = null
                            main.removeCallbacks(bondTimeout)
                            Settings.addKnownHost(d.address)
                            setState(State.CONNECTING, "Paired with ${name(d)}. Connecting trackpad and keyboard…")
                            // Give the host a moment to finish reading the HID record, and to connect in by itself
                            // (TVs and phones usually do); connect() is a no-op once it has.
                            main.postDelayed({ if (host?.address != d.address) connect(d) }, 2500)
                        }
                        BluetoothDevice.BOND_NONE -> {
                            pairingTarget = null
                            main.removeCallbacks(bondTimeout)
                            setState(State.READY, pairingFailed(d))
                        }
                    }
                }
            }
        }
    }

    private fun pairingFailed(d: BluetoothDevice) = when {
        isTv(d) -> "Pairing with ${name(d)} didn't complete. Keep the TV on its Bluetooth or accessory pairing screen, " +
            "accept the prompt on the TV, then try again."
        isComputer(d) -> "Pairing with ${name(d)} didn't complete. Keep the computer's Bluetooth settings open, " +
            "accept the prompt on the computer, then try again."
        else -> "Pairing with ${name(d)} didn't complete. Keep its Bluetooth settings open, accept the prompt on it, then try again."
    }

    /** Until when a bond made from the other side counts as a PadBridge pairing (sheet open or phone made visible). */
    @Volatile private var pairingWindowUntil = 0L
    fun openPairingWindow(ms: Long = 5 * 60_000L) {
        ensureDiscoveryReceiver()
        pairingWindowUntil = android.os.SystemClock.uptimeMillis() + ms
    }

    /** Some hosts never answer the pairing request; the stack can sit in BONDING for a long time. */
    private val bondTimeout = Runnable {
        val d = pairingTarget ?: return@Runnable
        if (d.bondState != BluetoothDevice.BOND_BONDING) return@Runnable
        pairingTarget = null
        setState(State.READY, pairingFailed(d))
    }

    private fun ensureDiscoveryReceiver() {
        if (discoveryReceiverOn) return
        val f = android.content.IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(discoveryReceiver, f, Context.RECEIVER_EXPORTED)
        else ctx.registerReceiver(discoveryReceiver, f)
        discoveryReceiverOn = true
    }

    /** Scan for nearby Bluetooth devices (about 12 s). Needs [Perms.hasScan]. */
    fun startDiscovery(): Boolean {
        val a = adapter ?: return false
        ensureDiscoveryReceiver()
        found.clear()
        return try {
            if (a.isDiscovering) a.cancelDiscovery()
            a.startDiscovery().also { discovering = it; notifyChanged() }
        } catch (e: SecurityException) { false }
    }

    fun stopDiscovery() {
        try { adapter?.cancelDiscovery() } catch (_: SecurityException) {}
        discovering = false; notifyChanged()
    }

    /** HID Device works over classic Bluetooth only, so LE-only entries (e.g. Windows' Nearby/Swift Pair beacons) are useless. */
    fun isClassic(d: BluetoothDevice) = try {
        d.type == BluetoothDevice.DEVICE_TYPE_CLASSIC || d.type == BluetoothDevice.DEVICE_TYPE_DUAL
    } catch (_: SecurityException) { true }

    /** Pair from the phone (if needed) and connect HID. */
    fun pairAndConnect(d: BluetoothDevice) {
        ensureDiscoveryReceiver()
        openPairingWindow()
        if (!isClassic(d)) {
            stopDiscovery()
            setState(State.READY, "${name(d)} was found over Bluetooth LE only. Keep its Bluetooth settings open and scan again for its classic entry.")
            return
        }
        if (d.bondState == BluetoothDevice.BOND_BONDED) { stopDiscovery(); connect(d); return }
        pairingTarget = d
        setState(State.CONNECTING, "Pairing with ${name(d)}…")
        // Bonding while inquiry is still running fails or stalls on many stacks (the radio is busy scanning), and
        // cancelDiscovery() is asynchronous, so wait until the scan has really stopped.
        val a = adapter
        val scanning = try { a?.isDiscovering == true } catch (_: SecurityException) { false }
        stopDiscovery()
        main.postDelayed({ startBond(d) }, if (scanning) 800L else 0L)
    }

    private fun startBond(d: BluetoothDevice) {
        if (pairingTarget?.address != d.address) return
        val ok = try { d.createBond() } catch (e: SecurityException) { false }
        if (!ok) {
            pairingTarget = null
            setState(State.READY, "Couldn't start pairing with ${name(d)}. Make sure it's visible and try again.")
            return
        }
        main.removeCallbacks(bondTimeout)
        main.postDelayed(bondTimeout, 45_000)
    }

    private fun major(d: BluetoothDevice): Int? = try { d.bluetoothClass?.majorDeviceClass } catch (_: SecurityException) { null }

    private val tvName = Regex("\\b(tv|television|bravia|firestick|fire tv|chromecast|shield|roku|tizen|webos)\\b")

    /** Televisions, displays, set-top boxes and streaming sticks (not headphones or speakers). */
    fun isTv(d: BluetoothDevice): Boolean = try {
        val c = d.bluetoothClass
        // Unknown class (not seen yet, or a device that reports none): go by the name.
        if (c == null || c.majorDeviceClass == BluetoothClass.Device.Major.MISC ||
            c.majorDeviceClass == BluetoothClass.Device.Major.UNCATEGORIZED) return tvName.containsMatchIn(name(d).lowercase())
        // Some TVs misreport their minor class (one MediaTek TV says "car audio"), so trust a TV-ish name too.
        val n = name(d).lowercase()
        if (c.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO &&
            tvName.containsMatchIn(n)) return true
        c.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && when (c.deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR,
            BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX,
            BluetoothClass.Device.AUDIO_VIDEO_VCR,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
            BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> true
            else -> false
        }
    } catch (_: SecurityException) { false }

    fun isPhone(d: BluetoothDevice) = major(d) == BluetoothClass.Device.Major.PHONE

    /** Car head units (car audio / hands-free), unless the name says it's really a TV that misreports its class. */
    fun isCar(d: BluetoothDevice): Boolean = try {
        val c = d.bluetoothClass
        c != null && !isTv(d) && c.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO &&
            (c.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO || c.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE)
    } catch (_: SecurityException) { false }

    /** Something that can act as an HID host: computers, TVs, phones and tablets, or anything that has been one. */
    fun isHostLike(d: BluetoothDevice): Boolean {
        val m = major(d)
        return m == BluetoothClass.Device.Major.COMPUTER || m == BluetoothClass.Device.Major.UNCATEGORIZED ||
            m == BluetoothClass.Device.Major.MISC || m == BluetoothClass.Device.Major.PHONE || m == null ||
            isTv(d) || d.address == Settings.lastHost || d.address in Settings.knownHosts
    }

    fun isComputer(d: BluetoothDevice) = try {
        val major = d.bluetoothClass?.majorDeviceClass
        major == BluetoothClass.Device.Major.COMPUTER
    } catch (_: SecurityException) { false }

    /** Computers the phone is bonded with, best candidates first. */
    fun bondedHosts(showAll: Boolean): List<BluetoothDevice> {
        val a = adapter ?: return emptyList()
        if (demo) return demoHosts.keys.map { a.getRemoteDevice(it) }
        val all = try { a.bondedDevices?.toList() ?: emptyList() } catch (e: SecurityException) { emptyList() }
        val filtered = if (showAll) all else all.filter { isHostLike(it) }
        // One row per computer: a PC can be bonded twice (classic + an LE bond whose identity resolves to the same
        // machine, e.g. Windows found over BLE). Keep the classic entry, which is the one HID uses.
        val deduped = filtered.groupBy { name(it) }.values.map { same ->
            same.minByOrNull {
                when (try { it.type } catch (_: SecurityException) { BluetoothDevice.DEVICE_TYPE_CLASSIC }) {
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> 0
                    BluetoothDevice.DEVICE_TYPE_DUAL -> 1
                    else -> 2
                }
            }!!
        }
        val last = Settings.lastHost
        val lastName = last?.let { a -> filtered.firstOrNull { it.address == a }?.let { name(it) } }
        return deduped.sortedWith(compareBy({ it.address != last && name(it) != lastName }, { name(it).lowercase() }))
    }

    /** Same computer as [d]? Matches the address or, for a duplicate bond of the same PC, the name. */
    fun sameHost(d: BluetoothDevice, address: String?, hostName: String?): Boolean =
        d.address == address || (hostName != null && name(d) == hostName)

    /** The computer used last, if Bluetooth still knows it. */
    fun lastHostDevice(): BluetoothDevice? = Settings.lastHost?.let {
        try { adapter?.getRemoteDevice(it) } catch (_: Exception) { null }
    }

    fun name(d: BluetoothDevice): String = if (demo && d.address in demoHosts) demoHosts.getValue(d.address) else try {
        (if (android.os.Build.VERSION.SDK_INT >= 30) d.alias else null) ?: d.name ?: d.address
    } catch (e: SecurityException) { d.address }

    @Volatile private var connectRetries = 0
    @Volatile private var connectAttempt = 0

    private fun notAnswering(d: BluetoothDevice) =
        "${name(d)} isn't answering. Wake it and make sure Bluetooth is on; it can also connect to this phone " +
            "by itself, PadBridge stays ready."

    fun connect(device: BluetoothDevice, retry: Boolean = false) {
        if (!retry) connectRetries = 0
        io.post {
            val h = hid
            if (h == null || !registered) {
                pendingConnect = device
                return@post
            }
            val current = host
            if (current != null && current.address == device.address) return@post
            connectStartedAt = android.os.SystemClock.uptimeMillis()
            hostRefused = false
            if (current != null) {
                releaseAllInternal()
                try { h.disconnect(current) } catch (_: Exception) {}
            }
            connectingTo = device
            setState(State.CONNECTING, "Connecting to ${name(device)}…")
            val ok = try { h.connect(device) } catch (e: Exception) { false }
            if (!ok) {
                connectingTo = null
                setState(State.READY, "Couldn't start a connection to ${name(device)}.")
                return@post
            }
            // Fall back to READY if the host never answers. Each attempt has its own number, so a timeout left
            // over from an earlier attempt can't fire in the middle of a retry.
            val attempt = ++connectAttempt
            main.postDelayed({
                if (attempt == connectAttempt && state == State.CONNECTING && connectingTo?.address == device.address) {
                    connectingTo = null
                    connectRetries = 0
                    setState(State.READY, notAnswering(device))
                }
            }, 12000)
        }
    }

    fun disconnect() {
        io.post {
            releaseAllInternal()
            val d = host ?: connectingTo ?: return@post
            try { hid?.disconnect(d) } catch (_: Exception) {}
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            this@HidManager.registered = registered
            Log.i(TAG, "onAppStatusChanged registered=$registered plugged=${pluggedDevice?.address}")
            if (registered) { main.removeCallbacks(registerWatchdog); registerAttempts = 0 }
            if (!registered) {
                host = null
                // registerAttempts > 0: the watchdog unregistered on purpose and re-registers itself.
                if (hid != null && started && registerAttempts == 0) {
                    setState(State.REGISTERING, "HID registration lost, retrying…")
                    scheduleReRegister()
                }
                return
            }
            retryCount = 0
            if (state != State.CONNECTED) setState(State.READY, "Pick a computer below, or find a new one.")
            val target = pendingConnect
                ?: pluggedDevice
                ?: Settings.lastHost?.takeIf { Settings.autoReconnect }?.let {
                    try { adapter?.getRemoteDevice(it) } catch (_: Exception) { null }
                }
            pendingConnect = null
            if (target != null && host == null) connect(target)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, st: Int) {
            when (st) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device; connectingTo = null; connectRetries = 0; connectAttempt++
                    Settings.lastHost = device.address
                    Settings.addKnownHost(device.address)
                    resetReportState()
                    setState(State.CONNECTED, "Connected to ${name(device)}")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    connectingTo = device
                    setState(State.CONNECTING, "Connecting to ${name(device)}…")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (host?.address == device.address || connectingTo?.address == device.address) {
                        val wasConnected = host?.address == device.address
                        host = null; connectingTo = null
                        hiResWheel = false; hiResPan = false
                        // Like the resolution multiplier, Input Mode is renegotiated on every connection.
                        ptpInputMode = 0; ptpSwitches = 0x03
                        // Dropped within ~1.5 s of our connect, never connected: the computer refused the HID
                        // channel. Windows does this (L2CAP "no resources") when HID isn't enabled for the phone.
                        val elapsed = android.os.SystemClock.uptimeMillis() - connectStartedAt
                        val refused = !wasConnected && elapsed < 1500
                        hostRefused = refused
                        // Not refused, just no answer (Bluetooth page timeout, ~5 s): the computer may be asleep or
                        // not listening yet. Retry a couple of times; HID stays registered, so the computer can also
                        // connect in by itself (Windows often does a minute later).
                        val noAnswer = !wasConnected && !refused && elapsed < 9000
                        if (noAnswer && connectRetries < 2) {
                            connectRetries++
                            setState(State.CONNECTING, "${name(device)} didn't answer, trying again ($connectRetries/2)…")
                            main.postDelayed({ if (host == null) connect(device, retry = true) }, 2500)
                        } else {
                            connectRetries = 0
                            setState(
                                State.READY,
                                when {
                                    refused && isCar(device) -> carRefusalHint(device)
                                    refused && !isComputer(device) -> tvRefusalHint(device)
                                    refused -> "${name(device)} refused the trackpad and keyboard connection. " + refusalHint()
                                    noAnswer -> notAnswering(device)
                                    else -> "Disconnected from ${name(device)}"
                                }
                            )
                        }
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val h = hid ?: return
            val reportId = id.toInt()
            try {
                when (type) {
                    BluetoothHidDevice.REPORT_TYPE_FEATURE -> {
                        val reply: ByteArray? = when (reportId) {
                            HidDescriptor.ID_MOUSE -> byteArrayOf(featureByte())
                            // Precision Touchpad features (only declared when ptpRegistered).
                            HidDescriptor.ID_PTP_CAPS -> if (ptpRegistered) byteArrayOf(HidDescriptor.PTP_FINGERS.toByte()) else null // max 5, click-pad
                            HidDescriptor.ID_PTP_BLOB -> if (ptpRegistered) HidDescriptor.PTPHQA else null
                            HidDescriptor.ID_PTP_MODE -> if (ptpRegistered) byteArrayOf(ptpInputMode.toByte()) else null
                            HidDescriptor.ID_PTP_SWITCH -> if (ptpRegistered) byteArrayOf(ptpSwitches.toByte()) else null
                            HidDescriptor.ID_PTP_LATENCY -> if (ptpRegistered) byteArrayOf(0) else null
                            else -> null
                        }
                        if (reply != null) {
                            Log.i(TAG, "GET_FEATURE $reportId -> ${reply.size} bytes (buffer $bufferSize)")
                            h.replyReport(device, type, id, reply)
                        } else h.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                    }
                    BluetoothHidDevice.REPORT_TYPE_INPUT -> {
                        val len = when (reportId) {
                            HidDescriptor.ID_KEYBOARD -> HidDescriptor.KEYBOARD_LEN
                            HidDescriptor.ID_MOUSE -> HidDescriptor.MOUSE_LEN
                            HidDescriptor.ID_CONSUMER -> HidDescriptor.CONSUMER_LEN
                            HidDescriptor.ID_GAMEPAD -> HidDescriptor.GAMEPAD_LEN
                            HidDescriptor.ID_NKRO -> if (nkroRegistered) HidDescriptor.NKRO_LEN else -1
                            HidDescriptor.ID_PTP_INPUT -> if (ptpRegistered) HidDescriptor.PTP_LEN else -1
                            else -> -1
                        }
                        if (len < 0) h.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                        else h.replyReport(device, type, id, neutralReport(reportId, len))
                    }
                    else -> h.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
                }
            } catch (e: Exception) { Log.w(TAG, "onGetReport", e) }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            val reportId = id.toInt()
            // Some stacks include the report ID as the first byte, some don't: the payload is the last byte.
            val payload = data.lastOrNull()?.toInt()?.and(0xFF) ?: 0
            if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE && ptpRegistered && reportId == HidDescriptor.ID_PTP_MODE) {
                // Host switches between mouse (0) and touchpad (3) reporting. Anything else means mouse.
                val mode = if (payload == 3) 3 else 0
                Log.i(TAG, "PTP input mode -> $mode")
                if (mode != ptpInputMode) { ptpInputMode = mode; releaseAll(); notifyChanged() }
            } else if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE && ptpRegistered && reportId == HidDescriptor.ID_PTP_SWITCH) {
                ptpSwitches = payload and 0x03
                Log.i(TAG, "PTP surface/button switch -> $ptpSwitches")
            } else if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE && reportId == HidDescriptor.ID_MOUSE) {
                hiResWheel = payload and 0x03 != 0
                hiResPan = (payload shr 2) and 0x03 != 0
                Log.i(TAG, "resolution multiplier wheel=$hiResWheel pan=$hiResPan")
            } else if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && reportId == HidDescriptor.ID_KEYBOARD) {
                leds = payload; notifyChanged()
            }
            try { hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS) } catch (_: Exception) {}
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            if (reportId.toInt() == HidDescriptor.ID_KEYBOARD && data.isNotEmpty()) {
                leds = data.last().toInt() and 0xFF; notifyChanged()
            }
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            if (host?.address != device.address) return
            host = null
            setState(State.READY, "${name(device)} unplugged")
        }
    }

    // ------------------------------------------------------------------ report state (io thread only)

    @Volatile private var hiResPan = false
    private var mouseButtons = 0
    private var accX = 0f
    private var accY = 0f
    private var accWheel = 0f
    private var accPan = 0f
    private var flushPending = false
    private var kbMods = 0
    private val kbKeys = LinkedHashSet<Int>()
    private var lastGamepad = ByteArray(HidDescriptor.GAMEPAD_LEN)

    private fun featureByte(): Byte = ((if (hiResWheel) 1 else 0) or ((if (hiResPan) 1 else 0) shl 2)).toByte()

    private fun neutralReport(id: Int, len: Int): ByteArray = when (id) {
        HidDescriptor.ID_GAMEPAD -> neutralGamepad()
        HidDescriptor.ID_MOUSE -> ByteArray(len).also { it[0] = mouseButtons.toByte() }
        else -> ByteArray(len)
    }

    // ------------------------------------------------------------------ Precision Touchpad

    /** 0 = mouse (default), 3 = the host asked for touchpad reports. Not persistent: resets per connection. */
    @Volatile var ptpInputMode = 0; private set
    @Volatile private var ptpSwitches = 0x03

    /** True when the computer is taking raw touches and doing gestures itself (Windows PTP / Linux multitouch). */
    val ptpActive get() = ptpRegistered && ptpInputMode == 3 && isConnected

    data class Contact(val id: Int, val x: Int, val y: Int, val tip: Boolean)

    /** One touch frame: up to 5 contacts (x/y 0..4095), lifted contacts sent once with tip = false. */
    fun touchFrame(contacts: List<Contact>, button: Boolean, eventTimeMs: Long) {
        val r = ByteArray(HidDescriptor.PTP_LEN)
        contacts.take(HidDescriptor.PTP_FINGERS).forEachIndexed { i, c ->
            val o = i * 6
            r[o] = ((if (c.tip) 0x02 else 0) or 0x01).toByte() // tip switch, confidence
            r[o + 1] = (c.id and 0x0F).toByte()
            r[o + 2] = c.x.toByte(); r[o + 3] = (c.x shr 8).toByte()
            r[o + 4] = c.y.toByte(); r[o + 5] = (c.y shr 8).toByte()
        }
        val scan = (eventTimeMs * 10) and 0xFFFF // 100 us units, wraps
        val b = HidDescriptor.PTP_FINGERS * 6
        r[b] = scan.toByte(); r[b + 1] = (scan shr 8).toByte()
        r[b + 2] = contacts.size.coerceAtMost(HidDescriptor.PTP_FINGERS).toByte()
        r[b + 3] = (if (button) 1 else 0).toByte()
        io.post { send(HidDescriptor.ID_PTP_INPUT, r) }
    }

    private fun resetReportState() = io.post {
        mouseButtons = 0; accX = 0f; accY = 0f; accWheel = 0f; accPan = 0f
        kbMods = 0; kbKeys.clear(); lastBoot = ByteArray(HidDescriptor.KEYBOARD_LEN); lastGamepad = neutralGamepad()
    }

    private fun send(id: Int, data: ByteArray) {
        val h = hid ?: return
        val d = host ?: return
        try { h.sendReport(d, id, data) } catch (e: Exception) { Log.w(TAG, "sendReport", e) }
    }

    private fun releaseAllInternal() {
        if (host == null) return
        if (mouseButtons != 0) { mouseButtons = 0; sendMouse(0, 0, 0, 0) }
        if (kbMods != 0 || kbKeys.isNotEmpty()) { kbMods = 0; kbKeys.clear(); sendKeyboard() }
        send(HidDescriptor.ID_CONSUMER, ByteArray(2))
        lastGamepad = neutralGamepad(); send(HidDescriptor.ID_GAMEPAD, lastGamepad)
    }

    fun releaseAll() = io.post { releaseAllInternal() }

    // ------------------------------------------------------------------ mouse

    private fun sendMouse(x: Int, y: Int, wheel: Int, pan: Int) {
        send(
            HidDescriptor.ID_MOUSE, byteArrayOf(
                mouseButtons.toByte(),
                x.toByte(), (x shr 8).toByte(),
                y.toByte(), (y shr 8).toByte(),
                wheel.toByte(), pan.toByte(),
            )
        )
    }

    private val flush = Runnable {
        flushPending = false
        val x = accX.toInt().coerceIn(-32767, 32767)
        val y = accY.toInt().coerceIn(-32767, 32767)
        val w = accWheel.toInt().coerceIn(-127, 127)
        val p = accPan.toInt().coerceIn(-127, 127)
        accX -= x; accY -= y; accWheel -= w; accPan -= p
        if (x != 0 || y != 0 || w != 0 || p != 0) sendMouse(x, y, w, p)
    }

    private fun scheduleFlush() {
        if (!flushPending) { flushPending = true; io.post(flush) }
    }

    /** Relative pointer motion in HID counts. */
    fun move(dx: Float, dy: Float) = io.post {
        accX += dx; accY += dy; scheduleFlush()
    }

    /** Scroll in wheel notches; positive [v] scrolls up, positive [h] scrolls right. */
    fun scroll(v: Float, h: Float) = io.post {
        accWheel += v * (if (hiResWheel) HidDescriptor.HIRES_MULTIPLIER else 1)
        accPan += h * (if (hiResPan) HidDescriptor.HIRES_MULTIPLIER else 1)
        scheduleFlush()
    }

    /** Clear sub-notch scroll leftovers so a new gesture starts clean. */
    fun resetScroll() = io.post { accWheel = 0f; accPan = 0f }

    fun setButton(button: Int, down: Boolean) = io.post {
        val nb = if (down) mouseButtons or button else mouseButtons and button.inv()
        if (nb != mouseButtons) {
            flush.run()
            mouseButtons = nb
            sendMouse(0, 0, 0, 0)
        }
    }

    fun click(button: Int, count: Int = 1) = io.post {
        flush.run()
        repeat(count) {
            mouseButtons = mouseButtons or button; sendMouse(0, 0, 0, 0)
            mouseButtons = mouseButtons and button.inv(); sendMouse(0, 0, 0, 0)
        }
    }

    /** Wheel steps with extra modifiers held (Ctrl+wheel zoom). Whole notches regardless of hi-res. */
    fun modifiedWheel(mods: Int, notches: Int) = io.post {
        if (notches == 0) return@post
        val saved = kbMods
        kbMods = kbMods or mods; sendKeyboard()
        val step = if (hiResWheel) HidDescriptor.HIRES_MULTIPLIER else 1
        repeat(abs(notches)) { sendMouse(0, 0, if (notches > 0) step else -step, 0) }
        kbMods = saved; sendKeyboard()
    }

    // ------------------------------------------------------------------ keyboard

    private var lastBoot = ByteArray(HidDescriptor.KEYBOARD_LEN)

    private fun sendKeyboard() {
        if (nkroRegistered) {
            // Everything the bitmap covers goes in the N-key report; rare keys outside it use the 6-key report.
            val n = ByteArray(HidDescriptor.NKRO_LEN)
            n[0] = kbMods.toByte()
            val rest = ArrayList<Int>()
            for (k in kbKeys) {
                if (k in HidDescriptor.NKRO_FIRST..HidDescriptor.NKRO_LAST) {
                    val bit = k - HidDescriptor.NKRO_FIRST
                    n[1 + bit / 8] = (n[1 + bit / 8].toInt() or (1 shl (bit % 8))).toByte()
                } else rest += k
            }
            send(HidDescriptor.ID_NKRO, n)
            val r = ByteArray(HidDescriptor.KEYBOARD_LEN)
            rest.take(6).forEachIndexed { i, k -> r[2 + i] = k.toByte() }
            if (!r.contentEquals(lastBoot)) { lastBoot = r; send(HidDescriptor.ID_KEYBOARD, r) }
            return
        }
        val r = ByteArray(HidDescriptor.KEYBOARD_LEN)
        r[0] = kbMods.toByte()
        kbKeys.take(6).forEachIndexed { i, k -> r[2 + i] = k.toByte() }
        send(HidDescriptor.ID_KEYBOARD, r)
    }

    /** Full keyboard state from the on-screen keyboard (held modifiers + held keys). */
    fun setKeyboardState(mods: Int, keys: Collection<Int>) = io.post {
        kbMods = mods; kbKeys.clear(); kbKeys.addAll(keys); sendKeyboard()
    }

    /** Press and release a combo. Modifiers go down first so macOS reliably sees the chord. */
    fun tap(code: Int, mods: Int = 0) = io.post { tapInternal(code, mods) }

    private fun tapInternal(code: Int, mods: Int) {
        val base = kbMods
        val m = base or mods
        if (mods != 0) { kbMods = m; sendKeyboard() }
        if (code != 0) {
            kbMods = m; kbKeys.add(code); sendKeyboard()
            kbKeys.remove(code); sendKeyboard()
        }
        if (mods != 0) { kbMods = base; sendKeyboard() }
    }

    fun combo(c: Combo) = tap(c.code, c.mods)

    /** Types text as US-layout keystrokes. Returns the number of characters that could not be typed. */
    fun type(text: CharSequence): Int {
        var skipped = 0
        val strokes = ArrayList<Keys.Stroke>(text.length)
        for (ch in text) {
            val s = Keys.strokeFor(ch)
            if (s == null) skipped++ else strokes.add(s)
        }
        io.post { strokes.forEach { tapInternal(it.code, it.mods) } }
        return skipped
    }

    fun backspace(n: Int) = io.post { repeat(n) { tapInternal(Keys.BACKSPACE, 0) } }

    // ------------------------------------------------------------------ consumer

    fun consumer(usage: Int) = io.post {
        send(HidDescriptor.ID_CONSUMER, byteArrayOf(usage.toByte(), (usage shr 8).toByte()))
        send(HidDescriptor.ID_CONSUMER, ByteArray(2))
    }

    // ------------------------------------------------------------------ gamepad

    fun neutralGamepad() = byteArrayOf(0, 0, 8, 128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 0, 0)

    fun gamepad(buttons: Int, hat: Int, lx: Float, ly: Float, rx: Float, ry: Float, lt: Float, rt: Float) {
        fun axis(v: Float) = (128 + v.coerceIn(-1f, 1f) * 127f).roundToInt().coerceIn(0, 255).toByte()
        fun trig(v: Float) = (v.coerceIn(0f, 1f) * 255f).roundToInt().toByte()
        val r = byteArrayOf(
            buttons.toByte(), (buttons shr 8).toByte(), (hat and 0x0F).toByte(),
            axis(lx), axis(ly), axis(rx), axis(ry), trig(lt), trig(rt),
        )
        io.post {
            if (!r.contentEquals(lastGamepad)) { lastGamepad = r; send(HidDescriptor.ID_GAMEPAD, r) }
        }
    }
}
