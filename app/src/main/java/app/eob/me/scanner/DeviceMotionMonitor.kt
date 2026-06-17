package app.eob.me.scanner

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Tracks device shake magnitude for motion-blur guardrails during smart shutter capture.
 */
class DeviceMotionMonitor(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var motionMagnitude: Float = 0f
        private set

    var isShaking: Boolean = false
        private set

    fun start(threshold: Float = SHAKE_THRESHOLD) {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        shakeThreshold = threshold
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        motionMagnitude = 0f
        isShaking = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        motionMagnitude = sqrt(x * x + y * y + z * z)
        isShaking = motionMagnitude > shakeThreshold
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private var shakeThreshold = SHAKE_THRESHOLD

    companion object {
        const val SHAKE_THRESHOLD = 12.5f
    }
}
