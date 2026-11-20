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

/**
 * Google TV app-style swipe pad: drag anywhere to move the TV's focus (one arrow key per step of travel),
 * tap to select, press and hold to long-press OK (Android TV's context menus).
 */
class SwipePadView(ctx: Context) : View(ctx) {

    private val density = resources.displayMetrics.density
    private val step = 40 * density
    private val slop = 10 * density

    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var accX = 0f
    private var accY = 0f
    private var down = false
    private var moved = false
    private var holding = false
    private var flash = -1 // 0 up, 1 right, 2 down, 3 left
    private val rect = RectF()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val chevron = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    private val hold = Runnable {
        if (down && !moved) {
            holding = true
            RemoteSender.down(RemoteOut.Key(Keys.ENTER))
            Haptics.heavy(); invalidate()
        }
    }
    private val unflash = Runnable { flash = -1; invalidate() }

    init { isHapticFeedbackEnabled = false }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                startX = e.x; startY = e.y; lastX = e.x; lastY = e.y; accX = 0f; accY = 0f
                down = true; moved = false; holding = false
                postDelayed(hold, 550)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!holding) {
                    accX += e.x - lastX; accY += e.y - lastY
                    if (!moved && hypot(e.x - startX, e.y - startY) > slop) { moved = true; removeCallbacks(hold) }
                    if (moved) while (abs(accX) >= step || abs(accY) >= step) {
                        val dir: Int
                        if (abs(accX) >= abs(accY)) {
                            dir = if (accX > 0) 1 else 3
                            accX -= if (accX > 0) step else -step
                            accY = 0f // stay on one axis per step, like the Google TV app
                        } else {
                            dir = if (accY > 0) 2 else 0
                            accY -= if (accY > 0) step else -step
                            accX = 0f
                        }
                        RemoteSender.tap(RemoteOut.Key(CODES[dir]))
                        flash = dir; Haptics.soft(0.45f)
                        removeCallbacks(unflash); postDelayed(unflash, 140)
                    }
                }
                lastX = e.x; lastY = e.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(hold)
                if (holding) RemoteSender.up(RemoteOut.Key(Keys.ENTER))
                else if (!moved && e.actionMasked == MotionEvent.ACTION_UP) {
                    RemoteSender.tap(RemoteOut.Key(Keys.ENTER)); Haptics.click()
                }
                down = false; holding = false; moved = false
            }
        }
        invalidate()
        return true
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hold)
        if (holding) RemoteSender.up(RemoteOut.Key(Keys.ENTER))
        holding = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val r = 28 * density
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (Real.active) Real.trackpad(c, rect, r, density)
        else {
            fill.fillOrOutline(if (holding) C.SURFACE3 else C.SURFACE2, density, holding)
            c.drawRoundRect(rect, r, r, fill)
        }

        // Chevrons at the edges; the one in the direction just sent lights up.
        val cx = width / 2f; val cy = height / 2f
        val s = 9 * density
        val inset = 26 * density
        chevron.strokeWidth = 2.5f * density
        for (d in 0..3) {
            chevron.color = if (d == flash) C.ACCENT else C.MUTED
            chevron.alpha = if (d == flash) 255 else 110
            path.reset()
            when (d) {
                0 -> { path.moveTo(cx - s, inset + s); path.lineTo(cx, inset); path.lineTo(cx + s, inset + s) }
                1 -> { val x = width - inset; path.moveTo(x - s, cy - s); path.lineTo(x, cy); path.lineTo(x - s, cy + s) }
                2 -> { val y = height - inset; path.moveTo(cx - s, y - s); path.lineTo(cx, y); path.lineTo(cx + s, y - s) }
                else -> { path.moveTo(inset + s, cy - s); path.lineTo(inset, cy); path.lineTo(inset + s, cy + s) }
            }
            c.drawPath(path, chevron)
        }

        if (!down) {
            text.color = C.MUTED
            text.textSize = 15 * density
            c.drawText("Swipe to move · tap to select", cx, cy - 4 * density, text)
            text.textSize = 12 * density
            c.drawText("Hold for a long press", cx, cy + 16 * density, text)
        } else if (!C.AOD) {
            fill.style = Paint.Style.FILL
            fill.color = C.TEXT
            fill.alpha = 30
            c.drawCircle(lastX, lastY, 36 * density, fill)
            fill.alpha = 255
        }
    }

    private companion object {
        val CODES = intArrayOf(Keys.UP, Keys.RIGHT, Keys.DOWN, Keys.LEFT)
    }
}
