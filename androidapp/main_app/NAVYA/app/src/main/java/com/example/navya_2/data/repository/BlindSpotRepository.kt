package com.example.navya_2.data.repository

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.navya_2.data.dto.CameraStateDto
import com.example.navya_2.data.local.SharedPrefsManager
import com.example.navya_2.data.vhal.VhalManager
import com.example.navya_2.util.DistanceState
import com.example.navya_2.util.SwitchState

class BlindSpotRepository(
    private val context: Context,
    private val vhalManager: VhalManager,
    private val sharedPrefsManager: SharedPrefsManager
) {
    private val _cameraState = MutableLiveData<CameraStateDto>()
    val cameraState: LiveData<CameraStateDto> = _cameraState
    private val TAG = "BlindSpotRepository"

    fun readSwitchState(): Int {
        return try {
            val state = vhalManager.readSwitchState()
            sharedPrefsManager.saveSwitchState(state)
            state
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error reading switch state: ${e.message}")
            SwitchState.SWITCH_INVALID
        }
    }

    fun configureCameraUseCases(): Pair<ProcessCameraProvider, Pair<Preview, ImageAnalysis>> {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val cameraSelector = findCompatibleCamera(cameraProvider)
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(com.example.navya_2.util.ObjectDetectorHelper.INPUT_SIZE, com.example.navya_2.util.ObjectDetectorHelper.INPUT_SIZE),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                    )
                )
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            return Pair(cameraProvider, Pair(preview, imageAnalysis))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error configuring camera: ${e.message}")
            throw e
        }
    }

    fun updateCameraState(
        isCameraOn: Boolean,
        switchState: Int = _cameraState.value?.switchState ?: SwitchState.SWITCH_INVALID,
        closestDistance: Float = _cameraState.value?.closestDistance ?: Float.MAX_VALUE,
        closestLabel: String? = _cameraState.value?.closestLabel,
        distanceState: Int = _cameraState.value?.distanceState ?: DistanceState.FAR
    ) {
        val currentState = _cameraState.value ?: CameraStateDto(
            switchState = switchState,
            distanceState = distanceState,
            closestDistance = closestDistance,
            closestLabel = closestLabel,
            isCameraOn = isCameraOn
        )
        _cameraState.postValue(
            currentState.copy(
                isCameraOn = isCameraOn,
                switchState = switchState,
                closestDistance = closestDistance,
                closestLabel = closestLabel,
                distanceState = distanceState
            )
        )
        sharedPrefsManager.saveSwitchState(switchState)
        if (!isCameraOn) {
            sharedPrefsManager.saveDistanceState(DistanceState.FAR)
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalLensFacing::class)
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
}