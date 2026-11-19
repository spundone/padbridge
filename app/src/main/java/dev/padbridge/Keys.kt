package dev.padbridge

import android.view.KeyEvent

/** USB HID keyboard usages (page 0x07) and helpers. Host layout is assumed to be US / ANSI. */
object Keys {
    // Modifier bits in byte 0 of the keyboard report
    const val L_CTRL = 0x01
    const val L_SHIFT = 0x02
    const val L_ALT = 0x04
    const val L_GUI = 0x08
    const val R_CTRL = 0x10
    const val R_SHIFT = 0x20
    const val R_ALT = 0x40
    const val R_GUI = 0x80

    const val A = 0x04
    const val ENTER = 0x28
    const val ESC = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val MINUS = 0x2D
    const val EQUAL = 0x2E
    const val LBRACKET = 0x2F
    const val RBRACKET = 0x30
    const val BACKSLASH = 0x31
    const val SEMICOLON = 0x33
    const val QUOTE = 0x34
    const val GRAVE = 0x35
    const val COMMA = 0x36
    const val PERIOD = 0x37
    const val SLASH = 0x38
    const val CAPS = 0x39
    const val F1 = 0x3A
    const val PRINT_SCREEN = 0x46
    const val SCROLL_LOCK = 0x47
    const val PAUSE = 0x48
    const val INSERT = 0x49
    const val HOME = 0x4A
    const val PAGE_UP = 0x4B
    const val DELETE = 0x4C
    const val END = 0x4D
    const val PAGE_DOWN = 0x4E
    const val RIGHT = 0x4F
    const val LEFT = 0x50
    const val DOWN = 0x51
    const val UP = 0x52
    const val NON_US_BACKSLASH = 0x64
    const val MENU = 0x65

    fun letter(c: Char) = A + (c.lowercaseChar() - 'a')
    fun digit(c: Char) = if (c == '0') 0x27 else 0x1E + (c - '1')
    fun f(n: Int) = if (n <= 12) F1 + n - 1 else 0x68 + n - 13

    // Consumer page usages
    const val C_POWER = 0x30
    const val C_PLAY_PAUSE = 0xCD
    const val C_NEXT = 0xB5
    const val C_PREV = 0xB6
    const val C_MUTE = 0xE2
    const val C_VOL_UP = 0xE9
    const val C_VOL_DOWN = 0xEA
    const val C_BRIGHT_UP = 0x6F
    const val C_BRIGHT_DOWN = 0x70

    data class Stroke(val code: Int, val mods: Int)

    private val shiftedDigits = ")!@#$%^&*("
    private val punct = mapOf(
        '-' to MINUS, '=' to EQUAL, '[' to LBRACKET, ']' to RBRACKET, '\\' to BACKSLASH,
        ';' to SEMICOLON, '\'' to QUOTE, '`' to GRAVE, ',' to COMMA, '.' to PERIOD, '/' to SLASH,
    )
    private val shiftedPunct = mapOf(
        '_' to MINUS, '+' to EQUAL, '{' to LBRACKET, '}' to RBRACKET, '|' to BACKSLASH,
        ':' to SEMICOLON, '"' to QUOTE, '~' to GRAVE, '<' to COMMA, '>' to PERIOD, '?' to SLASH,
    )

    /** Maps a character to a US-layout keystroke, or null when the US layout cannot type it. */
    fun strokeFor(c: Char): Stroke? = when {
        c in 'a'..'z' -> Stroke(letter(c), 0)
        c in 'A'..'Z' -> Stroke(letter(c), L_SHIFT)
        c in '0'..'9' -> Stroke(digit(c), 0)
        c == ' ' -> Stroke(SPACE, 0)
        c == '\n' || c == '\r' -> Stroke(ENTER, 0)
        c == '\t' -> Stroke(TAB, 0)
        c in shiftedDigits -> Stroke(digit('0' + shiftedDigits.indexOf(c)), L_SHIFT)
        punct.containsKey(c) -> Stroke(punct.getValue(c), 0)
        shiftedPunct.containsKey(c) -> Stroke(shiftedPunct.getValue(c), L_SHIFT)
        else -> fallback(c)
    }

    /** Typographic characters IMEs like to insert, folded to their ASCII equivalent. */
    private fun fallback(c: Char): Stroke? = when (c) {
        '‘', '’' -> strokeFor('\'')
        '“', '”' -> strokeFor('"')
        '–', '—' -> strokeFor('-')
        ' ' -> strokeFor(' ')
        '…' -> null
        else -> null
    }

    /** Android hardware/IME key codes to HID usages for non-character keys. */
    fun fromAndroidKeyCode(code: Int): Int? = when (code) {
        KeyEvent.KEYCODE_DEL -> BACKSPACE
        KeyEvent.KEYCODE_FORWARD_DEL -> DELETE
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> ENTER
        KeyEvent.KEYCODE_TAB -> TAB
        KeyEvent.KEYCODE_ESCAPE -> ESC
        KeyEvent.KEYCODE_SPACE -> SPACE
        KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT
        KeyEvent.KEYCODE_DPAD_UP -> UP
        KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
        KeyEvent.KEYCODE_MOVE_HOME -> HOME
        KeyEvent.KEYCODE_MOVE_END -> END
        KeyEvent.KEYCODE_PAGE_UP -> PAGE_UP
        KeyEvent.KEYCODE_PAGE_DOWN -> PAGE_DOWN
        KeyEvent.KEYCODE_INSERT -> INSERT
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> F1 + (code - KeyEvent.KEYCODE_F1)
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> A + (code - KeyEvent.KEYCODE_A)
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> digit('0' + (code - KeyEvent.KEYCODE_0))
        else -> null
    }

    fun modsFromAndroidMeta(meta: Int): Int {
        var m = 0
        if (meta and KeyEvent.META_CTRL_ON != 0) m = m or L_CTRL
        if (meta and KeyEvent.META_SHIFT_ON != 0) m = m or L_SHIFT
        if (meta and KeyEvent.META_ALT_ON != 0) m = m or L_ALT
        if (meta and KeyEvent.META_META_ON != 0) m = m or L_GUI
        return m
    }
}
