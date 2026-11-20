package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Gyro air mouse. Point the phone and turn it: the pointer follows.
 *
 *  - Hold the big pad to aim (or switch on "Always aim"). A quick tap on the pad is a left click.
 *  - Hold Left to drag: it presses the button and aims at the same time.
 *  - The strip between the buttons is a scroll wheel.
 */
class AirMouseView(ctx: Context) : View(ctx) {

    /** Move without holding the pad. */
    var alwaysAim = false
        set(v) { field = v; invalidate() }

    /** The whole view is the aim pad (AOD mode): no buttons or wheel. */
    var padOnly = false

    private enum class Zone { AIM, LEFT, RIGHT, SCROLL }

    private val density = resources.displayMetrics.density
    private val motion = Motion(this) { m, dt -> onGyro(m, dt) }
    val hasGyro get() = motion.available

    private val pad = RectF()
    private val leftBtn = RectF()
    private val rightBtn = RectF()
    private val wheel = RectF()

    private val owner = HashMap<Int, Zone>()
    private val downAt = HashMap<Int, Long>()
    private val downPos = HashMap<Int, Pair<Float, Float>>()
    private val lastY = HashMap<Int, Float>()
    private var turnSinceDown = 0f
    private var wheelOffset = 0f

    // Smoothed rates (rad/s) and a decaying trail for the on-screen dot.
    private var sYaw = 0f
    private var sPitch = 0f
    private var dotX = 0f
    private var dotY = 0f

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2 * density }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val path = Path()

    init { isHapticFeedbackEnabled = false }

    private val aiming
        get() = alwaysAim || owner.values.any { it == Zone.AIM || it == Zone.LEFT }

    // ------------------------------------------------------------------ lifecycle

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) motion.start() else { motion.stop(); releaseAll() }
    }

    override fun onDetachedFromWindow() {
        motion.stop(); releaseAll()
        super.onDetachedFromWindow()
    }

    fun releaseAll() {
        if (owner.containsValue(Zone.LEFT)) HidManager.setButton(TrackpadView.BTN_LEFT, false)
        if (owner.containsValue(Zone.RIGHT)) HidManager.setButton(TrackpadView.BTN_RIGHT, false)
        owner.clear(); downAt.clear(); downPos.clear(); lastY.clear()
        invalidate()
    }

    // ------------------------------------------------------------------ layout

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (padOnly) {
            pad.set(0f, 0f, w.toFloat(), h.toFloat())
            leftBtn.setEmpty(); rightBtn.setEmpty(); wheel.setEmpty()
            return
        }
        val gap = 8 * density
        val rowH = (h * 0.24f).coerceIn(84 * density, 140 * density)
        pad.set(0f, 0f, w.toFloat(), h - rowH - gap)
        val top = h - rowH
        val wheelW = max(w * 0.18f, 56 * density)
        val btnW = (w - wheelW - 2 * gap) / 2
        leftBtn.set(0f, top, btnW, h.toFloat())
        wheel.set(btnW + gap, top, btnW + gap + wheelW, h.toFloat())
        rightBtn.set(w - btnW, top, w.toFloat(), h.toFloat())
    }

    private fun zoneAt(x: Float, y: Float): Zone? = when {
        pad.contains(x, y) -> Zone.AIM
        leftBtn.contains(x, y) -> Zone.LEFT
        rightBtn.contains(x, y) -> Zone.RIGHT
        wheel.contains(x, y) -> Zone.SCROLL
        else -> null
    }

    // ------------------------------------------------------------------ touch

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val i = e.actionIndex
                val id = e.getPointerId(i)
                val x = e.getX(i); val y = e.getY(i)
                val z = zoneAt(x, y) ?: return true
                val wasAiming = aiming
                owner[id] = z
                downAt[id] = e.eventTime
                downPos[id] = x to y
                lastY[id] = y
                when (z) {
                    Zone.AIM -> { turnSinceDown = 0f; if (!wasAiming) Haptics.tick() }
                    Zone.LEFT -> { HidManager.setButton(TrackpadView.BTN_LEFT, true); Haptics.click() }
                    Zone.RIGHT -> { HidManager.setButton(TrackpadView.BTN_RIGHT, true); Haptics.click() }
                    Zone.SCROLL -> { HidManager.resetScroll(); Haptics.tick() }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (owner[id] != Zone.SCROLL) continue
                    val y = e.getY(i)
                    val dy = y - (lastY[id] ?: y)
                    lastY[id] = y
                    scrollBy(dy)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val ids = if (e.actionMasked == MotionEvent.ACTION_CANCEL) owner.keys.toList()
                else listOf(e.getPointerId(e.actionIndex))
                for (id in ids) {
                    val z = owner.remove(id) ?: continue
                    val i = e.findPointerIndex(id)
                    when (z) {
                        Zone.AIM -> if (e.actionMasked != MotionEvent.ACTION_CANCEL && i >= 0) {
                            val (x0, y0) = downPos[id] ?: (0f to 0f)
                            val still = hypot(e.getX(i) - x0, e.getY(i) - y0) < 24 * density
                            val quick = e.eventTime - (downAt[id] ?: 0L) < 260
                            if (still && quick && turnSinceDown < 0.08f) { HidManager.click(TrackpadView.BTN_LEFT); Haptics.click() }
                        }
                        Zone.LEFT -> if (!owner.containsValue(Zone.LEFT)) HidManager.setButton(TrackpadView.BTN_LEFT, false)
                        Zone.RIGHT -> if (!owner.containsValue(Zone.RIGHT)) HidManager.setButton(TrackpadView.BTN_RIGHT, false)
                        Zone.SCROLL -> {}
                    }
                    downAt.remove(id); downPos.remove(id); lastY.remove(id)
                }
            }
        }
        invalidate()
        return true
    }

    private fun scrollBy(dyPx: Float) {
        wheelOffset += dyPx
        val divisor = if (HidManager.hiResWheel) 30f else 22f
        val notches = dyPx / density / divisor * Settings.scrollSpeed
        // A physical wheel: rolling it toward you (finger moves down) scrolls the page down.
        HidManager.scroll(-notches, 0f)
        // A ratchet tick per notch, like a real wheel.
        wheelFeel += kotlin.math.abs(notches)
        if (wheelFeel >= 1f) { wheelFeel %= 1f; Haptics.soft(0.3f) }
    }

    private var wheelFeel = 0f

    // ------------------------------------------------------------------ gyro

    private fun onGyro(m: Motion, dt: Float) {
        // Light smoothing removes hand tremor without adding noticeable lag at 200 Hz.
        sYaw += (m.yaw - sYaw) * 0.45f
        sPitch += (m.pitch - sPitch) * 0.45f
        if (!aiming) {
            sYaw = 0f; sPitch = 0f
            if (dotX != 0f || dotY != 0f) { dotX *= 0.8f; dotY *= 0.8f; if (abs(dotX) + abs(dotY) < 0.5f) { dotX = 0f; dotY = 0f }; postInvalidateOnAnimation() }
            return
        }
        val rate = hypot(sYaw, sPitch)
        turnSinceDown += rate * dt
        // Soft dead zone: nothing below ~0.7°/s, full response from ~2.5°/s.
        val gate = ((rate - 0.012f) / 0.03f).coerceIn(0f, 1f)
        if (gate == 0f) return
        // Slow turns stay precise, fast flicks cross the screen.
        val accel = 1f + 1.2f * min(max(rate - 0.4f, 0f), 2.5f)
        val gain = 1400f * Settings.airSpeed * accel * gate * dt
        val dx = -sYaw * gain
        val dy = -sPitch * gain
        HidManager.move(dx, dy)
        dotX = (dotX * 0.85f + dx * 0.02f).coerceIn(-1f, 1f)
        dotY = (dotY * 0.85f + dy * 0.02f).coerceIn(-1f, 1f)
        postInvalidateOnAnimation()
    }

    // ------------------------------------------------------------------ drawing

    override fun onDraw(c: Canvas) {
        val r = 28 * density
        val on = aiming
        c.drawRoundRect(pad, r, r, fill.fillOrOutline(if (on) C.ACCENT_DIM else C.SURFACE, density))
        stroke.color = if (on) C.ACCENT else C.STROKE
        c.drawRoundRect(pad, r, r, stroke)

        // Crosshair and a dot that leans in the direction the pointer is moving.
        val cx = pad.centerX(); val cy = pad.centerY()
        val ring = min(pad.width(), pad.height()) * 0.16f
        stroke.color = if (on) C.ACCENT else C.MUTED
        c.drawCircle(cx, cy, ring, stroke)
        c.drawLine(cx - ring * 1.4f, cy, cx - ring * 0.6f, cy, stroke)
        c.drawLine(cx + ring * 0.6f, cy, cx + ring * 1.4f, cy, stroke)
        c.drawLine(cx, cy - ring * 1.4f, cx, cy - ring * 0.6f, stroke)
        c.drawLine(cx, cy + ring * 0.6f, cx, cy + ring * 1.4f, stroke)
        fill.fillOrOutline(if (on) C.ACCENT else C.SURFACE3, density, on)
        c.drawCircle(cx + dotX * ring * 0.8f, cy + dotY * ring * 0.8f, ring * 0.22f, fill)

        text.textSize = 16 * density; text.isFakeBoldText = true
        text.color = if (on) C.ON_ACCENT_DIM else C.TEXT
        val title = when {
            !hasGyro -> "This phone has no gyroscope"
            alwaysAim -> "Aiming · tap to click"
            on -> "Aiming"
            else -> "Hold here and point the phone"
        }
        c.drawText(title, cx, cy + ring * 2.2f, text)
        text.textSize = 13 * density; text.isFakeBoldText = false
        text.color = if (on) C.ON_ACCENT_DIM else C.MUTED
        c.drawText("Tap to click · hold Left to drag", cx, cy + ring * 2.2f + 22 * density, text)

        // Left / Right buttons, connected-button shapes like the trackpad's click bar.
        val outer = 28 * density; val inner = 8 * density
        for ((rect, zone, label) in listOf(Triple(leftBtn, Zone.LEFT, "Left"), Triple(rightBtn, Zone.RIGHT, "Right"))) {
            val pressed = owner.containsValue(zone)
            fill.fillOrOutline(if (pressed) C.ACCENT else C.SURFACE2, density, pressed)
            val innerR = if (pressed) outer else inner
            val radii = if (zone == Zone.LEFT) floatArrayOf(outer, outer, innerR, innerR, innerR, innerR, outer, outer)
            else floatArrayOf(innerR, innerR, outer, outer, outer, outer, innerR, innerR)
            path.reset(); path.addRoundRect(rect, radii, Path.Direction.CW)
            c.drawPath(path, fill)
            text.textSize = 15 * density; text.isFakeBoldText = true
            text.color = if (pressed) C.ON_ACCENT else C.TEXT
            c.drawText(label, rect.centerX(), rect.centerY() - (text.ascent() + text.descent()) / 2, text)
        }

        // Scroll wheel with ridges that roll with the finger.
        val scrolling = owner.containsValue(Zone.SCROLL)
        fill.fillOrOutline(if (scrolling) C.ACCENT_DIM else C.SURFACE2, density, scrolling)
        c.drawRoundRect(wheel, inner, inner, fill)
        stroke.color = if (scrolling) C.ACCENT else C.STROKE
        val step = 14 * density
        val shift = ((wheelOffset % step) + step) % step
        var y = wheel.top + shift
        c.save(); c.clipRect(wheel)
        while (y < wheel.bottom) {
            c.drawLine(wheel.left + wheel.width() * 0.25f, y, wheel.right - wheel.width() * 0.25f, y, stroke)
            y += step
        }
        c.restore()
    }
}
