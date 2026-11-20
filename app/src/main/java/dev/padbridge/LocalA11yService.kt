package dev.padbridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs

/**
 * Accessibility backend for [Local] (no root): the pointer becomes touch gestures at the cursor.
 *
 * - Left button: a finger that goes down where the cursor is and follows it while held (live drags, via continued
 *   strokes), so tap = click, hold and move = drag or select.
 * - Right button: a long press.
 * - Wheel: short slow swipes at the cursor.
 * - Keys: typed text goes into the focused field; Enter, Backspace, arrows and Ctrl / Cmd + A, C, X, V edit it;
 *   Esc = Back, the Windows / Command key alone = Home, Alt / Cmd + Tab = Recents.
 *
 * Accessibility can't hover or send raw key presses, so games and shortcuts beyond these need root.
 */
class LocalA11yService : AccessibilityService(), Local.Backend {
    override val label = "Accessibility (touch gestures and text)"
    private val main = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
        Local.init(this)
        Local.chooseBackend()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        Local.chooseBackend()
        return super.onUnbind(intent)
    }

    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    // ---------------------------------------------------------------- pointer -> gestures

    private var held = 0
    private var stroke: GestureDescription.StrokeDescription? = null
    private var lastX = 0f
    private var lastY = 0f
    private var busy = false
    private var pendingMove: Pair<Float, Float>? = null
    private var pendingUp = false
    private var display = 0

    override fun pointer(displayId: Int, x: Float, y: Float, buttons: Int) = main.post {
        display = displayId
        val was = held
        held = buttons
        val left = buttons and 1 != 0
        if (buttons and 2 != 0 && was and 2 == 0) longPress(x, y)
        if (left && was and 1 == 0) {
            // Wait a moment: a press that lifts without moving is a plain tap (one simple gesture).
            downAt = x to y
            main.postDelayed(startHold, 120)
        } else if (left) {
            if (downAt != null) { main.removeCallbacks(startHold); startHold.run() }
            pendingMove = x to y; pump()
        } else if (was and 1 != 0) {
            val d = downAt
            if (d != null) {
                main.removeCallbacks(startHold); downAt = null
                dispatch(GestureDescription.StrokeDescription(path(d.first, d.second, d.first, d.second), 0, 40))
            } else { pendingMove = x to y; pendingUp = true; pump() }
        }
    }.let {}

    /** Where the last path ended: a continued stroke must start exactly there. */
    private var endX = 0f
    private var endY = 0f

    private fun path(x0: Float, y0: Float, x1: Float, y1: Float) = Path().apply {
        moveTo(x0, y0)
        // A stroke needs some length; nudge a stationary one by half a pixel (alternating, so it doesn't drift).
        val ey = if (x0 == x1 && y0 == y1) (if (y1 >= 1f) y1 - 0.5f else y1 + 0.5f) else y1
        lineTo(x1, ey)
        endX = x1; endY = ey
    }

    private fun dispatch(s: GestureDescription.StrokeDescription, then: () -> Unit = {}) {
        val b = GestureDescription.Builder().addStroke(s)
        if (Build.VERSION.SDK_INT >= 30) b.setDisplayId(display)
        busy = true
        val ok = dispatchGesture(b.build(), object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { busy = false; then(); pump() }
            override fun onCancelled(g: GestureDescription?) {
                android.util.Log.d(TAG, "gesture cancelled on display $display")
                busy = false; stroke = null; pendingUp = false; pump()
            }
        }, main)
        if (!ok) { android.util.Log.w(TAG, "gesture rejected on display $display"); busy = false; stroke = null }
    }

    private var downAt: Pair<Float, Float>? = null
    private val startHold = Runnable { downAt?.let { (x, y) -> downAt = null; fingerDown(x, y) } }

    private fun fingerDown(x: Float, y: Float) {
        pendingUp = false; pendingMove = null
        val s = GestureDescription.StrokeDescription(path(x, y, x, y), 0, 30, true)
        lastX = endX; lastY = endY
        stroke = s
        dispatch(s)
    }

    /** Sends the next piece of a held finger once the previous one finished (gestures can't overlap). */
    private fun pump() {
        if (busy) return
        val s = stroke ?: return
        val (x, y) = pendingMove ?: (lastX to lastY)
        val end = pendingUp
        if (pendingMove == null && !end) return
        pendingMove = null
        val next = s.continueStroke(path(lastX, lastY, x, y), 0, 24, !end)
        lastX = endX; lastY = endY
        stroke = if (end) null else next
        if (end) pendingUp = false
        dispatch(next)
    }

    private fun longPress(x: Float, y: Float) {
        if (stroke != null) return
        dispatch(GestureDescription.StrokeDescription(path(x, y, x, y), 0, 650))
    }

    // ---------------------------------------------------------------- wheel -> swipes

    private var accV = 0f
    private var accH = 0f
    private var scrolling = false

    override fun scroll(displayId: Int, x: Float, y: Float, v: Float, h: Float) = main.post {
        display = displayId
        accV += v; accH += h
        if (!scrolling && stroke == null) swipe(x, y)
    }.let {}

    private fun swipe(x: Float, y: Float) {
        val step = 60f * resources.displayMetrics.density / 2.75f * 2f // about a wheel notch of content
        val max = 0.4f * minOf(Local.width, Local.height)
        // Wheel up = content moves down = the finger moves down. At most [max] per swipe; the rest follows.
        val dy = (accV * step).coerceIn(-max, max); val dx = (-accH * step).coerceIn(-max, max)
        if (abs(dy) < 4 && abs(dx) < 4) return
        accV -= dy / step; accH += dx / step
        scrolling = true
        // The finger must stay on the display: with no room ahead of the cursor, start further back.
        fun fit(p: Float, d: Float, size: Float): Float {
            val lo = 2f; val hi = size - 3f
            return when {
                p + d < lo -> (lo - d).coerceAtMost(hi)
                p + d > hi -> (hi - d).coerceAtLeast(lo)
                else -> p
            }
        }
        val sx = fit(x, dx, Local.width); val sy = fit(y, dy, Local.height)
        // Slow enough not to fling.
        val s = GestureDescription.StrokeDescription(path(sx, sy, sx + dx, sy + dy), 0, 160)
        val b = GestureDescription.Builder().addStroke(s)
        if (Build.VERSION.SDK_INT >= 30) b.setDisplayId(display)
        val ok = dispatchGesture(b.build(), object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { scrolling = false; swipe(x, y) }
            override fun onCancelled(g: GestureDescription?) { scrolling = false }
        }, main)
        if (!ok) scrolling = false
    }

    // ---------------------------------------------------------------- keys -> text and actions

    private var guiAlone = false

    override fun key(displayId: Int, usage: Int, mods: Int, down: Boolean) = main.post {
        display = displayId
        val gui = usage == 0xE3 || usage == 0xE7
        if (gui) {
            if (down) guiAlone = true
            else if (guiAlone) { guiAlone = false; performGlobalAction(GLOBAL_ACTION_HOME) }
            return@post
        }
        if (usage in 0xE0..0xE7 || !down) return@post
        guiAlone = false
        val ctrl = mods and 0x11 != 0
        val cmd = mods and 0x88 != 0
        val alt = mods and 0x44 != 0
        when {
            usage == Keys.TAB && (alt || cmd) -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            usage == Keys.ESC -> performGlobalAction(GLOBAL_ACTION_BACK)
            ctrl || cmd -> shortcut(usage)
            usage == Keys.BACKSPACE -> edit { t, s, e -> if (s != e) Triple(t.removeRange(s, e), s, s) else if (s > 0) Triple(t.removeRange(s - 1, s), s - 1, s - 1) else null }
            usage == Keys.DELETE -> edit { t, s, e -> if (s != e) Triple(t.removeRange(s, e), s, s) else if (s < t.length) Triple(t.removeRange(s, s + 1), s, s) else null }
            usage == Keys.LEFT -> moveCaret(-1)
            usage == Keys.RIGHT -> moveCaret(1)
            usage == Keys.ENTER -> enter()
            else -> Local.charFor(usage, mods)?.let { c -> insert(c.toString()) }
        }
    }.let {}

    /** The focused text field on the controlled display. */
    private fun focused(): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= 30) {
            windowsOnAllDisplays.get(display)?.forEach { w -> w.root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { return it } }
        }
        return rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    private fun textOf(n: AccessibilityNodeInfo): String =
        if (Build.VERSION.SDK_INT >= 26 && n.isShowingHintText) "" else n.text?.toString() ?: ""

    private fun edit(change: (String, Int, Int) -> Triple<String, Int, Int>?) {
        val n = focused() ?: return
        val t = textOf(n)
        var s = n.textSelectionStart; var e = n.textSelectionEnd
        if (s < 0 || e < 0) { s = t.length; e = t.length }
        val (nt, ns, ne) = change(t, minOf(s, e).coerceIn(0, t.length), maxOf(s, e).coerceIn(0, t.length)) ?: return
        n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, nt)
        })
        select(n, ns, ne)
    }

    private fun select(n: AccessibilityNodeInfo, s: Int, e: Int) {
        n.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, s)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, e)
        })
    }

    private fun insert(s: String) = edit { t, a, b -> Triple(t.substring(0, a) + s + t.substring(b), a + s.length, a + s.length) }

    private fun moveCaret(d: Int) {
        val n = focused() ?: return
        val len = textOf(n).length
        val p = (n.textSelectionEnd.takeIf { it >= 0 } ?: len) + d
        select(n, p.coerceIn(0, len), p.coerceIn(0, len))
    }

    private fun enter() {
        val n = focused() ?: return
        if (Build.VERSION.SDK_INT >= 30 && n.actionList.any { it.id == android.R.id.accessibilityActionImeEnter }) {
            n.performAction(android.R.id.accessibilityActionImeEnter)
        } else if (n.isMultiLine) insert("\n")
    }

    private fun shortcut(usage: Int) {
        val n = focused() ?: return
        when (usage) {
            Keys.letter('a') -> select(n, 0, textOf(n).length)
            Keys.letter('c') -> n.performAction(AccessibilityNodeInfo.ACTION_COPY)
            Keys.letter('x') -> n.performAction(AccessibilityNodeInfo.ACTION_CUT)
            Keys.letter('v') -> n.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
    }

    companion object {
        private const val TAG = "PadBridgeA11y"
        @Volatile var instance: LocalA11yService? = null; private set
    }
}
