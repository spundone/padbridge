package dev.padbridge

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.abs
import kotlin.math.max

/**
 * Floating trackpad: a small trackpad window that stays on top of other apps (display over other apps). Drag the
 * bar to move it, the corner to resize it, the dash to fold it into a bubble (tap the bubble to open it again), the
 * cross to close it. It drives whatever PadBridge is driving: the Bluetooth computer, or this phone in
 * "Control this phone" mode, so you can use the phone's apps and still move the pointer.
 */
class FloatingPadService : Service() {
    private lateinit var wm: WindowManager
    private var root: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private var collapsed = false
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val d get() = resources.displayMetrics.density

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        running = true
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (root == null) show()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
        running = false
        instance = null
        super.onDestroy()
    }

    private fun show() {
        val w = (Settings.floatW.takeIf { it > 0 } ?: 260) * d
        val h = (Settings.floatH.takeIf { it > 0 } ?: 190) * d
        params = WindowManager.LayoutParams(
            w.toInt(), h.toInt(), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable: the app underneath keeps the keyboard and focus.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val dm = resources.displayMetrics
            x = Settings.floatX.takeIf { it >= 0 } ?: (dm.widthPixels - w - 16 * d).toInt()
            y = Settings.floatY.takeIf { it >= 0 } ?: (dm.heightPixels * 0.55f).toInt()
            title = "PadBridge floating trackpad"
            alpha = Settings.floatAlpha
        }
        val v = buildPanel()
        root = v
        wm.addView(v, params)
    }

    // ---------------------------------------------------------------- the panel

    @SuppressLint("ClickableViewAccessibility")
    private fun buildPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = 22 * d
                setColor((C.BG and 0x00FFFFFF) or 0xF0000000.toInt())
                setStroke(d.toInt().coerceAtLeast(1), C.STROKE)
            }
            elevation = 8 * d
            setPadding((6 * d).toInt(), 0, (6 * d).toInt(), (6 * d).toInt())
        }
        val bar = Bar(this)
        panel.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (28 * d).toInt()))
        val pad = TrackpadView(this).apply { compact = true }
        val padBox = FrameLayout(this)
        padBox.addView(pad, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        val grip = Grip(this)
        padBox.addView(grip, FrameLayout.LayoutParams((26 * d).toInt(), (26 * d).toInt(), Gravity.BOTTOM or Gravity.END))
        panel.addView(padBox, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Bar: drag to move; the dash folds it into a bubble, the cross closes it.
        var sx = 0f; var sy = 0f; var px = 0; var py = 0; var moved = false
        bar.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = params.x; py = params.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(e.rawX - sx) + abs(e.rawY - sy) > 8 * d) moved = true
                    if (moved) { params.x = px + (e.rawX - sx).toInt(); params.y = py + (e.rawY - sy).toInt(); update() }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) when (bar.hit(e.x)) {
                        Bar.CLOSE -> stopSelf()
                        Bar.FOLD -> fold(true)
                        Bar.OPACITY -> {
                            // Cycle 100 -> 80 -> 60 -> 40 %, so apps underneath show through.
                            val steps = floatArrayOf(1f, 0.8f, 0.6f, 0.4f)
                            val i = steps.indexOfFirst { kotlin.math.abs(it - params.alpha) < 0.05f }
                            setOpacity(steps[(i + 1).mod(steps.size)])
                            Haptics.soft(0.4f)
                        }
                    } else savePos()
                }
            }
            true
        }
        // Corner grip: resize.
        var sw = 0; var sh = 0
        grip.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; sw = params.width; sh = params.height }
                MotionEvent.ACTION_MOVE -> {
                    params.width = max((160 * d).toInt(), sw + (e.rawX - sx).toInt())
                    params.height = max((130 * d).toInt(), sh + (e.rawY - sy).toInt())
                    update()
                }
                MotionEvent.ACTION_UP -> {
                    Settings.floatW = (params.width / d).toInt(); Settings.floatH = (params.height / d).toInt()
                }
            }
            true
        }
        return panel
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun fold(on: Boolean) {
        collapsed = on
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        if (on) {
            val size = (54 * d).toInt()
            params.width = size; params.height = size
            val bubble = Bubble(this)
            var sx = 0f; var sy = 0f; var px = 0; var py = 0; var moved = false
            bubble.setOnTouchListener { _, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = params.x; py = params.y; moved = false }
                    MotionEvent.ACTION_MOVE -> {
                        if (abs(e.rawX - sx) + abs(e.rawY - sy) > 8 * d) moved = true
                        if (moved) { params.x = px + (e.rawX - sx).toInt(); params.y = py + (e.rawY - sy).toInt(); update() }
                    }
                    MotionEvent.ACTION_UP -> if (!moved) fold(false) else savePos()
                }
                true
            }
            root = bubble
        } else {
            params.width = ((Settings.floatW.takeIf { it > 0 } ?: 260) * d).toInt()
            params.height = ((Settings.floatH.takeIf { it > 0 } ?: 190) * d).toInt()
            root = buildPanel()
        }
        wm.addView(root, params)
    }

    private fun update() { try { wm.updateViewLayout(root, params) } catch (_: Exception) {} }

    private fun setOpacity(a: Float) {
        params.alpha = a.coerceIn(0.25f, 1f)
        Settings.floatAlpha = params.alpha
        update()
    }
    private fun savePos() { Settings.floatX = params.x; Settings.floatY = params.y }

    /** Title bar: a grab handle in the middle, fold (dash) and close (cross) at the right. */
    private class Bar(ctx: Context) : View(ctx) {
        private val d = ctx.resources.displayMetrics.density
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        fun hit(x: Float) = when {
            x > width - 34 * d -> CLOSE
            x > width - 68 * d -> FOLD
            x < 40 * d -> OPACITY
            else -> NONE
        }
        override fun onDraw(c: Canvas) {
            val cy = height / 2f
            p.color = C.MUTED; p.strokeWidth = 4 * d
            c.drawLine(width / 2f - 18 * d, cy, width / 2f + 18 * d, cy, p)
            p.strokeWidth = 2 * d; p.color = C.TEXT
            val fx = width - 51 * d
            c.drawLine(fx - 6 * d, cy, fx + 6 * d, cy, p)
            val cx = width - 17 * d; val s = 5.5f * d
            c.drawLine(cx - s, cy - s, cx + s, cy + s, p); c.drawLine(cx - s, cy + s, cx + s, cy - s, p)
            // Opacity: a half-filled circle at the left.
            val ox = 18 * d; val r = 6.5f * d
            p.style = Paint.Style.STROKE; c.drawCircle(ox, cy, r, p)
            p.style = Paint.Style.FILL; c.drawArc(ox - r, cy - r, ox + r, cy + r, 90f, 180f, true, p)
            p.style = Paint.Style.STROKE
        }
        companion object { const val NONE = 0; const val FOLD = 1; const val CLOSE = 2; const val OPACITY = 3 }
    }

    /** Resize grip: three short diagonal lines in the corner. */
    private class Grip(ctx: Context) : View(ctx) {
        private val d = ctx.resources.displayMetrics.density
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeWidth = 1.6f * d }
        override fun onDraw(c: Canvas) {
            p.color = C.MUTED
            val w = width.toFloat(); val h = height.toFloat()
            for (i in 1..3) { val o = i * 5 * d; c.drawLine(w - o - 4 * d, h - 4 * d, w - 4 * d, h - o - 4 * d, p) }
        }
    }

    /** Folded: a round bubble with a small trackpad glyph. */
    private class Bubble(ctx: Context) : View(ctx) {
        private val d = ctx.resources.displayMetrics.density
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val r = RectF()
        init { elevation = 6 * d }
        override fun onDraw(c: Canvas) {
            val cx = width / 2f; val cy = height / 2f
            p.style = Paint.Style.FILL; p.color = C.ACCENT_DIM
            c.drawCircle(cx, cy, width / 2f - 2 * d, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2 * d; p.color = C.ON_ACCENT_DIM
            r.set(cx - 11 * d, cy - 8 * d, cx + 11 * d, cy + 8 * d)
            c.drawRoundRect(r, 3 * d, 3 * d, p)
            c.drawLine(cx, cy + 3 * d, cx, cy + 8 * d, p)
        }
    }

    companion object {
        const val ACTION_STOP = "dev.padbridge.FLOAT_STOP"
        @Volatile var running = false; private set

        /** Opens the floating trackpad, or the "display over other apps" settings page if that isn't allowed yet. */
        fun start(ctx: Context): Boolean {
            if (!android.provider.Settings.canDrawOverlays(ctx)) {
                ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return false
            }
            ctx.startService(Intent(ctx, FloatingPadService::class.java))
            return true
        }

        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, FloatingPadService::class.java)) }

        /** The open window, so the settings slider can change its opacity live. */
        @Volatile var instance: FloatingPadService? = null; private set

        fun applyOpacity(a: Float) { Settings.floatAlpha = a; instance?.let { s -> s.main.post { s.setOpacity(a) } } }
    }
}
