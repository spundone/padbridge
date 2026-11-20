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
 * overwrites these from the Material 3 color scheme (wallpaper colors on Android 12+, otherwise PadBridge's palette).
 */
object C {
    /** Always-on-display style: black, thin dim outlines, no fills. Set by the Compose AodTheme. */
    var AOD = false
    var AOD_LINE = Color.parseColor("#3C3C3C")
    var BG = Color.parseColor("#11131C")
    var SURFACE = Color.parseColor("#1D1F29")
    var SURFACE2 = Color.parseColor("#282934")
    var SURFACE3 = Color.parseColor("#32343F")
    var STROKE = Color.parseColor("#444654")
    var ACCENT = Color.parseColor("#B7C4FF")
    var ON_ACCENT = Color.parseColor("#002682")
    var ACCENT_DIM = Color.parseColor("#0038B6")
    var ON_ACCENT_DIM = Color.parseColor("#DCE1FF")
    var TERTIARY = Color.parseColor("#FFB59F")
    var TEXT = Color.parseColor("#E1E1EF")
    var MUTED = Color.parseColor("#C4C5D6")
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
    private var audio: android.media.AudioManager? = null

    fun init(ctx: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        audio = ctx.getSystemService(android.media.AudioManager::class.java)
    }

    /** This device can vibrate at all (many tablets, like some Galaxy Tabs, have no motor). */
    val available get() = vibrator?.hasVibrator() == true

    /**
     * The motor understands Android's preset effects (tick, click, heavy click) and fine amplitude: a phone-class
     * haptics motor. Basic motors (most tablets, older or budget phones) get plain pulses instead, which they can
     * actually play: presets and very short pulses do nothing on them.
     */
    val rich: Boolean by lazy {
        val v = vibrator ?: return@lazy false
        if (Build.VERSION.SDK_INT >= 30) {
            v.areAllEffectsSupported(VibrationEffect.EFFECT_TICK, VibrationEffect.EFFECT_CLICK) ==
                Vibrator.VIBRATION_EFFECT_SUPPORT_YES
        } else v.hasAmplitudeControl()
    }

    /** Rich effects in use: [Settings.hapticStyle] 0 = automatic, 1 = rich, 2 = basic. */
    private val useRich get() = when (Settings.hapticStyle) { 1 -> true; 2 -> false; else -> rich }

    private var lastBasic = 0L

    /** Basic motor: a plain pulse of [ms]. Light ones are spaced out, so a texture is a gentle patter, not a buzz. */
    private fun basic(ms: Long, light: Boolean) {
        val now = android.os.SystemClock.uptimeMillis()
        if (light && now - lastBasic < 45) return
        lastBasic = now
        try { vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}
    }

    /** No motor: a quiet key click instead, when the user asked for it ([Settings.hapticSound]). */
    private fun sound(light: Boolean) {
        if (!Settings.hapticSound || light) return
        try { audio?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.4f) } catch (_: Exception) {}
    }

    private fun play(predefined: Int, fallbackMs: Long, amp: Int, basicMs: Long) {
        if (!Settings.haptics) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) { sound(false); return }
        if (!useRich) { basic(basicMs, false); return }
        try {
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(predefined))
            else v.vibrate(VibrationEffect.createOneShot(fallbackMs, amp))
        } catch (_: Exception) {}
    }

    /**
     * A very light primitive (Android 11+ on phones whose motor supports it), scaled by [scale] 0..1; falls back to a
     * tick. For textures: space bar cursor steps, TrackPoint detents and scroll notches.
     */
    fun soft(scale: Float = 0.4f) {
        if (!Settings.haptics) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return // too frequent for click sounds
        if (!useRich) { basic(if (scale >= 0.4f) 12 else 9, true); return }
        try {
            val low = Build.VERSION.SDK_INT >= 31 &&
                v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
            val tick = Build.VERSION.SDK_INT >= 30 &&
                v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
            val sc = scale.coerceIn(0f, 1f)
            if (low) {
                v.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, sc).compose())
            } else if (tick) {
                v.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, sc).compose())
            } else if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            else v.vibrate(VibrationEffect.createOneShot(5, (255 * scale).toInt().coerceIn(1, 255)))
        } catch (_: Exception) {}
    }

    @Suppress("UNUSED_PARAMETER")
    fun tick(view: View? = null) = play(VibrationEffect.EFFECT_TICK, 8, 60, 14)
    fun click() = play(VibrationEffect.EFFECT_CLICK, 14, 140, 22)
    fun heavy() = play(VibrationEffect.EFFECT_HEAVY_CLICK, 24, 255, 40)
}
