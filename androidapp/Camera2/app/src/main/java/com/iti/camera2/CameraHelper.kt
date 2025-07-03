package com.iti.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class CameraHelper(private val context: Context, private val textureView: TextureView) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var isCameraOpen = false

    @SuppressLint("MissingPermission")
    fun startCamera() {
        if (isCameraOpen) {
            Log.w("CameraHelper", "Camera already started")
            return
        }

        startBackgroundThread()

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: run {
            Log.e("CameraHelper", "No back camera found")
            return
        }

        val surfaceTexture = textureView.surfaceTexture ?: run {
            Log.e("CameraHelper", "SurfaceTexture is null")
            return
        }
        surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
        val surface = Surface(surfaceTexture)

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("CameraHelper", "Camera opened")
                    this@CameraHelper.cameraDevice = camera
                    isCameraOpen = true
                    createCaptureSession(camera, surface)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d("CameraHelper", "Camera disconnected")
                    cleanupCamera()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("CameraHelper", "Camera error: $error")
                    cleanupCamera()
                }
            }, backgroundHandler)
        } catch (e: SecurityException) {
            Log.e("CameraHelper", "Security exception: ${e.message}")
        } catch (e: CameraAccessException) {
            Log.e("CameraHelper", "Camera access exception: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("CameraHelper", "Illegal state: ${e.message}")
        }
    }

    private fun createCaptureSession(camera: CameraDevice, surface: Surface) {
        // Prepare capture request builder
        val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(surface)

//        previewRequestBuilder.set(
//            CaptureRequest.CONTROL_AF_MODE,
//            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
//        )
//        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
//        previewRequestBuilder.set(
//            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
//            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
//        )

        // Create state callback for session creation
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Log.d("CameraHelper", "Capture session configured")
                captureSession = session
                try {
                    // Set repeating request for preview
                    session.setRepeatingRequest(
                        previewRequestBuilder.build(),
                        null,
                        backgroundHandler
                    )
                } catch (e: CameraAccessException) {
                    Log.e("CameraHelper", "Failed to start preview", e)
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("CameraHelper", "Capture session configuration failed")
            }
        }

        // Create session using non-deprecated method
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            createCaptureSessionV28(camera, surface, stateCallback)
        } else {
            createCaptureSessionLegacy(camera, surface, stateCallback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createCaptureSessionV28(
        camera: CameraDevice,
        surface: Surface,
        stateCallback: CameraCaptureSession.StateCallback
    ) {
        val outputConfiguration = OutputConfiguration(surface)
        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(outputConfiguration),
            ContextCompat.getMainExecutor(context),
            stateCallback
        )
        camera.createCaptureSession(sessionConfiguration)
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSessionLegacy(
        camera: CameraDevice,
        surface: Surface,
        stateCallback: CameraCaptureSession.StateCallback
    ) {
        camera.createCaptureSession(
            listOf(surface),
            stateCallback,
            backgroundHandler
        )
    }

    fun closeCamera() {
        if (!isCameraOpen) {
            Log.w("CameraHelper", "Camera not open")
            return
        }

        Log.d("CameraHelper", "Closing camera resources")
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            stopBackgroundThread()
            isCameraOpen = false
        } catch (e: Exception) {
            Log.e("CameraHelper", "Error closing camera", e)
        }
    }

    private fun cleanupCamera() {
        try {
            cameraDevice?.close()
            cameraDevice = null
            isCameraOpen = false
        } catch (e: Exception) {
            Log.e("CameraHelper", "Error in cleanup", e)
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e("CameraHelper", "Error stopping background thread", e)
        }
    }
}