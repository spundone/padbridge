package dev.padbridge

import dev.padbridge.Keys.L_ALT
import dev.padbridge.Keys.L_CTRL
import dev.padbridge.Keys.L_GUI
import dev.padbridge.Keys.L_SHIFT

/** A key combination sent as one press/release. */
data class Combo(val mods: Int, val code: Int)

data class Shortcut(val label: String, val combo: Combo)

enum class Swipe { UP, DOWN, LEFT, RIGHT }

/**
 * Per-host behaviour: modifier names, shortcut list and what multi-finger gestures send.
 * macOS ignores generic HID multi-touch, so gestures are recognised on the phone and sent
 * as the keyboard shortcuts each OS binds to them by default.
 */
enum class HostOs(val label: String) {
    WINDOWS("Windows"), MAC("macOS"), LINUX("Linux");

    /** The modifier used for copy/paste etc. */
    val primary: Int get() = if (this == MAC) L_GUI else L_CTRL

    /** Pinch zoom step: Ctrl+wheel on Windows/Linux, Cmd +/- on macOS. Returns null when wheel is used. */
    fun zoomCombo(zoomIn: Boolean): Combo? =
        if (this == MAC) Combo(L_GUI, if (zoomIn) Keys.EQUAL else Keys.MINUS) else null

    val shortcuts: List<Shortcut>
        get() {
            val p = primary
            fun k(c: Char) = Keys.letter(c)
            val common = listOf(
                Shortcut("Copy", Combo(p, k('c'))),
                Shortcut("Paste", Combo(p, k('v'))),
                Shortcut("Cut", Combo(p, k('x'))),
                Shortcut("Undo", Combo(p, k('z'))),
                Shortcut(
                    "Redo",
                    if (this == MAC) Combo(p or L_SHIFT, k('z')) else Combo(p, k('y'))
                ),
                Shortcut("Select all", Combo(p, k('a'))),
                Shortcut("Find", Combo(p, k('f'))),
                Shortcut("Save", Combo(p, k('s'))),
                Shortcut("New tab", Combo(p, k('t'))),
                Shortcut("Close tab", Combo(p, k('w'))),
                Shortcut("Reopen tab", Combo(p or L_SHIFT, k('t'))),
            )
            val specific = when (this) {
                WINDOWS -> listOf(
                    Shortcut("Start", Combo(L_GUI, 0)),
                    Shortcut("Alt+Tab", Combo(L_ALT, Keys.TAB)),
                    Shortcut("Task view", Combo(L_GUI, Keys.TAB)),
                    Shortcut("Desktop", Combo(L_GUI, k('d'))),
                    Shortcut("Snip", Combo(L_GUI or L_SHIFT, k('s'))),
                    Shortcut("Explorer", Combo(L_GUI, k('e'))),
                    Shortcut("Search", Combo(L_GUI, k('s'))),
                    Shortcut("Emoji", Combo(L_GUI, Keys.PERIOD)),
                    Shortcut("Task Mgr", Combo(L_CTRL or L_SHIFT, Keys.ESC)),
                    Shortcut("Close app", Combo(L_ALT, Keys.f(4))),
                    Shortcut("Lock", Combo(L_GUI, k('l'))),
                )
                MAC -> listOf(
                    Shortcut("Spotlight", Combo(L_GUI, Keys.SPACE)),
                    Shortcut("⌘Tab", Combo(L_GUI, Keys.TAB)),
                    Shortcut("Mission Ctrl", Combo(L_CTRL, Keys.UP)),
                    Shortcut("Quit", Combo(L_GUI, k('q'))),
                    Shortcut("Hide", Combo(L_GUI, k('h'))),
                    Shortcut("Shot area", Combo(L_GUI or L_SHIFT, Keys.digit('4'))),
                    Shortcut("Shot menu", Combo(L_GUI or L_SHIFT, Keys.digit('5'))),
                    Shortcut("Emoji", Combo(L_CTRL or L_GUI, Keys.SPACE)),
                    Shortcut("Force quit", Combo(L_ALT or L_GUI, Keys.ESC)),
                    Shortcut("Lock", Combo(L_CTRL or L_GUI, k('q'))),
                )
                LINUX -> listOf(
                    Shortcut("Super", Combo(L_GUI, 0)),
                    Shortcut("Alt+Tab", Combo(L_ALT, Keys.TAB)),
                    Shortcut("Terminal", Combo(L_CTRL or L_ALT, k('t'))),
                    Shortcut("Run", Combo(L_ALT, Keys.f(2))),
                    Shortcut("Close win", Combo(L_ALT, Keys.f(4))),
                    Shortcut("Screenshot", Combo(0, Keys.PRINT_SCREEN)),
                    Shortcut("Lock", Combo(L_GUI, k('l'))),
                )
            }
            return specific + common
        }

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: WINDOWS
    }
}
