package dev.padbridge

import java.util.concurrent.TimeUnit

/**
 * Experimental root features. Everything here changes Bluetooth system properties with `resetprop`
 * (Magisk / KernelSU) or `setprop`, then restarts Bluetooth so the stack re-reads them. Changes are
 * not persistent: a reboot restores the phone's defaults.
 *
 * Property names come from AOSP: BluetoothProperties.sysprop (profile switches, class of device) and
 * packages/modules/Bluetooth sysprop/device_id.sysprop + a2dp.sysprop.
 */
object Root {
    data class Result(val ok: Boolean, val out: String)

    /** Runs a command as root. Blocking; call off the main thread. */
    fun run(cmd: String, timeoutSec: Long = 20): Result = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        val done = p.waitFor(timeoutSec, TimeUnit.SECONDS)
        val out = p.inputStream.bufferedReader().readText().trim()
        if (!done) { p.destroy(); Result(false, "timed out") } else Result(p.exitValue() == 0, out)
    } catch (e: Exception) {
        Result(false, e.message ?: "su not available")
    }

    /** True when `su` grants uid 0. The root manager may show a prompt the first time. */
    fun available(): Boolean = run("id -u", 15).let { it.ok && it.out.lines().lastOrNull()?.trim() == "0" }

    /** Reads a system property (no root needed). */
    fun getprop(name: String): String = try {
        val p = ProcessBuilder("getprop", name).start()
        p.waitFor(3, TimeUnit.SECONDS)
        p.inputStream.bufferedReader().readText().trim()
    } catch (_: Exception) { "" }

    private fun quote(v: String) = "'" + v.replace("'", "'\\''") + "'"

    /** Sets (or with an empty value, deletes) a property. Prefers Magisk's resetprop, which ignores property contexts. */
    fun setprop(name: String, value: String): Result {
        val cmd = if (value.isEmpty()) {
            "(command -v resetprop >/dev/null && resetprop --delete $name) || setprop $name ''"
        } else {
            "(command -v resetprop >/dev/null && resetprop $name ${quote(value)}) || setprop $name ${quote(value)}"
        }
        val r = run(cmd)
        val now = getprop(name)
        return Result(now == value, if (now == value) "$name=$now" else "$name is '$now' (wanted '$value'). ${r.out}")
    }

    /** Bluetooth only reads these properties when it starts, so bounce it. */
    fun restartBluetooth(): Result = run(
        "cmd bluetooth_manager disable; sleep 3; cmd bluetooth_manager enable; sleep 2", 30,
    )

    // ------------------------------------------------------------------ features

    const val A2DP_SINK = "bluetooth.profile.a2dp.sink.enabled"
    const val AVRCP_CONTROLLER = "bluetooth.profile.avrcp.controller.enabled"
    const val SRC_SINK_COEXIST = "bluetooth.a2dp.src_sink_coexist.enabled"
    const val CLASS_OF_DEVICE = "bluetooth.device.class_of_device"
    const val DID_VENDOR = "bluetooth.device_id.vendor_id"
    const val DID_VENDOR_SOURCE = "bluetooth.device_id.vendor_id_source"
    const val DID_PRODUCT = "bluetooth.device_id.product_id"

    fun speakerEnabled() = getprop(A2DP_SINK) == "true"

    private val speakerProps = listOf(SRC_SINK_COEXIST, A2DP_SINK, AVRCP_CONTROLLER)

    /**
     * Bluetooth speaker: A2DP sink + AVRCP controller, keeping A2DP source for headphones (coexist).
     * The ROM's original values are saved first and put back exactly when turning it off.
     */
    fun setSpeaker(on: Boolean): List<Result> = if (on) {
        if (Settings.rootOriginalSpeaker == null) {
            Settings.rootOriginalSpeaker = speakerProps.joinToString("|") { getprop(it) }
        }
        speakerProps.map { setprop(it, "true") }
    } else {
        val saved = Settings.rootOriginalSpeaker?.split("|")
        val results = speakerProps.mapIndexed { i, p -> setprop(p, saved?.getOrNull(i) ?: "") }
        if (results.all { it.ok }) Settings.rootOriginalSpeaker = null
        results
    }

    /** Class of Device presets. The property is 3 bytes, most significant first (phones: 90,2,12 = 0x5A020C). */
    enum class DeviceClass(val label: String, val value: String, val blurb: String) {
        DEFAULT("Phone (default)", "", "What the phone normally reports"),
        KEYBOARD_MOUSE("Keyboard + mouse", "0,5,192", "0x0005C0 peripheral combo. Windows should load HID instead of phone audio"),
        GAMEPAD("Gamepad", "0,5,8", "0x000508 peripheral gamepad"),
    }

    fun currentClass(): String = getprop(CLASS_OF_DEVICE)

    fun setDeviceClass(c: DeviceClass): Result {
        if (c == DeviceClass.DEFAULT) {
            val r = setprop(CLASS_OF_DEVICE, Settings.rootOriginalCod ?: "")
            if (r.ok) Settings.rootOriginalCod = null
            return r
        }
        if (Settings.rootOriginalCod == null) Settings.rootOriginalCod = currentClass()
        return setprop(CLASS_OF_DEVICE, c.value)
    }

    /** Bluetooth Device ID record presets (vendor ID source 2 = USB-IF). */
    enum class ControllerId(val label: String, val vendor: Int, val product: Int, val blurb: String) {
        NONE("Off (phone's own ID)", 0, 0, "Default Device ID record"),
        XBOX("Xbox Wireless Controller", 0x045E, 0x02FD, "045E:02FD, Xbox One S over Bluetooth"),
        DUALSENSE("DualSense", 0x054C, 0x0CE6, "054C:0CE6, PlayStation 5 controller"),
        DUALSHOCK4("DualShock 4", 0x054C, 0x09CC, "054C:09CC, PlayStation 4 controller v2"),
        SWITCH_PRO("Switch Pro Controller", 0x057E, 0x2009, "057E:2009, Nintendo Switch Pro"),
    }

    fun currentControllerId(): String {
        val v = getprop(DID_VENDOR); val p = getprop(DID_PRODUCT)
        return if (v.isEmpty() && p.isEmpty()) "" else "$v:$p"
    }

    fun setControllerId(c: ControllerId): List<Result> = if (c == ControllerId.NONE) listOf(
        setprop(DID_VENDOR, ""), setprop(DID_VENDOR_SOURCE, ""), setprop(DID_PRODUCT, ""),
    ) else listOf(
        setprop(DID_VENDOR, c.vendor.toString()),
        setprop(DID_VENDOR_SOURCE, "2"),
        setprop(DID_PRODUCT, c.product.toString()),
    )
}
