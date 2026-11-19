package dev.padbridge

import dev.padbridge.Keys.L_ALT
import dev.padbridge.Keys.L_CTRL
import dev.padbridge.Keys.L_GUI
import dev.padbridge.Keys.L_SHIFT

/** What a remote button sends: a key (held while pressed), or a consumer-control usage. */
sealed class RemoteOut {
    data class Key(val code: Int, val mods: Int = 0) : RemoteOut()
    data class Consumer(val usage: Int) : RemoteOut()
    data object Nothing : RemoteOut()
}

data class RemoteButton(val label: String, val out: RemoteOut)

/**
 * D-pad remote profiles. Directions are always the arrow keys and Select is Enter; Back, Home and
 * the extra buttons differ per device.
 *
 * Android TV maps HID "AC Back" / "AC Home" (consumer 0x224 / 0x223) to its Back and Home keys and
 * keyboard Menu (0x65) to KEYCODE_MENU. Apple TV takes keyboards for navigation (arrows, Return,
 * Esc = Back/Menu); its handling of the consumer Home usage is unverified.
 */
enum class RemoteProfile(val label: String) {
    ANDROID_TV("Android TV"),
    APPLE_TV("Apple TV"),
    MAC("Mac"),
    PC("PC"),
    SLIDES("Slides");

    val select get() = RemoteButton("OK", RemoteOut.Key(Keys.ENTER))

    val back: RemoteButton
        get() = when (this) {
            ANDROID_TV -> RemoteButton("Back", RemoteOut.Consumer(AC_BACK))
            APPLE_TV -> RemoteButton("Menu", RemoteOut.Key(Keys.ESC))
            SLIDES -> RemoteButton("End show", RemoteOut.Key(Keys.ESC))
            else -> RemoteButton("Back", RemoteOut.Key(Keys.ESC))
        }

    fun home(os: HostOs): RemoteButton = when (this) {
        ANDROID_TV -> RemoteButton("Home", RemoteOut.Consumer(AC_HOME))
        APPLE_TV -> RemoteButton("Home", RemoteOut.Consumer(AC_HOME))
        MAC -> RemoteButton("Mission Control", RemoteOut.Key(Keys.UP, L_CTRL))
        PC -> RemoteButton("Start", RemoteOut.Key(0, L_GUI))
        SLIDES -> when (os) {
            // PowerPoint for Mac / Keynote both accept Cmd+Shift+Return to play from the start (Keynote: Option+Cmd+P too).
            HostOs.MAC -> RemoteButton("Start show", RemoteOut.Key(Keys.ENTER, L_GUI or L_SHIFT))
            else -> RemoteButton("Start show", RemoteOut.Key(Keys.f(5)))
        }
    }

    /** Previous / next, under the D-pad. Slides use them for slides, everything else for tracks. */
    val prevNext: Pair<RemoteButton, RemoteButton>
        get() = if (this == SLIDES) RemoteButton("Previous", RemoteOut.Key(Keys.LEFT)) to RemoteButton("Next", RemoteOut.Key(Keys.RIGHT))
        else RemoteButton("Previous", RemoteOut.Consumer(Keys.C_PREV)) to RemoteButton("Next", RemoteOut.Consumer(Keys.C_NEXT))

    fun extras(os: HostOs): List<RemoteButton> = when (this) {
        ANDROID_TV -> listOf(
            RemoteButton("Menu", RemoteOut.Key(Keys.MENU)),
            RemoteButton("Search", RemoteOut.Consumer(AC_SEARCH)),
            RemoteButton("Rewind", RemoteOut.Consumer(REWIND)),
            RemoteButton("Fast fwd", RemoteOut.Consumer(FAST_FORWARD)),
        )
        APPLE_TV -> listOf(
            RemoteButton("Space", RemoteOut.Key(Keys.SPACE)),
            RemoteButton("Rewind", RemoteOut.Consumer(REWIND)),
            RemoteButton("Fast fwd", RemoteOut.Consumer(FAST_FORWARD)),
        )
        MAC -> listOf(
            RemoteButton("Space", RemoteOut.Key(Keys.SPACE)),
            RemoteButton("Full screen", RemoteOut.Key(Keys.letter('f'), L_CTRL or L_GUI)),
            RemoteButton("⌘Tab", RemoteOut.Key(Keys.TAB, L_GUI)),
            RemoteButton("Spotlight", RemoteOut.Key(Keys.SPACE, L_GUI)),
        )
        PC -> listOf(
            RemoteButton("Space", RemoteOut.Key(Keys.SPACE)),
            RemoteButton("F", RemoteOut.Key(Keys.letter('f'))),
            RemoteButton("Alt+Tab", RemoteOut.Key(Keys.TAB, L_ALT)),
            RemoteButton("Full screen", RemoteOut.Key(Keys.f(11))),
        )
        SLIDES -> listOf(
            when (os) {
                HostOs.MAC -> RemoteButton("From here", RemoteOut.Key(Keys.ENTER, L_GUI))
                else -> RemoteButton("From here", RemoteOut.Key(Keys.f(5), L_SHIFT))
            },
            RemoteButton("Black", RemoteOut.Key(Keys.letter('b'))),
            RemoteButton("White", RemoteOut.Key(Keys.letter('w'))),
            RemoteButton("Laser", RemoteOut.Key(Keys.letter('l'), L_CTRL)),
        )
    }

    companion object {
        const val AC_SEARCH = 0x221
        const val AC_HOME = 0x223
        const val AC_BACK = 0x224
        const val FAST_FORWARD = 0xB3
        const val REWIND = 0xB4

        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: ANDROID_TV
    }
}

/** Press / release helpers so arrows repeat on the host while held. */
object RemoteSender {
    fun down(o: RemoteOut) {
        when (o) {
            is RemoteOut.Key -> when {
                o.code == 0 -> HidManager.combo(Combo(o.mods, 0))
                o.mods != 0 -> {
                    // Modifiers first so macOS sees the chord.
                    HidManager.setKeyboardState(o.mods, emptyList())
                    HidManager.setKeyboardState(o.mods, listOf(o.code))
                }
                else -> HidManager.setKeyboardState(0, listOf(o.code))
            }
            is RemoteOut.Consumer -> HidManager.consumer(o.usage)
            RemoteOut.Nothing -> {}
        }
    }

    fun up(o: RemoteOut) {
        if (o is RemoteOut.Key && o.code != 0) HidManager.setKeyboardState(0, emptyList())
    }

    fun tap(o: RemoteOut) { down(o); up(o) }
}
