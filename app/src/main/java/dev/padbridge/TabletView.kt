package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Drawing tablet: an active area on the phone maps onto the computer's whole screen (absolute position, like a
 * Wacom). The area keeps the display's aspect ratio ([Settings.tabletRatio]) so circles stay round, and can be moved
 * and resized in [editing] mode.
 *
 * - S Pen / stylus: real hover (the pointer follows above the glass), pressure, the side button (right click /
 *   barrel), the eraser end.
 * - Finger, draw mode: touching is the pen down.
 * - Finger, hover mode: touching moves the pointer without clicking; a quick tap clicks, double-tap and hold drags,
 *   a two-finger tap right-clicks.
 *
 * Sent as a pen (pressure, hover; Windows Ink, Linux) or an absolute mouse (macOS), see [Settings.tabletSendAs].
 */
class TabletView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density

    /** Move / resize the area instead of drawing. */
    var editing = false
        set(v) { field = v; invalidate() }

    /** Called after the area changes (so the screen can show its size). */
    var onAreaChanged: (() -> Unit)? = null

    private val area = RectF()
    private val pad = RectF()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    // ---------------------------------------------------------------- area

    /** Lays the area out from the saved fractions, fitted to the ratio and kept inside the pad. */
    fun layoutArea() {
        val inset = 10 * d
        pad.set(inset, inset, width - inset, height - inset)
        if (pad.width() <= 0 || pad.height() <= 0) return
        val ratio = Settings.tabletRatio
        if (ratio <= 0f) { area.set(pad); invalidate(); return }
        val a = Settings.tabletArea
        var w = a[2] * pad.width()
        var h = w / ratio
        if (h > pad.height()) { h = pad.height(); w = h * ratio }
        if (w > pad.width()) { w = pad.width(); h = w / ratio }
        // Never moved (negative): centered.
        val l = (if (a[0] < 0f) pad.centerX() - w / 2 else pad.left + a[0] * pad.width()).coerceIn(pad.left, pad.right - w)
        val t = (if (a[1] < 0f) pad.centerY() - h / 2 else pad.top + a[1] * pad.height()).coerceIn(pad.top, pad.bottom - h)
        area.set(l, t, l + w, t + h)
        invalidate()
    }

    private fun saveArea() {
        if (pad.width() <= 0) return
        Settings.tabletArea = floatArrayOf(
            (area.left - pad.left) / pad.width(), (area.top - pad.top) / pad.height(), area.width() / pad.width(),
        )
        onAreaChanged?.invoke()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = layoutArea()

    // ---------------------------------------------------------------- output

    private val useMouse get() = when (Settings.tabletSendAs) {
        1 -> false
        2 -> true
        else -> Settings.hostOs == HostOs.MAC
    }

    private var hx = -1f
    private var hy = -1f
    private var down = false

    private fun send(x: Float, y: Float, inRange: Boolean, tip: Boolean, barrel: Boolean, eraser: Boolean, pressure: Float) {
        val nx = ((x - area.left) / area.width()).coerceIn(0f, 1f)
        val ny = ((y - area.top) / area.height()).coerceIn(0f, 1f)
        val ax = (nx * HidDescriptor.TABLET_MAX).toInt(); val ay = (ny * HidDescriptor.TABLET_MAX).toInt()
        hx = x; hy = y; down = tip
        if (useMouse) {
            // Absolute mouse: hover = just move; tip = left (right with the side button).
            HidManager.absMouse(ax, ay, if (!tip) 0 else if (barrel) 2 else 1)
        } else {
            HidManager.pen(ax, ay, inRange, tip, barrel, eraser, pressure)
        }
        invalidate()
    }

    /** Pressure curve: soft (light strokes reach full pressure sooner), linear, or firm. */
    private fun curve(p: Float): Float = when (Settings.tabletCurve) {
        0 -> p.coerceIn(0f, 1f).pow(0.6f)
        2 -> p.coerceIn(0f, 1f).pow(1.7f)
        else -> p.coerceIn(0f, 1f)
    }

    /** The pen was last seen (hovering or touching): fingers are a resting hand until a moment after. */
    private var penSeen = 0L
    private fun penRecently(t: Long) = t - penSeen < 1500

    private fun leave() {
        if (hx < 0) return
        if (!useMouse) HidManager.pen(0, 0, false, false, false, false, 0f)
        hx = -1f; hy = -1f; down = false
        invalidate()
    }

    // ---------------------------------------------------------------- stylus hover

    override fun onHoverEvent(e: MotionEvent): Boolean {
        if (editing) return true
        val t = e.getToolType(0)
        if (t != MotionEvent.TOOL_TYPE_STYLUS && t != MotionEvent.TOOL_TYPE_ERASER) return super.onHoverEvent(e)
        penSeen = e.eventTime
        when (e.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> send(e.x, e.y, true, false,
                e.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0, t == MotionEvent.TOOL_TYPE_ERASER, 0f)
            MotionEvent.ACTION_HOVER_EXIT -> leave()
        }
        return true
    }

    // ---------------------------------------------------------------- touch

    private var editMode = 0 // 1 move, 2 resize
    private var ex = 0f
    private var ey = 0f
    private val start = RectF()

    // Finger hover mode
    private var fingerDownAt = 0L
    private var fingerX0 = 0f
    private var fingerY0 = 0f
    private var lastTapUp = 0L
    private var dragging = false
    private var maxFingers = 0

    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        if (editing) return editTouch(e)
        val t = e.getToolType(0)
        val pen = t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER
        if (pen) {
            penSeen = e.eventTime
            val barrel = e.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0
            val eraser = t == MotionEvent.TOOL_TYPE_ERASER
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { send(e.x, e.y, true, true, barrel, eraser, curve(e.pressure)); Haptics.soft(0.25f) }
                MotionEvent.ACTION_MOVE -> {
                    for (h in 0 until e.historySize) send(e.getHistoricalX(h), e.getHistoricalY(h), true, true, barrel, eraser, curve(e.getHistoricalPressure(h)))
                    send(e.x, e.y, true, true, barrel, eraser, curve(e.pressure))
                }
                // Lifted: still in range (hovering) until the hover exit.
                MotionEvent.ACTION_UP -> send(e.x, e.y, true, false, barrel, eraser, 0f)
                MotionEvent.ACTION_CANCEL -> leave()
            }
            return true
        }
        // Palm rejection: while the pen is in use, and for any palm-sized contact, fingers don't draw or point;
        // two or more still pan, zoom and undo.
        val idx = e.actionIndex
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            palm = Typing.isPalm(e, idx, resources.displayMetrics.ydpi) || penRecently(e.eventTime)
        }
        if (palm && e.pointerCount < 2 && !gesturing) return true
        return if (Settings.tabletFingerHover && !palm) fingerHover(e) else fingerDraw(e)
    }

    private var palm = false

    /**
     * Finger draws: touch = pen down at full pressure. The stroke starts once the finger moves (or after a beat), so
     * a second finger landing right away turns the touch into a gesture instead of a dot:
     * two fingers pan (scroll), pinch zooms, a two-finger tap undoes, a three-finger tap redoes.
     */
    private fun fingerDraw(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gesturing = false; stroking = false; maxFingers = 1
                fingerDownAt = e.eventTime; fingerX0 = e.x; fingerY0 = e.y
                if (!palm) postDelayed(startStroke, 90)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                removeCallbacks(startStroke)
                if (stroking) { send(hx, hy, true, false, false, false, 0f); leave(); stroking = false }
                maxFingers = max(maxFingers, e.pointerCount)
                gesturing = true; gestureMoved = false
                gx = centroidX(e); gy = centroidY(e); span0 = spread(e); zoomSteps = 0
                fingerX0 = gx; fingerY0 = gy // the gesture's start, to tell a tap from a pan
                if (fingerDownAt == 0L) fingerDownAt = e.eventTime
            }
            MotionEvent.ACTION_MOVE -> {
                if (gesturing) { gesture(e); return true }
                if (palm) return true
                if (!stroking && hypot(e.x - fingerX0, e.y - fingerY0) > 3 * d) { removeCallbacks(startStroke); startStroke.run() }
                if (stroking) {
                    for (h in 0 until e.historySize) send(e.getHistoricalX(h), e.getHistoricalY(h), true, true, false, false, 1f)
                    send(e.x, e.y, true, true, false, false, 1f)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (gesturing) {
                // Re-anchor on the fingers that stay, so lifting one doesn't jump the pan.
                val up = e.actionIndex
                var sx = 0f; var sy = 0f; var n = 0
                for (k in 0 until e.pointerCount) if (k != up) { sx += e.getX(k); sy += e.getY(k); n++ }
                if (n > 0) { gx = sx / n; gy = sy / n }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(startStroke)
                if (gesturing) {
                    val quick = e.eventTime - fingerDownAt < 300 && !gestureMoved
                    if (quick && maxFingers == 2) { HidManager.combo(undoCombo()); Haptics.click() }
                    if (quick && maxFingers >= 3) { HidManager.combo(redoCombo()); Haptics.click() }
                } else if (!palm) {
                    if (!stroking) startStroke.run() // a tap is a dot
                    send(e.x, e.y, true, false, false, false, 0f); leave()
                }
                gesturing = false; stroking = false; fingerDownAt = 0L
            }
            MotionEvent.ACTION_CANCEL -> { removeCallbacks(startStroke); if (stroking) leave(); gesturing = false; stroking = false }
        }
        return true
    }

    private var stroking = false
    private var gesturing = false
    private var gestureMoved = false
    private var gx = 0f
    private var gy = 0f
    private var span0 = 0f
    private var zoomSteps = 0

    private val startStroke = Runnable {
        if (gesturing || stroking) return@Runnable
        stroking = true
        send(fingerX0, fingerY0, true, true, false, false, 1f); Haptics.soft(0.25f)
    }

    private fun centroidX(e: MotionEvent): Float { var s = 0f; for (k in 0 until e.pointerCount) s += e.getX(k); return s / e.pointerCount }
    private fun centroidY(e: MotionEvent): Float { var s = 0f; for (k in 0 until e.pointerCount) s += e.getY(k); return s / e.pointerCount }
    private fun spread(e: MotionEvent): Float {
        val cx = centroidX(e); val cy = centroidY(e); var s = 0f
        for (k in 0 until e.pointerCount) s += hypot(e.getX(k) - cx, e.getY(k) - cy)
        return s / e.pointerCount
    }

    /** Two fingers: the canvas follows them (scroll); spreading or pinching zooms in steps. */
    private fun gesture(e: MotionEvent) {
        if (e.pointerCount < 2) return
        val cx = centroidX(e); val cy = centroidY(e)
        val dx = cx - gx; val dy = cy - gy
        gx = cx; gy = cy
        val sp = spread(e)
        if (span0 > 0f) {
            val ratio = sp / span0
            if (ratio > 1.15f || ratio < 1f / 1.15f) {
                val zoomIn = ratio > 1f
                HidManager.combo(zoomCombo(zoomIn)); Haptics.soft(0.5f)
                span0 = sp; gestureMoved = true; zoomSteps++
                return
            }
        }
        if (hypot(cx - fingerX0, cy - fingerY0) > 8 * d) gestureMoved = true
        // Content follows the fingers (natural), like panning a canvas.
        // Same sign as the trackpad: computers like macOS reverse the wheel themselves.
        val sign = if (Settings.naturalScroll) 1f else -1f
        if (gestureMoved) HidManager.scroll(sign * dy / d / 22f, -sign * dx / d / 22f)
    }

    private val mod get() = if (Settings.hostOs == HostOs.MAC) Keys.L_GUI else Keys.L_CTRL

    fun undoCombo() = Combo(mod, Keys.letter('z'))
    fun redoCombo() = when (Settings.hostOs) {
        HostOs.WINDOWS -> Combo(Keys.L_CTRL, Keys.letter('y'))
        else -> Combo(mod or Keys.L_SHIFT, Keys.letter('z'))
    }
    fun zoomCombo(zoomIn: Boolean) = Combo(mod, if (zoomIn) Keys.EQUAL else Keys.MINUS)

    /** Finger hovers: moving points without clicking; tap = click, double-tap-hold = drag, two-finger tap = right. */
    private fun fingerHover(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fingerDownAt = e.eventTime; fingerX0 = e.x; fingerY0 = e.y; maxFingers = 1
                dragging = e.eventTime - lastTapUp < 300
                send(e.x, e.y, true, dragging, false, false, if (dragging) 1f else 0f)
                if (dragging) Haptics.soft(0.4f)
            }
            MotionEvent.ACTION_POINTER_DOWN -> maxFingers = max(maxFingers, e.pointerCount)
            MotionEvent.ACTION_MOVE -> if (e.pointerCount == 1) send(e.x, e.y, true, dragging, false, false, if (dragging) 1f else 0f)
            MotionEvent.ACTION_UP -> {
                val quick = e.eventTime - fingerDownAt < 220 && hypot(e.x - fingerX0, e.y - fingerY0) < 10 * d
                when {
                    dragging -> { send(e.x, e.y, true, false, false, false, 0f); dragging = false; lastTapUp = 0 }
                    quick && maxFingers >= 2 -> { click(fingerX0, fingerY0, right = true); lastTapUp = 0 }
                    quick -> { click(fingerX0, fingerY0, right = false); lastTapUp = e.eventTime }
                    else -> lastTapUp = 0
                }
            }
            MotionEvent.ACTION_CANCEL -> { if (dragging) send(hx, hy, true, false, false, false, 0f); dragging = false }
        }
        return true
    }

    private fun click(x: Float, y: Float, right: Boolean) {
        send(x, y, true, true, right, false, 1f)
        send(x, y, true, false, right, false, 0f)
        Haptics.tick()
    }

    /** Edit mode: drag inside the area to move it, the corner handle to resize it (keeping the ratio). */
    private fun editTouch(e: MotionEvent): Boolean {
        val ratio = Settings.tabletRatio
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ex = e.x; ey = e.y; start.set(area)
                editMode = when {
                    hypot(e.x - area.right, e.y - area.bottom) < 36 * d -> 2
                    area.contains(e.x, e.y) -> 1
                    else -> 0
                }
                if (editMode != 0) Haptics.soft(0.3f)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - ex; val dy = e.y - ey
                if (editMode == 1) {
                    val l = (start.left + dx).coerceIn(pad.left, pad.right - start.width())
                    val t = (start.top + dy).coerceIn(pad.top, pad.bottom - start.height())
                    area.offsetTo(l, t)
                } else if (editMode == 2) {
                    var w = max(80 * d, start.width() + max(dx, if (ratio > 0) dy * ratio else dx))
                    w = min(w, pad.right - start.left)
                    var h = if (ratio > 0) w / ratio else max(60 * d, start.height() + dy)
                    if (start.top + h > pad.bottom) { h = pad.bottom - start.top; if (ratio > 0) w = h * ratio }
                    area.set(start.left, start.top, start.left + w, start.top + h)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (editMode != 0) { saveArea(); editMode = 0 }
        }
        return true
    }

    override fun onDetachedFromWindow() { leave(); super.onDetachedFromWindow() }

    // ---------------------------------------------------------------- drawing

    override fun onDraw(c: Canvas) {
        val r = 28 * d
        val full = RectF(0f, 0f, width.toFloat(), height.toFloat())
        if (Real.active) Real.trackpad(c, full, r, d)
        else c.drawRoundRect(full, r, r, fill.fillOrOutline(C.SURFACE, d))

        // The active area: the computer's screen, in miniature.
        val ar = 14 * d
        if (!C.AOD) {
            fill.style = Paint.Style.FILL; fill.color = C.SURFACE2
            c.drawRoundRect(area, ar, ar, fill)
        }
        line.color = if (editing) C.ACCENT else C.STROKE
        line.strokeWidth = (if (editing) 2f else 1.2f) * d
        c.drawRoundRect(area, ar, ar, line)
        // Thirds guides, faint.
        dash.color = C.STROKE; dash.strokeWidth = d; dash.alpha = 90
        for (i in 1..2) {
            val gx = area.left + area.width() * i / 3; val gy = area.top + area.height() * i / 3
            c.drawLine(gx, area.top + ar, gx, area.bottom - ar, dash)
            c.drawLine(area.left + ar, gy, area.right - ar, gy, dash)
        }

        text.color = C.MUTED
        text.textSize = 13 * d
        val ratio = Settings.tabletRatio
        val label = when {
            editing -> "Drag to move · corner to resize"
            ratio <= 0f -> "Whole pad"
            else -> ratioName(ratio)
        }
        c.drawText(label, area.centerX(), area.bottom - 12 * d, text)

        if (editing) {
            // Resize handle.
            fill.style = Paint.Style.FILL; fill.color = C.ACCENT
            c.drawCircle(area.right, area.bottom, 9 * d, fill)
        }

        // Where the pen is: a ring when hovering, a dot when touching.
        if (hx >= 0 && !editing) {
            val px = hx.coerceIn(area.left, area.right); val py = hy.coerceIn(area.top, area.bottom)
            if (down) { fill.style = Paint.Style.FILL; fill.color = C.ACCENT; c.drawCircle(px, py, 5 * d, fill) }
            else { line.color = C.ACCENT; line.strokeWidth = 1.5f * d; c.drawCircle(px, py, 9 * d, line) }
        }
    }

    companion object {
        val RATIOS = listOf(16f / 9f to "16:9", 16f / 10f to "16:10", 3f / 2f to "3:2", 4f / 3f to "4:3",
            21f / 9f to "21:9", 32f / 9f to "32:9", 9f / 16f to "9:16 (portrait)")

        fun ratioName(r: Float): String =
            RATIOS.firstOrNull { abs(it.first - r) < 0.01f }?.second ?: "%.2f:1".format(r)
    }
}
