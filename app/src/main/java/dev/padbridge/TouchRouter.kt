package dev.padbridge

import android.view.MotionEvent
import android.view.View
import java.lang.ref.WeakReference

/** A touch view whose fingers [TouchRouter] can hand over. */
interface Routable {
    /** The view's own touch handling, with only the fingers that landed on it. */
    fun handleTouch(e: MotionEvent): Boolean
}

/**
 * Splits multi-touch between PadBridge's touch views (keyboard, trackpad, click buttons), like a ViewGroup that
 * splits motion events.
 *
 * Compose sends every finger of a gesture to the embedded view the first finger landed on, and nothing to the
 * others. Without this, a palm resting on the laptop deck's trackpad swallowed every key you typed (and turned each
 * one into a second trackpad finger), and a thumb on a click button stole the finger moving on the trackpad.
 * The view that gets the gesture (the owner) hands each finger to the registered view it landed on.
 */
object TouchRouter {
    private val views = ArrayList<WeakReference<View>>()
    private var owner: View? = null
    /** Pointer id -> the view it landed on, or null when it landed on none of them (ignored). */
    private val assigned = HashMap<Int, View?>()
    private val loc = IntArray(2)
    private val tloc = IntArray(2)

    fun register(v: View) {
        views.removeAll { it.get() == null || it.get() === v }
        views += WeakReference(v)
    }

    fun unregister(v: View) {
        views.removeAll { it.get() == null || it.get() === v }
        if (owner === v) { owner = null; assigned.clear() }
    }

    /** Call from [View.onTouchEvent] of a [Routable] view. */
    fun route(self: View, e: MotionEvent): Boolean {
        val local = self as Routable
        val action = e.actionMasked
        if (action == MotionEvent.ACTION_DOWN) { owner = self; assigned.clear() }
        if (owner !== self) {
            // Another view owns this gesture and forwards our fingers; don't handle them twice.
            if (owner?.isAttachedToWindow == true) return true
            return local.handleTouch(e)
        }
        val idx = e.actionIndex
        val pid = e.getPointerId(idx)
        self.getLocationOnScreen(loc)
        val ox = loc[0]; val oy = loc[1]
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            val x = e.getX(idx); val y = e.getY(idx)
            assigned[pid] = if (x >= 0 && y >= 0 && x < self.width && y < self.height) self else find(ox + x, oy + y, self)
        }

        var allMine = true
        for (i in 0 until e.pointerCount) if (assigned[e.getPointerId(i)] !== self) { allMine = false; break }
        if (allMine) {
            val handled = local.handleTouch(e)
            if (action == MotionEvent.ACTION_DOWN && !handled) { owner = null; assigned.clear(); return false }
        } else {
            for (t in assigned.values.filterNotNull().distinct()) {
                val sub = split(e, t, ox, oy) ?: continue
                (t as Routable).handleTouch(sub)
                sub.recycle()
            }
        }
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { assigned.clear(); owner = null }
            MotionEvent.ACTION_POINTER_UP -> assigned.remove(pid)
        }
        return true
    }

    /** The registered view on screen under (sx, sy), other than [self]. */
    private fun find(sx: Float, sy: Float, self: View): View? {
        for (ref in views) {
            val v = ref.get() ?: continue
            if (v === self || !v.isAttachedToWindow || !v.isShown) continue
            v.getLocationOnScreen(tloc)
            if (sx >= tloc[0] && sy >= tloc[1] && sx < tloc[0] + v.width && sy < tloc[1] + v.height) return v
        }
        return null
    }

    /** [e] with only [t]'s fingers, in [t]'s coordinates; null when nothing in [e] concerns [t]. */
    private fun split(e: MotionEvent, t: View, ox: Int, oy: Int): MotionEvent? {
        val keep = (0 until e.pointerCount).filter { assigned[e.getPointerId(it)] === t }
        if (keep.isEmpty()) return null
        val action = e.actionMasked
        val pid = e.getPointerId(e.actionIndex)
        val at = keep.indexOfFirst { e.getPointerId(it) == pid }
        val out = when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (at < 0) return null // another view's finger went down or up
                val down = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                if (keep.size == 1) (if (down) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP)
                else (if (down) MotionEvent.ACTION_POINTER_DOWN else MotionEvent.ACTION_POINTER_UP) or
                    (at shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
            else -> action
        }
        t.getLocationOnScreen(tloc)
        val dx = (ox - tloc[0]).toFloat(); val dy = (oy - tloc[1]).toFloat()
        val props = Array(keep.size) { MotionEvent.PointerProperties().also { p -> e.getPointerProperties(keep[it], p) } }
        val coords = Array(keep.size) {
            MotionEvent.PointerCoords().also { c -> e.getPointerCoords(keep[it], c); c.x += dx; c.y += dy }
        }
        return MotionEvent.obtain(
            e.downTime, e.eventTime, out, keep.size, props, coords, e.metaState, e.buttonState,
            e.xPrecision, e.yPrecision, e.deviceId, e.edgeFlags, e.source, e.flags,
        )
    }
}
