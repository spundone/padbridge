package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-touch trackpad. Gestures are recognised here and sent as mouse reports or as the
 * keyboard shortcut each OS binds to the equivalent native gesture, so they work on hosts
 * that ignore HID multi-touch (macOS) or need a certified Precision Touchpad (Windows).
 *
 * Built in:  1-finger move / tap / tap-and-drag / double tap, 2-finger scroll with momentum
 *            and axis rails, pinch zoom, right-edge and bottom-edge scroll strips.
 * Mappable (see [GestureSlot]): 2/3/4-finger taps, press-and-hold, 2-finger horizontal swipe,
 *            rotate, 3- and 4-finger swipes, edge swipes. Optional 3-finger drag.
 */
class TrackpadView(ctx: Context) : View(ctx) {

    private enum class Mode { NONE, MOVE, SCROLL, NAV, PINCH, ROTATE, SWIPE, THREE_DRAG, EDGE_SCROLL_V, EDGE_SCROLL_H, EDGE_SWIPE, DONE }
    private enum class Edge { NONE, LEFT, RIGHT, TOP, RIGHT_STRIP, BOTTOM_STRIP }
    private enum class Axis { FREE, VERTICAL, HORIZONTAL }

    private val density = resources.displayMetrics.density
    private val tapSlop = 10f * density
    private val pinchSlop = 28f * density
    private val swipeDistance = 70f * density
    private val navDistance = 90f * density
    private val edgeZone = 16f * density
    private val stripZone = 30f * density
    private val edgeSwipeDistance = 60f * density
    private val rotateStep = 40.0
    private val tapMaxDuration = 260L
    private val longPressDelay = 650L

    private val last = HashMap<Int, PointF>()
    private val start = HashMap<Int, PointF>()
    private var mode = Mode.NONE
    private var axis = Axis.FREE
    private var edge = Edge.NONE
    private var maxPointers = 0
    private var downTime = 0L
    private var lastEventTime = 0L
    private var moved = false
    private var dragging = false
    private var threeDragging = false
    private var lastTapUp = 0L
    private val lastTapPos = PointF()
    private var startX = 0f

    private var span0 = 0f
    private var angle0 = 0.0
    private var angleAcc = 0.0
    private var travelX = 0f
    private var travelY = 0f
    private var pinchRef = 0f
    private var fired = false

    private var velocity: VelocityTracker? = null
    private val momentumV = PointF()
    private var momentumLast = 0L
    private var momentumRunning = false

    private var label: String? = null
    private var labelUntil = 0L

    private val padPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fingerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13 * density; textAlign = Paint.Align.CENTER }
    private val labelBg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 17 * density; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val rect = RectF()

    init { isHapticFeedbackEnabled = false }

    private val os get() = Settings.hostOs

    private fun fire(slot: GestureSlot): Boolean {
        val action = Settings.gesture(slot)
        if (action == GAction.NONE) return false
        if (action.perform(os)) showLabel(action.label) else showLabel("${action.label}: not on ${os.label}")
        Haptics.click()
        return true
    }

    private val longPress = Runnable {
        if (mode != Mode.NONE || moved || dragging || last.size != 1) return@Runnable
        if (Settings.gesture(GestureSlot.LONG_PRESS) == GAction.HOLD_DRAG) {
            // Samsung DeX style: touch and hold, then move to drag or select text.
            dragging = true
            HidManager.setButton(BTN_LEFT, true)
            showLabel("Drag"); Haptics.heavy()
            return@Runnable
        }
        if (fire(GestureSlot.LONG_PRESS)) { mode = Mode.DONE; Haptics.heavy() }
    }

    /** Pointers treated as a resting finger for the rest of this gesture. */
    private val ignored = HashSet<Int>()
    private val downAt = HashMap<Int, Long>()

    // ------------------------------------------------------------------ touch

    // ------------------------------------------------------------------ native touchpad (PTP)

    /** pointer id -> contact id (0..15), stable for the contact's lifetime as PTP requires. */
    private val ptpIds = HashMap<Int, Int>()
    private var ptpLastFrame: List<HidManager.Contact> = emptyList()

    /** Resend held contacts so hosts don't treat a resting finger as lifted (Linux releases after ~100 ms). */
    private val ptpKeepAlive = object : Runnable {
        override fun run() {
            if (ptpLastFrame.isEmpty() || !HidManager.ptpActive) return
            HidManager.touchFrame(ptpLastFrame, false, SystemClock.uptimeMillis())
            postDelayed(this, 50)
        }
    }

    /** Host is in touchpad mode: send raw contacts and let Windows / libinput do every gesture. */
    private fun handlePtp(e: MotionEvent) {
        parent?.requestDisallowInterceptTouchEvent(true)
        val action = e.actionMasked
        val lifted = when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> e.actionIndex
            MotionEvent.ACTION_CANCEL -> ALL
            else -> NONE_LIFTED
        }
        val frame = ArrayList<HidManager.Contact>(e.pointerCount)
        for (i in 0 until e.pointerCount) {
            val pid = e.getPointerId(i)
            val cid = ptpIds.getOrPut(pid) { (0..15).first { it !in ptpIds.values } }
            val x = (e.getX(i) / width * HidDescriptor.PTP_MAX).toInt().coerceIn(0, HidDescriptor.PTP_MAX)
            val y = (e.getY(i) / height * HidDescriptor.PTP_MAX).toInt().coerceIn(0, HidDescriptor.PTP_MAX)
            val tip = lifted != i && lifted != ALL
            frame += HidManager.Contact(cid, x, y, tip)
            if (tip) last.getOrPut(pid) { PointF() }.set(e.getX(i), e.getY(i)) else last.remove(pid)
        }
        HidManager.touchFrame(frame, false, e.eventTime)
        if (lifted >= 0) ptpIds.remove(e.getPointerId(lifted))
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) { ptpIds.clear(); last.clear() }
        ptpLastFrame = frame.filter { it.tip }
        removeCallbacks(ptpKeepAlive)
        if (ptpLastFrame.isNotEmpty()) postDelayed(ptpKeepAlive, 50)
    }

    // ------------------------------------------------------------------ S Pen / stylus

    private fun isPen(e: MotionEvent, i: Int = 0): Boolean {
        val t = e.getToolType(i)
        return t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER
    }

    /** The pen is touching or hovering; fingers are ignored until shortly after it leaves (palm rejection). */
    private var penNearUntil = 0L
    private var penTouching = false
    private var penButton = 0
    private var penSideDown = false
    private val penLast = PointF()
    private var penHasLast = false

    private fun penNear() = penTouching || SystemClock.uptimeMillis() < penNearUntil

    private fun penMove(x: Float, y: Float) {
        if (penHasLast) {
            // Precise and linear: no acceleration, so the pen feels like a drawing tablet in mouse mode.
            val gain = 1.6f * Settings.pointerSpeed
            HidManager.move((x - penLast.x) / density * gain, (y - penLast.y) / density * gain)
        }
        penLast.set(x, y); penHasLast = true
    }

    /** Hovering pen: move the pointer; the side button clicks right. */
    override fun onHoverEvent(e: MotionEvent): Boolean {
        if (!Settings.stylus || !isPen(e)) return super.onHoverEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER -> { penHasLast = false; penLast.set(e.x, e.y); penHasLast = true }
            MotionEvent.ACTION_HOVER_MOVE -> penMove(e.x, e.y)
            MotionEvent.ACTION_HOVER_EXIT -> penHasLast = false
        }
        penNearUntil = SystemClock.uptimeMillis() + 600
        val side = e.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0
        if (side && !penSideDown) HidManager.click(BTN_RIGHT)
        penSideDown = side
        return true
    }

    /** Pen on the glass: holds a button while touching (left, or right with the side button pressed). */
    private fun handlePen(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                cancel(); stopMomentum()
                penTouching = true
                penButton = if (e.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0 ||
                    e.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) BTN_RIGHT else BTN_LEFT
                penHasLast = false; penLast.set(e.x, e.y); penHasLast = true
                HidManager.setButton(penButton, true)
            }
            MotionEvent.ACTION_MOVE -> penMove(e.x, e.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                HidManager.setButton(penButton, false)
                penTouching = false
                penNearUntil = SystemClock.uptimeMillis() + 600
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (Settings.stylus) {
            if (penTouching || (e.actionMasked == MotionEvent.ACTION_DOWN && isPen(e))) { handlePen(e); return true }
            // Palm rejection: a hand resting on the screen while the pen is near does nothing.
            if (penNear() && e.actionMasked == MotionEvent.ACTION_DOWN) return false
        }
        if (HidManager.ptpActive) { handlePtp(e); invalidate(); return true }
        velocity?.addMovement(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> onFirstDown(e)
            MotionEvent.ACTION_POINTER_DOWN -> onExtraDown(e)
            MotionEvent.ACTION_MOVE -> onMove(e)
            MotionEvent.ACTION_POINTER_UP -> {
                val id = e.getPointerId(e.actionIndex)
                last.remove(id); start.remove(id); ignored.remove(id); downAt.remove(id)
                val remaining = active(e).filter { it != e.actionIndex }
                if (remaining.size == 2) resetTwoFinger(e, remaining[0], remaining[1])
            }
            MotionEvent.ACTION_UP -> onLastUp(e)
            MotionEvent.ACTION_CANCEL -> cancel()
        }
        invalidate()
        return true
    }

    private fun cancel() {
        removeCallbacks(longPress)
        if (dragging || threeDragging) HidManager.setButton(BTN_LEFT, false)
        dragging = false; threeDragging = false
        last.clear(); start.clear(); ignored.clear(); downAt.clear(); mode = Mode.NONE
    }

    private fun active(e: MotionEvent) = (0 until e.pointerCount).filter { e.getPointerId(it) !in ignored }

    private fun onFirstDown(e: MotionEvent) {
        parent?.requestDisallowInterceptTouchEvent(true)
        stopMomentum()
        HidManager.resetScroll()
        velocity?.recycle()
        velocity = VelocityTracker.obtain().also { it.addMovement(e) }
        last.clear(); start.clear(); ignored.clear(); downAt.clear()
        val id = e.getPointerId(0)
        last[id] = PointF(e.x, e.y); start[id] = PointF(e.x, e.y); downAt[id] = e.eventTime
        mode = Mode.NONE; axis = Axis.FREE
        maxPointers = 1
        moved = false; fired = false
        travelX = 0f; travelY = 0f
        downTime = e.eventTime; lastEventTime = e.eventTime
        startX = e.x

        edge = when {
            Settings.edgeScroll && e.x > width - stripZone -> Edge.RIGHT_STRIP
            Settings.edgeScroll && e.y > height - stripZone -> Edge.BOTTOM_STRIP
            e.x > width - edgeZone -> Edge.RIGHT
            e.x < edgeZone -> Edge.LEFT
            e.y < edgeZone -> Edge.TOP
            else -> Edge.NONE
        }

        if (Settings.tapToClick && e.eventTime - lastTapUp < Settings.doubleTapMs.toLong() &&
            hypot(e.x - lastTapPos.x, e.y - lastTapPos.y) < 60 * density
        ) {
            // Second touch right after a tap: hold the button. Lift without moving = double click.
            dragging = true
            HidManager.setButton(BTN_LEFT, true)
        } else {
            postDelayed(longPress, longPressDelay)
        }
    }

    private fun onExtraDown(e: MotionEvent) {
        removeCallbacks(longPress)
        val i = e.actionIndex
        val id = e.getPointerId(i)
        last[id] = PointF(e.getX(i), e.getY(i)); start[id] = PointF(e.getX(i), e.getY(i)); downAt[id] = e.eventTime
        maxPointers = max(maxPointers, e.pointerCount - ignored.size)
        if (!dragging && mode in setOf(Mode.NONE, Mode.MOVE, Mode.EDGE_SWIPE)) {
            // A finger joined: decide again (e.g. one finger rested, then two-finger scroll).
            mode = Mode.NONE; axis = Axis.FREE
        }
        travelX = 0f; travelY = 0f; fired = false
        val act = active(e)
        if (act.size == 2) resetTwoFinger(e, act[0], act[1])
    }

    private fun resetTwoFinger(e: MotionEvent, a: Int, b: Int) {
        val dx = e.getX(b) - e.getX(a); val dy = e.getY(b) - e.getY(a)
        span0 = hypot(dx, dy); pinchRef = span0
        angle0 = atan2(dy.toDouble(), dx.toDouble()); angleAcc = 0.0
        travelX = 0f; travelY = 0f
    }

    private fun dist(e: MotionEvent, i: Int): Float {
        val s = start[e.getPointerId(i)] ?: return 0f
        return hypot(e.getX(i) - s.x, e.getY(i) - s.y)
    }

    /**
     * Resting-finger rejection (as in Samsung DeX): before a gesture is chosen, a finger that stays put
     * while another clearly moves, and that wasn't placed together with it, is ignored from then on.
     */
    private fun rejectRestingFingers(e: MotionEvent) {
        if (!Settings.restRejection || mode != Mode.NONE || dragging) return
        val act = active(e)
        if (act.size < 2) return
        val movers = act.filter { dist(e, it) > tapSlop * 1.5f }
        val still = act.filter { dist(e, it) < tapSlop * 0.4f }
        if (movers.isEmpty() || still.isEmpty() || movers.size + still.size != act.size) return
        val moverDown = movers.maxOf { downAt[e.getPointerId(it)] ?: 0L }
        val rest = still.filter { abs((downAt[e.getPointerId(it)] ?: 0L) - moverDown) > 120 }
        if (rest.isEmpty()) return
        rest.forEach { ignored += e.getPointerId(it) }
        travelX = 0f; travelY = 0f
        maxPointers = active(e).size
        val left = active(e)
        if (left.size == 2) resetTwoFinger(e, left[0], left[1])
    }

    private fun onMove(e: MotionEvent) {
        rejectRestingFingers(e)
        val act = active(e)
        val n = act.size
        var sx = 0f; var sy = 0f; var counted = 0
        for (i in 0 until e.pointerCount) {
            val id = e.getPointerId(i)
            val p = last[id] ?: continue
            if (id in ignored) { p.set(e.getX(i), e.getY(i)); continue }
            sx += e.getX(i) - p.x; sy += e.getY(i) - p.y; counted++
            p.set(e.getX(i), e.getY(i))
            val s = start[id]
            if (s != null && hypot(e.getX(i) - s.x, e.getY(i) - s.y) > tapSlop) moved = true
        }
        if (counted == 0) return
        if (moved) removeCallbacks(longPress)
        val dx = sx / counted
        val dy = sy / counted
        val dt = max(1L, e.eventTime - lastEventTime)
        lastEventTime = e.eventTime
        travelX += dx; travelY += dy

        if (mode == Mode.DONE) return

        when {
            n == 1 -> oneFinger(dx, dy, dt)
            dragging -> {}
            n == 2 -> twoFingers(e, act[0], act[1], dx, dy)
            n == 3 && Settings.threeFingerDrag -> threeDrag(dx, dy, dt)
            n >= 3 -> multiSwipe(n, dx, dy)
        }
    }

    private fun oneFinger(dx: Float, dy: Float, dt: Long) {
        if (mode == Mode.NONE && moved) {
            mode = if (dragging) Mode.MOVE else when (edge) {
                Edge.RIGHT_STRIP ->
                    if (abs(travelY) >= abs(travelX)) Mode.EDGE_SCROLL_V
                    else if (travelX < 0 && startX > width - edgeZone &&
                        Settings.gesture(GestureSlot.EDGE_RIGHT) != GAction.NONE) Mode.EDGE_SWIPE
                    else Mode.MOVE
                Edge.BOTTOM_STRIP -> if (abs(travelX) > abs(travelY)) Mode.EDGE_SCROLL_H else Mode.MOVE
                Edge.RIGHT -> if (travelX < 0 && abs(travelX) > abs(travelY) &&
                    Settings.gesture(GestureSlot.EDGE_RIGHT) != GAction.NONE) Mode.EDGE_SWIPE else Mode.MOVE
                Edge.LEFT -> if (travelX > 0 && abs(travelX) > abs(travelY) &&
                    Settings.gesture(GestureSlot.EDGE_LEFT) != GAction.NONE) Mode.EDGE_SWIPE else Mode.MOVE
                Edge.TOP -> if (travelY > 0 && abs(travelY) > abs(travelX) &&
                    Settings.gesture(GestureSlot.EDGE_TOP) != GAction.NONE) Mode.EDGE_SWIPE else Mode.MOVE
                Edge.NONE -> Mode.MOVE
            }
            // Don't lose the slop distance when the pointer starts moving.
            if (mode == Mode.MOVE) { movePointer(travelX, travelY, dt); return }
        }
        when (mode) {
            Mode.MOVE -> movePointer(dx, dy, dt)
            Mode.EDGE_SCROLL_V -> scrollBy(0f, dy)
            Mode.EDGE_SCROLL_H -> scrollBy(dx, 0f)
            Mode.EDGE_SWIPE -> if (!fired && max(abs(travelX), abs(travelY)) > edgeSwipeDistance) {
                fired = true
                fire(when (edge) {
                    Edge.LEFT -> GestureSlot.EDGE_LEFT
                    Edge.TOP -> GestureSlot.EDGE_TOP
                    else -> GestureSlot.EDGE_RIGHT
                })
            }
            else -> {}
        }
    }

    private fun twoFingers(e: MotionEvent, a: Int, b: Int, dx: Float, dy: Float) {
        if (mode !in setOf(Mode.NONE, Mode.SCROLL, Mode.PINCH, Mode.NAV, Mode.ROTATE)) return
        val vx = e.getX(b) - e.getX(a); val vy = e.getY(b) - e.getY(a)
        val span = hypot(vx, vy)
        val angle = atan2(vy.toDouble(), vx.toDouble())

        if (mode == Mode.NONE) {
            val travel = hypot(travelX, travelY)
            val spanDelta = abs(span - span0)
            val rot = Math.toDegrees(wrap(angle - angle0))
            val rotateMapped = Settings.gesture(GestureSlot.ROTATE_CW) != GAction.NONE ||
                Settings.gesture(GestureSlot.ROTATE_CCW) != GAction.NONE
            when {
                rotateMapped && abs(rot) > 22 && spanDelta < span0 * 0.25f && travel < 40 * density -> {
                    mode = Mode.ROTATE; angle0 = angle; angleAcc = rot
                }
                spanDelta > pinchSlop && spanDelta > travel * 1.2f -> { mode = Mode.PINCH; pinchRef = span }
                travel > tapSlop -> {
                    axis = when {
                        !Settings.scrollRails -> Axis.FREE
                        abs(travelY) > abs(travelX) * 1.8f -> Axis.VERTICAL
                        abs(travelX) > abs(travelY) * 1.8f -> Axis.HORIZONTAL
                        else -> Axis.FREE
                    }
                    val horizontal = abs(travelX) > abs(travelY) * 1.8f
                    mode = if (horizontal && Settings.twoFingerNavigate) Mode.NAV else Mode.SCROLL
                    if (mode == Mode.SCROLL) scrollBy(travelX, travelY)
                    return
                }
            }
        }
        when (mode) {
            Mode.SCROLL -> scrollBy(dx, dy)
            Mode.PINCH -> pinch(span)
            Mode.NAV -> if (!fired && abs(travelX) > navDistance) {
                fired = true
                fire(if (travelX < 0) GestureSlot.TWO_LEFT else GestureSlot.TWO_RIGHT)
            }
            Mode.ROTATE -> {
                angleAcc += Math.toDegrees(wrap(angle - angle0)); angle0 = angle
                if (abs(angleAcc) >= rotateStep) {
                    fire(if (angleAcc > 0) GestureSlot.ROTATE_CW else GestureSlot.ROTATE_CCW)
                    angleAcc = 0.0
                }
            }
            else -> {}
        }
    }

    private fun threeDrag(dx: Float, dy: Float, dt: Long) {
        if (mode == Mode.NONE && hypot(travelX, travelY) > tapSlop) {
            mode = Mode.THREE_DRAG; threeDragging = true
            HidManager.setButton(BTN_LEFT, true)
            showLabel("Drag")
        }
        if (mode == Mode.THREE_DRAG) movePointer(dx, dy, dt)
    }

    private fun multiSwipe(n: Int, dx: Float, dy: Float) {
        if (mode != Mode.NONE && mode != Mode.SWIPE) return
        if (!fired && max(abs(travelX), abs(travelY)) > swipeDistance) {
            mode = Mode.SWIPE; fired = true
            val dir = if (abs(travelX) > abs(travelY)) {
                if (travelX < 0) Swipe.LEFT else Swipe.RIGHT
            } else {
                if (travelY < 0) Swipe.UP else Swipe.DOWN
            }
            fire(GestureSlot.swipe(n, dir))
        }
    }

    private fun onLastUp(e: MotionEvent) {
        removeCallbacks(longPress)
        val duration = e.eventTime - downTime
        if (threeDragging) {
            HidManager.setButton(BTN_LEFT, false); threeDragging = false
        } else if (dragging) {
            HidManager.setButton(BTN_LEFT, false)
            dragging = false
            lastTapUp = 0L
        } else if (!moved && mode == Mode.NONE && duration < tapMaxDuration * (if (maxPointers > 1) 1.4f else 1f)) {
            if (maxPointers == 1 && Settings.tapToClick || maxPointers > 1) ripple(e.x, e.y, maxPointers)
            when (maxPointers) {
                1 -> if (Settings.tapToClick) {
                    HidManager.click(BTN_LEFT); Haptics.tick()
                    lastTapUp = e.eventTime; lastTapPos.set(e.x, e.y)
                }
                2 -> fire(GestureSlot.TWO_TAP)
                3 -> fire(GestureSlot.THREE_TAP)
                else -> fire(GestureSlot.FOUR_TAP)
            }
        }
        if (Settings.momentum && mode in setOf(Mode.SCROLL, Mode.EDGE_SCROLL_V, Mode.EDGE_SCROLL_H)) {
            velocity?.let {
                it.computeCurrentVelocity(1) // px per ms
                startMomentum(
                    if (mode == Mode.EDGE_SCROLL_V || axis == Axis.VERTICAL) 0f else it.xVelocity,
                    if (mode == Mode.EDGE_SCROLL_H || axis == Axis.HORIZONTAL) 0f else it.yVelocity,
                )
            }
        }
        velocity?.recycle(); velocity = null
        last.clear(); start.clear(); ignored.clear(); downAt.clear()
        mode = Mode.NONE
    }

    private fun wrap(a: Double): Double {
        var r = a
        while (r > PI) r -= 2 * PI
        while (r < -PI) r += 2 * PI
        return r
    }

    // ------------------------------------------------------------------ output

    private fun movePointer(dxPx: Float, dyPx: Float, dtMs: Long) {
        val dx = dxPx / density
        val dy = dyPx / density
        val speed = hypot(dx, dy) / dtMs // dp per ms
        val accel = 1f + 1.4f * min(max(speed - 0.12f, 0f), 1.6f)
        val gain = 1.6f * Settings.pointerSpeed * accel
        HidManager.move(dx * gain, dy * gain)
    }

    private fun scrollBy(dxPx: Float, dyPx: Float) {
        var x = dxPx; var y = dyPx
        if (axis == Axis.VERTICAL) x = 0f
        if (axis == Axis.HORIZONTAL) y = 0f
        val divisor = if (HidManager.hiResWheel) 30f else 22f
        val speed = Settings.scrollSpeed
        val sdy = y / density / divisor * speed
        val sdx = x / density / divisor * speed
        // Natural: content follows the fingers (fingers down = scroll up = positive wheel).
        val sign = if (Settings.naturalScroll) 1f else -1f
        HidManager.scroll(sdy * sign, -sdx * sign)
    }

    private fun pinch(span: Float) {
        val ratio = span / pinchRef
        val step = 1.12f
        val zoomIn = ratio > step
        if (!zoomIn && ratio > 1f / step) return
        pinchRef = span
        val combo = os.zoomCombo(zoomIn)
        if (combo != null) HidManager.combo(combo)
        else HidManager.modifiedWheel(Keys.L_CTRL, if (zoomIn) 1 else -1)
        showLabel(if (zoomIn) "Zoom in" else "Zoom out")
        Haptics.tick()
    }

    private fun startMomentum(vx: Float, vy: Float) {
        if (hypot(vx, vy) / density < 0.45f) return
        momentumV.set(vx, vy)
        momentumLast = SystemClock.uptimeMillis()
        momentumRunning = true
        postOnAnimation(momentumStep)
    }

    private val momentumStep = object : Runnable {
        override fun run() {
            if (!momentumRunning) return
            val now = SystemClock.uptimeMillis()
            val dt = (now - momentumLast).coerceIn(1, 50)
            momentumLast = now
            scrollBy(momentumV.x * dt, momentumV.y * dt)
            val decay = exp(-dt / 325f)
            momentumV.set(momentumV.x * decay, momentumV.y * decay)
            if (hypot(momentumV.x, momentumV.y) / density < 0.03f) momentumRunning = false
            else postOnAnimation(this)
        }
    }

    private fun stopMomentum() {
        momentumRunning = false
        removeCallbacks(momentumStep)
    }

    private fun showLabel(s: String) {
        label = s; labelUntil = SystemClock.uptimeMillis() + 1000
        invalidate()
        postDelayed({ invalidate() }, 1050)
    }

    // ------------------------------------------------------------------ drawing

    /** Small pad next to the keyboard: no hint text. */
    var compact = false

    /** Full display mode: the pad is the whole screen, its corners follow the display's own. */
    var fullBleed = false
        set(v) { field = v; invalidate() }

    private fun cornerRadius(): Float {
        if (!fullBleed) return (if (compact) 20 else 32) * density
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            rootWindowInsets?.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_LEFT)?.let { return it.radius.toFloat() }
        }
        return 0f
    }

    // Tap ripples: a ring that grows and fades where a tap registered (bigger for multi-finger taps).
    private class Ripple(val x: Float, val y: Float, val start: Long, val size: Float)
    private val ripples = ArrayList<Ripple>()
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rippleMs = 420L

    private fun ripple(x: Float, y: Float, fingers: Int) {
        if (C.AOD) return
        ripples.add(Ripple(x, y, SystemClock.uptimeMillis(), (if (fingers > 1) 64f else 44f) * density))
        postInvalidateOnAnimation()
    }

    private fun drawRipples(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        ripples.removeAll { now - it.start > rippleMs }
        for (rp in ripples) {
            val t = (now - rp.start) / rippleMs.toFloat()
            val ease = 1f - (1f - t) * (1f - t) * (1f - t) // ease-out cubic
            ripplePaint.color = C.ACCENT
            ripplePaint.alpha = ((1f - t) * 160).toInt()
            ripplePaint.strokeWidth = (3f - 2f * t) * density
            c.drawCircle(rp.x, rp.y, rp.size * (0.35f + 0.65f * ease), ripplePaint)
        }
        if (ripples.isNotEmpty()) postInvalidateOnAnimation()
    }

    override fun onDraw(c: Canvas) {
        val r = cornerRadius()
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (C.AOD) rect.inset(density, density)
        c.drawRoundRect(rect, r, r, padPaint.fillOrOutline(C.SURFACE, density))

        if (!C.AOD) {
            dotPaint.color = C.SURFACE3
            val step = 28 * density
            var y = step
            while (y < height) {
                var x = step
                while (x < width) { c.drawCircle(x, y, 1.3f * density, dotPaint); x += step }
                y += step
            }
        }

        if (Settings.edgeScroll) {
            val w = 4 * density
            val active = mode == Mode.EDGE_SCROLL_V
            stripPaint.fillOrOutline(if (active) C.ACCENT else C.SURFACE3, density, active)
            rect.set(width - stripZone / 2 - w / 2, r, width - stripZone / 2 + w / 2, height - r - stripZone)
            c.drawRoundRect(rect, w, w, stripPaint)
            val hActive = mode == Mode.EDGE_SCROLL_H
            stripPaint.fillOrOutline(if (hActive) C.ACCENT else C.SURFACE3, density, hActive)
            rect.set(r, height - stripZone / 2 - w / 2, width - r - stripZone, height - stripZone / 2 + w / 2)
            c.drawRoundRect(rect, w, w, stripPaint)
        }

        fingerPaint.color = C.ACCENT
        for ((id, p) in last) {
            val resting = id in ignored
            if (C.AOD) {
                // Outline rings only, like an ambient watch face.
                fingerPaint.style = Paint.Style.STROKE; fingerPaint.strokeWidth = 1.2f * density
                fingerPaint.color = if (resting) C.AOD_LINE else C.ACCENT
                c.drawCircle(p.x, p.y, 30 * density, fingerPaint)
                fingerPaint.style = Paint.Style.FILL
                continue
            }
            fingerPaint.alpha = if (resting) 30 else 70
            c.drawCircle(p.x, p.y, 30 * density, fingerPaint)
            fingerPaint.alpha = if (resting) 90 else 255
            c.drawCircle(p.x, p.y, 9 * density, fingerPaint)
        }

        drawRipples(c)

        val l = label
        if (l != null && SystemClock.uptimeMillis() < labelUntil) {
            val w = labelPaint.measureText(l) + 40 * density
            val h = 48 * density
            rect.set(width / 2f - w / 2, height / 2f - h / 2, width / 2f + w / 2, height / 2f + h / 2)
            c.drawRoundRect(rect, h / 2, h / 2, labelBg.fillOrOutline(C.ACCENT_DIM, density))
            labelPaint.color = C.ON_ACCENT_DIM
            c.drawText(l, width / 2f, height / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2, labelPaint)
        } else if (last.isEmpty() && !compact) {
            hintPaint.color = C.MUTED
            val cx = width / 2f
            var ty = height / 2f - 30 * density
            val lines = listOf(
                "Tap to click · hold or tap-and-drag to select",
                "Two fingers: scroll, tap, pinch, rotate" + if (Settings.twoFingerNavigate) ", swipe sideways for back" else "",
                "Three / four fingers: ${os.label} gestures" + if (Settings.edgeScroll) " · edges scroll" else "",
            )
            val base = if (HidManager.ptpActive) listOf(
                "Native touchpad: the computer handles taps and gestures",
                "Use its own touchpad settings for speed, scrolling and gestures",
            ) else lines
            val all = if (fullBleed) base + "Tap the handle at the top for controls" else base
            for (line in all) { c.drawText(line, cx, ty, hintPaint); ty += 22 * density }
        }
    }

    /** Keep Android's back gesture from stealing swipes that start at the pad's side edges. */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val band = (200 * density).toInt().coerceAtMost(height)
            val t = (height - band) / 2
            val w = (40 * density).toInt()
            systemGestureExclusionRects = listOf(
                android.graphics.Rect(0, t, w, t + band),
                android.graphics.Rect(width - w, t, width, t + band),
            )
        }
    }

    override fun onDetachedFromWindow() {
        stopMomentum()
        cancel()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val ALL = -2
        private const val NONE_LIFTED = -1
        const val BTN_LEFT = 1
        const val BTN_RIGHT = 2
        const val BTN_MIDDLE = 4
    }
}

/** Physical-style click buttons under the pad. Separate view, so you can click with a thumb while dragging. */
class ClickBar(ctx: Context) : View(ctx) {
    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14 * density; textAlign = Paint.Align.CENTER }
    private val held = HashMap<Int, Int>() // pointer id -> button
    private val rect = RectF()
    private val path = android.graphics.Path()

    private fun buttonAt(x: Float) = if (x < width / 2f) TrackpadView.BTN_LEFT else TrackpadView.BTN_RIGHT

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val b = buttonAt(e.getX(i))
                held[e.getPointerId(i)] = b
                HidManager.setButton(b, true); Haptics.click()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                held.remove(e.getPointerId(e.actionIndex))?.let { b ->
                    if (!held.containsValue(b)) HidManager.setButton(b, false)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                held.values.toSet().forEach { HidManager.setButton(it, false) }; held.clear()
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(c: Canvas) {
        val gap = 4 * density
        val outer = 28 * density
        val inner = 8 * density
        val half = width / 2f
        for ((i, b) in listOf(TrackpadView.BTN_LEFT, TrackpadView.BTN_RIGHT).withIndex()) {
            val on = held.containsValue(b)
            val l = if (i == 0) 0f else half + gap / 2
            val rr = if (i == 0) half - gap / 2 else width.toFloat()
            rect.set(l, 0f, rr, height.toFloat())
            fill.fillOrOutline(if (on) C.ACCENT else C.SURFACE2, density, on)
            // Connected-button shape: round outside corners, tight inner corners (morph to round when pressed).
            val innerR = if (on) outer else inner
            val radii = if (i == 0) floatArrayOf(outer, outer, innerR, innerR, innerR, innerR, outer, outer)
            else floatArrayOf(innerR, innerR, outer, outer, outer, outer, innerR, innerR)
            path.reset(); path.addRoundRect(rect, radii, android.graphics.Path.Direction.CW)
            c.drawPath(path, fill)
            text.color = if (on) C.ON_ACCENT else C.TEXT
            c.drawText(if (i == 0) "Left" else "Right", rect.centerX(), rect.centerY() - (text.ascent() + text.descent()) / 2, text)
        }
    }
}
