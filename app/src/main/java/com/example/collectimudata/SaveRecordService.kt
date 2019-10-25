package com.example.collectimudata

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.util.*


private const val NUM_OF_AXES = 3
private const val NUM_OF_SENSORS = 3

class SaveRecordService : IntentService("SaveRecordService") {

    private var sensorManager: SensorManager? = null
    private var acc: Sensor? = null
    private var gyro: Sensor? = null
    private var mag: Sensor? = null
    private var ImuData = Array(NUM_OF_SENSORS) { FloatArray(NUM_OF_AXES) }
    private var recordFile: File? = null

    private val saveStatListener = object: SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val timestamp = Date().time + (event!!.timestamp - System.nanoTime()) / 1000000L
            when(event.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    ImuData[0] = event.values
                    recordFile?.appendText("${event.sensor.name}:${timestamp}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}\n")
                }
                Sensor.TYPE_GYROSCOPE -> {
                    ImuData[1] = event.values
                    recordFile?.appendText("${event.sensor.name}:${timestamp}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}\n")
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    ImuData[2] = event.values
                    recordFile?.appendText("${event.sensor.name}:${timestamp}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}\n")
                }
                else -> Log.d("DEBUG INFORMATION:", "This is debug log")
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onHandleIntent(intent: Intent?) {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.apply {
            acc = getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyro =getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            mag = getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }

        recordFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${Date().time}.txt")
        acc?.also {
            sensorManager?.registerListener(saveStatListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyro?.also {
            sensorManager?.registerListener(saveStatListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        mag?.also {
            sensorManager?.registerListener(saveStatListener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        while (true) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(saveStatListener)
    }
}