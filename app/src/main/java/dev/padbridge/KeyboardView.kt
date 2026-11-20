package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import dev.padbridge.Keys.L_ALT
import dev.padbridge.Keys.L_CTRL
import dev.padbridge.Keys.L_GUI
import dev.padbridge.Keys.L_SHIFT
import dev.padbridge.Keys.R_ALT
import dev.padbridge.Keys.R_CTRL
import dev.padbridge.Keys.R_GUI
import dev.padbridge.Keys.R_SHIFT
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** On-screen keyboard layouts. */
/** [cols] x [rows]: the layout's size in key units, used to give keys a sensible shape. */
enum class KbLayout(val label: String, val blurb: String, val cols: Float, val rows: Int) {
    LAPTOP("Laptop", "Five rows like a laptop. Fn + number row = F1–F12, Fn + Esc = `", 15f, 5),
    FULL("Full", "Separate F-row, numbers, arrows", 15f, 6),
    THINKPAD("ThinkPad", "Classic ThinkPad: Fn bottom-left, PgUp/PgDn by the arrows, TrackPoint and its 3 buttons", 15f, 6),
    PHONE("Phone", "Big keys like a phone keyboard. Fn: F-keys, symbols, Home/End", 10f, 6),
    FULL_NUMPAD("Full + numpad", "Everything, with a number pad (landscape / tablets)", 19.5f, 6),
    COMPACT_65("65%", "No F-row; Del, PgUp, PgDn, End column. Fn + number = F-key", 16f, 5),
    SPLIT("Split", "65% halves at each edge, for two thumbs", 19f, 5),
    GAMING("Gaming", "Big WASD block, arrows and action keys", 11.75f, 5),
    NUMPAD("Numpad", "Number pad only, for data entry", 5.25f, 5);

    companion object {
        fun from(name: String?, def: KbLayout) = entries.firstOrNull { it.name == name } ?: def
    }
}

/**
 * On-screen keyboard in Windows, Mac or Linux labels and several layouts. Keys are real key-down /
 * key-up (holding repeats, games see held keys). Modifiers: tap = one-shot, tap again = lock,
 * hold while pressing another key = chord. Keys sit on a unit grid, so keys can span rows
 * (numpad + and Enter) and layouts can leave gaps (split, gaming).
 */
class KeyboardView(ctx: Context) : View(ctx), Routable {

    class Key(
        val label: String,
        val code: Int = 0,
        val w: Float = 1f,
        val shiftLabel: String? = null,
        val mod: Int = 0,
        val fn: Boolean = false,
        val fnCode: Int = 0,
        val fnLabel: String? = null,
        val small: Boolean = false,
        val h: Float = 1f,
        /** A mouse button (TrackPoint buttons): its HID button bit. */
        val mouse: Int = 0,
    ) {
        val rect = RectF()
        var gx = 0f
        var gy = 0f
        val isModifier get() = mod != 0 || fn
    }

    private enum class ModState { OFF, ONESHOT, LOCKED }

    private val density = resources.displayMetrics.density
    private var keys: List<Key> = emptyList()
    private var gridW = 15f
    private var gridH = 6f
    private var os: HostOs? = null
    private var layout: KbLayout? = null
    private var splitGap = 2f

    private val pointerKeys = HashMap<Int, Key>()
    private val heldCodes = LinkedHashMap<Key, Int>()   // key -> code actually sent (fn-resolved)
    private val modState = HashMap<Key, ModState>()
    private val modHeld = HashMap<Key, Boolean>()        // finger currently on the modifier
    private val modUsed = HashSet<Key>()                 // another key was pressed while holding it

    /**
     * A character key (with [flick]) or the space bar (with [spaceCursor]) that hasn't typed yet: it types
     * when lifted, shifted if flicked up; held still it turns into a normal held key; dragged, the space
     * bar moves the cursor instead.
     */
    private inner class Pending(val k: Key, val x0: Float, val y0: Float) {
        var flicked = false
        var cursor = false
        var lastX = x0
        var lastY = y0
        val hold = Runnable { holdTimeout(this) }
    }
    private val pending = HashMap<Int, Pending>()
    private val mouseDown = HashSet<Key>()
    private var unitW = 1f
    private var unitH = 1f

    // TrackPoint (ThinkPad layout): a pointing stick between G, H and B. Push it and the pointer moves at a
    // speed set by how far you push; hold the middle button to scroll with it instead.
    private var nubGx = -1f
    private var nubGy = -1f
    private val nubC = PointF()
    private var nubR = 0f
    private var nubId = -1
    private val nubOrigin = PointF()
    private val nubAt = PointF()
    private var nubDown = 0L
    private var nubMax = 0f
    private var nubLast = 0L
    private var middleHeld = false
    private var middleScrolled = false
    private val nubPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nubLoop = object : Runnable {
        override fun run() {
            if (nubId < 0) return
            nubStep()
            postOnAnimation(this)
        }
    }

    private val deckRect = android.graphics.RectF()

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = C.TEXT }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = C.MUTED }
    private val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbleText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val bubble = RectF()

    /** Show an enlarged label above the pressed key (fingers hide small keys). */
    var keyPreview = true

    init { isHapticFeedbackEnabled = false }

    /** Flick up on a key to type its shifted character. Keys with a flick type when lifted (or when held). */
    var flick = true
    /** Drag on the space bar to send arrow keys. */
    var spaceCursor = true
    /** A TrackPoint between G, H and B on the Laptop and Full layouts too (ThinkPad always has one). */
    var trackPoint = true
        set(v) {
            if (field == v) return
            field = v
            val o = os; val l = layout
            if (o != null && l != null) { releaseEverything(); build(o, l); layoutKeys(); invalidate() }
        }

    fun setOs(o: HostOs) = configure(o, layout ?: KbLayout.LAPTOP, splitGap)

    fun configure(o: HostOs, l: KbLayout, gap: Float = splitGap) {
        if (o == os && l == layout && gap == splitGap) return
        releaseEverything()
        os = o; layout = l; splitGap = gap
        build(o, l)
        layoutKeys(); invalidate()
    }

    // ------------------------------------------------------------------ layouts

    private class Grid {
        val keys = ArrayList<Key>()
        var w = 0f
        var h = 0f
        fun row(y: Float, x0: Float, list: List<Key>): Grid {
            var x = x0
            for (k in list) { put(k, x, y); x += k.w }
            return this
        }
        fun rowEndingAt(y: Float, xEnd: Float, list: List<Key>) = row(y, xEnd - list.sumOf { it.w.toDouble() }.toFloat(), list)
        fun put(k: Key, x: Float, y: Float) {
            k.gx = x; k.gy = y; keys += k
            w = max(w, x + k.w); h = max(h, y + k.h)
        }
    }

    private fun build(o: HostOs, l: KbLayout) {
        val mac = o == HostOs.MAC
        nubGx = -1f; nubGy = -1f
        fun lt(c: Char, w: Float = 1f) = Key(c.toString(), Keys.letter(c), w, c.uppercase())
        fun s(lbl: String, shift: String, code: Int, w: Float = 1f, fnCode: Int = 0, fnLabel: String? = null) =
            Key(lbl, code, w, shift, fnCode = fnCode, fnLabel = fnLabel)
        fun sm(lbl: String, code: Int, w: Float = 1f, fnCode: Int = 0, fnLabel: String? = null, h: Float = 1f) =
            Key(lbl, code, w, small = true, fnCode = fnCode, fnLabel = fnLabel, h = h)
        val digits = "1234567890"
        val shifted = "!@#$%^&*()"
        fun digit(i: Int) = s(digits[i].toString(), shifted[i].toString(), Keys.digit(digits[i]), fnCode = Keys.f(i + 1), fnLabel = "F${i + 1}")
        fun minus() = s("-", "_", Keys.MINUS, fnCode = Keys.f(11), fnLabel = "F11")
        fun equal() = s("=", "+", Keys.EQUAL, fnCode = Keys.f(12), fnLabel = "F12")
        fun bksp(w: Float = 2f) = Key(if (mac) "delete" else "⌫", Keys.BACKSPACE, w, small = mac, fnCode = Keys.DELETE, fnLabel = "Del")
        fun tab(w: Float = 1.5f) = sm(if (mac) "tab" else "Tab", Keys.TAB, w)
        fun caps(w: Float = 1.75f) = sm(if (mac) "caps" else "Caps", Keys.CAPS, w)
        fun enter(w: Float = 2.25f, h: Float = 1f) = sm(if (mac) "return" else "Enter", Keys.ENTER, w, h = h)
        fun lshift(w: Float = 2.25f) = Key(if (mac) "shift" else "Shift", mod = L_SHIFT, w = w, small = true)
        fun rshift(w: Float = 2.75f) = Key(if (mac) "shift" else "Shift", mod = R_SHIFT, w = w, small = true)
        fun esc(w: Float = 1f, grave: Boolean = false) =
            if (grave) Key(if (mac) "esc" else "Esc", Keys.ESC, w, small = true, fnCode = Keys.GRAVE, fnLabel = "`")
            else sm(if (mac) "esc" else "Esc", Keys.ESC, w)
        fun left() = Key("←", Keys.LEFT, fnCode = Keys.HOME, fnLabel = "Home")
        fun up() = Key("↑", Keys.UP, fnCode = Keys.PAGE_UP, fnLabel = "PgUp")
        fun down() = Key("↓", Keys.DOWN, fnCode = Keys.PAGE_DOWN, fnLabel = "PgDn")
        fun right() = Key("→", Keys.RIGHT, fnCode = Keys.END, fnLabel = "End")
        fun space(w: Float) = Key(" ", Keys.SPACE, w)
        fun ctrl(right: Boolean = false, w: Float = 1.25f) =
            Key(if (mac) "⌃" else "Ctrl", mod = if (right) R_CTRL else L_CTRL, w = w, small = !mac)
        fun gui(right: Boolean = false, w: Float = 1.25f) = Key(
            when (o) { HostOs.MAC -> "⌘"; HostOs.LINUX -> "Super"; else -> "Win" },
            mod = if (right) R_GUI else L_GUI, w = w, small = !mac,
        )
        fun alt(right: Boolean = false, w: Float = 1.25f) = Key(
            if (mac) "⌥" else if (right && o == HostOs.WINDOWS) "AltGr" else "Alt",
            mod = if (right) R_ALT else L_ALT, w = w, small = !mac,
        )
        fun fnKey(w: Float = 1f) = Key(if (mac) "fn" else "Fn", fn = true, w = w, small = true)
        fun letters(str: String) = str.map { lt(it) }
        fun kp(lbl: String, code: Int, w: Float = 1f, h: Float = 1f) = Key(lbl, code, w, h = h)

        // Numpad block (4 wide x 5 high) placed with its top-left at (x, y).
        fun numpad(g: Grid, x: Float, y: Float) {
            g.row(y, x, listOf(
                sm(if (mac) "clear" else "Num", 0x53), kp("/", 0x54), kp("*", 0x55), kp("−", 0x56),
            ))
            g.row(y + 1, x, listOf(kp("7", 0x5F), kp("8", 0x60), kp("9", 0x61)))
            g.put(kp("+", 0x57, h = 2f), x + 3, y + 1)
            g.row(y + 2, x, listOf(kp("4", 0x5C), kp("5", 0x5D), kp("6", 0x5E)))
            g.row(y + 3, x, listOf(kp("1", 0x59), kp("2", 0x5A), kp("3", 0x5B)))
            g.put(sm(if (mac) "enter" else "Enter", 0x58, h = 2f), x + 3, y + 3)
            g.row(y + 4, x, listOf(kp("0", 0x62, w = 2f), kp(".", 0x63)))
        }

        val g = Grid()
        when (l) {
            KbLayout.PHONE -> {
                // Ten columns like a phone keyboard, so letters are big enough to hit. Modifiers and arrows sit in
                // a slim top row; Fn turns the number row into F1–F10 and the top letter row into the symbols a
                // phone keyboard hides (shift still applies: Fn + Shift + - types _).
                fun lf(c: Char, code: Int, label: String) = Key(c.toString(), Keys.letter(c), 1f, c.uppercase(), fnCode = code, fnLabel = label)
                g.row(0f, 0f, listOf(esc(), tab(1f), ctrl(w = 1f), alt(w = 1f), gui(w = 1f), left(), up(), down(), right(),
                    sm(if (mac) "⌦" else "Del", Keys.DELETE, fnCode = Keys.INSERT, fnLabel = "Ins")))
                g.row(1f, 0f, (0..9).map { digit(it) })
                g.row(2f, 0f, listOf(
                    lf('q', Keys.GRAVE, "`"), lf('w', Keys.MINUS, "-"), lf('e', Keys.EQUAL, "="),
                    lf('r', Keys.LBRACKET, "["), lf('t', Keys.RBRACKET, "]"), lf('y', Keys.BACKSLASH, "\\"),
                    lf('u', Keys.SEMICOLON, ";"), lf('i', Keys.QUOTE, "'"), lf('o', Keys.f(11), "F11"), lf('p', Keys.f(12), "F12"),
                ))
                g.row(3f, 0.5f, listOf(
                    lf('a', Keys.HOME, "Home"), lf('s', Keys.PAGE_UP, "PgUp"), lf('d', Keys.PAGE_DOWN, "PgDn"), lf('f', Keys.END, "End"),
                ) + letters("ghjkl"))
                g.row(4f, 0f, listOf(lshift(1.5f)) + letters("zxcvbnm") + bksp(1.5f))
                g.row(5f, 0f, listOf(fnKey(1.5f), s(",", "<", Keys.COMMA), space(4f), s(".", ">", Keys.PERIOD),
                    s("/", "?", Keys.SLASH), enter(1.5f)))
            }
            KbLayout.LAPTOP -> {
                // Like Windows' "traditional" touch keyboard: no separate F-row, so five taller rows.
                g.row(0f, 0f, listOf(esc(grave = true)) + (0..9).map { digit(it) } + minus() + equal() + bksp())
                g.row(1f, 0f, listOf(tab()) + letters("qwertyuiop") +
                    s("[", "{", Keys.LBRACKET) + s("]", "}", Keys.RBRACKET) + s("\\", "|", Keys.BACKSLASH, 1.5f))
                g.row(2f, 0f, listOf(caps()) + letters("asdfghjkl") + s(";", ":", Keys.SEMICOLON) + s("'", "\"", Keys.QUOTE) + enter())
                g.row(3f, 0f, listOf(lshift()) + letters("zxcvbnm") +
                    s(",", "<", Keys.COMMA) + s(".", ">", Keys.PERIOD) + s("/", "?", Keys.SLASH) + rshift())
                val arrows = listOf(left(), up(), down(), right())
                g.row(4f, 0f, if (mac) listOf(fnKey(), ctrl(), alt(), gui(w = 1.5f), space(3.5f), gui(true, 1.5f), alt(true, 1f)) + arrows
                else listOf(ctrl(), gui(), alt(), space(4.25f), alt(true, 1f), fnKey(), ctrl(true, 1f)) + arrows)
                if (trackPoint) { nubGx = 6.75f; nubGy = 3f }
            }
            KbLayout.THINKPAD -> {
                // Low F-row with Home/End/Ins/Del, Fn bottom-left, PrtSc between Alt and Ctrl, half-height arrows
                // with PgUp/PgDn beside Up, a red TrackPoint between G, H and B and its three buttons below.
                val fw = 15f / 17f
                val fh = 0.7f
                fun f(lbl: String, code: Int) = Key(lbl, code, fw, small = true, h = fh)
                g.row(0f, 0f, listOf(f(if (mac) "esc" else "Esc", Keys.ESC)) + (1..12).map { f("F$it", Keys.f(it)) } +
                    f("Home", Keys.HOME) + f("End", Keys.END) + f("Ins", Keys.INSERT) + f(if (mac) "⌦" else "Del", Keys.DELETE))
                val y = fh
                g.row(y, 0f, listOf(s("`", "~", Keys.GRAVE)) + (0..9).map { digit(it) } + minus() + equal() + bksp())
                g.row(y + 1, 0f, listOf(tab()) + letters("qwertyuiop") +
                    s("[", "{", Keys.LBRACKET) + s("]", "}", Keys.RBRACKET) + s("\\", "|", Keys.BACKSLASH, 1.5f))
                g.row(y + 2, 0f, listOf(caps()) + letters("asdfghjkl") + s(";", ":", Keys.SEMICOLON) + s("'", "\"", Keys.QUOTE) + enter())
                g.row(y + 3, 0f, listOf(lshift()) + letters("zxcvbnm") +
                    s(",", "<", Keys.COMMA) + s(".", ">", Keys.PERIOD) + s("/", "?", Keys.SLASH) + rshift())
                g.row(y + 4, 0f, listOf(fnKey(), ctrl(w = 1.25f), gui(w = 1f), alt(w = 1.25f), space(5f), alt(true, 1f),
                    sm("PrtSc", Keys.PRINT_SCREEN), ctrl(true, 1f)))
                val aw = 2.5f / 3f
                g.row(y + 4, 12.5f, listOf(
                    Key("PgUp", Keys.PAGE_UP, aw, small = true, h = 0.5f),
                    Key("↑", Keys.UP, aw, h = 0.5f, fnCode = Keys.PAGE_UP, fnLabel = "PgUp"),
                    Key("PgDn", Keys.PAGE_DOWN, aw, small = true, h = 0.5f),
                ))
                g.row(y + 4.5f, 12.5f, listOf(
                    Key("←", Keys.LEFT, aw, h = 0.5f, fnCode = Keys.HOME, fnLabel = "Home"),
                    Key("↓", Keys.DOWN, aw, h = 0.5f, fnCode = Keys.PAGE_DOWN, fnLabel = "PgDn"),
                    Key("→", Keys.RIGHT, aw, h = 0.5f, fnCode = Keys.END, fnLabel = "End"),
                ))
                // TrackPoint buttons centred under the stick.
                g.row(y + 5.1f, 4.75f, listOf(
                    Key("", w = 1.5f, h = 0.55f, small = true, mouse = TrackpadView.BTN_LEFT),
                    Key("", w = 1f, h = 0.55f, small = true, mouse = TrackpadView.BTN_MIDDLE),
                    Key("", w = 1.5f, h = 0.55f, small = true, mouse = TrackpadView.BTN_RIGHT),
                ))
                nubGx = 6.75f; nubGy = y + 3
            }
            KbLayout.FULL, KbLayout.FULL_NUMPAD -> {
                g.row(0f, 0f, listOf(esc()) + (1..12).map { sm("F$it", Keys.f(it)) } +
                    sm(if (mac) "⌦" else "Del", Keys.DELETE, w = 2f, fnCode = Keys.INSERT, fnLabel = "Ins"))
                g.row(1f, 0f, listOf(s("`", "~", Keys.GRAVE)) + (0..9).map { digit(it) } + minus() + equal() + bksp())
                g.row(2f, 0f, listOf(tab()) + letters("qwertyuiop") +
                    s("[", "{", Keys.LBRACKET) + s("]", "}", Keys.RBRACKET) + s("\\", "|", Keys.BACKSLASH, 1.5f))
                g.row(3f, 0f, listOf(caps()) + letters("asdfghjkl") + s(";", ":", Keys.SEMICOLON) + s("'", "\"", Keys.QUOTE) + enter())
                g.row(4f, 0f, listOf(lshift()) + letters("zxcvbnm") +
                    s(",", "<", Keys.COMMA) + s(".", ">", Keys.PERIOD) + s("/", "?", Keys.SLASH) + rshift())
                val arrows = listOf(left(), up(), down(), right())
                g.row(5f, 0f, if (mac) listOf(fnKey(), ctrl(), alt(), gui(w = 1.5f), space(3.5f), gui(true, 1.5f), alt(true, 1f)) + arrows
                else listOf(ctrl(), gui(), alt(), space(4.25f), alt(true, 1f), fnKey(), ctrl(true, 1f)) + arrows)
                if (trackPoint && l == KbLayout.FULL) { nubGx = 6.75f; nubGy = 4f }
                if (l == KbLayout.FULL_NUMPAD) {
                    val x = 15.5f
                    g.row(0f, x, listOf(sm("PrtSc", Keys.PRINT_SCREEN), sm("Ins", Keys.INSERT), sm("PgUp", Keys.PAGE_UP), sm("PgDn", Keys.PAGE_DOWN)))
                    numpad(g, x, 1f)
                }
            }
            KbLayout.COMPACT_65, KbLayout.SPLIT -> {
                // Rows of a 65% board, each as (left half, right half) so Split can pull them apart.
                val rows = listOf(
                    listOf(esc(grave = true)) + (0..5).map { digit(it) } to
                        (6..9).map { digit(it) } + minus() + equal() + bksp() + sm(if (mac) "⌦" else "Del", Keys.DELETE),
                    listOf(tab()) + letters("qwert") to
                        letters("yuiop") + s("[", "{", Keys.LBRACKET) + s("]", "}", Keys.RBRACKET) +
                        s("\\", "|", Keys.BACKSLASH, 1.5f) + sm("PgUp", Keys.PAGE_UP),
                    listOf(caps()) + letters("asdfg") to
                        letters("hjkl") + s(";", ":", Keys.SEMICOLON) + s("'", "\"", Keys.QUOTE) + enter() + sm("PgDn", Keys.PAGE_DOWN),
                    listOf(lshift()) + letters("zxcvb") to
                        letters("nm") + s(",", "<", Keys.COMMA) + s(".", ">", Keys.PERIOD) + s("/", "?", Keys.SLASH) +
                        rshift(1.75f) + up() + sm("End", Keys.END),
                    (if (mac) listOf(fnKey(), ctrl(), alt(), gui(w = 1.5f)) else listOf(ctrl(), gui(), alt())) to
                        (if (mac) listOf(gui(true, 1.5f), alt(true, 1f)) else listOf(alt(true, 1f), fnKey(), ctrl(true, 1f))) +
                        left() + down() + right(),
                )
                if (l == KbLayout.COMPACT_65) {
                    rows.forEachIndexed { i, (a, b) ->
                        val bottom = i == rows.lastIndex
                        val leftW = a.sumOf { it.w.toDouble() }.toFloat()
                        val rightW = b.sumOf { it.w.toDouble() }.toFloat()
                        g.row(i.toFloat(), 0f, if (bottom) a + space(16f - leftW - rightW) + b else a + b)
                    }
                } else {
                    val leftMax = 7.25f
                    val rightMax = 9.5f
                    val total = leftMax + splitGap + rightMax
                    rows.forEachIndexed { i, (a, b) ->
                        val bottom = i == rows.lastIndex
                        val aa = if (bottom) a + space(leftMax - a.sumOf { it.w.toDouble() }.toFloat()) else a
                        val bb = if (bottom) listOf(space(rightMax - b.sumOf { it.w.toDouble() }.toFloat())) + b else b
                        g.row(i.toFloat(), 0f, aa)
                        g.rowEndingAt(i.toFloat(), total, bb)
                    }
                }
            }
            KbLayout.GAMING -> {
                // Left hand: big WASD block. Right hand: function, action and arrow keys.
                g.row(0f, 0f, listOf(esc(grave = true)) + (0..5).map { digit(it) })
                g.row(1f, 0f, listOf(tab()) + letters("qwert"))
                g.row(2f, 0f, listOf(caps()) + letters("asdfg"))
                g.row(3f, 0f, listOf(lshift()) + letters("zxcvb"))
                g.row(4f, 0f, listOf(ctrl(), alt(), space(4.75f)))
                val x = 8.75f
                g.row(0f, x, listOf(sm("F5", Keys.f(5)), sm("F9", Keys.f(9)), bksp(1f)))
                g.row(1f, x, listOf(lt('i'), lt('m'), enter(1f)))
                g.row(2f, x, listOf(lt('j'), lt('k'), lt('l')))
                g.put(up(), x + 1, 3f)
                g.row(4f, x, listOf(left(), down(), right()))
            }
            KbLayout.NUMPAD -> {
                g.row(0f, 0f, listOf(esc()))
                g.row(1f, 0f, listOf(tab(1f)))
                g.row(2f, 0f, listOf(bksp(1f)))
                g.row(3f, 0f, listOf(sm(if (mac) "⌦" else "Del", Keys.DELETE)))
                g.row(4f, 0f, listOf(s("=", "+", Keys.EQUAL)))
                numpad(g, 1.25f, 0f)
            }
        }
        keys = g.keys
        gridW = g.w
        gridH = g.h
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = layoutKeys()

    private fun layoutKeys() {
        if (width == 0 || keys.isEmpty()) return
        val gap = 4 * density
        unitW = (width - gap) / gridW
        unitH = (height - gap) / gridH
        for (k in keys) {
            k.rect.set(
                gap + k.gx * unitW, gap + k.gy * unitH,
                (k.gx + k.w) * unitW, (k.gy + k.h) * unitH,
            )
        }
        if (nubGx >= 0) {
            nubC.set(gap / 2 + nubGx * unitW, gap / 2 + nubGy * unitH)
            nubR = minOf(unitW, unitH) * 0.2f
        }
        val ref = minOf(unitH, unitW * 1.1f)
        labelPaint.textSize = (ref * 0.38f).coerceAtMost(22 * density)
        subPaint.textSize = (ref * 0.27f).coerceAtMost(14 * density)
        bubbleText.textSize = (ref * 0.6f).coerceAtMost(34 * density)
    }

    // ------------------------------------------------------------------ state

    private val fnActive get() = modState.entries.any { it.key.fn && it.value != ModState.OFF } ||
        modHeld.entries.any { it.key.fn && it.value }

    private fun activeMods(): Int {
        var m = 0
        for (k in keys) if (k.mod != 0) {
            if ((modState[k] ?: ModState.OFF) != ModState.OFF || modHeld[k] == true) m = m or k.mod
        }
        return m
    }

    private fun push() = HidManager.setKeyboardState(activeMods(), heldCodes.values)

    private fun keyAt(x: Float, y: Float): Key? {
        for (k in keys) if (k.rect.contains(x, y)) return k
        // Nearest key within half a key, so gaps never swallow a touch but empty areas (split gap) do.
        var best: Key? = null; var bestD = Float.MAX_VALUE
        for (k in keys) {
            val dx = max(0f, max(k.rect.left - x, x - k.rect.right))
            val dy = max(0f, max(k.rect.top - y, y - k.rect.bottom))
            val d = dx * dx + dy * dy
            if (d < bestD) { bestD = d; best = k }
        }
        val limit = 12 * density
        return if (bestD <= limit * limit) best else null
    }

    private fun press(k: Key) {
        if (k.isModifier) {
            modHeld[k] = true
            modUsed.remove(k)
        } else {
            val code = if (fnActive && k.fnCode != 0) k.fnCode else k.code
            if (code == 0) return
            heldCodes[k] = code
            modHeld.filterValues { it }.keys.forEach { modUsed.add(it) }
            Typing.keyPressed()
        }
        Haptics.tick()
        push()
    }

    private fun release(k: Key) {
        if (k.isModifier) {
            modHeld[k] = false
            val prev = modState[k] ?: ModState.OFF
            modState[k] = if (modUsed.remove(k)) ModState.OFF else when (prev) {
                ModState.OFF -> ModState.ONESHOT
                ModState.ONESHOT -> ModState.LOCKED
                ModState.LOCKED -> ModState.OFF
            }
            push()
        } else {
            if (heldCodes.remove(k) == null) return
            push()
            if (heldCodes.isEmpty()) {
                var changed = false
                for (e in modState.entries) if (e.value == ModState.ONESHOT) { e.setValue(ModState.OFF); changed = true }
                if (changed) push()
            }
        }
    }

    fun releaseEverything() {
        pending.values.forEach { removeCallbacks(it.hold) }; pending.clear()
        if (nubId >= 0) { nubId = -1; removeCallbacks(nubLoop) }
        if (middleHeld) middleHeld = false
        for (k in pointerKeys.values) if (k.mouse != 0 && k.mouse != TrackpadView.BTN_MIDDLE) HidManager.setButton(k.mouse, false)
        mouseDown.clear(); pointerKeys.clear(); heldCodes.clear(); modState.clear(); modHeld.clear(); modUsed.clear()
        HidManager.setKeyboardState(0, emptyList())
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean = TouchRouter.route(this, e)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        TouchRouter.register(this)
    }

    /** Flick and the space-bar cursor are off where held keys matter more than typing (games, numpad). */
    private val typingLayout get() = layout != KbLayout.GAMING && layout != KbLayout.NUMPAD

    private fun defers(k: Key) = typingLayout && k.mouse == 0 && !k.isModifier &&
        ((k.code == Keys.SPACE && spaceCursor) || (flick && k.code != Keys.SPACE && k.shiftLabel != null))

    private fun nubHit(x: Float, y: Float) = nubGx >= 0 && hypot(x - nubC.x, y - nubC.y) <= nubR * 2.2f

    override fun handleTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val i = e.actionIndex
                val pid = e.getPointerId(i)
                // Compose passes the whole MotionEvent: a finger that went down on the trackpad isn't ours.
                val x = e.getX(i); val y = e.getY(i)
                if (x < 0f || y < 0f || x > width || y > height) return true
                // The heel of a hand resting on the keys doesn't type.
                if (Settings.palmRejection && Typing.isPalm(e, i, resources.displayMetrics.ydpi)) return true
                if (nubId < 0 && nubHit(x, y)) {
                    nubId = pid; nubOrigin.set(x, y); nubAt.set(x, y); nubDown = e.eventTime; nubMax = 0f
                    nubLast = System.nanoTime()
                    HidManager.resetScroll()
                    postOnAnimation(nubLoop)
                    invalidate(); return true
                }
                val k = keyAt(x, y) ?: return true
                if (k.mouse != 0) {
                    pointerKeys[pid] = k
                    if (k.mouse == TrackpadView.BTN_MIDDLE) { middleHeld = true; middleScrolled = false }
                    else HidManager.setButton(k.mouse, true)
                    mouseDown += k
                    Haptics.tick(); invalidate(); return true
                }
                // A new key types whatever is still waiting first, so fast rollover keeps its order.
                if (!k.isModifier) flushPending()
                if (defers(k)) {
                    val p = Pending(k, x, y)
                    pending[pid] = p
                    if (k.code != Keys.SPACE) postDelayed(p.hold, HOLD_MS)
                    Typing.keyPressed()
                    Haptics.tick()
                } else {
                    pointerKeys[pid] = k
                    press(k)
                }
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) {
                val pid = e.getPointerId(i)
                if (pid == nubId) { nubAt.set(e.getX(i), e.getY(i)); continue }
                val p = pending[pid] ?: continue
                movePending(p, e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pid = e.getPointerId(e.actionIndex)
                if (pid == nubId) nubUp(e.eventTime)
                pending.remove(pid)?.let { commit(it) }
                pointerKeys.remove(pid)?.let { k -> if (k.mouse != 0) mouseUp(k) else release(k) }
            }
            MotionEvent.ACTION_CANCEL -> {
                pending.values.forEach { removeCallbacks(it.hold) }; pending.clear()
                if (nubId >= 0) { nubId = -1; removeCallbacks(nubLoop) }
                pointerKeys.values.toList().forEach { if (it.mouse != 0) mouseUp(it) else release(it) }; pointerKeys.clear()
            }
        }
        invalidate()
        return true
    }

    private fun movePending(p: Pending, x: Float, y: Float) {
        if (p.k.code == Keys.SPACE) {
            // Space bar cursor: once the finger has slid, every step sends an arrow key.
            val step = max(14 * density, unitW * 0.5f)
            if (!p.cursor && (abs(x - p.x0) > step * 0.8f || abs(y - p.y0) > step * 1.2f)) {
                p.cursor = true; p.lastX = p.x0; p.lastY = p.y0
            }
            if (!p.cursor) return
            while (x - p.lastX >= step) { p.lastX += step; arrow(Keys.RIGHT) }
            while (p.lastX - x >= step) { p.lastX -= step; arrow(Keys.LEFT) }
            val vstep = step * 1.6f
            while (y - p.lastY >= vstep) { p.lastY += vstep; arrow(Keys.DOWN) }
            while (p.lastY - y >= vstep) { p.lastY -= vstep; arrow(Keys.UP) }
        } else {
            val up = p.y0 - y
            p.flicked = up > max(16 * density, unitH * 0.45f) && up > abs(x - p.x0)
        }
    }

    private fun arrow(code: Int) {
        tapCode(code, 0)
    }

    /** Held still: becomes an ordinary held key, so the computer's key repeat works. */
    private fun holdTimeout(p: Pending) {
        val pid = pending.entries.firstOrNull { it.value === p }?.key ?: return
        if (p.flicked) return
        pending.remove(pid)
        pointerKeys[pid] = p.k
        press(p.k)
        invalidate()
    }

    private fun commit(p: Pending) {
        removeCallbacks(p.hold)
        if (p.cursor) return
        val code = if (fnActive && p.k.fnCode != 0) p.k.fnCode else p.k.code
        if (code == 0) return
        tapCode(code, if (p.flicked) L_SHIFT else 0)
    }

    private fun flushPending() {
        val it = pending.entries.iterator()
        while (it.hasNext()) {
            val p = it.next().value
            if (p.cursor) continue
            it.remove(); commit(p)
        }
    }

    /** Press and release [code] with the current modifiers plus [extra]; one-shot modifiers are used up. */
    private fun tapCode(code: Int, extra: Int) {
        modHeld.filterValues { it }.keys.forEach { modUsed.add(it) }
        push()
        HidManager.tap(code, extra)
        Typing.keyPressed()
        if (heldCodes.isEmpty()) {
            var changed = false
            for (e in modState.entries) if (e.value == ModState.ONESHOT) { e.setValue(ModState.OFF); changed = true }
            if (changed) push()
        }
        invalidate()
    }

    private fun mouseUp(k: Key) {
        mouseDown.remove(k)
        if (k.mouse == TrackpadView.BTN_MIDDLE) {
            middleHeld = false
            if (!middleScrolled) HidManager.click(TrackpadView.BTN_MIDDLE)
        } else HidManager.setButton(k.mouse, false)
    }

    /** One frame of TrackPoint motion: speed grows faster than linearly with how far the finger pushed. */
    private fun nubStep() {
        val now = System.nanoTime()
        val dt = ((now - nubLast) / 1e9f).coerceIn(0f, 0.05f)
        nubLast = now
        val dx = nubAt.x - nubOrigin.x; val dy = nubAt.y - nubOrigin.y
        val d = hypot(dx, dy) / density
        nubMax = max(nubMax, d)
        val push = d - NUB_DEAD_DP
        if (push <= 0f || d == 0f) return
        // A modifier held while using the stick was used (Fn to scroll, Shift-select): it doesn't turn one-shot.
        modHeld.filterValues { it }.keys.forEach { modUsed.add(it) }
        // Scroll with the middle TrackPoint button held, or Fn on layouts without the buttons.
        if (middleHeld || modHeld.entries.any { it.key.fn && it.value }) {
            middleScrolled = true
            val rate = 0.35f * push.pow(1.3f) * Settings.trackPointSpeed // notches per second
            HidManager.scroll(-dy / (d * density) * rate * dt, dx / (d * density) * rate * dt)
        } else {
            val speed = 16f * push.pow(1.5f) * Settings.pointerSpeed * Settings.trackPointSpeed // counts per second
            HidManager.move(dx / (d * density) * speed * dt, dy / (d * density) * speed * dt)
        }
    }

    private fun nubUp(t: Long) {
        nubId = -1
        removeCallbacks(nubLoop)
        // A quick tap on the stick clicks, like ThinkPad's press-to-select.
        if (t - nubDown < 180 && nubMax < 4f && !middleHeld && !middleScrolled) HidManager.click(TrackpadView.BTN_LEFT)
        if (!middleHeld) middleScrolled = false
    }

    override fun onDetachedFromWindow() {
        TouchRouter.unregister(this)
        releaseEverything()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val HOLD_MS = 380L
        const val NUB_DEAD_DP = 1.5f
        const val NUB_RED = 0xFFE2231A.toInt()
    }

    // ------------------------------------------------------------------ drawing

    private fun labelFor(k: Key, shift: Boolean, caps: Boolean, fn: Boolean): String {
        val isLetter = k.label.length == 1 && k.label[0].isLetter()
        return when {
            fn && k.fnLabel != null -> k.fnLabel
            isLetter && (shift xor caps) -> k.shiftLabel ?: k.label
            !isLetter && shift && k.shiftLabel != null -> k.shiftLabel
            else -> k.label
        }
    }

    override fun onDraw(c: Canvas) {
        val r = 9 * density
        val shift = activeMods() and (L_SHIFT or R_SHIFT) != 0
        val caps = HidManager.leds and 0x02 != 0
        val fn = fnActive
        val real = Real.active
        // Realistic: the keys sit in an aluminum (or space black) deck.
        if (real) { deckRect.set(0f, 0f, width.toFloat(), height.toFloat()); Real.deck(c, deckRect, 14 * density, density) }
        for (k in keys) {
            val pressed = heldCodes.containsKey(k) || modHeld[k] == true || pending.values.any { it.k === k } || k in mouseDown
            val st = modState[k] ?: ModState.OFF
            val lit = pressed || st != ModState.OFF
            if (real) {
                Real.key(c, k.rect, r * 0.75f, density, pressed, k.isModifier || k.small)
                val label = labelFor(k, shift, caps, fn)
                val paint = if (k.small || label.length > 2) subPaint else labelPaint
                paint.color = if (k.small) C.MUTED else C.TEXT
                val cy = Real.capCenterY(k.rect, density, pressed)
                c.drawText(label, k.rect.centerX(), cy - (paint.ascent() + paint.descent()) / 2, paint)
                val isLetter = k.label.length == 1 && k.label[0].isLetter()
                if (!isLetter && k.shiftLabel != null && !shift && !fn && k.rect.height() > 34 * density) {
                    subPaint.color = C.MUTED
                    c.drawText(k.shiftLabel, k.rect.centerX(), k.rect.top + subPaint.textSize + 2 * density, subPaint)
                }
                if (k.mouse != 0 && k.mouse != TrackpadView.BTN_MIDDLE) {
                    nubPaint.color = NUB_RED
                    c.drawRect(k.rect.left + 6 * density, k.rect.top + 3 * density, k.rect.right - 6 * density,
                        k.rect.top + 5 * density, nubPaint)
                }
                // Status lights: Caps Lock, and latched (locked / one-shot) modifiers.
                if (k.code == Keys.CAPS) Real.led(c, k.rect.left + 8 * density, k.rect.top + 8 * density, density, caps)
                else if (st != ModState.OFF) Real.led(c, k.rect.left + 8 * density, k.rect.top + 8 * density, density, true, st == ModState.ONESHOT)
                continue
            }
            keyPaint.fillOrOutline(
                when {
                    pressed -> C.ACCENT
                    st == ModState.LOCKED -> C.ACCENT
                    st == ModState.ONESHOT -> C.ACCENT_DIM
                    k.isModifier || k.small -> C.SURFACE2
                    else -> C.SURFACE3
                },
                density, lit,
            )
            val kr = if (pressed) r * 2.2f else r
            c.drawRoundRect(k.rect, kr, kr, keyPaint)

            val isLetter = k.label.length == 1 && k.label[0].isLetter()
            val label = labelFor(k, shift, caps, fn)
            val paint = if (k.small || label.length > 2) subPaint else labelPaint
            paint.color = when {
                C.AOD -> if (lit) C.ACCENT else if (k.small) C.MUTED else C.TEXT
                pressed || st == ModState.LOCKED -> C.ON_ACCENT
                st == ModState.ONESHOT -> C.ON_ACCENT_DIM
                k.small -> C.MUTED
                else -> C.TEXT
            }
            c.drawText(label, k.rect.centerX(), k.rect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)

            if (!isLetter && k.shiftLabel != null && !shift && !fn && k.rect.height() > 34 * density) {
                subPaint.color = C.MUTED
                c.drawText(k.shiftLabel, k.rect.centerX(), k.rect.top + subPaint.textSize + 2 * density, subPaint)
            }
            if (k.mouse != 0) {
                // TrackPoint buttons: ThinkPad's red stripes on left and right.
                if (k.mouse != TrackpadView.BTN_MIDDLE) {
                    nubPaint.color = NUB_RED
                    c.drawRect(k.rect.left + 6 * density, k.rect.top + 3 * density, k.rect.right - 6 * density,
                        k.rect.top + 5 * density, nubPaint)
                }
            }
            if (k.code == Keys.CAPS) {
                ledPaint.color = if (caps) C.OK else C.STROKE
                c.drawCircle(k.rect.left + 8 * density, k.rect.top + 8 * density, 3 * density, ledPaint)
            }
        }

        // TrackPoint, drawn over the corners of G, H and B; it leans the way it's pushed.
        if (nubGx >= 0) {
            val lean = if (nubId >= 0) min(1f, hypot(nubAt.x - nubOrigin.x, nubAt.y - nubOrigin.y) / (24 * density)) else 0f
            val ang = atan2(nubAt.y - nubOrigin.y, nubAt.x - nubOrigin.x)
            val cx = nubC.x + cos(ang) * lean * nubR * 0.6f
            val cy = nubC.y + sin(ang) * lean * nubR * 0.6f
            nubPaint.color = if (C.AOD) C.BG else C.SURFACE
            c.drawCircle(nubC.x, nubC.y, nubR * 1.35f, nubPaint)
            nubPaint.color = if (C.AOD) C.ACCENT else NUB_RED
            c.drawCircle(cx, cy, nubR * (if (nubId >= 0) 1.1f else 1f), nubPaint)
        }

        // Preview bubbles for held (and waiting) character keys, drawn above the finger.
        val bubbles = heldCodes.keys.map { it to false } + pending.values.filter { !it.cursor }.map { it.k to it.flicked }
        if (keyPreview) for ((k, flicked) in bubbles) {
            if (k.small || k.code == Keys.SPACE || k.mouse != 0) continue
            val label = labelFor(k, shift || flicked, caps, fn)
            val w = max(k.rect.width() * 1.3f, bubbleText.measureText(label) + 24 * density)
            val h = k.rect.height() * 1.15f
            var top = k.rect.top - h - 6 * density
            if (top < 0) top = k.rect.bottom + 6 * density
            val left = (k.rect.centerX() - w / 2).coerceIn(0f, width - w)
            bubble.set(left, top, left + w, top + h)
            bubblePaint.fillOrOutline(C.ACCENT_DIM, density, true)
            c.drawRoundRect(bubble, h / 3, h / 3, bubblePaint)
            bubbleText.color = if (C.AOD) C.ACCENT else C.ON_ACCENT_DIM
            c.drawText(label, bubble.centerX(), bubble.centerY() - (bubbleText.ascent() + bubbleText.descent()) / 2, bubbleText)
        }
    }
}
