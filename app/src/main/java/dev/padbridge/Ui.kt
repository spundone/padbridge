package dev.padbridge

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View

/**
 * Colors used by the custom-drawn views (trackpad, keyboard, gamepad). The Compose theme
 * overwrites these from the Material 3 color scheme (dynamic wallpaper colors on Android 12+).
 */
object C {
    /** Always-on-display style: black, thin dim outlines, no fills. Set by the Compose AodTheme. */
    var AOD = false
    var AOD_LINE = Color.parseColor("#3C3C3C")
    var BG = Color.parseColor("#111318")
    var SURFACE = Color.parseColor("#1D2024")
    var SURFACE2 = Color.parseColor("#282A2F")
    var SURFACE3 = Color.parseColor("#33353A")
    var STROKE = Color.parseColor("#44474E")
    var ACCENT = Color.parseColor("#AAC7FF")
    var ON_ACCENT = Color.parseColor("#0A305F")
    var ACCENT_DIM = Color.parseColor("#284777")
    var ON_ACCENT_DIM = Color.parseColor("#D6E3FF")
    var TERTIARY = Color.parseColor("#D6BEE4")
    var TEXT = Color.parseColor("#E2E2E9")
    var MUTED = Color.parseColor("#C4C6D0")
    var OK = Color.parseColor("#7DDC9A")
}

/** Fill normally; in AOD draw a thin outline instead (brighter when [lit], e.g. pressed). */
fun android.graphics.Paint.fillOrOutline(color: Int, density: Float, lit: Boolean = false): android.graphics.Paint {
    if (C.AOD) {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1.2f * density
        this.color = if (lit) C.ACCENT else C.AOD_LINE
    } else {
        style = android.graphics.Paint.Style.FILL
        this.color = color
    }
    return this
}

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

object Haptics {
    private var vibrator: Vibrator? = null

    fun init(ctx: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun play(predefined: Int, fallbackMs: Long, amp: Int) {
        if (!Settings.haptics) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(predefined))
            else v.vibrate(VibrationEffect.createOneShot(fallbackMs, amp))
        } catch (_: Exception) {}
    }

    @Suppress("UNUSED_PARAMETER")
    fun tick(view: View? = null) = play(VibrationEffect.EFFECT_TICK, 8, 60)
    fun click() = play(VibrationEffect.EFFECT_CLICK, 14, 140)
    fun heavy() = play(VibrationEffect.EFFECT_HEAVY_CLICK, 24, 255)
}
