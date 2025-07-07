package com.example.navya_2.feature.blindspot.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navya_2.data.dto.CameraStateDto
import com.example.navya_2.data.repository.BlindSpotRepository
import com.example.navya_2.util.DistanceState
import com.example.navya_2.util.ObjectDetectorHelper
import com.example.navya_2.util.SwitchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BlindSpotViewModel(
    private val context: Context,
    private val blindSpotRepository: BlindSpotRepository,
    private val objectDetector: ObjectDetectorHelper
) : ViewModel() {
    val cameraState: LiveData<CameraStateDto> = blindSpotRepository.cameraState
    private var reusableBitmap: Bitmap? = null
    private var lastAnalysisTime = 0L
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val TAG = "BlindSpotViewModel"

    companion object {
        private const val DETECTION_INTERVAL = 100
        private const val CLOSE_DISTANCE = 5.0f
        private const val NEAR_DISTANCE = 15.0f
        private const val FOCAL_LENGTH_PX = 270
        private const val MODEL_THRESHOLD = 0.5f
        val knownHeights = mapOf(
            "person" to 1.7f, "car" to 1.5f, "bicycle" to 1.2f, "motorcycle" to 1.3f,
            "bus" to 3.0f, "truck" to 3.5f, "cat" to 0.3f, "dog" to 0.5f,
            "horse" to 1.6f, "sheep" to 0.9f, "cow" to 1.5f, "elephant" to 3.2f,
            "zebra" to 1.4f, "giraffe" to 5.5f, "train" to 3.6f, "boat" to 2.5f
        )
    }

    fun startSwitchPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val newState = blindSpotRepository.readSwitchState()
                    val currentState = cameraState.value?.switchState ?: SwitchState.SWITCH_INVALID
                    if (newState != currentState) {
                        blindSpotRepository.updateCameraState(
                            isCameraOn = cameraState.value?.isCameraOn ?: false,
                            switchState = newState
                        )
                    }
                    kotlinx.coroutines.delay(100)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in switch polling: ${e.message}")
                }
            }
        }
    }

    suspend fun startCamera(fragment: Fragment, previewView: PreviewView) {
        try {
            objectDetector.warmUp()
            withContext(Dispatchers.Main) {
                val (provider, useCases) = blindSpotRepository.configureCameraUseCases()
                val (preview, imageAnalysis) = useCases
                cameraProvider = provider
                provider.unbindAll()
                provider.bindToLifecycle(fragment, findCompatibleCamera(provider), preview, imageAnalysis)
                preview.surfaceProvider = previewView.surfaceProvider
                imageAnalysis.setAnalyzer(cameraExecutor, ::analyzeImage)
                blindSpotRepository.updateCameraState(isCameraOn = true)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error starting camera: ${e.message}")
            throw e
        }
    }

    suspend fun stopCamera() {
        try {
            withContext(Dispatchers.Main) {
                cameraProvider?.unbindAll()
                cameraProvider = null
                blindSpotRepository.updateCameraState(isCameraOn = false)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error stopping camera: ${e.message}")
        }
    }

    @OptIn(ExperimentalLensFacing::class)
    private fun findCompatibleCamera(cameraProvider: ProcessCameraProvider): CameraSelector {
        val externalSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
            .build()
        return if (cameraProvider.hasCamera(externalSelector)) {
            android.util.Log.d(TAG, "Using external camera")
            externalSelector
        } else {
            android.util.Log.w(TAG, "No external camera found, using default selector")
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeImage(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < DETECTION_INTERVAL) {
            imageProxy.close()
            return
        }
        lastAnalysisTime = currentTime

        try {
            val image = imageProxy.image ?: return
            val width = image.width
            val height = image.height

            if (reusableBitmap == null || reusableBitmap?.width != width || reusableBitmap?.height != height) {
                reusableBitmap?.recycle()
                reusableBitmap = createBitmap(width, height)
            }

            val bitmap = reusableBitmap
            image.planes[0].buffer.rewind()
            bitmap?.copyPixelsFromBuffer(image.planes[0].buffer) ?: return

            val detections = objectDetector.detect(bitmap)
            processDetections(detections)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error processing image: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun processDetections(detections: List<org.tensorflow.lite.task.vision.detector.Detection>) {
        var minDistance = Float.MAX_VALUE
        var closestLabel: String? = null

        for (detection in detections) {
            val category = detection.categories.maxByOrNull { it.score } ?: continue
            val label = category.label
            val score = category.score

            if (label !in knownHeights.keys || score < MODEL_THRESHOLD) continue

            val boxHeight = detection.boundingBox.height()
            val distance = estimateDistance(label, boxHeight)

            if (distance < minDistance) {
                minDistance = distance
                closestLabel = label
            }
        }

        val distanceState = when {
            minDistance < CLOSE_DISTANCE -> DistanceState.CLOSE
            minDistance < NEAR_DISTANCE -> DistanceState.NEAR
            else -> DistanceState.FAR
        }

        cameraState.value?.let { currentState ->
            blindSpotRepository.updateCameraState(
                isCameraOn = currentState.isCameraOn,
                switchState = currentState.switchState,
                closestDistance = if (minDistance < Float.MAX_VALUE) minDistance else Float.MAX_VALUE,
                closestLabel = closestLabel,
                distanceState = distanceState
            )
        }
    }

    private fun estimateDistance(label: String, boxHeight: Float): Float {
        val knownHeight = knownHeights[label] ?: return Float.MAX_VALUE
        return (FOCAL_LENGTH_PX * knownHeight) / boxHeight
    }

    override fun onCleared() {
        super.onCleared()
        reusableBitmap?.recycle()
        reusableBitmap = null
        objectDetector.close()
        cameraExecutor.shutdown()
    }
}