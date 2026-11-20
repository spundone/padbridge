package dev.padbridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * "Control this phone": no Bluetooth. The trackpad, keyboard and remote drive this phone's own screen, or the
 * external display it's showing (desktop mode, Samsung DeX, an extended display).
 *
 * Everything PadBridge would send to a computer still goes through [HidManager]; while this is on, its reports come
 * here instead of over Bluetooth. They're decoded (pointer motion, buttons, wheel, keys, media keys) and replayed by
 * a [Backend]: the root helper ([RootInput], real mouse and key events) when root is available, else the
 * accessibility service ([LocalA11yService]: taps, drags, scroll swipes, text). A cursor is drawn on the target
 * display, since neither path shows the system pointer.
 */
object Local {
    private const val TAG = "PadBridgeLocal"

    /** What replays the input. */
    interface Backend {
        val label: String
        /** Pointer at (x, y) on [displayId] with mouse [buttons] (HID bits) held. */
        fun pointer(displayId: Int, x: Float, y: Float, buttons: Int)
        /** Wheel notches at the pointer: [v] > 0 scrolls up, [h] > 0 scrolls right. */
        fun scroll(displayId: Int, x: Float, y: Float, v: Float, h: Float)
        /** A key changed (HID usage, HID modifier byte after the change). */
        fun key(displayId: Int, usage: Int, mods: Int, down: Boolean)
        fun close() {}
    }

    // ---------------------------------------------------------------- state

    /** Switched on by the user (Connect screen). */
    var enabled by mutableStateOf(false); private set
    /** Which backend is driving; null while none is available (no root, accessibility service off). */
    var backendLabel by mutableStateOf<String?>(null); private set
    /** The display being controlled, and its name for the status chip. */
    var displayName by mutableStateOf<String?>(null); private set

    val active get() = enabled && backend != null

    @Volatile private var backend: Backend? = null
    @Volatile var displayId = Display.DEFAULT_DISPLAY; private set
    var width = 1080f; private set
    var height = 1920f; private set
    private var density = 2f
    private var x = 0f
    private var y = 0f
    private var buttons = 0

    private lateinit var app: Context
    private val main = Handler(Looper.getMainLooper())

    fun init(ctx: Context) { app = ctx.applicationContext }

    fun setEnabled(ctx: Context, on: Boolean) {
        init(ctx)
        if (on == enabled) return
        enabled = on
        Settings.localControl = on
        if (on) {
            pickDisplay()
            chooseBackend()
            app.getSystemService(DisplayManager::class.java).registerDisplayListener(displays, main)
        } else {
            app.getSystemService(DisplayManager::class.java).unregisterDisplayListener(displays)
            release()
            backend?.close(); backend = null; backendLabel = null
            RootInput.stop()
            Cursor.hide()
        }
        HidManager.notifyChanged()
    }

    /** Root first (real events), else the accessibility service. Called again when either becomes available. */
    fun chooseBackend() {
        if (!enabled || !::app.isInitialized) return
        // Starting the root helper waits on su (and maybe its grant prompt): never on the main thread.
        Thread {
            val root = Settings.localUseRoot && RootInput.ensure(app)
            // With root, allow our own pointer overlay (no accessibility service needed to see the cursor).
            if (root && !android.provider.Settings.canDrawOverlays(app)) try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "appops set ${app.packageName} SYSTEM_ALERT_WINDOW allow")).waitFor()
            } catch (_: Exception) {}
            main.post { applyBackend(root) }
        }.start()
    }

    private fun applyBackend(root: Boolean) {
        if (!enabled) return
        val b: Backend? = when {
            root -> RootInput
            LocalA11yService.instance != null -> LocalA11yService.instance
            else -> null
        }
        if (b !== backend) { backend?.takeIf { it !== RootInput }?.close(); backend = b }
        backendLabel = b?.label
        if (b != null) Cursor.show(app, displayId, x, y) else Cursor.hide()
        HidManager.notifyChanged()
    }

    private val displays = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) { pickDisplay(); Cursor.show(app, displayId, x, y) }
        override fun onDisplayRemoved(id: Int) { if (id == displayId) { pickDisplay(); Cursor.show(app, displayId, x, y) } }
        override fun onDisplayChanged(id: Int) { if (id == displayId) measure() }
    }

    /** The external display when there is one (desktop mode, DeX), else this phone's screen; or the user's pick. */
    fun pickDisplay() {
        val dm = app.getSystemService(DisplayManager::class.java)
        val all = dm.displays.filter { it.state != Display.STATE_OFF }
        val wanted = Settings.localDisplay
        val d = all.firstOrNull { it.displayId == wanted }
            ?: all.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && it.flags and Display.FLAG_PRIVATE == 0 }
            ?: dm.getDisplay(Display.DEFAULT_DISPLAY)
        displayId = d.displayId
        displayName = if (d.displayId == Display.DEFAULT_DISPLAY) "This phone" else d.name
        measure()
        x = width / 2; y = height / 2
    }

    /** Displays to offer in the picker: id and name. */
    fun displayChoices(ctx: Context): List<Pair<Int, String>> =
        ctx.getSystemService(DisplayManager::class.java).displays
            .filter { it.flags and Display.FLAG_PRIVATE == 0 }
            .map { it.displayId to (if (it.displayId == Display.DEFAULT_DISPLAY) "This phone's screen" else it.name) }

    @Suppress("DEPRECATION")
    private fun measure() {
        val d = app.getSystemService(DisplayManager::class.java).getDisplay(displayId) ?: return
        val m = android.util.DisplayMetrics()
        d.getRealMetrics(m)
        width = m.widthPixels.toFloat(); height = m.heightPixels.toFloat(); density = m.density
        x = x.coerceIn(0f, width - 1); y = y.coerceIn(0f, height - 1)
    }

    // ---------------------------------------------------------------- reports from HidManager (io thread)

    private var lastKbMods = 0
    private val bootKeys = HashSet<Int>()
    private val nkroKeys = HashSet<Int>()
    private val down = HashSet<Int>()

    /** Called by [HidManager] instead of sending a report over Bluetooth. */
    fun onReport(id: Int, data: ByteArray) {
        val b = backend ?: return
        try {
            when (id) {
                HidDescriptor.ID_MOUSE -> mouse(b, data)
                HidDescriptor.ID_KEYBOARD -> {
                    bootKeys.clear()
                    for (i in 2 until data.size) data[i].toInt().and(0xFF).let { if (it != 0) bootKeys += it }
                    keyboard(b, data[0].toInt() and 0xFF)
                }
                HidDescriptor.ID_NKRO -> {
                    nkroKeys.clear()
                    for (bit in 0..(HidDescriptor.NKRO_LAST - HidDescriptor.NKRO_FIRST)) {
                        if (data[1 + bit / 8].toInt() and (1 shl (bit % 8)) != 0) nkroKeys += HidDescriptor.NKRO_FIRST + bit
                    }
                    keyboard(b, data[0].toInt() and 0xFF)
                }
                HidDescriptor.ID_CONSUMER -> consumer((data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8))
            }
        } catch (e: Exception) {
            Log.w(TAG, "replay", e)
        }
    }

    private fun mouse(b: Backend, d: ByteArray) {
        val nb = d[0].toInt() and 0x07
        val dx = ((d[1].toInt() and 0xFF) or (d[2].toInt() shl 8)).toShort().toInt()
        val dy = ((d[3].toInt() and 0xFF) or (d[4].toInt() shl 8)).toShort().toInt()
        val wheel = d[5].toInt()
        val pan = d[6].toInt()
        // HID counts to pixels: about 1.5 px per count on a 2x screen, scaled with the display's density.
        val k = 0.75f * density
        if (dx != 0 || dy != 0) {
            x = (x + dx * k).coerceIn(0f, width - 1)
            y = (y + dy * k).coerceIn(0f, height - 1)
            Cursor.moveTo(x, y)
        }
        if (dx != 0 || dy != 0 || nb != buttons) {
            buttons = nb
            b.pointer(displayId, x, y, buttons)
        }
        if (wheel != 0 || pan != 0) {
            val wm = if (HidManager.hiResWheel) HidDescriptor.HIRES_MULTIPLIER.toFloat() else 1f
            val pm = if (HidManager.hiResPan) HidDescriptor.HIRES_MULTIPLIER.toFloat() else 1f
            // Computers like macOS reverse the wheel themselves, so PadBridge may be sending "traditional"
            // scrolling; on this phone it always follows the fingers, like touch scrolling.
            val flip = if (Settings.naturalScroll) 1f else -1f
            b.scroll(displayId, x, y, flip * wheel / wm, flip * pan / pm)
        }
    }

    private fun keyboard(b: Backend, mods: Int) {
        val now = HashSet<Int>(bootKeys).apply { addAll(nkroKeys) }
        // Modifiers first (so a key goes down with them already held), then keys up, then keys down.
        for (bit in 0..7) {
            val m = 1 shl bit
            if (mods and m != lastKbMods and m) b.key(displayId, 0xE0 + bit, mods, mods and m != 0)
        }
        lastKbMods = mods
        for (k in down.toList()) if (k !in now) { down -= k; b.key(displayId, k, mods, false) }
        for (k in now) if (k !in down) { down += k; b.key(displayId, k, mods, true) }
    }

    /** Media and volume keys work the same whatever the backend: straight to this phone's audio. */
    private fun consumer(usage: Int) {
        if (usage == 0) return
        val am = app.getSystemService(AudioManager::class.java)
        fun media(code: Int) {
            val t = SystemClock.uptimeMillis()
            am.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
            am.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
        }
        when (usage) {
            Keys.C_VOL_UP -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            Keys.C_VOL_DOWN -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            Keys.C_MUTE -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            Keys.C_PLAY_PAUSE -> media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Keys.C_NEXT -> media(KeyEvent.KEYCODE_MEDIA_NEXT)
            Keys.C_PREV -> media(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    /** Let go of everything (switching off, or the backend changing). */
    private fun release() {
        val b = backend ?: return
        if (buttons != 0) { buttons = 0; b.pointer(displayId, x, y, 0) }
        for (k in down) b.key(displayId, k, 0, false)
        down.clear(); bootKeys.clear(); nkroKeys.clear()
        if (lastKbMods != 0) { for (bit in 0..7) if (lastKbMods and (1 shl bit) != 0) b.key(displayId, 0xE0 + bit, 0, false) }
        lastKbMods = 0
    }

    // ---------------------------------------------------------------- key mapping

    /** HID usage -> Android key code, built from [Keys.fromAndroidKeyCode]. */
    val androidKey: Map<Int, Int> by lazy {
        val m = HashMap<Int, Int>()
        for (code in 1..KeyEvent.getMaxKeyCode()) Keys.fromAndroidKeyCode(code)?.let { if (it !in m) m[it] = code }
        m[0xE0] = KeyEvent.KEYCODE_CTRL_LEFT; m[0xE1] = KeyEvent.KEYCODE_SHIFT_LEFT
        m[0xE2] = KeyEvent.KEYCODE_ALT_LEFT; m[0xE3] = KeyEvent.KEYCODE_META_LEFT
        m[0xE4] = KeyEvent.KEYCODE_CTRL_RIGHT; m[0xE5] = KeyEvent.KEYCODE_SHIFT_RIGHT
        m[0xE6] = KeyEvent.KEYCODE_ALT_RIGHT; m[0xE7] = KeyEvent.KEYCODE_META_RIGHT
        m
    }

    /** HID modifier byte -> Android meta state. */
    fun metaState(mods: Int): Int {
        var m = 0
        if (mods and 0x01 != 0) m = m or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (mods and 0x10 != 0) m = m or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON
        if (mods and 0x02 != 0) m = m or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (mods and 0x20 != 0) m = m or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON
        if (mods and 0x04 != 0) m = m or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (mods and 0x40 != 0) m = m or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
        if (mods and 0x08 != 0) m = m or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        if (mods and 0x80 != 0) m = m or KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON
        return m
    }

    /** The character a HID key types with these modifiers (US layout), or null for non-character keys. */
    fun charFor(usage: Int, mods: Int): Char? {
        val shift = mods and 0x22 != 0
        val wantMods = if (shift) Keys.L_SHIFT else 0
        return chars[usage to wantMods]
    }

    private val chars: Map<Pair<Int, Int>, Char> by lazy {
        val m = HashMap<Pair<Int, Int>, Char>()
        for (c in ' '..'~') Keys.strokeFor(c)?.let { s -> m.putIfAbsent(s.code to (s.mods and Keys.L_SHIFT), c) }
        m
    }

    // ---------------------------------------------------------------- cursor

    /** The pointer drawn on the target display: an arrow in a tiny untouchable window that follows the motion. */
    object Cursor {
        private var view: View? = null
        private var wm: WindowManager? = null
        private var params: WindowManager.LayoutParams? = null
        private var shownOn = -1
        @Volatile private var tx = 0f
        @Volatile private var ty = 0f
        private var posted = false

        @SuppressLint("ClickableViewAccessibility")
        fun show(ctx: Context, displayId: Int, x: Float, y: Float) = main.post {
            tx = x; ty = y
            if (view != null && shownOn == displayId) { place(); return@post }
            hideNow()
            val dm = ctx.getSystemService(DisplayManager::class.java)
            val display = dm.getDisplay(displayId) ?: return@post
            // The accessibility service may draw on any display without the overlay permission.
            val svc = LocalA11yService.instance
            val type: Int
            val base: Context
            if (svc != null) {
                type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY; base = svc
            } else if (android.provider.Settings.canDrawOverlays(ctx)) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; base = ctx
            } else return@post
            val dctx = base.createDisplayContext(display).let {
                if (Build.VERSION.SDK_INT >= 30) it.createWindowContext(type, null) else it
            }
            val d = dctx.resources.displayMetrics.density
            val size = (26 * d).toInt()
            val v = ArrowView(dctx)
            val p = WindowManager.LayoutParams(
                size, size, type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                title = "PadBridge cursor"
                if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            val w = dctx.getSystemService(WindowManager::class.java)
            try { w.addView(v, p) } catch (e: Exception) { Log.w(TAG, "cursor", e); return@post }
            view = v; wm = w; params = p; shownOn = displayId
            place()
        }

        fun moveTo(x: Float, y: Float) {
            tx = x; ty = y
            if (!posted) { posted = true; main.postAtFrontOfQueue { posted = false; place() } }
        }

        private fun place() {
            val p = params ?: return
            p.x = tx.toInt(); p.y = ty.toInt()
            try { wm?.updateViewLayout(view, p) } catch (_: Exception) {}
        }

        fun hide() = main.post { hideNow() }

        private fun hideNow() {
            try { view?.let { wm?.removeView(it) } } catch (_: Exception) {}
            view = null; wm = null; params = null; shownOn = -1
        }
    }

    /** A classic arrow pointer: white with a dark outline, tip at the top-left corner. */
    private class ArrowView(ctx: Context) : View(ctx) {
        private val d = ctx.resources.displayMetrics.density
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.4f * d; strokeJoin = Paint.Join.ROUND
        }
        private val path = Path().apply {
            moveTo(1f, 1f); lineTo(1f, 19f); lineTo(5.5f, 14.8f); lineTo(8.6f, 21.5f); lineTo(11.4f, 20.3f)
            lineTo(8.4f, 13.7f); lineTo(14.6f, 13.7f); close()
            val m = android.graphics.Matrix(); m.setScale(d, d); m.postTranslate(d, d); transform(m)
        }
        override fun onDraw(c: Canvas) { c.drawPath(path, fill); c.drawPath(path, edge) }
    }
}
