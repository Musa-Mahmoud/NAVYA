package com.example.navya_2

import android.annotation.SuppressLint
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.navya_2.ObjectDetectorHelper.Companion.INPUT_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraFeedFragment : Fragment() {
    private lateinit var previewView: PreviewView
    private lateinit var instructionText: TextView
    private lateinit var objectDetector: ObjectDetectorHelper
    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }
    private var reusableBitmap: Bitmap? = null
    private var lastAnalysisTime = 0L
    private var closestDistance: Float = Float.MAX_VALUE
    private var isCameraOn = false
    private var switchPollingJob: Job? = null
    private var isSwitchActive = false
    private var currentSwitchState: Int = SwitchState.SWITCH_CENTER

    companion object {
        private const val DETECTION_INTERVAL = 100
        private const val CLOSE_DISTANCE = 5.0f
        private const val NEAR_DISTANCE = 15.0f
        private const val MODEL_THRESHOLD = 0.5f
        private var FOCAL_LENGTH_PX = 270
        private const val PROP_ID = 557842692
        private const val AREA_ID = 0
        val knownHeights = mapOf(
            "person" to 1.7f, "car" to 1.5f, "bicycle" to 1.2f, "motorcycle" to 1.3f,
            "bus" to 3.0f, "truck" to 3.5f, "cat" to 0.3f, "dog" to 0.5f,
            "horse" to 1.6f, "sheep" to 0.9f, "cow" to 1.5f, "elephant" to 3.2f,
            "zebra" to 1.4f, "giraffe" to 5.5f, "train" to 3.6f, "boat" to 2.5f
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        lifecycleScope.launch(Dispatchers.Default) {
            findCompatibleCamera(ProcessCameraProvider.getInstance(requireContext()).get())
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        objectDetector = ObjectDetectorHelper(requireContext())
        // Warm-up object detector in background
        cameraExecutor.execute {
            objectDetector.detect(createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera_feed, container, false)

        previewView = view.findViewById(R.id.preview_view)
        instructionText = view.findViewById(R.id.instruction_text)

        initializeCarService()

        // Initialize with camera off
        instructionText.text = getString(R.string.camera_view)
        startSwitchPolling()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        stopSwitchPolling()
        car.disconnect()
        objectDetector.close()
        cameraExecutor.shutdown()
        reusableBitmap?.recycle()
    }

    private fun initializeCarService() {
        try {
            car = Car.createCar(requireContext())
            carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        } catch (e: Exception) {
            Log.e("CarService", "Failed to initialize car service", e)
            isSwitchActive = false
            currentSwitchState = SwitchState.SWITCH_INVALID
            setCameraState(false)
        }
    }

    private fun startSwitchPolling() {
        stopSwitchPolling()
        switchPollingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val newState = readSwitchState()
                    val newActiveState =
                        newState == SwitchState.SWITCH_LEFT || newState == SwitchState.SWITCH_RIGHT
                    Log.d("SwitchState", "New state: $newState")

                    if (newState != currentSwitchState) {
                        currentSwitchState = newState
                        Log.d("SwitchState", "Current state: $currentSwitchState")

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
        switchPollingJob?.cancel("Stopping for lifecycle")
        switchPollingJob = null
    }

    private fun handleSwitchActiveChange() {
        Log.d("SwitchState", "Active state changed: $isSwitchActive")
        setCameraState(isSwitchActive)
    }

    private fun readSwitchState(): Int {
        return try {
            val prop = carPropertyManager.getProperty(Integer::class.java, PROP_ID, AREA_ID)
            prop.value.toInt()
        } catch (e: Exception) {
            Log.e("CarService", "Error reading switch", e)
            SwitchState.SWITCH_INVALID
        }
    }

    fun setCameraState(isOn: Boolean) {
        if (isOn != isCameraOn) {
            isCameraOn = isOn
            if (isOn) {
                startCamera()
            } else {
                stopCamera()
                instructionText.text = getString(R.string.camera_view)
            }
            requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putInt(SharedState.KEY_SWITCH_STATE, currentSwitchState)
                    putInt(SharedState.KEY_DISTANCE_SAFETY_STATE, getDistanceState(closestDistance))
                }
        }
    }

    private fun startCamera() {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                withContext(Dispatchers.Main) {
                    instructionText.text = getString(R.string.initializing_camera)
                    previewView.visibility = View.GONE
                    instructionText.visibility = View.VISIBLE
                }

                suspendCoroutine { continuation ->
                    lifecycleScope.launch(Dispatchers.Default) {
                        repeat(3) { attempt ->
                            try {
                                val cameraProvider =
                                    ProcessCameraProvider.getInstance(requireContext()).get()
                                val resolutionSelector = ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            Size(640, 480),
                                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                                        )
                                    )
                                    .build()

                                lateinit var preview: Preview
                                withContext(Dispatchers.Main) {
                                    preview = Preview.Builder()
                                        .setResolutionSelector(resolutionSelector)
                                        .build()
                                        .apply { surfaceProvider = previewView.surfaceProvider }
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setResolutionSelector(resolutionSelector)
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .build()
                                    .apply { setAnalyzer(cameraExecutor, ::analyzeImage) }

                                val cameraSelector = findCompatibleCamera(cameraProvider)

                                withContext(Dispatchers.Main) {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        this@CameraFeedFragment,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    previewView.visibility = View.VISIBLE
                                    instructionText.visibility = View.GONE
                                    isCameraOn = true
                                    requireContext().getSharedPreferences(
                                        SharedState.PREFS_NAME,
                                        Context.MODE_PRIVATE
                                    ).edit {
                                        putInt(SharedState.KEY_SWITCH_STATE, currentSwitchState)
                                        putInt(
                                            SharedState.KEY_DISTANCE_SAFETY_STATE,
                                            getDistanceState(closestDistance)
                                        )
                                    }
                                    continuation.resume(Unit)
                                }
                                return@launch // Success
                            } catch (e: Exception) {
                                Log.e("CameraX", "Camera start attempt $attempt failed", e)
                                if (attempt < 2) delay(500)
                            }
                        }
                        throw TimeoutException("Camera initialization failed after retries")
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraX", "Error starting camera", e)
                withContext(Dispatchers.Main) {
                    isCameraOn = false
                    previewView.visibility = View.GONE
                    instructionText.visibility = View.VISIBLE
                    instructionText.text = getString(R.string.camera_unavailable)
                    Toast.makeText(
                        context,
                        "Failed to start camera: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    requireContext().getSharedPreferences(
                        SharedState.PREFS_NAME,
                        Context.MODE_PRIVATE
                    ).edit {
                        putInt(SharedState.KEY_SWITCH_STATE, currentSwitchState)
                        putInt(
                            SharedState.KEY_DISTANCE_SAFETY_STATE,
                            getDistanceState(closestDistance)
                        )
                    }
                }
            }
        }
    }

    private fun stopCamera() {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get()
                withContext(Dispatchers.Main) {
                    cameraProvider.unbindAll()
                    previewView.visibility = View.GONE
                    instructionText.visibility = View.VISIBLE
                    instructionText.text = getString(R.string.camera_view)
                    isCameraOn = false
                    requireContext().getSharedPreferences(
                        SharedState.PREFS_NAME,
                        Context.MODE_PRIVATE
                    ).edit {
                        putInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_CENTER)
                        putInt(
                            SharedState.KEY_DISTANCE_SAFETY_STATE,
                            DistanceState.FAR
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraX", "Error stopping camera", e)
            }
        }
    }

    @OptIn(ExperimentalLensFacing::class)
    private fun findCompatibleCamera(cameraProvider: ProcessCameraProvider): CameraSelector {
        val externalSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
            .build()
        return if (cameraProvider.hasCamera(externalSelector)) {
            Log.d("CameraX", "Using external camera")
            externalSelector
        } else {
            Log.w("CameraX", "No external camera found, using default selector")
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
            bitmap?.copyPixelsFromBuffer(image.planes[0].buffer) ?: return

            val detections = objectDetector.detect(bitmap)
            processDetections(detections)
        } catch (e: Exception) {
            Log.e("AnalyzeImage", "Error processing image", e)
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

        lifecycleScope.launch(Dispatchers.Main) {
            if (!isCameraOn) return@launch
            closestDistance = if (minDistance < Float.MAX_VALUE) minDistance else Float.MAX_VALUE
            instructionText.text = if (closestLabel != null) {
                "$closestLabel (${"%.1f".format(closestDistance)}m)"
            } else {
                "No objects detected"
            }
            requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putInt(SharedState.KEY_SWITCH_STATE, currentSwitchState)
                    putInt(SharedState.KEY_DISTANCE_SAFETY_STATE, getDistanceState(closestDistance))
                }
        }
    }

    private fun estimateDistance(label: String, boxHeight: Float): Float {
        val knownHeight = knownHeights[label] ?: return Float.MAX_VALUE
        return (FOCAL_LENGTH_PX * knownHeight) / boxHeight
    }

    private fun getDistanceState(distance: Float): Int {
        return when {
            distance < CLOSE_DISTANCE -> DistanceState.CLOSE
            distance < NEAR_DISTANCE -> DistanceState.NEAR
            else -> DistanceState.FAR
        }
    }
}