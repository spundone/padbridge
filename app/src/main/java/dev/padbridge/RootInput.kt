package dev.padbridge

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Writer

/**
 * Root backend for [Local]: real mouse and keyboard events, injected by a small helper ([RootInputServer]) that runs
 * as root from this APK with app_process (as scrcpy does over adb). It gets true hover, right and middle click,
 * smooth wheel scrolling and every key, on any display.
 *
 * The app talks to it over the su process's stdin, one short text command per line.
 */
object RootInput : Local.Backend {
    private const val TAG = "PadBridgeRoot"
    override val label = "Root (real mouse and keyboard)"

    private var proc: Process? = null
    private var out: Writer? = null

    /** Starts the helper if needed. False when there's no root (or it was refused). */
    @Synchronized
    fun ensure(ctx: Context): Boolean {
        if (proc?.isAlive == true && out != null) return true
        return try {
            val p = ProcessBuilder("su").redirectErrorStream(true).start()
            val w = OutputStreamWriter(p.outputStream)
            val apk = ctx.applicationInfo.sourceDir
            w.write("CLASSPATH=$apk exec app_process /system/bin ${RootInputServer::class.java.name}\n")
            w.flush()
            // The helper answers "ready" once it can inject; anything else (su refused) means no root.
            val r = BufferedReader(InputStreamReader(p.inputStream))
            var line: String? = null
            val reader = Thread { line = try { r.readLine() } catch (_: Exception) { null } }
            reader.start(); reader.join(6000) // the su grant prompt may take a moment
            if (line?.trim() != "ready") {
                Log.w(TAG, "helper didn't start: $line")
                p.destroy(); false
            } else {
                proc = p; out = w
                Thread { try { while (r.readLine() != null) {} } catch (_: Exception) {} }.start()
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "no root", e); false
        }
    }

    @Synchronized
    fun stop() {
        try { out?.write("q\n"); out?.flush() } catch (_: Exception) {}
        proc?.destroy(); proc = null; out = null
    }

    @Synchronized
    private fun send(line: String) {
        val w = out ?: return
        try { w.write(line); w.write("\n"); w.flush() } catch (e: Exception) { Log.w(TAG, "send", e); out = null }
    }

    override fun pointer(displayId: Int, x: Float, y: Float, buttons: Int) = send("m $displayId $x $y $buttons")
    override fun scroll(displayId: Int, x: Float, y: Float, v: Float, h: Float) = send("s $displayId $x $y $v $h")
    override fun key(displayId: Int, usage: Int, mods: Int, down: Boolean) {
        val code = Local.androidKey[usage] ?: return
        send("k $displayId $code ${Local.metaState(mods)} ${if (down) 1 else 0}")
    }
}

/**
 * Runs as root: `CLASSPATH=<apk> app_process /system/bin dev.padbridge.RootInputServer`. Reads commands from stdin
 * and injects input events through the hidden InputManager API.
 *
 * - `m <display> <x> <y> <buttons>`: pointer position and held mouse buttons (HID bits: 1 left, 2 right, 4 middle)
 * - `s <display> <x> <y> <v> <h>`: wheel notches
 * - `k <display> <keycode> <meta> <down>`: a key
 * - `q`: quit
 */
class RootInputServer private constructor() {
    private val inject: (InputEvent) -> Unit
    private val setDisplay: (InputEvent, Int) -> Unit
    private var buttons = 0
    private var downTime = 0L

    init {
        // Android 14 moved injection to InputManagerGlobal; older versions have it on InputManager.
        val (target, method) = try {
            val c = Class.forName("android.hardware.input.InputManagerGlobal")
            val im = c.getMethod("getInstance").invoke(null)
            im to c.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
        } catch (_: Exception) {
            val c = Class.forName("android.hardware.input.InputManager")
            val im = c.getMethod("getInstance").invoke(null)
            im to c.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
        }
        inject = { e -> method.invoke(target, e, 0) } // 0 = async
        val sd = InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
        setDisplay = { e, id -> sd.invoke(e, id) }
    }

    private fun motion(action: Int, x: Float, y: Float, display: Int, actionButton: Int = 0, vs: Float = 0f, hs: Float = 0f) {
        val now = SystemClock.uptimeMillis()
        val props = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE })
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            this.x = x; this.y = y; pressure = if (buttons != 0) 1f else 0f
            setAxisValue(MotionEvent.AXIS_VSCROLL, vs); setAxisValue(MotionEvent.AXIS_HSCROLL, hs)
        })
        val e = MotionEvent.obtain(
            if (buttons != 0) downTime else now, now, action, 1, props, coords, 0, buttons,
            1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0,
        )
        if (actionButton != 0) MotionEvent::class.java.getMethod("setActionButton", Int::class.javaPrimitiveType).invoke(e, actionButton)
        setDisplay(e, display)
        inject(e)
        e.recycle()
    }

    /** HID button bits -> Android's MotionEvent button bits. */
    private fun androidButtons(hid: Int): Int {
        var b = 0
        if (hid and 1 != 0) b = b or MotionEvent.BUTTON_PRIMARY
        if (hid and 2 != 0) b = b or MotionEvent.BUTTON_SECONDARY
        if (hid and 4 != 0) b = b or MotionEvent.BUTTON_TERTIARY
        return b
    }

    private fun pointer(display: Int, x: Float, y: Float, hidButtons: Int) {
        val nb = androidButtons(hidButtons)
        if (nb == buttons) {
            motion(if (buttons == 0) MotionEvent.ACTION_HOVER_MOVE else MotionEvent.ACTION_MOVE, x, y, display)
            return
        }
        // Press: DOWN (first button), then BUTTON_PRESS for each; release: BUTTON_RELEASE, then UP (last button).
        val pressed = nb and buttons.inv()
        val released = buttons and nb.inv()
        for (bit in listOf(MotionEvent.BUTTON_PRIMARY, MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_TERTIARY)) {
            if (released and bit != 0) {
                buttons = buttons and bit.inv()
                motion(MotionEvent.ACTION_BUTTON_RELEASE, x, y, display, bit)
                if (buttons == 0) {
                    motion(MotionEvent.ACTION_UP, x, y, display)
                    motion(MotionEvent.ACTION_HOVER_ENTER, x, y, display)
                }
            }
        }
        for (bit in listOf(MotionEvent.BUTTON_PRIMARY, MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_TERTIARY)) {
            if (pressed and bit != 0) {
                if (buttons == 0) {
                    motion(MotionEvent.ACTION_HOVER_EXIT, x, y, display)
                    downTime = SystemClock.uptimeMillis()
                    buttons = buttons or bit
                    motion(MotionEvent.ACTION_DOWN, x, y, display)
                } else buttons = buttons or bit
                motion(MotionEvent.ACTION_BUTTON_PRESS, x, y, display, bit)
            }
        }
    }

    private fun key(display: Int, code: Int, meta: Int, down: Boolean) {
        val now = SystemClock.uptimeMillis()
        val e = KeyEvent(now, now, if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code, 0, meta,
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD)
        setDisplay(e, display)
        inject(e)
    }

    private fun run() {
        println("ready")
        System.out.flush()
        val r = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            val line = r.readLine() ?: break
            val p = line.trim().split(' ')
            try {
                when (p[0]) {
                    "m" -> pointer(p[1].toInt(), p[2].toFloat(), p[3].toFloat(), p[4].toInt())
                    "s" -> motion(MotionEvent.ACTION_SCROLL, p[2].toFloat(), p[3].toFloat(), p[1].toInt(),
                        vs = p[4].toFloat(), hs = p[5].toFloat())
                    "k" -> key(p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4] == "1")
                    "q" -> return
                }
            } catch (e: Exception) {
                System.err.println("PadBridge input: ${e.message}")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try { RootInputServer().run() } catch (e: Throwable) { println("error ${e.message}") }
        }
    }
}
