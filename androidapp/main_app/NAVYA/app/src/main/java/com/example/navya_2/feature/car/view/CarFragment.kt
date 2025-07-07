package com.example.navya_2.feature.car.view

import android.animation.ValueAnimator
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.navya_2.R
import com.example.navya_2.data.dto.AmbientLightStateDto
import com.example.navya_2.data.dto.CameraStateDto
import com.example.navya_2.feature.ambientlight.viewmodel.AmbientLightViewModel
import com.example.navya_2.feature.ambientlight.viewmodel.AmbientLightViewModelFactory
import com.example.navya_2.feature.car.viewmodel.CarViewModel
import com.example.navya_2.feature.car.viewmodel.CarViewModelFactory
import com.example.navya_2.util.DistanceState
import com.example.navya_2.util.SwitchState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CarFragment : Fragment() {
    private lateinit var carLeft: ImageView
    private lateinit var carRight: ImageView
    private lateinit var carSafe: ImageView
    private lateinit var carMedium: ImageView
    private lateinit var carDanger: ImageView
    private lateinit var glowView: ImageView

    private val viewModel: CarViewModel by viewModels { CarViewModelFactory() }
    private val ambientLightViewModel: AmbientLightViewModel by viewModels { AmbientLightViewModelFactory() }

    private var blinkJob: Job? = null
    private var blinkerJob: Job? = null
    private var stateBlinkJob: Job? = null
    private var blinkerPlayer: MediaPlayer? = null
    private var dangerPlayer: MediaPlayer? = null
    private var glowAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_car, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carLeft = view.findViewById(R.id.car_left)
        carRight = view.findViewById(R.id.car_right)
        carSafe = view.findViewById(R.id.car_safe)
        carMedium = view.findViewById(R.id.car_medium)
        carDanger = view.findViewById(R.id.car_danger)
        glowView = view.findViewById(R.id.car_shadow_background)

        viewModel.cameraState.observe(viewLifecycleOwner) { state: CameraStateDto ->
            updateIndicators(state.switchState)
            updateDistanceState(state.distanceState)
        }

        ambientLightViewModel.ambientLightState.observe(viewLifecycleOwner) { state: AmbientLightStateDto ->
            applyGlowColor(state.color)
        }

        // Initialize with current state
        viewModel.cameraState.value?.let { state ->
            updateIndicators(state.switchState)
            updateDistanceState(state.distanceState)
        }
        ambientLightViewModel.ambientLightState.value?.let { applyGlowColor(it.color) }
    }

    private fun updateIndicators(switchState: Int) {
        stopBlinking()
        when (switchState) {
            SwitchState.SWITCH_LEFT -> startBlinking(carLeft)
            SwitchState.SWITCH_RIGHT -> startBlinking(carRight)
            else -> {
                carLeft.visibility = View.GONE
                carRight.visibility = View.GONE
            }
        }
    }

    private fun startBlinking(view: ImageView) {
        stopBlinking()
        blinkJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                view.visibility = if (view.isVisible) View.GONE else View.VISIBLE
                delay(500)
            }
        }
        blinkerJob = viewLifecycleOwner.lifecycleScope.launch {
            blinkerPlayer = MediaPlayer.create(requireContext(), R.raw.blinker)
            blinkerPlayer?.isLooping = true
            blinkerPlayer?.start()
        }
    }

    private fun stopBlinking() {
        blinkJob?.cancel()
        blinkJob = null
        blinkerJob?.cancel()
        blinkerJob = null
        carLeft.visibility = View.GONE
        carRight.visibility = View.GONE
        blinkerPlayer?.stop()
        blinkerPlayer?.release()
        blinkerPlayer = null
    }

    private fun updateDistanceState(state: Int) {
        stopStateBlinking()
        when (state) {
            DistanceState.FAR -> {
                carSafe.visibility = View.VISIBLE
                carMedium.visibility = View.GONE
                carDanger.visibility = View.GONE
            }
            DistanceState.NEAR -> {
                carSafe.visibility = View.GONE
                carMedium.visibility = View.VISIBLE
                carDanger.visibility = View.GONE
                stateBlinkJob = viewLifecycleOwner.lifecycleScope.launch {
                    while (isActive) {
                        carMedium.visibility = if (carMedium.isVisible) View.GONE else View.VISIBLE
                        delay(500)
                    }
                }
            }
            DistanceState.CLOSE -> {
                carSafe.visibility = View.GONE
                carMedium.visibility = View.GONE
                carDanger.visibility = View.VISIBLE
                stateBlinkJob = viewLifecycleOwner.lifecycleScope.launch {
                    while (isActive) {
                        carDanger.visibility = if (carDanger.isVisible) View.GONE else View.VISIBLE
                        delay(500)
                    }
                }
                dangerPlayer = MediaPlayer.create(requireContext(), R.raw.danger_warning)
                dangerPlayer?.isLooping = true
                dangerPlayer?.start()
            }
        }
    }

    private fun stopStateBlinking() {
        stateBlinkJob?.cancel()
        stateBlinkJob = null
        carSafe.visibility = View.GONE
        carMedium.visibility = View.GONE
        carDanger.visibility = View.GONE
        dangerPlayer?.stop()
        dangerPlayer?.release()
        dangerPlayer = null
    }

    private fun applyGlowColor(color: Int) {
        glowView.setColorFilter(color)
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0.3f, 0.7f).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { glowView.alpha = it.animatedValue as Float }
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopBlinking()
        stopStateBlinking()
        glowAnimator?.cancel()
    }
}