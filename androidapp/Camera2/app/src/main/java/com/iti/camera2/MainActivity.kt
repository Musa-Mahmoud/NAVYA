package com.iti.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.iti.camera2.dto.Detection
//import com.iti.camera2.dto.DistanceCategory
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var objectCountTextView: TextView
    private lateinit var distanceFrame: View
    private lateinit var overlayView: OverlayView
    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraHelper: CameraHelper

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var detectionJob: Job? = null
    private var switchPollingJob: Job? = null

    private var surfaceAvailable = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var shouldStartCameraOnResume = false

    private var currentSwitchState: Int = SWITCH_CENTER
    private var isSwitchActive = false
    private var isCameraRunning = false

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        objectCountTextView = findViewById(R.id.objectCountTextView)
        distanceFrame = findViewById(R.id.distanceFrame)
        overlayView = findViewById(R.id.overlayView)

        objectDetector = ObjectDetector(this)
        cameraHelper = CameraHelper(this, textureView)
        car = Car.createCar(this)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        setupSurfaceListener()
        checkCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")

        startSwitchPolling()

        // Restart surface listener if needed
        if (!surfaceAvailable) {
            setupSurfaceListener()
        }

        // Start camera if we have permission and surface is ready
        if (isSwitchActive && hasCameraPermission() && surfaceAvailable) {
//        if (hasCameraPermission() && surfaceAvailable) {
            startCameraAndDetection()
        } else {
            shouldStartCameraOnResume = true
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
        stopSwitchPolling()
        stopCameraAndDetection()
        shouldStartCameraOnResume = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
        stopCameraAndDetection()
        coroutineScope.cancel()
        objectDetector.close()
    }

    private fun setupSurfaceListener() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d("Surface", "Surface available: $width x $height")
                surfaceAvailable = true
                surfaceWidth = width
                surfaceHeight = height

                if (isSwitchActive && hasCameraPermission()) {
//                if (hasCameraPermission()) {
                    startCameraAndDetection()
                } else {
                    shouldStartCameraOnResume = true
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d("Surface", "Surface size changed: $width x $height")
                surfaceWidth = width
                surfaceHeight = height

                // Recreate camera session on size change
                if (hasCameraPermission() && surfaceAvailable) {
                    restartCamera()
                }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d("Surface", "Surface destroyed")
                surfaceAvailable = false
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) { }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() {
        if (hasCameraPermission()) {
            if (surfaceAvailable) {
                startCameraAndDetection()
            } else {
                shouldStartCameraOnResume = true
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isSwitchActive && surfaceAvailable) {
//            if (surfaceAvailable) {
                startCameraAndDetection()
            } else {
                shouldStartCameraOnResume = true
            }
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startCameraAndDetection() {
        if (isCameraRunning) return

        Log.d("MainActivity", "Starting camera and detection")
        try {
            cameraHelper.startCamera()
            startDetectionLoop()
            isCameraRunning = true
            shouldStartCameraOnResume = false
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start camera", e)
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopCameraAndDetection() {
        if (!isCameraRunning) return

        Log.d("MainActivity", "Stopping camera and detection")
        stopDetectionLoop()
        cameraHelper.closeCamera()
        isCameraRunning = false
    }

    private fun restartCamera() {
        Log.d("MainActivity", "Restarting camera")
        stopCameraAndDetection()
        startCameraAndDetection()
    }

    @SuppressLint("SetTextI18n")
    private fun startDetectionLoop() {
        stopDetectionLoop()

        detectionJob = coroutineScope.launch {
            Log.d("Detection", "Detection loop STARTED")
            while (isActive) {
                try {
                    val bitmap = textureView.bitmap
                    if (bitmap == null) {
                        Log.d("Detection", "Bitmap is null - skipping")
                        delay(50)
                        continue
                    }

                    Log.d("Detection", "Processing frame: ${bitmap.width}x${bitmap.height}")
                    val detections = objectDetector.detect(bitmap)

                    withContext(Dispatchers.Main) {
                        objectCountTextView.text = "Objects detected: ${detections.size}"
                        overlayView.setDetections(detections)
//                        updateDistanceFrame(detections)
                        Log.d("Detection", "Found ${detections.size} objects")
                        detections.forEach {
                            Log.d("Detection", "${it.label} (${it.score}) at ${it.location}")
                        }
                    }

                    delay(500)
                } catch (e: CancellationException) {
                    Log.d("Detection", "Detection loop CANCELLED")
                    throw e
                } catch (e: Exception) {
                    Log.e("Detection", "Detection error", e)
                }
            }
            Log.d("Detection", "Detection loop ENDED")
        }
    }

    private fun stopDetectionLoop() {
        detectionJob?.cancel("Stopping for lifecycle")
        detectionJob = null
    }

//    private fun updateDistanceFrame(detections: List<Detection>) {
//        val closest = detections.minByOrNull { it.distance ?: Float.MAX_VALUE }
//
//        val frameColor = when (closest?.category) {
//            DistanceCategory.CLOSE -> Color.RED
//            DistanceCategory.NEAR -> Color.YELLOW
//            DistanceCategory.FAR -> Color.GREEN
//            else -> Color.WHITE // Default
//        }
//
//        // Get the current drawable and apply tint to the stroke only
//        val drawable = distanceFrame.background as? GradientDrawable
//        drawable?.setStroke(
//            8,
//            frameColor
//        )
//    }

    private fun startSwitchPolling() {
        stopSwitchPolling()
        switchPollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val newState = readSwitchState()
                    val newActiveState = newState == SWITCH_LEFT || newState == SWITCH_RIGHT
                    Log.d("SwitchState", "New state: $newState")

                    // Handle state changes
                    if (newState != currentSwitchState) {
                        currentSwitchState = newState
                        Log.d("SwitchState", "New state: $currentSwitchState")

                        if (newActiveState != isSwitchActive) {
                            isSwitchActive = newActiveState
                            withContext(Dispatchers.Main) {
                                handleSwitchActiveChange()
                            }
                        }
                    }
                    delay(100) // Check every 100ms
                } catch (e: Exception) {
                    Log.e("SwitchPolling", "Error reading switch", e)
                }
            }
        }
    }

    private fun stopSwitchPolling() {
        switchPollingJob?.cancel()
        switchPollingJob = null
    }

    private fun handleSwitchActiveChange() {
        Log.d("SwitchState", "Active state changed: $isSwitchActive")
        if (isSwitchActive) {
            // Start camera if conditions are met
            if (hasCameraPermission() && surfaceAvailable) {
                startCameraAndDetection()
            } else {
                Log.w("SwitchState", "Conditions not met to start camera")
            }
        } else {
            // Immediately stop camera when switch deactivates
            stopCameraAndDetection()
        }
    }

    private fun readSwitchState(): Int {
        return try {
            val prop = carPropertyManager.getProperty(
                Integer::class.java, PROP_ID, AREA_ID
            )
             prop.value.toInt()

        } catch (e: Exception) {
            Log.e(TAG, "Error reading switch", e)
            0
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 10
        private const val TAG = "Camera2"
        private const val PROP_ID = 557842692
        private const val AREA_ID = 0
        private const val CAMERA_ID = "0"

        // Switch state constants
        private const val SWITCH_INVALID = 0
        private const val SWITCH_LEFT = 1
        private const val SWITCH_RIGHT = 2
        private const val SWITCH_CENTER = 3
    }
}