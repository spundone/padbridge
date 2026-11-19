package dev.padbridge

import dev.padbridge.Keys.L_ALT
import dev.padbridge.Keys.L_CTRL
import dev.padbridge.Keys.L_GUI
import dev.padbridge.Keys.L_SHIFT

/** Every configurable trackpad gesture. */
enum class GestureSlot(val title: String, val group: String) {
    TWO_TAP("Two-finger tap", "Taps"),
    THREE_TAP("Three-finger tap", "Taps"),
    FOUR_TAP("Four-finger tap", "Taps"),
    LONG_PRESS("Press and hold", "Taps"),

    TWO_LEFT("Two-finger swipe left", "Two fingers"),
    TWO_RIGHT("Two-finger swipe right", "Two fingers"),
    ROTATE_CW("Rotate clockwise", "Two fingers"),
    ROTATE_CCW("Rotate counter-clockwise", "Two fingers"),

    THREE_UP("Three-finger swipe up", "Three fingers"),
    THREE_DOWN("Three-finger swipe down", "Three fingers"),
    THREE_LEFT("Three-finger swipe left", "Three fingers"),
    THREE_RIGHT("Three-finger swipe right", "Three fingers"),

    FOUR_UP("Four-finger swipe up", "Four fingers"),
    FOUR_DOWN("Four-finger swipe down", "Four fingers"),
    FOUR_LEFT("Four-finger swipe left", "Four fingers"),
    FOUR_RIGHT("Four-finger swipe right", "Four fingers"),

    EDGE_LEFT("Swipe in from left edge", "Edges"),
    EDGE_RIGHT("Swipe in from right edge", "Edges"),
    EDGE_TOP("Swipe down from top edge", "Edges");

    companion object {
        fun swipe(fingers: Int, dir: Swipe): GestureSlot = if (fingers >= 4) when (dir) {
            Swipe.UP -> FOUR_UP; Swipe.DOWN -> FOUR_DOWN; Swipe.LEFT -> FOUR_LEFT; Swipe.RIGHT -> FOUR_RIGHT
        } else when (dir) {
            Swipe.UP -> THREE_UP; Swipe.DOWN -> THREE_DOWN; Swipe.LEFT -> THREE_LEFT; Swipe.RIGHT -> THREE_RIGHT
        }
    }
}

/** What a gesture does. Each resolves to a click, media key or the OS's default keyboard shortcut. */
enum class GAction(val label: String) {
    NONE("Nothing"),
    LEFT_CLICK("Left click"),
    RIGHT_CLICK("Right click"),
    MIDDLE_CLICK("Middle click"),
    DOUBLE_CLICK("Double click"),
    HOLD_DRAG("Drag (hold left button)"),
    BACK("Back"),
    FORWARD("Forward"),
    APP_SWITCH("Switch app"),
    APP_SWITCH_BACK("Switch app (reverse)"),
    OVERVIEW("Overview / Mission Control"),
    APP_WINDOWS("App windows / Exposé"),
    SHOW_DESKTOP("Show desktop"),
    NEXT_DESKTOP("Next desktop / Space"),
    PREV_DESKTOP("Previous desktop / Space"),
    LAUNCHER("Start / Launcher"),
    SEARCH("Search / Spotlight"),
    NOTIFICATIONS("Notifications"),
    WIDGETS("Widgets"),
    NEXT_TAB("Next tab"),
    PREV_TAB("Previous tab"),
    NEW_TAB("New tab"),
    CLOSE_TAB("Close tab"),
    REOPEN_TAB("Reopen closed tab"),
    REFRESH("Refresh"),
    ZOOM_IN("Zoom in"),
    ZOOM_OUT("Zoom out"),
    ZOOM_RESET("Reset zoom"),
    ROTATE_RIGHT("Rotate right"),
    ROTATE_LEFT("Rotate left"),
    FULLSCREEN("Toggle full screen"),
    MINIMIZE("Minimize window"),
    CLOSE_WINDOW("Close window"),
    SCREENSHOT("Screenshot"),
    PLAY_PAUSE("Play / pause"),
    NEXT_TRACK("Next track"),
    PREV_TRACK("Previous track"),
    VOLUME_UP("Volume up"),
    VOLUME_DOWN("Volume down"),
    MUTE("Mute"),
    COPY("Copy"),
    PASTE("Paste"),
    UNDO("Undo"),
    ESCAPE("Escape"),
    ENTER("Enter");

    /** The shortcut for [os], or null if the action isn't a keyboard shortcut or has none on that OS. */
    fun combo(os: HostOs): Combo? {
        val mac = os == HostOs.MAC
        val p = os.primary
        fun c(m: Int, k: Int) = Combo(m, k)
        fun letter(ch: Char) = Keys.letter(ch)
        return when (this) {
            BACK -> if (mac) c(L_GUI, Keys.LBRACKET) else c(L_ALT, Keys.LEFT)
            FORWARD -> if (mac) c(L_GUI, Keys.RBRACKET) else c(L_ALT, Keys.RIGHT)
            APP_SWITCH -> if (mac) c(L_GUI, Keys.TAB) else c(L_ALT, Keys.TAB)
            APP_SWITCH_BACK -> if (mac) c(L_GUI or L_SHIFT, Keys.TAB) else c(L_ALT or L_SHIFT, Keys.TAB)
            OVERVIEW -> when (os) { HostOs.WINDOWS -> c(L_GUI, Keys.TAB); HostOs.MAC -> c(L_CTRL, Keys.UP); HostOs.LINUX -> c(L_GUI, 0) }
            APP_WINDOWS -> when (os) { HostOs.WINDOWS -> c(L_GUI, Keys.TAB); HostOs.MAC -> c(L_CTRL, Keys.DOWN); HostOs.LINUX -> c(L_GUI, 0) }
            SHOW_DESKTOP -> when (os) { HostOs.WINDOWS -> c(L_GUI, letter('d')); HostOs.MAC -> c(0, Keys.f(11)); HostOs.LINUX -> c(L_GUI, letter('d')) }
            NEXT_DESKTOP -> when (os) { HostOs.WINDOWS -> c(L_CTRL or L_GUI, Keys.RIGHT); HostOs.MAC -> c(L_CTRL, Keys.RIGHT); HostOs.LINUX -> c(L_CTRL or L_ALT, Keys.RIGHT) }
            PREV_DESKTOP -> when (os) { HostOs.WINDOWS -> c(L_CTRL or L_GUI, Keys.LEFT); HostOs.MAC -> c(L_CTRL, Keys.LEFT); HostOs.LINUX -> c(L_CTRL or L_ALT, Keys.LEFT) }
            LAUNCHER -> when (os) { HostOs.WINDOWS -> c(L_GUI, 0); HostOs.MAC -> c(L_GUI, Keys.SPACE); HostOs.LINUX -> c(L_GUI, letter('a')) }
            SEARCH -> when (os) { HostOs.WINDOWS -> c(L_GUI, letter('s')); HostOs.MAC -> c(L_GUI, Keys.SPACE); HostOs.LINUX -> c(L_GUI, 0) }
            NOTIFICATIONS -> when (os) { HostOs.WINDOWS -> c(L_GUI, letter('n')); HostOs.MAC -> null; HostOs.LINUX -> c(L_GUI, letter('v')) }
            WIDGETS -> if (os == HostOs.WINDOWS) c(L_GUI, letter('w')) else null
            NEXT_TAB -> c(L_CTRL, Keys.TAB)
            PREV_TAB -> c(L_CTRL or L_SHIFT, Keys.TAB)
            NEW_TAB -> c(p, letter('t'))
            CLOSE_TAB -> c(p, letter('w'))
            REOPEN_TAB -> c(p or L_SHIFT, letter('t'))
            REFRESH -> if (mac) c(L_GUI, letter('r')) else c(0, Keys.f(5))
            ZOOM_IN -> c(p, Keys.EQUAL)
            ZOOM_OUT -> c(p, Keys.MINUS)
            ZOOM_RESET -> c(p, Keys.digit('0'))
            ROTATE_RIGHT -> if (mac) c(L_GUI, letter('r')) else c(L_CTRL, letter('r'))
            ROTATE_LEFT -> if (mac) c(L_GUI, letter('l')) else c(L_CTRL or L_SHIFT, letter('r'))
            FULLSCREEN -> if (mac) c(L_CTRL or L_GUI, letter('f')) else c(0, Keys.f(11))
            MINIMIZE -> when (os) { HostOs.WINDOWS -> c(L_GUI, Keys.DOWN); HostOs.MAC -> c(L_GUI, letter('m')); HostOs.LINUX -> c(L_GUI, letter('h')) }
            CLOSE_WINDOW -> if (mac) c(L_GUI, letter('w')) else c(L_ALT, Keys.f(4))
            SCREENSHOT -> when (os) { HostOs.WINDOWS -> c(L_GUI or L_SHIFT, letter('s')); HostOs.MAC -> c(L_GUI or L_SHIFT, Keys.digit('4')); HostOs.LINUX -> c(0, Keys.PRINT_SCREEN) }
            COPY -> c(p, letter('c'))
            PASTE -> c(p, letter('v'))
            UNDO -> c(p, letter('z'))
            ESCAPE -> c(0, Keys.ESC)
            ENTER -> c(0, Keys.ENTER)
            else -> null
        }
    }

    fun availableOn(os: HostOs) = when (this) {
        NOTIFICATIONS -> os != HostOs.MAC
        WIDGETS -> os == HostOs.WINDOWS
        else -> true
    }

    /** Sends the action. Returns false when it has no equivalent on [os]. */
    fun perform(os: HostOs): Boolean {
        when (this) {
            NONE -> return false
            LEFT_CLICK -> HidManager.click(TrackpadView.BTN_LEFT)
            RIGHT_CLICK -> HidManager.click(TrackpadView.BTN_RIGHT)
            MIDDLE_CLICK -> HidManager.click(TrackpadView.BTN_MIDDLE)
            DOUBLE_CLICK -> HidManager.click(TrackpadView.BTN_LEFT, 2)
            // Only meaningful for press-and-hold, where the trackpad keeps the button held; elsewhere it clicks.
            HOLD_DRAG -> HidManager.click(TrackpadView.BTN_LEFT)
            PLAY_PAUSE -> HidManager.consumer(Keys.C_PLAY_PAUSE)
            NEXT_TRACK -> HidManager.consumer(Keys.C_NEXT)
            PREV_TRACK -> HidManager.consumer(Keys.C_PREV)
            VOLUME_UP -> HidManager.consumer(Keys.C_VOL_UP)
            VOLUME_DOWN -> HidManager.consumer(Keys.C_VOL_DOWN)
            MUTE -> HidManager.consumer(Keys.C_MUTE)
            else -> HidManager.combo(combo(os) ?: return false)
        }
        return true
    }

    companion object {
        /** Samsung DeX touchpad conventions, translated to the host's shortcuts. Other gestures keep OS defaults. */
        val DEX_PRESET = mapOf(
            GestureSlot.TWO_TAP to RIGHT_CLICK,
            GestureSlot.THREE_TAP to BACK,
            GestureSlot.FOUR_TAP to LAUNCHER,
            GestureSlot.LONG_PRESS to HOLD_DRAG,
            GestureSlot.THREE_UP to OVERVIEW,
            GestureSlot.THREE_DOWN to SHOW_DESKTOP,
            GestureSlot.THREE_LEFT to APP_SWITCH,
            GestureSlot.THREE_RIGHT to APP_SWITCH_BACK,
        )

        fun from(name: String?) = entries.firstOrNull { it.name == name }

        /** Defaults mirror each OS's own trackpad gestures where one exists. */
        fun default(os: HostOs, slot: GestureSlot): GAction = when (slot) {
            GestureSlot.TWO_TAP -> RIGHT_CLICK
            GestureSlot.THREE_TAP -> MIDDLE_CLICK
            GestureSlot.FOUR_TAP -> when (os) { HostOs.WINDOWS -> NOTIFICATIONS; HostOs.MAC -> SEARCH; HostOs.LINUX -> LAUNCHER }
            // Like Samsung DeX: touch and hold, then move, drags or selects text.
            GestureSlot.LONG_PRESS -> HOLD_DRAG
            // Content follows fingers: swiping right reveals the previous page.
            GestureSlot.TWO_LEFT -> FORWARD
            GestureSlot.TWO_RIGHT -> BACK
            // Off by default: the rotate shortcuts (Ctrl/Cmd+R) also mean "reload" in browsers.
            GestureSlot.ROTATE_CW -> NONE
            GestureSlot.ROTATE_CCW -> NONE
            GestureSlot.THREE_UP, GestureSlot.FOUR_UP -> OVERVIEW
            GestureSlot.THREE_DOWN -> if (os == HostOs.MAC) APP_WINDOWS else SHOW_DESKTOP
            GestureSlot.FOUR_DOWN -> if (os == HostOs.MAC) APP_WINDOWS else SHOW_DESKTOP
            GestureSlot.THREE_LEFT -> if (os == HostOs.WINDOWS) APP_SWITCH else NEXT_DESKTOP
            GestureSlot.THREE_RIGHT -> if (os == HostOs.WINDOWS) APP_SWITCH_BACK else PREV_DESKTOP
            GestureSlot.FOUR_LEFT -> NEXT_DESKTOP
            GestureSlot.FOUR_RIGHT -> PREV_DESKTOP
            GestureSlot.EDGE_LEFT -> when (os) { HostOs.WINDOWS -> WIDGETS; HostOs.MAC -> SHOW_DESKTOP; HostOs.LINUX -> LAUNCHER }
            GestureSlot.EDGE_RIGHT -> when (os) { HostOs.WINDOWS, HostOs.LINUX -> NOTIFICATIONS; HostOs.MAC -> APP_WINDOWS }
            GestureSlot.EDGE_TOP -> NONE
        }
    }
}
