package dev.padbridge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.View
import kotlin.math.sqrt

/**
 * Gyroscope + gravity, expressed the way a pointing device needs them, independent of how the phone
 * is held or how the screen is rotated:
 *
 *  - [yaw]   turn rate around the world's vertical axis (rad/s, positive = turning left)
 *  - [pitch] tilt rate around the screen's horizontal axis (rad/s, positive = aiming up)
 *  - [gx], [gy], [gz] unit "up" vector in screen coordinates (x right, y up, z out of the screen)
 *
 * Works for a phone held flat like a remote (top edge forward) and upright like a camera alike.
 */
class Motion(private val view: View, private val onSample: (Motion, Float) -> Unit) : SensorEventListener {
    private val sm = view.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyro: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravity: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val lowPass = gravity?.type == Sensor.TYPE_ACCELEROMETER

    val available get() = gyro != null && gravity != null

    var yaw = 0f; private set
    var pitch = 0f; private set
    var gx = 0f; private set
    var gy = 1f; private set
    var gz = 0f; private set

    private val g = floatArrayOf(0f, 9.81f, 0f) // device coordinates
    private var lastTs = 0L
    private var running = false

    fun start() {
        if (running || !available) return
        running = true
        lastTs = 0L
        sm.registerListener(this, gyro, 5_000)      // ~200 Hz, the uncapped maximum
        sm.registerListener(this, gravity, 10_000)
    }

    fun stop() {
        if (!running) return
        running = false
        sm.unregisterListener(this)
        yaw = 0f; pitch = 0f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type != Sensor.TYPE_GYROSCOPE) {
            if (lowPass) for (i in 0..2) g[i] += (e.values[i] - g[i]) * 0.1f
            else for (i in 0..2) g[i] = e.values[i]
            return
        }
        val dt = if (lastTs == 0L) 0f else ((e.timestamp - lastTs) / 1e9f).coerceIn(0f, 0.05f)
        lastTs = e.timestamp

        // Device axes -> screen axes for the current display rotation.
        val rot = view.display?.rotation ?: Surface.ROTATION_0
        val (wx, wy) = toScreen(rot, e.values[0], e.values[1])
        val wz = e.values[2]
        val (ax, ay) = toScreen(rot, g[0], g[1])
        val n = sqrt(ax * ax + ay * ay + g[2] * g[2]).takeIf { it > 1f } ?: return
        gx = ax / n; gy = ay / n; gz = g[2] / n

        // Turning around the world vertical, whatever the phone's tilt.
        yaw = wx * gx + wy * gy + wz * gz
        // Tilting around the screen's x axis, flattened to horizontal so rolling the phone doesn't leak in.
        var rx = 1f - gx * gx; var ry = -gx * gy; var rz = -gx * gz
        val rn = sqrt(rx * rx + ry * ry + rz * rz)
        if (rn > 0.2f) { rx /= rn; ry /= rn; rz /= rn } else { rx = 1f; ry = 0f; rz = 0f }
        pitch = wx * rx + wy * ry + wz * rz

        if (dt > 0f) onSample(this, dt)
    }

    private fun toScreen(rot: Int, x: Float, y: Float): Pair<Float, Float> = when (rot) {
        Surface.ROTATION_90 -> -y to x
        Surface.ROTATION_180 -> -x to -y
        Surface.ROTATION_270 -> y to -x
        else -> x to y
    }

    companion object {
        fun hasGyro(ctx: Context) =
            (ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }
}
