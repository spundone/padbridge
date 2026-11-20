package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * Round TV-remote clickpad. Wedges send arrow keys, held while pressed (the host repeats them);
 * the center sends OK on tap. Swiping across the center works like a Siri Remote / touch remote:
 * every ~48 dp of travel sends one arrow in that direction.
 */
class DpadView(ctx: Context) : View(ctx) {

    enum class Dir(val code: Int, val angle: Float) {
        UP(Keys.UP, 270f), RIGHT(Keys.RIGHT, 0f), DOWN(Keys.DOWN, 90f), LEFT(Keys.LEFT, 180f)
    }

    var onSelect: () -> Unit = { RemoteSender.tap(RemoteOut.Key(Keys.ENTER)) }

    private val density = resources.displayMetrics.density
    private val swipeStep = 48 * density
    private var cx = 0f
    private var cy = 0f
    private var outerR = 0f
    private var innerR = 0f

    private var held: Dir? = null
    private var centerDown = false
    private var swiping = false
    private var startX = 0f
    private var startY = 0f
    private var accX = 0f
    private var accY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var flash: Dir? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val oval = RectF()
    private val innerOval = RectF()

    init { isHapticFeedbackEnabled = false }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f; cy = h / 2f
        outerR = min(w, h) / 2f - 4 * density
        innerR = outerR * 0.42f
        oval.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
        innerOval.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
    }

    private fun dirAt(x: Float, y: Float): Dir? {
        val d = hypot(x - cx, y - cy)
        if (d < innerR || d > outerR * 1.25f) return null
        val a = ((Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())) + 360) % 360).toFloat()
        return when {
            a >= 315 || a < 45 -> Dir.RIGHT
            a < 135 -> Dir.DOWN
            a < 225 -> Dir.LEFT
            else -> Dir.UP
        }
    }

    private fun press(d: Dir) {
        held = d
        RemoteSender.down(RemoteOut.Key(d.code))
        Haptics.click()
    }

    private fun releaseHeld() {
        held?.let { RemoteSender.up(RemoteOut.Key(it.code)) }
        held = null
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                startX = e.x; startY = e.y; lastX = e.x; lastY = e.y; accX = 0f; accY = 0f
                swiping = false
                val d = dirAt(e.x, e.y)
                if (d != null) press(d) else if (hypot(e.x - cx, e.y - cy) < innerR) { centerDown = true; Haptics.tick() }
            }
            MotionEvent.ACTION_MOVE -> {
                if (held != null) {
                    // Slide between wedges without lifting.
                    val d = dirAt(e.x, e.y)
                    if (d != null && d != held) { releaseHeld(); press(d) }
                } else if (centerDown) {
                    accX += e.x - lastX; accY += e.y - lastY
                    if (!swiping && hypot(e.x - startX, e.y - startY) > 16 * density) swiping = true
                    if (swiping) {
                        while (abs(accX) >= swipeStep || abs(accY) >= swipeStep) {
                            val d = if (abs(accX) >= abs(accY)) {
                                val r = if (accX > 0) Dir.RIGHT else Dir.LEFT
                                accX -= if (accX > 0) swipeStep else -swipeStep; r
                            } else {
                                val r = if (accY > 0) Dir.DOWN else Dir.UP
                                accY -= if (accY > 0) swipeStep else -swipeStep; r
                            }
                            RemoteSender.tap(RemoteOut.Key(d.code))
                            flash = d; Haptics.soft(0.45f) // a light detent per step, like the keyboard's
                            postDelayed({ flash = null; invalidate() }, 120)
                        }
                    }
                }
                lastX = e.x; lastY = e.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (held != null) releaseHeld()
                else if (centerDown && !swiping && e.actionMasked == MotionEvent.ACTION_UP) { onSelect(); Haptics.click() }
                centerDown = false; swiping = false
            }
        }
        invalidate()
        return true
    }

    override fun onDetachedFromWindow() { releaseHeld(); super.onDetachedFromWindow() }

    /**
     * Siri Remote-style clickpad: one flat disc, a hairline around the center, four small dots for the
     * directions. A pressed direction lights a soft wedge; a swipe shows a glow under the thumb.
     */
    override fun onDraw(c: Canvas) {
        if (Real.active) Real.disc(c, cx, cy, outerR, density)
        else {
            fill.fillOrOutline(C.SURFACE2, density)
            c.drawCircle(cx, cy, outerR, fill)
        }

        for (d in Dir.entries) {
            val on = held == d || flash == d
            if (on) {
                path.reset()
                path.arcTo(oval, d.angle - 45f, 90f)
                path.arcTo(innerOval, d.angle + 45f, -90f)
                path.close()
                if (C.AOD) fill.fillOrOutline(0, density, true)
                else { fill.style = Paint.Style.FILL; fill.color = C.SURFACE3 }
                c.drawPath(path, fill)
            }
            val rad = Math.toRadians(d.angle.toDouble())
            val r = (outerR + innerR) / 2f
            fill.style = Paint.Style.FILL
            fill.color = if (on) C.TEXT else C.MUTED
            c.drawCircle(cx + (Math.cos(rad) * r).toFloat(), cy + (Math.sin(rad) * r).toFloat(), (if (on) 4f else 3f) * density, fill)
        }

        // Center: same surface, set off by a hairline; brightens while pressed.
        val centerOn = centerDown && !swiping
        if (centerOn) {
            fill.fillOrOutline(C.SURFACE3, density, true)
            c.drawCircle(cx, cy, innerR, fill)
        }
        line.color = if (C.AOD) C.AOD_LINE else C.STROKE
        line.strokeWidth = density
        c.drawCircle(cx, cy, innerR, line)

        // Swipe glow under the thumb, kept inside the disc.
        if (swiping && !C.AOD) {
            val gx = lastX; val gy = lastY
            val dist = hypot(gx - cx, gy - cy)
            val k = if (dist > outerR - 24 * density) (outerR - 24 * density) / dist else 1f
            fill.style = Paint.Style.FILL
            fill.color = C.TEXT
            fill.alpha = 28
            c.drawCircle(cx + (gx - cx) * k, cy + (gy - cy) * k, 28 * density, fill)
            fill.alpha = 255
        }
    }
}
