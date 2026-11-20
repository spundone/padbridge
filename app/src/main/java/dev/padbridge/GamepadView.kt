package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/** Gamepad layouts for different kinds of games. */
enum class GamePreset(val label: String, val blurb: String, val defaultGyro: Int) {
    STANDARD("Standard", "Two sticks, D-pad, face buttons, bumpers and triggers", 0),
    RACING("Racing", "Tilt to steer, analog gas and brake pedals, gear buttons", 2),
    FIRST_PERSON("First person", "Move stick, drag-to-look surface with gyro aim, big Fire and Aim", 1),
    THIRD_PERSON("Third person", "Move stick, camera surface, full face buttons and shoulders", 0),
    RETRO("Retro", "Big D-pad and face buttons for platformers and emulators", 0);

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

/**
 * Landscape touch gamepad sending a standard HID gamepad report.
 *
 * Button bits follow the Xbox Wireless (Bluetooth) controller's HID order (bit = HID usage - 1;
 * bits 2 and 5 unused), which Linux maps to BTN_SOUTH/EAST/NORTH/WEST:
 *  0 A/Cross  1 B/Circle  3 X/Square  4 Y/Triangle  6 LB/L1  7 RB/R1  8 LT/L2  9 RT/R2
 *  10 View/Create  11 Menu/Options  12 Home/PS  13 L3  14 R3  15 Share/Touchpad
 *
 * A [GamePreset] decides which controls exist and where: analog pedals for racing, a drag-to-look
 * surface for shooters (mapped to the right stick like a trackball), floating move sticks, etc.
 */
class GamepadView(ctx: Context) : View(ctx) {

    enum class Style { XBOX, PLAYSTATION }

    var style = Style.XBOX
        set(v) { field = v; invalidate() }

    var preset = GamePreset.STANDARD
        set(v) { if (field != v) { field = v; releaseAll(); layoutControls(); invalidate() } }

    /** 0 off, 1 aim (phone turn drives the right stick), 2 steer (tilt drives the left stick). */
    var gyroMode = 0
        set(v) { if (field != v) { field = v; updateMotion(); send(); invalidate() } }

    private val motion = Motion(this) { m, _ -> onGyro(m) }
    private var shown = false
    private var gyroRx = 0f
    private var gyroRy = 0f
    private var steer = 0f
    private var sYaw = 0f
    private var sPitch = 0f
    private var lastGyroSend = 0L
    // What was last sent, so the sticks draw where the computer thinks they are.
    private var outLx = 0f; private var outLy = 0f; private var outRx = 0f; private var outRy = 0f

    private sealed class Control {
        abstract fun hit(x: Float, y: Float): Boolean
    }

    private class Button(val bit: Int, var cx: Float = 0f, var cy: Float = 0f, var r: Float = 0f) : Control() {
        val rect = RectF()
        var isRect = false
        /** Action name shown instead of the button's own label (e.g. "Jump" on A). */
        var caption: String? = null
        override fun hit(x: Float, y: Float) =
            if (isRect) rect.contains(x, y) else hypot(x - cx, y - cy) <= r * 1.25f
    }

    private class Stick(val thumbBit: Int) : Control() {
        val center = PointF(); var r = 0f
        val home = PointF()
        val value = PointF()
        var lastRelease = 0L
        var atEdge = false
        var engaged = false
        /** Floating: re-centers under the thumb anywhere inside [zone]. */
        var floating = false
        val zone = RectF()
        override fun hit(x: Float, y: Float) =
            if (floating) zone.contains(x, y) else hypot(x - center.x, y - center.y) <= r * 1.6f
    }

    private class DPad : Control() {
        val center = PointF(); var r = 0f
        var hat = HAT_NONE
        override fun hit(x: Float, y: Float) = hypot(x - center.x, y - center.y) <= r * 1.35f
    }

    /** Analog trigger: press further down the pedal for more. */
    private class Pedal(val name: String) : Control() {
        val rect = RectF()
        var value = 0f
        override fun hit(x: Float, y: Float) = rect.contains(x, y)
    }

    /** Drag-to-look surface: finger speed becomes right-stick deflection, like a trackball. */
    private class LookPad : Control() {
        val rect = RectF()
        val value = PointF()
        var lastX = 0f; var lastY = 0f; var lastT = 0L
        override fun hit(x: Float, y: Float) = rect.contains(x, y)
    }

    private val density = resources.displayMetrics.density

    private val btnA = Button(0); private val btnB = Button(1)
    private val btnX = Button(3); private val btnY = Button(4)
    private val lb = Button(6); private val rb = Button(7)
    private val lt = Button(8); private val rt = Button(9)
    private val view = Button(10); private val menu = Button(11)
    private val home = Button(12); private val share = Button(15)
    private val l3 = Button(13); private val r3 = Button(14)
    private val faceButtons = listOf(btnA, btnB, btnX, btnY)
    private val allButtons = listOf(btnA, btnB, btnX, btnY, lb, rb, lt, rt, view, menu, home, share, l3, r3)
    private val leftStick = Stick(13)
    private val rightStick = Stick(14)
    private val dpad = DPad()
    private val brake = Pedal("Brake")
    private val gas = Pedal("Gas")
    private val look = LookPad()

    // Active controls for the current preset (hit-test order: buttons, pedals, d-pad, sticks, look).
    private var buttons: List<Button> = emptyList()
    private var sticks: List<Stick> = emptyList()
    private var pedals: List<Pedal> = emptyList()
    private var dpadOn = true
    private var lookOn = false

    private val owner = HashMap<Int, Control>()
    private val pressed = HashSet<Button>()
    private var thumbBits = 0

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * density; color = C.STROKE
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val path = Path()
    private val tmp = RectF()

    init { isHapticFeedbackEnabled = false }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = layoutControls()

    // ------------------------------------------------------------------ layouts

    private fun layoutControls() {
        if (width == 0) return
        val W = width.toFloat()
        // Tablets: lay the controls out for a phone-sized area (about a big phone held sideways) at the bottom of
        // the screen, then shift them down. Sizes stay comfortable instead of growing with the screen, and the
        // sticks and buttons sit where thumbs rest on a tablet held at its lower corners.
        val cap = 440f * resources.displayMetrics.density
        val H = min(height.toFloat(), cap)
        val oy = height - H
        val u = min(W, H)
        allButtons.forEach { it.caption = null; it.isRect = false }
        leftStick.floating = false; rightStick.floating = false
        fun circle(b: Button, x: Float, y: Float, r: Float) { b.isRect = false; b.cx = W * x; b.cy = H * y; b.r = u * r }
        fun rect(b: Button, l: Float, t: Float, r: Float, bt: Float) { b.isRect = true; b.rect.set(W * l, H * t, W * r, H * bt) }
        fun stick(s: Stick, x: Float, y: Float, r: Float) { s.center.set(W * x, H * y); s.home.set(s.center); s.r = u * r }
        fun face(cx: Float, cy: Float, r: Float, off: Float) {
            circle(btnY, cx, cy, r); btnY.cy -= u * off
            circle(btnA, cx, cy, r); btnA.cy += u * off
            circle(btnX, cx, cy, r); btnX.cx -= u * off
            circle(btnB, cx, cy, r); btnB.cx += u * off
        }
        fun shoulders() {
            rect(lt, 0.02f, 0.03f, 0.22f, 0.15f); rect(lb, 0.02f, 0.17f, 0.22f, 0.27f)
            rect(rt, 0.78f, 0.03f, 0.98f, 0.15f); rect(rb, 0.78f, 0.17f, 0.98f, 0.27f)
        }
        fun center() { circle(view, 0.41f, 0.30f, 0.042f); circle(menu, 0.59f, 0.30f, 0.042f) }

        when (preset) {
            GamePreset.STANDARD -> {
                stick(leftStick, 0.15f, 0.55f, 0.16f); stick(rightStick, 0.66f, 0.78f, 0.145f)
                dpad.center.set(W * 0.33f, H * 0.78f); dpad.r = u * 0.136f
                face(0.86f, 0.52f, 0.055f, 0.115f)
                shoulders()
                circle(view, 0.41f, 0.47f, 0.042f); circle(menu, 0.59f, 0.47f, 0.042f); circle(home, 0.5f, 0.34f, 0.048f)
                rect(share, 0.44f, 0.56f, 0.56f, 0.64f)
                buttons = listOf(btnA, btnB, btnX, btnY, lb, rb, lt, rt, view, menu, home, share)
                sticks = listOf(leftStick, rightStick); pedals = emptyList(); dpadOn = true; lookOn = false
            }
            GamePreset.RACING -> {
                brake.rect.set(W * 0.03f, H * 0.30f, W * 0.19f, H * 0.95f)
                gas.rect.set(W * 0.81f, H * 0.30f, W * 0.97f, H * 0.95f)
                rect(lb, 0.21f, 0.74f, 0.35f, 0.95f); lb.caption = "Gear −"
                rect(rb, 0.65f, 0.74f, 0.79f, 0.95f); rb.caption = "Gear +"
                stick(leftStick, 0.30f, 0.45f, 0.11f)
                dpad.center.set(W * 0.70f, H * 0.45f); dpad.r = u * 0.10f
                face(0.5f, 0.66f, 0.045f, 0.09f)
                btnA.caption = "A"; btnB.caption = "B"
                center(); circle(home, 0.5f, 0.20f, 0.04f)
                buttons = listOf(btnA, btnB, btnX, btnY, lb, rb, view, menu, home)
                sticks = listOf(leftStick); pedals = listOf(brake, gas); dpadOn = true; lookOn = false
            }
            GamePreset.FIRST_PERSON, GamePreset.THIRD_PERSON -> {
                val fps = preset == GamePreset.FIRST_PERSON
                stick(leftStick, 0.17f, 0.66f, 0.15f)
                leftStick.floating = true; leftStick.zone.set(0f, H * 0.36f, W * 0.42f, H)
                // Third person keeps its shoulder buttons above the camera surface.
                look.rect.set(W * 0.45f, H * (if (fps) 0.22f else 0.30f), W, H)
                if (fps) {
                    circle(rt, 0.87f, 0.40f, 0.11f); rt.caption = "Fire"
                    circle(lt, 0.09f, 0.34f, 0.085f); lt.caption = "Aim"
                    circle(btnA, 0.92f, 0.80f, 0.075f); btnA.caption = "Jump"
                    circle(btnB, 0.79f, 0.88f, 0.068f); btnB.caption = "Crouch"
                    circle(btnX, 0.74f, 0.66f, 0.064f); btnX.caption = "Reload"
                    circle(btnY, 0.93f, 0.60f, 0.06f); btnY.caption = "Switch"
                    circle(rb, 0.71f, 0.36f, 0.06f); rb.caption = "Grenade"
                    circle(lb, 0.23f, 0.30f, 0.06f); lb.caption = "Tactical"
                    circle(l3, 0.36f, 0.58f, 0.06f); l3.caption = "Sprint"
                    circle(r3, 0.62f, 0.88f, 0.06f); r3.caption = "Melee"
                    buttons = listOf(rt, lt, btnA, btnB, btnX, btnY, rb, lb, l3, r3, view, menu)
                } else {
                    face(0.86f, 0.62f, 0.06f, 0.12f)
                    btnA.caption = "Jump"; btnB.caption = "Dodge"; btnX.caption = "Attack"; btnY.caption = "Heavy"
                    shoulders()
                    circle(l3, 0.36f, 0.58f, 0.06f); l3.caption = "Sprint"
                    circle(r3, 0.62f, 0.88f, 0.06f); r3.caption = "Lock on"
                    buttons = listOf(btnA, btnB, btnX, btnY, lb, rb, lt, rt, l3, r3, view, menu)
                }
                // View / Menu sit left of the look surface so they never cover it.
                circle(view, 0.30f, 0.30f, 0.042f); circle(menu, 0.38f, 0.30f, 0.042f)
                dpad.center.set(W * 0.47f, H * 0.62f); dpad.r = u * 0.085f
                sticks = listOf(leftStick); pedals = emptyList(); dpadOn = true; lookOn = true
            }
            GamePreset.RETRO -> {
                dpad.center.set(W * 0.22f, H * 0.60f); dpad.r = u * 0.30f
                face(0.80f, 0.60f, 0.10f, 0.17f)
                rect(lb, 0.02f, 0.05f, 0.24f, 0.20f); rect(rb, 0.76f, 0.05f, 0.98f, 0.20f)
                circle(view, 0.43f, 0.42f, 0.045f); circle(menu, 0.57f, 0.42f, 0.045f)
                buttons = listOf(btnA, btnB, btnX, btnY, lb, rb, view, menu)
                sticks = emptyList(); pedals = emptyList(); dpadOn = true; lookOn = false
            }
        }
        if (oy > 0f) {
            allButtons.forEach { it.cy += oy; it.rect.offset(0f, oy) }
            for (st in listOf(leftStick, rightStick)) {
                st.center.offset(0f, oy); st.home.offset(0f, oy)
                st.zone.bottom += oy // floating zones reach from where they started down to the bottom
            }
            dpad.center.offset(0f, oy)
            brake.rect.offset(0f, oy); gas.rect.offset(0f, oy)
            look.rect.bottom += oy // the look surface also covers the space above
        }
    }

    // ------------------------------------------------------------------ input

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val i = e.actionIndex
                val x = e.getX(i); val y = e.getY(i)
                val c = controlAt(x, y)
                if (c != null && owner.values.none { it === c && (it is Stick || it is LookPad || it is Pedal) }) {
                    owner[e.getPointerId(i)] = c
                    when (c) {
                        is Button -> { pressed += c; Haptics.click() }
                        is Stick -> {
                            if (c.floating) {
                                // Re-center under the thumb, kept fully on screen.
                                c.center.set(x.coerceIn(c.r, width - c.r), y.coerceIn(c.r, height - c.r))
                            } else if (e.eventTime - c.lastRelease < 280) {
                                thumbBits = thumbBits or (1 shl c.thumbBit); Haptics.heavy()
                            }
                            updateStick(c, x, y)
                        }
                        is DPad -> updateDpad(x, y)
                        is Pedal -> { updatePedal(c, y); Haptics.tick() }
                        is LookPad -> { c.lastX = x; c.lastY = y; c.lastT = e.eventTime }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val c = owner[e.getPointerId(i)] ?: continue
                    val x = e.getX(i); val y = e.getY(i)
                    when (c) {
                        is Stick -> updateStick(c, x, y)
                        is DPad -> updateDpad(x, y)
                        is Button -> if (c in faceButtons && preset == GamePreset.STANDARD) slideFace(e.getPointerId(i), c, x, y)
                        is Pedal -> updatePedal(c, y)
                        is LookPad -> updateLook(c, x, y, e.eventTime)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val ids = if (e.actionMasked == MotionEvent.ACTION_CANCEL) owner.keys.toList()
                else listOf(e.getPointerId(e.actionIndex))
                for (id in ids) {
                    when (val c = owner.remove(id)) {
                        is Button -> if (owner.values.none { it === c }) pressed -= c
                        is Stick -> {
                            c.value.set(0f, 0f); c.atEdge = false; c.engaged = false
                            c.lastRelease = e.eventTime
                            if (c.floating) c.center.set(c.home)
                            thumbBits = thumbBits and (1 shl c.thumbBit).inv()
                        }
                        is DPad -> dpad.hat = HAT_NONE
                        is Pedal -> c.value = 0f
                        is LookPad -> { c.value.set(0f, 0f); removeCallbacks(lookDecay) }
                        null -> {}
                    }
                }
            }
        }
        send()
        invalidate()
        return true
    }

    private fun controlAt(x: Float, y: Float): Control? {
        buttons.firstOrNull { it.hit(x, y) }?.let { return it }
        pedals.firstOrNull { it.hit(x, y) }?.let { return it }
        if (dpadOn && dpad.hit(x, y)) return dpad
        sticks.firstOrNull { it.hit(x, y) }?.let { return it }
        if (lookOn && look.hit(x, y)) return look
        // Generous fallback: the left side steers the left stick, so thumbs can land anywhere.
        if (leftStick in sticks && x < width * 0.28f && y > height * 0.3f) return leftStick
        return null
    }

    /** Sliding a thumb across the face buttons presses the one under it (rolling A→B etc.). */
    private fun slideFace(id: Int, current: Button, x: Float, y: Float) {
        val next = faceButtons.firstOrNull { hypot(x - it.cx, y - it.cy) <= it.r } ?: return
        if (next === current) return
        owner[id] = next
        if (owner.values.none { it === current }) pressed -= current
        pressed += next
        Haptics.tick()
    }

    private fun updateStick(s: Stick, x: Float, y: Float) {
        var dx = (x - s.center.x) / s.r
        var dy = (y - s.center.y) / s.r
        val mag = hypot(dx, dy)
        if (mag > 1f) { dx /= mag; dy /= mag }
        if (mag < 0.06f) { dx = 0f; dy = 0f }
        s.value.set(dx, dy)
        val edge = mag >= 0.98f
        if (edge && !s.atEdge) Haptics.tick()
        s.atEdge = edge
        // A faint tick as the stick leaves its center, so you feel it engage.
        val out = mag > 0.25f
        if (out && !s.engaged) Haptics.soft(0.25f)
        s.engaged = out
    }

    private fun updateDpad(x: Float, y: Float) {
        val dx = x - dpad.center.x; val dy = y - dpad.center.y
        val newHat = if (hypot(dx, dy) < dpad.r * 0.22f) HAT_NONE else {
            // 0 = up, clockwise in 45° steps
            val deg = (Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())) + 360) % 360
            ((deg / 45.0).roundToInt()) % 8
        }
        if (newHat != dpad.hat) { dpad.hat = newHat; if (newHat != HAT_NONE) Haptics.tick() }
    }

    /** Top 15% of the pedal is a light touch, the bottom is full press. */
    private fun updatePedal(p: Pedal, y: Float) {
        val v = ((y - p.rect.top) / p.rect.height()).coerceIn(0f, 1f)
        val nv = 0.15f + 0.85f * v
        if (nv >= 0.99f && p.value < 0.99f) Haptics.tick()
        else if (nv >= 0.5f && p.value < 0.5f) Haptics.soft(0.3f) // halfway detent
        p.value = nv
    }

    private val lookDecay = Runnable { look.value.set(0f, 0f); send(); invalidate() }

    /** Finger speed (dp/ms) -> right-stick deflection, past the game's dead zone; stops when the finger rests. */
    private fun updateLook(p: LookPad, x: Float, y: Float, t: Long) {
        val dt = (t - p.lastT).coerceAtLeast(1L).toFloat()
        val vx = (x - p.lastX) / density / dt
        val vy = (y - p.lastY) / density / dt
        p.lastX = x; p.lastY = y; p.lastT = t
        val sens = Settings.gamepadGyroSens
        fun axis(v: Float): Float {
            val a = abs(v)
            if (a < 0.01f) return 0f
            return Math.copySign((0.18f + 0.82f * (a * 1.6f * sens)).coerceAtMost(1f), v)
        }
        p.value.set(axis(vx), axis(vy))
        removeCallbacks(lookDecay)
        postDelayed(lookDecay, 60)
    }

    private fun send() {
        var bits = thumbBits
        for (b in pressed) bits = bits or (1 shl b.bit)
        // Gyro adds to the touch sticks, so a thumb can still correct.
        outLx = (leftStick.value.x + steer).coerceIn(-1f, 1f); outLy = leftStick.value.y
        val rx = if (lookOn) look.value.x else rightStick.value.x
        val ry = if (lookOn) look.value.y else rightStick.value.y
        outRx = (rx + gyroRx).coerceIn(-1f, 1f)
        outRy = (ry + gyroRy).coerceIn(-1f, 1f)
        val ltV = maxOf(if (lt in pressed) 1f else 0f, if (brake in pedals) brake.value else 0f)
        val rtV = maxOf(if (rt in pressed) 1f else 0f, if (gas in pedals) gas.value else 0f)
        if (ltV > 0f) bits = bits or (1 shl lt.bit)
        if (rtV > 0f) bits = bits or (1 shl rt.bit)
        HidManager.gamepad(bits, dpad.hat, outLx, outLy, outRx, outRy, ltV, rtV)
    }

    // ------------------------------------------------------------------ gyro

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        shown = isVisible
        updateMotion()
    }

    private fun updateMotion() {
        if (shown && gyroMode != 0) motion.start() else {
            motion.stop()
            gyroRx = 0f; gyroRy = 0f; steer = 0f; sYaw = 0f; sPitch = 0f
        }
    }

    private fun onGyro(m: Motion) {
        val sens = Settings.gamepadGyroSens
        when (gyroMode) {
            1 -> {
                sYaw += (m.yaw - sYaw) * 0.4f
                sPitch += (m.pitch - sPitch) * 0.4f
                val rate = hypot(sYaw, sPitch)
                if (rate < 0.035f) { gyroRx = 0f; gyroRy = 0f } else {
                    // Rate -> deflection, with an outer boost past the game's own stick dead zone (~0.15-0.2).
                    val v = ((rate - 0.035f) / 2.4f * sens).coerceIn(0f, 1f)
                    val out = 0.18f + 0.82f * v
                    gyroRx = -sYaw / rate * out
                    gyroRy = -sPitch / rate * out
                }
                steer = 0f
            }
            2 -> {
                // Steering wheel: rotate the phone clockwise to steer right. Full lock at 40° (sensitivity 1).
                val deg = Math.toDegrees(asin((-m.gx).coerceIn(-1f, 1f).toDouble())).toFloat()
                val maxDeg = 40f / sens
                val a = abs(deg)
                steer = if (a < 2f) 0f else Math.copySign(0.12f + 0.88f * ((a - 2f) / (maxDeg - 2f)).coerceIn(0f, 1f), deg)
                gyroRx = 0f; gyroRy = 0f
            }
            else -> { gyroRx = 0f; gyroRy = 0f; steer = 0f }
        }
        // The Bluetooth link runs at ~90 reports/s; don't flood it at the sensor's 200 Hz.
        val now = System.nanoTime()
        if (now - lastGyroSend >= 9_000_000L) {
            lastGyroSend = now
            send()
            postInvalidateOnAnimation()
        }
    }

    fun releaseAll() {
        owner.clear(); pressed.clear(); thumbBits = 0
        leftStick.value.set(0f, 0f); rightStick.value.set(0f, 0f); dpad.hat = HAT_NONE
        leftStick.center.set(leftStick.home); brake.value = 0f; gas.value = 0f; look.value.set(0f, 0f)
        send(); invalidate()
    }

    override fun onDetachedFromWindow() { motion.stop(); releaseAll(); super.onDetachedFromWindow() }

    // ------------------------------------------------------------------ drawing

    private fun faceColor(b: Button): Int = if (style == Style.XBOX) when (b) {
        btnA -> Color.parseColor("#5DBB63"); btnB -> Color.parseColor("#E5534B")
        btnX -> Color.parseColor("#4C8DFF"); else -> Color.parseColor("#F2C94C")
    } else when (b) {
        btnA -> Color.parseColor("#7DA7FF"); btnB -> Color.parseColor("#FF7A85")
        btnX -> Color.parseColor("#F08BD8"); else -> Color.parseColor("#4FD1B5")
    }

    private fun faceLabel(b: Button) = if (style == Style.XBOX) when (b) {
        btnA -> "A"; btnB -> "B"; btnX -> "X"; else -> "Y"
    } else when (b) {
        btnA -> "✕"; btnB -> "○"; btnX -> "□"; else -> "△"
    }

    private fun name(b: Button) = when (b) {
        lb -> if (style == Style.XBOX) "LB" else "L1"
        rb -> if (style == Style.XBOX) "RB" else "R1"
        lt -> if (style == Style.XBOX) "LT" else "L2"
        rt -> if (style == Style.XBOX) "RT" else "R2"
        view -> if (style == Style.XBOX) "⧉" else "Create"
        menu -> if (style == Style.XBOX) "≡" else "Options"
        home -> if (style == Style.XBOX) "Home" else "PS"
        share -> if (style == Style.XBOX) "Share" else "Touchpad"
        l3 -> "L3"
        r3 -> "R3"
        else -> ""
    }

    private val glyph = android.graphics.Path()
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    /** Outline house: roof and walls, [s] is half the width. */
    private fun drawHomeGlyph(c: Canvas, x: Float, y: Float, s: Float, color: Int) {
        glyph.reset()
        glyph.moveTo(x - s, y - s * 0.05f)
        glyph.lineTo(x, y - s * 0.95f)
        glyph.lineTo(x + s, y - s * 0.05f)
        glyph.moveTo(x - s * 0.72f, y - s * 0.3f)
        glyph.lineTo(x - s * 0.72f, y + s * 0.85f)
        glyph.lineTo(x + s * 0.72f, y + s * 0.85f)
        glyph.lineTo(x + s * 0.72f, y - s * 0.3f)
        glyphPaint.color = color
        glyphPaint.strokeWidth = 2.2f * density
        c.drawPath(glyph, glyphPaint)
    }

    private fun drawCentered(c: Canvas, text: String, x: Float, y: Float) =
        c.drawText(text, x, y - (label.ascent() + label.descent()) / 2, label)

    override fun onDraw(c: Canvas) {
        c.drawColor(C.BG)
        stroke.color = C.STROKE
        val u = min(width, height).toFloat()

        // Look surface
        if (lookOn) {
            val r = 28 * density
            fill.color = C.SURFACE
            c.drawRoundRect(look.rect, r, r, fill)
            label.textSize = u * 0.04f; label.color = C.MUTED
            drawCentered(c, if (gyroMode == 1) "Drag to look · gyro aim on" else "Drag to look", look.rect.centerX() - look.rect.width() * 0.12f, look.rect.top + u * 0.07f)
        }

        // Pedals
        for (p in pedals) {
            val r = 24 * density
            fill.color = C.SURFACE2
            c.drawRoundRect(p.rect, r, r, fill)
            if (p.value > 0f) {
                tmp.set(p.rect.left, p.rect.top + p.rect.height() * (1f - p.value), p.rect.right, p.rect.bottom)
                fill.color = C.ACCENT
                c.save(); c.clipRect(tmp); c.drawRoundRect(p.rect, r, r, fill); c.restore()
            }
            label.textSize = u * 0.05f; label.color = if (p.value > 0.5f) C.ON_ACCENT else C.TEXT
            drawCentered(c, p.name, p.rect.centerX(), p.rect.top + u * 0.08f)
            label.textSize = u * 0.035f; label.color = if (p.value > 0.15f) C.ON_ACCENT else C.MUTED
            drawCentered(c, if (p === gas) (if (style == Style.XBOX) "RT" else "R2") else (if (style == Style.XBOX) "LT" else "L2"),
                p.rect.centerX(), p.rect.bottom - u * 0.06f)
        }

        // Sticks
        for (s in sticks) {
            val gyroOn = (s === leftStick && gyroMode == 2) || (s === rightStick && gyroMode == 1)
            fill.color = C.SURFACE; c.drawCircle(s.center.x, s.center.y, s.r, fill)
            stroke.color = if (gyroOn) C.ACCENT else C.STROKE
            c.drawCircle(s.center.x, s.center.y, s.r, stroke)
            stroke.color = C.STROKE
            val thumbDown = thumbBits and (1 shl s.thumbBit) != 0
            fill.color = if (thumbDown) C.ACCENT else C.SURFACE3
            val vx = if (s === leftStick) outLx else outRx
            val vy = if (s === leftStick) outLy else outRy
            c.drawCircle(s.center.x + vx * s.r * 0.6f, s.center.y + vy * s.r * 0.6f, s.r * 0.45f, fill)
        }

        // D-pad
        if (dpadOn) {
            val d = dpad.r; val arm = d * 0.36f
            for (dir in 0 until 4) {
                c.save()
                c.rotate(dir * 90f, dpad.center.x, dpad.center.y)
                tmp.set(dpad.center.x - arm, dpad.center.y - d, dpad.center.x + arm, dpad.center.y - arm * 0.6f)
                val active = dpad.hat != HAT_NONE && hatIncludes(dpad.hat, dir * 2)
                fill.color = if (active) C.ACCENT else C.SURFACE2
                c.drawRoundRect(tmp, 8 * density, 8 * density, fill)
                path.reset()
                path.moveTo(dpad.center.x, dpad.center.y - d * 0.85f)
                path.lineTo(dpad.center.x - arm * 0.4f, dpad.center.y - d * 0.6f)
                path.lineTo(dpad.center.x + arm * 0.4f, dpad.center.y - d * 0.6f)
                path.close()
                fill.color = C.MUTED; c.drawPath(path, fill)
                c.restore()
            }
        }

        // Buttons
        for (b in buttons) {
            val on = b in pressed
            val isFace = b in faceButtons
            val caption = b.caption
            if (b.isRect) {
                fill.color = if (on) C.ACCENT else C.SURFACE2
                val rr = if (on) b.rect.height() / 2 else 16 * density
                c.drawRoundRect(b.rect, rr, rr, fill)
                label.textSize = u * 0.045f; label.color = if (on) C.ON_ACCENT else C.TEXT
                drawCentered(c, caption ?: name(b), b.rect.centerX(), b.rect.centerY())
                continue
            }
            val col = if (isFace) faceColor(b) else C.ACCENT
            fill.color = when {
                on && isFace -> col
                on -> C.ACCENT
                else -> C.SURFACE2
            }
            c.drawCircle(b.cx, b.cy, b.r, fill)
            if (isFace && caption != null && !on) {
                // Ring in the face color so action buttons stay recognisable as A/B/X/Y.
                stroke.color = col; c.drawCircle(b.cx, b.cy, b.r - stroke.strokeWidth, stroke); stroke.color = C.STROKE
            }
            if (b === home && caption == null && style == Style.XBOX) {
                // Xbox Guide: a home glyph, since an "X" label read as a second X face button.
                drawHomeGlyph(c, b.cx, b.cy, b.r * 0.5f, if (on) C.ON_ACCENT else C.TEXT)
                continue
            }
            val text = caption ?: if (isFace) faceLabel(b) else name(b)
            label.color = when {
                on && isFace -> C.BG
                on -> C.ON_ACCENT
                isFace && caption == null -> col
                else -> C.TEXT
            }
            label.textSize = when {
                caption == null && isFace -> b.r * 1.1f
                text.length <= 2 -> b.r * 0.8f
                else -> (b.r * 0.42f).coerceAtMost(u * 0.04f)
            }
            drawCentered(c, text, b.cx, b.cy)
        }

        label.textSize = 11 * density; label.color = C.MUTED; label.isFakeBoldText = false
        val hint = when {
            preset == GamePreset.RACING && gyroMode == 2 -> "Tilt the phone like a wheel to steer · press down the pedals for more gas or brake"
            preset == GamePreset.RACING -> "Turn on Gyro steer to steer by tilting · press down the pedals for more gas or brake"
            lookOn -> "Left side: move stick appears under your thumb · right side: drag to look"
            gyroMode == 1 -> "Gyro aim: turn the phone to move the right stick · double-tap a stick for L3 / R3"
            gyroMode == 2 -> "Gyro steer: tilt the phone like a wheel · double-tap a stick for L3 / R3"
            sticks.isEmpty() -> ""
            else -> "Double-tap a stick for L3 / R3"
        }
        if (hint.isNotEmpty()) c.drawText(hint, width / 2f, height - 10 * density, label)
        label.isFakeBoldText = true
    }

    private fun hatIncludes(hat: Int, dir: Int): Boolean {
        val diff = ((hat - dir) % 8 + 8) % 8
        return diff == 0 || diff == 1 || diff == 7
    }

    companion object { const val HAT_NONE = 8 }
}
