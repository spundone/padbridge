package dev.padbridge

import android.content.Context
import android.content.SharedPreferences

object Settings {
    private lateinit var p: SharedPreferences

    fun init(ctx: Context) {
        if (!::p.isInitialized) p = ctx.getSharedPreferences("padbridge", Context.MODE_PRIVATE)
    }

    private fun bool(key: String, def: Boolean) = p.getBoolean(key, def)
    private fun putBool(key: String, v: Boolean) = p.edit().putBoolean(key, v).apply()
    private fun float(key: String, def: Float) = p.getFloat(key, def)
    private fun putFloat(key: String, v: Float) = p.edit().putFloat(key, v).apply()

    var hostOs: HostOs
        get() = HostOs.from(p.getString("hostOs", null))
        set(v) {
            p.edit().putString("hostOs", v.name).apply()
            // macOS applies its own "natural scrolling" to wheel input, so default to plain wheel there.
            if (!p.contains("naturalScroll_${v.name}")) naturalScroll = v != HostOs.MAC
            syncRemoteToComputer(v)
        }

    /**
     * The remote's computer profile follows the computer type (Mac for macOS, PC for Windows and Linux).
     * TV and Slides profiles are a separate choice and are left alone.
     */
    private fun syncRemoteToComputer(os: HostOs) {
        val cur = RemoteProfile.from(p.getString("remoteProfile", null))
        if (cur != RemoteProfile.MAC && cur != RemoteProfile.PC) return
        val want = if (os == HostOs.MAC) RemoteProfile.MAC else RemoteProfile.PC
        if (cur != want) p.edit().putString("remoteProfile", want.name).apply()
    }

    /** Remembered per host OS because macOS inverts wheel direction itself. */
    var naturalScroll: Boolean
        get() = bool("naturalScroll_${hostOs.name}", hostOs != HostOs.MAC)
        set(v) = putBool("naturalScroll_${hostOs.name}", v)

    var pointerSpeed: Float
        get() = float("pointerSpeed", 1.0f)
        set(v) = putFloat("pointerSpeed", v)

    var scrollSpeed: Float
        get() = float("scrollSpeed", 1.0f)
        set(v) = putFloat("scrollSpeed", v)

    var momentum: Boolean
        get() = bool("momentum", true)
        set(v) = putBool("momentum", v)

    var tapToClick: Boolean
        get() = bool("tapToClick", true)
        set(v) = putBool("tapToClick", v)

    var showClickButtons: Boolean
        get() = bool("clickButtons", false)
        set(v) = putBool("clickButtons", v)

    var haptics: Boolean
        get() = bool("haptics", true)
        set(v) = putBool("haptics", v)

    var autoReconnect: Boolean
        get() = bool("autoReconnect", true)
        set(v) = putBool("autoReconnect", v)

    var keepScreenOn: Boolean
        get() = bool("keepScreenOn", true)
        set(v) = putBool("keepScreenOn", v)

    var predictiveTyping: Boolean
        get() = bool("predictive", false)
        set(v) = putBool("predictive", v)

    // ---- On-screen keyboard: layout and size are remembered separately for portrait and landscape.
    private fun o(landscape: Boolean) = if (landscape) "L" else "P"

    /**
     * Default: Laptop (five rows, F-keys on Fn) everywhere; in portrait on phones it is kept short (see
     * KeyboardArea) with a trackpad above.
     */
    fun kbLayout(landscape: Boolean): KbLayout =
        KbLayout.from(p.getString("kbLayout_${o(landscape)}", null), KbLayout.LAPTOP)

    /** Dim to outline (AOD) mode after this many seconds without touching the screen; 0 = never. */
    var idleAodSeconds: Int
        get() = p.getInt("idleAod", 60)
        set(v) = p.edit().putInt("idleAod", v).apply()
    fun setKbLayout(landscape: Boolean, l: KbLayout) = p.edit().putString("kbLayout_${o(landscape)}", l.name).apply()

    /**
     * Laptop deck: keyboard on top, a centered trackpad below, like a laptop's palm rest. Per orientation;
     * on by default for tablets in landscape ([tabletLandscape]).
     */
    fun kbLaptop(landscape: Boolean, tabletLandscape: Boolean) =
        if (p.contains("kbLaptop_${o(landscape)}")) p.getBoolean("kbLaptop_${o(landscape)}", false) else tabletLandscape
    fun setKbLaptop(landscape: Boolean, v: Boolean) = p.edit().putBoolean("kbLaptop_${o(landscape)}", v).apply()

    // Laptop deck geometry, per orientation. 0 / negative = automatic.
    /** Keyboard height as a fraction of the deck's height; 0 = automatic. */
    fun deckKeys(landscape: Boolean) = p.getFloat("deckKeys_${o(landscape)}", 0f)
    fun setDeckKeys(landscape: Boolean, v: Float) = p.edit().putFloat("deckKeys_${o(landscape)}", v).apply()
    /** Keyboard width as a fraction of the screen; 0 = automatic (full width, capped on big tablets). */
    fun deckKbWidth(landscape: Boolean) = p.getFloat("deckKbW_${o(landscape)}", 0f)
    fun setDeckKbWidth(landscape: Boolean, v: Float) = p.edit().putFloat("deckKbW_${o(landscape)}", v).apply()
    /** Trackpad width as a fraction of the screen. */
    fun deckPadWidth(landscape: Boolean) = p.getFloat("deckPadW_${o(landscape)}", 0.42f)
    fun setDeckPadWidth(landscape: Boolean, v: Float) = p.edit().putFloat("deckPadW_${o(landscape)}", v).apply()
    /** Trackpad horizontal position: 0 = left, 0.5 = centered, 1 = right. */
    fun deckPadPos(landscape: Boolean) = p.getFloat("deckPadX_${o(landscape)}", 0.5f)
    fun setDeckPadPos(landscape: Boolean, v: Float) = p.edit().putFloat("deckPadX_${o(landscape)}", v).apply()
    fun deckPadFull(landscape: Boolean) = p.getBoolean("deckPadFull_${o(landscape)}", false)
    fun setDeckPadFull(landscape: Boolean, v: Boolean) = p.edit().putBoolean("deckPadFull_${o(landscape)}", v).apply()
    /** Hide the deck's resize and move handles. */
    var deckLocked: Boolean
        get() = bool("deckLocked", false)
        set(v) = putBool("deckLocked", v)
    fun resetDeck(landscape: Boolean) = p.edit().apply {
        listOf("deckKeys_", "deckKbW_", "deckPadW_", "deckPadX_", "deckPadFull_").forEach { remove(it + o(landscape)) }
    }.apply()

    /**
     * S Pen / stylus on trackpads: hovering moves the pointer, touching the screen holds the left button
     * (tap = click, drag = drag), the pen button gives a right click, and fingers are ignored while the pen is
     * near the screen (palm rejection). Off: the pen acts like a finger.
     */
    var stylus: Boolean
        get() = bool("stylus", true)
        set(v) = putBool("stylus", v)

    /** Keyboard height in dp; 0 = fill the available space. */
    fun kbHeight(landscape: Boolean) = p.getFloat("kbHeight_${o(landscape)}", 0f)
    fun setKbHeight(landscape: Boolean, v: Float) = p.edit().putFloat("kbHeight_${o(landscape)}", v).apply()

    /** Keyboard width as a fraction of the screen (0.4..1). */
    fun kbWidth(landscape: Boolean) = p.getFloat("kbWidth_${o(landscape)}", 1f)
    fun setKbWidth(landscape: Boolean, v: Float) = p.edit().putFloat("kbWidth_${o(landscape)}", v).apply()

    /** 0 = left, 1 = center, 2 = right (when narrower than the screen). */
    fun kbAlign(landscape: Boolean) = p.getInt("kbAlign_${o(landscape)}", 1)
    fun setKbAlign(landscape: Boolean, v: Int) = p.edit().putInt("kbAlign_${o(landscape)}", v).apply()

    /** Gap between the halves of the split layout, in key widths. */
    var kbSplitGap: Float
        get() = float("kbSplitGap", 3f)
        set(v) = putFloat("kbSplitGap", v)

    /** Shortcut + media rows above the keyboard: on in portrait, off in landscape (height is scarce). */
    fun kbShowRows(landscape: Boolean) = bool("kbShowRows_${o(landscape)}", !landscape)
    fun setKbShowRows(landscape: Boolean, v: Boolean) = putBool("kbShowRows_${o(landscape)}", v)

    /** Space the keyboard leaves free (shrunk height, or narrow + aligned to a side) becomes a trackpad. */
    var kbFreePad: Boolean
        get() = bool("kbFreePad", true)
        set(v) = putBool("kbFreePad", v)

    /** Register the Precision Touchpad collection so Windows / Linux do gestures natively (needs re-pair). */
    var nativeTouchpad: Boolean
        get() = bool("nativeTouchpad", false)
        set(v) = putBool("nativeTouchpad", v)

    /** N-key rollover keyboard report (changes the HID descriptor: computers must re-pair). */
    var nkro: Boolean
        get() = bool("nkro", false)
        set(v) = putBool("nkro", v)

    var kbPreview: Boolean
        get() = bool("kbPreview", true)
        set(v) = putBool("kbPreview", v)

    /** Flick up on a key to type its shifted character (Windows touch keyboard style). */
    var kbFlick: Boolean
        get() = bool("kbFlick", true)
        set(v) = putBool("kbFlick", v)

    /** Drag on the space bar to move the cursor with arrow keys. */
    var kbSpaceCursor: Boolean
        get() = bool("kbSpaceCursor", true)
        set(v) = putBool("kbSpaceCursor", v)

    var remoteProfile: RemoteProfile
        get() {
            // One-time fix (1.1.0) for profiles saved before the remote followed the computer type.
            if (!p.contains("remoteSynced")) { syncRemoteToComputer(hostOs); p.edit().putBoolean("remoteSynced", true).apply() }
            return RemoteProfile.from(p.getString("remoteProfile", null))
        }
        set(v) = p.edit().putString("remoteProfile", v.name).apply()

    /** In the remote, the phone's volume buttons go to the computer / TV (Slides: previous / next slide). */
    var remoteVolumeKeys: Boolean
        get() = bool("remoteVolKeys", true)
        set(v) = putBool("remoteVolKeys", v)

    /** 0 = follow system, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = p.getInt("themeMode", 0)
        set(v) = p.edit().putInt("themeMode", v).apply()

    /** Wallpaper (Material You) colors on Android 12+; otherwise PadBridge's own palette. */
    var dynamicColor: Boolean
        get() = bool("dynamicColor", true)
        set(v) = putBool("dynamicColor", v)

    /** True-black backgrounds in dark mode (OLED). */
    var pureBlack: Boolean
        get() = bool("pureBlack", true)
        set(v) = putBool("pureBlack", v)

    /** First-run setup finished. */
    var setupDone: Boolean
        // Existing users who already connected somewhere skip it.
        get() = bool("setupDone", p.getString("lastHost", null) != null)
        set(v) = putBool("setupDone", v)

    /** The three speaker properties ("a|b|c") before root mode changed them, to restore. */
    var rootOriginalSpeaker: String?
        get() = p.getString("rootOrigSpeaker", null)
        set(v) = p.edit().putString("rootOrigSpeaker", v).apply()

    /** Class of device before root mode changed it, to restore. */
    var rootOriginalCod: String?
        get() = p.getString("rootOrigCod", null)
        set(v) = p.edit().putString("rootOrigCod", v).apply()

    var gamepadPreset: GamePreset
        get() = GamePreset.from(p.getString("gamepadPreset", null))
        set(v) = p.edit().putString("gamepadPreset", v.name).apply()

    var gamepadStyle: String
        get() = p.getString("gamepadStyle", "xbox") ?: "xbox"
        set(v) = p.edit().putString("gamepadStyle", v).apply()

    /** GitHub build: look for a new release at most once a day when the app opens. Play builds use the Play Store. */
    var autoUpdateCheck: Boolean
        get() = bool("autoUpdateCheck", true)
        set(v) = putBool("autoUpdateCheck", v)

    var lastUpdateCheck: Long
        get() = p.getLong("lastUpdateCheck", 0L)
        set(v) = p.edit().putLong("lastUpdateCheck", v).apply()

    /** A version the user dismissed; the banner stays hidden until a newer one appears. */
    var skippedUpdate: String?
        get() = p.getString("skippedUpdate", null)
        set(v) = p.edit().putString("skippedUpdate", v).apply()

    var lastHost: String?
        get() = p.getString("lastHost", null)
        set(v) = p.edit().putString("lastHost", v).apply()

    /** Hosts PadBridge has paired with or connected to, whatever their Bluetooth class (TVs, phones, tablets). */
    val knownHosts: Set<String>
        get() = p.getStringSet("knownHosts", emptySet())!!.toSet()

    fun addKnownHost(address: String) {
        val now = knownHosts
        if (address !in now) p.edit().putStringSet("knownHosts", now + address).apply()
    }

    fun gesture(slot: GestureSlot, os: HostOs = hostOs): GAction =
        GAction.from(p.getString("g_${os.name}_${slot.name}", null)) ?: GAction.default(os, slot)

    fun setGesture(slot: GestureSlot, action: GAction, os: HostOs = hostOs) =
        p.edit().putString("g_${os.name}_${slot.name}", action.name).apply()

    fun resetGestures(os: HostOs = hostOs) {
        val e = p.edit()
        GestureSlot.entries.forEach { e.remove("g_${os.name}_${it.name}") }
        e.apply()
    }

    fun applyDexPreset(os: HostOs = hostOs) {
        resetGestures(os)
        val e = p.edit()
        GAction.DEX_PRESET.forEach { (slot, a) -> e.putString("g_${os.name}_${slot.name}", a.name) }
        e.apply()
    }

    /** Ignore a finger resting still on the pad while others move (like DeX). */
    var restRejection: Boolean
        get() = bool("restReject", true)
        set(v) = putBool("restReject", v)

    /**
     * Laptop-style palm rejection: touches that land on the trackpad while you type, and palm-sized contacts,
     * are ignored until lifted.
     */
    var palmRejection: Boolean
        get() = bool("palmReject", true)
        set(v) = putBool("palmReject", v)

    /** Max gap between taps of a double tap / tap-and-drag, in ms. */
    var doubleTapMs: Float
        get() = float("doubleTapMs", 300f)
        set(v) = putFloat("doubleTapMs", v)

    /** Trackpad screen orientation: 0 auto, 1 portrait, 2 landscape. */
    var trackpadOrientation: Int
        get() = p.getInt("tpOrient", 0)
        set(v) = p.edit().putInt("tpOrient", v).apply()

    /** Two-finger horizontal swipe navigates back/forward instead of scrolling sideways (macOS default). */
    var twoFingerNavigate: Boolean
        get() = bool("twoNav_${hostOs.name}", hostOs == HostOs.MAC)
        set(v) = putBool("twoNav_${hostOs.name}", v)

    /** One finger along the right / bottom edge scrolls, like a laptop's scroll zone. */
    var edgeScroll: Boolean
        get() = bool("edgeScroll", true)
        set(v) = putBool("edgeScroll", v)

    /** Lock scrolling to one axis when the gesture starts mostly vertical or horizontal. */
    var scrollRails: Boolean
        get() = bool("scrollRails", true)
        set(v) = putBool("scrollRails", v)

    /** Three fingers drag windows/text (macOS accessibility style). Replaces three-finger swipes. */
    var threeFingerDrag: Boolean
        get() = bool("threeDrag", false)
        set(v) = putBool("threeDrag", v)

    var showAllDevices: Boolean
        get() = bool("showAll", false)
        set(v) = putBool("showAll", v)

    /** Trackpad, keyboard and air mouse hide the app bars and system bars. */
    var fullscreenInput: Boolean
        get() = bool("fullscreen", false)
        set(v) = putBool("fullscreen", v)

    /** Air mouse pointer speed multiplier. */
    var airSpeed: Float
        get() = float("airSpeed", 1.0f)
        set(v) = putFloat("airSpeed", v)

    /** Air mouse moves without holding the pad. */
    var airAlwaysAim: Boolean
        get() = bool("airAlways", false)
        set(v) = putBool("airAlways", v)

    /** Gamepad gyro: 0 off, 1 aim with the right stick, 2 steer with the left stick. */
    var gamepadGyro: Int
        get() = p.getInt("padGyro", 0)
        set(v) = p.edit().putInt("padGyro", v).apply()

    var gamepadGyroSens: Float
        get() = float("padGyroSens", 1.0f)
        set(v) = putFloat("padGyroSens", v)
}
