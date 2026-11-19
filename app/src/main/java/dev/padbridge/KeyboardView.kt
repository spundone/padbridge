package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
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
import kotlin.math.max

/** On-screen keyboard layouts. */
enum class KbLayout(val label: String, val blurb: String) {
    FULL("Full", "F-row, numbers, arrows"),
    FULL_NUMPAD("Full + numpad", "Everything, with a number pad (landscape / tablets)"),
    COMPACT_65("65%", "No F-row; Del, PgUp, PgDn, End column. Fn + number = F-key"),
    SPLIT("Split", "65% halves at each edge, for two thumbs"),
    GAMING("Gaming", "Big WASD block, arrows and action keys"),
    NUMPAD("Numpad", "Number pad only, for data entry");

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
class KeyboardView(ctx: Context) : View(ctx) {

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

    fun setOs(o: HostOs) = configure(o, layout ?: KbLayout.FULL, splitGap)

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
        val unitW = (width - gap) / gridW
        val unitH = (height - gap) / gridH
        for (k in keys) {
            k.rect.set(
                gap + k.gx * unitW, gap + k.gy * unitH,
                (k.gx + k.w) * unitW, (k.gy + k.h) * unitH,
            )
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
        pointerKeys.clear(); heldCodes.clear(); modState.clear(); modHeld.clear(); modUsed.clear()
        HidManager.setKeyboardState(0, emptyList())
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val i = e.actionIndex
                keyAt(e.getX(i), e.getY(i))?.let { k ->
                    pointerKeys[e.getPointerId(i)] = k
                    press(k)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                pointerKeys.remove(e.getPointerId(e.actionIndex))?.let { release(it) }
            }
            MotionEvent.ACTION_CANCEL -> {
                pointerKeys.values.toList().forEach { release(it) }; pointerKeys.clear()
            }
        }
        invalidate()
        return true
    }

    override fun onDetachedFromWindow() {
        releaseEverything()
        super.onDetachedFromWindow()
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
        for (k in keys) {
            val pressed = heldCodes.containsKey(k) || modHeld[k] == true
            val st = modState[k] ?: ModState.OFF
            val lit = pressed || st != ModState.OFF
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
            if (k.code == Keys.CAPS) {
                ledPaint.color = if (caps) C.OK else C.STROKE
                c.drawCircle(k.rect.left + 8 * density, k.rect.top + 8 * density, 3 * density, ledPaint)
            }
        }

        // Preview bubbles for held character keys, drawn above the finger.
        if (keyPreview) for (k in heldCodes.keys) {
            if (k.small || k.code == Keys.SPACE) continue
            val label = labelFor(k, shift, caps, fn)
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
