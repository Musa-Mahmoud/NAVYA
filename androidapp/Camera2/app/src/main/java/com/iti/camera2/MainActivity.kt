package com.iti.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var objectCountTextView: TextView
    private lateinit var overlayView: OverlayView
    private lateinit var objectDetector: ObjectDetector
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    // Track surface availability
    private var surfaceAvailable = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        objectCountTextView = findViewById(R.id.objectCountTextView)
        overlayView = findViewById(R.id.overlayView)

        objectDetector = ObjectDetector(this)

        setupSurfaceListener()

        checkCameraPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
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
                // Surface is ready, but we might not have permission yet
                surfaceAvailable = true
                surfaceWidth = width
                surfaceHeight = height

                // Start camera if we already have permission
                if (hasCameraPermission()) {
                    startCameraAndDetection()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surfaceWidth = width
                surfaceHeight = height
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
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
            // Permission already granted, start camera if surface is ready
            if (surfaceAvailable) {
                startCameraAndDetection()
            }
        } else {
            // Request permission
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
            // Permission granted, start camera if surface is ready
            if (surfaceAvailable) {
                startCameraAndDetection()
            }
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startCameraAndDetection() {
        CameraHelper(this@MainActivity, textureView).startCamera()
        startDetectionLoop()
    }

    @SuppressLint("SetTextI18n")
    private fun startDetectionLoop() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    val bitmap = textureView.bitmap ?: continue
                    val detections = objectDetector.detect(bitmap)

                    withContext(Dispatchers.Main) {
                        objectCountTextView.text = "Objects detected: ${detections.size}"
                        overlayView.setDetections(detections)

                        // Debug output
                        Log.d("Detection", "Found ${detections.size} objects")
                        detections.forEach {
                            Log.d("Detection", "${it.label} (${it.score}) at ${it.location}")
                        }
                    }

                    delay(500)
                } catch (e: Exception) {
                    Log.e("ObjectDetection", "Error during detection", e)
                }
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 10
    }
}