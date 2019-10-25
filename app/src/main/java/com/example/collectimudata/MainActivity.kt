package com.example.collectimudata

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate.
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Size
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.EnvironmentCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
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
const val REQUEST_VIDEO_CAPTURE = 1

class MainActivity : AppCompatActivity() {

    private var mCameraUri: Uri? = null
    private lateinit var mCameraVideoPath: String

//    data class SensorData(var acc:FloatArray = FloatArray(3), var gyro:FloatArray = FloatArray(3), var mag:FloatArray = FloatArray(3))
//    private var imu: SensorData = SensorData()
    private var ImuData = Array(NUM_OF_SENSORS) { FloatArray(NUM_OF_AXES) }
    private var sensorManager: SensorManager? = null
    private var acc: Sensor? = null
    private var gyro: Sensor? = null
    private var mag: Sensor? = null
    private var counts = 0
    private var recordFile: File? = null
//    inner class mSensorListener : SensorEventListener {
    private val showViewListener = object:SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val timestamp = Date().time + (event!!.timestamp - System.nanoTime()) / 1000000L
            when(event.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    ImuData[0] = event.values
                    accText.setText("${timestamp}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                }
                Sensor.TYPE_GYROSCOPE -> {
                    ImuData[1] = event.values
                    gyroText.setText("${timestamp}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    ImuData[2] = event.values
                    magText.setText("${timestamp}; ${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                }
                else -> Toast.makeText(this@MainActivity, "Sensor ${event.sensor.type} not implemented", Toast.LENGTH_SHORT)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
    private val saveStatListener = object:SensorEventListener {
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
                else -> Toast.makeText(this@MainActivity, "Sensor ${event.sensor.name} not implemented", Toast.LENGTH_SHORT)
            }
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
//    private var accListener = mSensorListener()
//    private var gyroListener = mSensorListener()
//    private var magListener = mSensorListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
        sensorManager?.apply {
            acc = getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyro =getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            mag = getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }

//        val acc = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
//            sensorManager?.registerListener(showViewListener, it, SensorManager.SENSOR_DELAY_GAME)
//        }
//        val gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
//            sensorManager?.registerListener(showViewListener, it, SensorManager.SENSOR_DELAY_GAME)
//        }
//        val mag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
//            sensorManager?.registerListener(showViewListener, it, SensorManager.SENSOR_DELAY_GAME)
//        }
//        sensorManager?.registerListener(showViewListener, acc, SensorManager.SENSOR_DELAY_GAME)
//        sensorManager?.registerListener(showViewListener, gyro, SensorManager.SENSOR_DELAY_GAME)
//        sensorManager?.registerListener(showViewListener, mag, SensorManager.SENSOR_DELAY_GAME)

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

        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
            dispatchTakeVideoIntent()
        }
    }

    override fun onResume() {
        super.onResume()
//        sensorManager?.unregisterListener(saveStatListener)
        Intent(this, SaveRecordService::class.java).also {
            stopService(it)
        }
        acc?.also {
            sensorManager?.registerListener(showViewListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyro?.also {
            sensorManager?.registerListener(showViewListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        mag?.also {
            sensorManager?.registerListener(showViewListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(showViewListener)
        Intent(this, SaveRecordService::class.java).also {
            startService(it)
        }
//        recordFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${counts++}_${Date().time}.txt")
//        acc?.also {
//            sensorManager?.registerListener(saveStatListener, it, SensorManager.SENSOR_DELAY_GAME)
//        }
//        gyro?.also {
//            sensorManager?.registerListener(saveStatListener, it, SensorManager.SENSOR_DELAY_GAME)
//        }
//        mag?.also {
//            sensorManager?.registerListener(saveStatListener, it, SensorManager.SENSOR_DELAY_GAME)
//        }
    }

    override fun onStop() {
        super.onStop()
        sensorManager?.unregisterListener(showViewListener)
//        sensorManager?.unregisterListener(saveStatListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Intent(this, SaveRecordService::class.java).also {
            stopService(it)
        }
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                var videoFile :File? = null
                var videoUri :Uri? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // if Android Q
                    videoUri = createVideoUri()
                } else {
                    try {
                        videoFile = createVideoFile()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    videoFile?.also {
                        mCameraVideoPath = it.absolutePath
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            // Android 7.0
                            try {
                                videoUri = FileProvider.getUriForFile(
                                    this,
                                    "${packageName}.fileprovider",
                                    it
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            videoUri = Uri.fromFile(it)
                        }
                    }
                    mCameraUri = videoUri
                    videoUri?.also {
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
                        takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode,resultCode,intent)
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
//            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
//                videoView.setVideoURI(mCameraUri)
//            } else {
//                videoView.setVideoPath(mCameraVideoPath)
//            }
//            videoView.setMediaController(MediaController(this))
//            videoView.start()
            Toast.makeText(this, "File saved to ${mCameraVideoPath}", Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Add this after onCreate

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
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

//        capture_button.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                capture_button.setBackgroundColor(Color.GREEN)
//
//            } else if (event.action == MotionEvent.ACTION_UP) {
//                capture_button.setBackgroundColor(Color.GRAY)
//            }
//            false
//        }

        // Create configuration object for the image capture use case
//        val imageCaptureConfig = ImageCaptureConfig.Builder()
//            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
//                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
//            }.build()

        // Build the image capture use case and attach button click listener
//        val imageCapture = ImageCapture(imageCaptureConfig)
//        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
//            val file = File(externalMediaDirs.first(),
//                "${System.currentTimeMillis()}.jpg")
//            capture_button.setBackgroundColor(Color.GREEN)
//
//            imageCapture.takePicture(file, executor,
//                object : ImageCapture.OnImageSavedListener {
//                    override fun onError(
//                        imageCaptureError: ImageCapture.ImageCaptureError,
//                        message: String,
//                        exc: Throwable?
//                    ) {
//                        val msg = "Photo capture failed: $message"
//                        Log.e("CameraXApp", msg, exc)
//                        viewFinder.post {
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                        }
//                    }
//
//                    override fun onImageSaved(file: File) {
//                        val msg = "Photo capture succeeded: ${file.absolutePath}"
//                        Log.d("CameraXApp", msg)
//                        viewFinder.post {
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                })
//        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview)//, imageCapture)
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
        val tempFile = File(storageDir, "${Date().time}.mp4")
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null
        }
        return tempFile
    }
}
