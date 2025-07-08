package com.example.navya_2.feature.blindspot.viewmodel

import android.graphics.Bitmap
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navya_2.data.repository.BlindSpotRepository
import com.example.navya_2.util.ObjectDetectorHelper
import com.example.navya_2.util.SwitchState
import com.example.navya_2.util.SharedState
import com.example.navya_2.util.DistanceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlindSpotViewModel(
    private val repository: BlindSpotRepository
) : ViewModel() {

    private val _cameraState = MutableLiveData<Boolean>()
    val cameraState: LiveData<Boolean> = _cameraState

    private val _detectionResult = MutableLiveData<String?>()
    val detectionResult: LiveData<String?> = _detectionResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var reusableBitmap: Bitmap? = null
    private var lastAnalysisTime = 0L
    private var switchPollingJob: Job? = null
    private var isSwitchActive = false
    private var currentSwitchState: Int = SwitchState.SWITCH_CENTER

    companion object {
        private const val DETECTION_INTERVAL = 100
        private const val CLOSE_DISTANCE = 5.0f
        private const val NEAR_DISTANCE = 15.0f
        private const val MODEL_THRESHOLD = 0.5f
        private const val FOCAL_LENGTH_PX = 270
        val knownHeights = mapOf(
            "person" to 1.7f, "car" to 1.5f, "bicycle" to 1.2f, "motorcycle" to 1.3f,
            "bus" to 3.0f, "truck" to 3.5f, "cat" to 0.3f, "dog" to 0.5f,
            "horse" to 1.6f, "sheep" to 0.9f, "cow" to 1.5f, "elephant" to 3.2f,
            "zebra" to 1.4f, "giraffe" to 5.5f, "train" to 3.6f, "boat" to 2.5f
        )
    }

    init {
        startSwitchPolling()
    }

    fun initializeCamera() {
        viewModelScope.launch {
            setCameraState(isSwitchActive)
        }
    }

    fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
        repository.setSurfaceProvider(surfaceProvider)
    }

    fun setLifecycleOwner(fragment: Fragment) {
        repository.setLifecycleOwner(fragment)
    }

    private fun startSwitchPolling() {
        stopSwitchPolling()
        switchPollingJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val newState = repository.readSwitchState()
                    val newActiveState = newState == SwitchState.SWITCH_LEFT || newState == SwitchState.SWITCH_RIGHT
                    if (newState != currentSwitchState) {
                        currentSwitchState = newState
                        if (newActiveState != isSwitchActive) {
                            isSwitchActive = newActiveState
                            setCameraState(isSwitchActive)
                        }
                    }
                    delay(100)
                } catch (e: Exception) {
                    _errorMessage.postValue("Error in switch polling: ${e.message}")
                }
            }
        }
    }

    private fun stopSwitchPolling() {
        switchPollingJob?.cancel()
        switchPollingJob = null
    }

    private fun setCameraState(isOn: Boolean) {
        if (_cameraState.value != isOn) {
            _cameraState.postValue(isOn)
            if (isOn) {
                startCamera()
            } else {
                stopCamera()
                _detectionResult.postValue(null)
            }
            repository.updateSharedPrefs(currentSwitchState, getDistanceState())
        }
    }

    fun stopCamera() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val cameraProvider = repository.getCameraProvider()
                withContext(Dispatchers.Main) {
                    cameraProvider.unbindAll()
                    _cameraState.postValue(false)
                    _detectionResult.postValue(null)
                    repository.updateSharedPrefs(SwitchState.SWITCH_CENTER, DistanceState.FAR)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error stopping camera: ${e.message}")
            }
        }
    }

    private fun startCamera() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _detectionResult.postValue("Initializing camera")
                val cameraProvider = repository.getCameraProvider()
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(ObjectDetectorHelper.INPUT_SIZE, ObjectDetectorHelper.INPUT_SIZE),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                        )
                    )
                    .build()

                lateinit var preview: Preview
                withContext(Dispatchers.Main) {
                    preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .apply { surfaceProvider = repository.getSurfaceProvider() }
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .apply { setAnalyzer(cameraExecutor, ::analyzeImage) }

                val cameraSelector = repository.findCompatibleCamera(cameraProvider)

                withContext(Dispatchers.Main) {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        repository.getLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    _cameraState.postValue(true)
                    repository.updateSharedPrefs(currentSwitchState, getDistanceState())
                }
            } catch (e: Exception) {
                _cameraState.postValue(false)
                _detectionResult.postValue("Camera unavailable")
                _errorMessage.postValue("Failed to start camera: ${e.message}")
                repository.updateSharedPrefs(currentSwitchState, getDistanceState())
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
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
                reusableBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }

            val bitmap = reusableBitmap
            image.planes[0].buffer.rewind()
            bitmap?.copyPixelsFromBuffer(image.planes[0].buffer) ?: return

            val detections = repository.detectObjects(bitmap)
            processDetections(detections)
        } catch (e: Exception) {
            _errorMessage.postValue("Error processing image: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun processDetections(detections: List<Detection>) {
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

        viewModelScope.launch(Dispatchers.Main) {
            if (!_cameraState.value!!) return@launch
            val distanceState = if (minDistance < Float.MAX_VALUE) minDistance else Float.MAX_VALUE
            _detectionResult.postValue(
                if (closestLabel != null) {
                    "$closestLabel (${"%.1f".format(distanceState)}m)"
                } else {
                    "No objects detected"
                }
            )
            repository.updateSharedPrefs(currentSwitchState, getDistanceState(distanceState))
        }
    }

    private fun estimateDistance(label: String, boxHeight: Float): Float {
        val knownHeight = knownHeights[label] ?: return Float.MAX_VALUE
        return (FOCAL_LENGTH_PX * knownHeight) / boxHeight
    }

    private fun getDistanceState(distance: Float = Float.MAX_VALUE): Int {
        return when {
            distance < CLOSE_DISTANCE -> DistanceState.CLOSE
            distance < NEAR_DISTANCE -> DistanceState.NEAR
            else -> DistanceState.FAR
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSwitchPolling()
        stopCamera()
        cameraExecutor.shutdown()
        reusableBitmap?.recycle()
        reusableBitmap = null
        repository.closeDetector()
    }
}