package com.example.collectimudata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate.
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.EnvironmentCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors

// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
private const val REQUEST_CODE_PERMISSIONS = 10
private const val NUM_OF_AXES = 3
private const val NUM_OF_SENSORS = 3
//enum class Sensors {ACC, GYRO, MAG}
//enum class Axes {X, Y, Z}

// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity() {

//    data class SensorData(var acc:FloatArray = FloatArray(3), var gyro:FloatArray = FloatArray(3), var mag:FloatArray = FloatArray(3))
//    private var imu: SensorData = SensorData()
    private var ImuData = Array(NUM_OF_SENSORS) { FloatArray(NUM_OF_AXES) }
    private var sensorManager: SensorManager? = null
    private var recordFile: File? = null
    private var isRecording: Boolean = false
    private var count = 0
    companion object {
        val STATE_COUNT = "COUNT"
    }

    private var acc: Sensor? = null
    private val accListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.also {
                ImuData[0] = event.values
                accText.setText("Acc: ${Date().time}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                if(isRecording) {
//                    recordFile?.appendText("Acc: ${Date().time}, ${event.values[0]}, ${event.values[1]}, ${event.values[2]}\n")
                    recordFile?.appendBytes("A".toByteArray() + Date().time.toByteArray() + event.values[0].toByteArray() + event.values[1].toByteArray() + event.values[2].toByteArray())
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private var gyro: Sensor? = null
    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.also {
                ImuData[1] = event.values
                gyroText.setText("Gyro: ${Date().time}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                if(isRecording) {
//                    recordFile?.appendText("Gyro: ${Date().time}, ${event.values[0]}, ${event.values[1]}, ${event.values[2]}\n")
                    recordFile?.appendBytes("G".toByteArray() + Date().time.toByteArray() + event.values[0].toByteArray() + event.values[1].toByteArray() + event.values[2].toByteArray())
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private var mag: Sensor? = null
    private val magListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.also {
                ImuData[0] = event.values
                magText.setText("Mag: ${Date().time}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                if(isRecording) {
//                    recordFile?.appendText("Mag: ${Date().time}, ${event.values[0]}, ${event.values[1]}, ${event.values[2]}\n")
                    recordFile?.appendBytes("M".toByteArray() + Date().time.toByteArray() + event.values[0].toByteArray() + event.values[1].toByteArray() + event.values[2].toByteArray())
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPreference()

        setContentView(R.layout.activity_main)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
        acc = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Add this at the end of onCreate function

        viewFinder = findViewById(R.id.view_finder)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

//        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
//            dispatchTakeVideoIntent()
//        }
    }

    override fun onResume() {
        super.onResume()
        isRecording = false
        acc?.also {
            sensorManager?.registerListener(accListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyro?.also {
            sensorManager?.registerListener(gyroListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        mag?.also {
            sensorManager?.registerListener(magListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        isRecording = false
        sensorManager?.unregisterListener(accListener, acc)
        sensorManager?.unregisterListener(gyroListener, gyro)
        sensorManager?.unregisterListener(magListener, mag)
    }

    override fun onStop() {
        super.onStop()
        isRecording = false
        sensorManager?.unregisterListener(accListener, acc)
        sensorManager?.unregisterListener(gyroListener, gyro)
        sensorManager?.unregisterListener(magListener, mag)
        savePreference()
    }

    private fun savePreference() = getPreferences(Context.MODE_PRIVATE).edit().putInt(STATE_COUNT, count).commit()
    private fun loadPreference() = getPreferences(Context.MODE_PRIVATE).getInt(STATE_COUNT, 0).also { count = it }

    // Add this after onCreate

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(1080, 1920))
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Add this before CameraX.bindToLifecycle

        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetRotation(viewFinder.display.rotation)
        }.build()
        val videoCapture = VideoCapture(videoCaptureConfig)

        val tag = MainActivity::class.java.simpleName

//        capture_button.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                recordFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${Date().time}.txt")
//                isRecording = true
//                capture_button.setBackgroundColor(Color.GREEN)
//                videoCapture.startRecording(createVideoFile()!!, executor, object: VideoCapture.OnVideoSavedListener {
//                    override fun onVideoSaved(file: File) {
//                        Log.i(tag, "Video File : $file")
//                    }
//
//                    override fun onError(
//                        videoCaptureError: VideoCapture.VideoCaptureError,
//                        message: String,
//                        cause: Throwable?
//                    ) {
//                        Log.i(tag, "Video Error: $message")
//                    }
//                })
//
//            } else if (event.action == MotionEvent.ACTION_UP) {
//                capture_button.setBackgroundColor(Color.GRAY)
//                isRecording = false
//                videoCapture.stopRecording()
//                Log.i(tag, "Video File stopped")
//            }
//            false
//        }

        capture_button.setOnClickListener {
            if (isRecording) {
//                capture_button.setBackgroundColor(Color.GRAY)
                isRecording = false
                videoCapture.stopRecording()
                Toast.makeText(this, "File saved to ${recordFile?.absolutePath}", Toast.LENGTH_SHORT).show()
                count++
            } else {
                recordFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${count}_${Date().time}.dat")
                isRecording = true
//                capture_button.setBackgroundColor(Color.GREEN)
                videoCapture.startRecording(createVideoFile()!!, executor, object: VideoCapture.OnVideoSavedListener {
                    override fun onVideoSaved(file: File) {
                        Log.i(tag, "Video File : $file")
                    }

                    override fun onError(
                        videoCaptureError: VideoCapture.VideoCaptureError,
                        message: String,
                        cause: Throwable?
                    ) {
                        Log.i(tag, "Video Error: $message")
                    }
                })
                Toast.makeText(this, "Start recording", Toast.LENGTH_SHORT).show()
            }
        }

        // Create configuration object for the image capture use case

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, videoCapture)//, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // used for android 10+
    private fun createVideoUri() : Uri? {
        val status = Environment.getExternalStorageState()
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            // if has SD card, use it
            return contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            )
        } else {
            // else use internal storage
            return contentResolver.insert(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                ContentValues()
            )
        }
    }

    @Throws(IOException::class)
    private fun createVideoFile() : File? {
//        val videoName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.mkdir()
        val tempFile = File(storageDir, "${count}_${Date().time}.mp4")
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null
        }
        return tempFile
    }
}

fun Float.toByteArray(): ByteArray = ByteBuffer.allocate(4).putFloat(this).array()
fun Long.toByteArray(): ByteArray = ByteBuffer.allocate(8).putLong(this).array()