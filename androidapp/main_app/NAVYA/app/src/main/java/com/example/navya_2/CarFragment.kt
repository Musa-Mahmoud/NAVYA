package com.example.navya_2

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.view.isVisible

class CarFragment : Fragment() {

    private lateinit var carLeft: ImageView
    private lateinit var carRight: ImageView
    private lateinit var carSafe: ImageView
    private lateinit var carMedium: ImageView
    private lateinit var carDanger: ImageView
    private lateinit var glowView: ImageView

    private lateinit var buttonLeft: View
    private lateinit var buttonRight: View
    private lateinit var buttonOff: View

    private lateinit var prefs: SharedPreferences

    private var blinkJob: Job? = null
    private var blinkerJob: Job? = null
    private var stateBlinkJob: Job? = null
    private var blinkerPlayer: MediaPlayer? = null
    private var dangerPlayer: MediaPlayer? = null
    private var glowAnimator: ValueAnimator? = null

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

    private fun handleStateChange(state: Int) {
        stopStateBlinking()
        when (state) {

            "safe" -> carSafe.visibility = View.VISIBLE
            "medium" -> {

            DistanceState.FAR -> {
                carSafe.visibility = View.VISIBLE
            }
            DistanceState.NEAR -> {

                stateBlinkJob = viewLifecycleOwner.lifecycleScope.launch {
                    while (isActive) {
                        carMedium.visibility = if (carMedium.isVisible) View.GONE else View.VISIBLE
                        delay(500)
                    }
                }
            }
            DistanceState.CLOSE -> {
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

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SharedState.KEY_STATE) {
                val state = prefs.getString(SharedState.KEY_STATE, null)
                handleStateChange(state)
            }
            if (key == SharedState.KEY_AMBIENT_COLOR) {
                val color = prefs.getInt(SharedState.KEY_AMBIENT_COLOR, Color.CYAN)
                applyGlowColor(color)

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            SharedState.KEY_DISTANCE_SAFETY_STATE -> {
                val state = prefs.getInt(SharedState.KEY_DISTANCE_SAFETY_STATE, DistanceState.FAR)
                handleStateChange(state)
            }
            SharedState.KEY_SWITCH_STATE -> {
                val switchState = prefs.getInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_INVALID)
                when (switchState) {
                    SwitchState.SWITCH_LEFT -> startBlinking(carLeft)
                    SwitchState.SWITCH_RIGHT -> startBlinking(carRight)
                    else -> stopBlinking()
                }

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_car, container, false)

        prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

        glowView = view.findViewById(R.id.car_shadow_background)
        carLeft = view.findViewById(R.id.car_left)
        carRight = view.findViewById(R.id.car_right)
        carSafe = view.findViewById(R.id.car_safe)
        carMedium = view.findViewById(R.id.car_medium)
        carDanger = view.findViewById(R.id.car_danger)

        buttonLeft = view.findViewById(R.id.button_left)
        buttonRight = view.findViewById(R.id.button_right)
        buttonOff = view.findViewById(R.id.button_off)

        buttonLeft.setOnClickListener { startBlinking(carLeft) }
        buttonRight.setOnClickListener { startBlinking(carRight) }
        buttonOff.setOnClickListener { stopBlinking() }


        handleStateChange(prefs.getString(SharedState.KEY_STATE, null))
        applyGlowColor(prefs.getInt(SharedState.KEY_AMBIENT_COLOR, Color.CYAN))

        val initialState = prefs.getInt(SharedState.KEY_DISTANCE_SAFETY_STATE, DistanceState.FAR)
        handleStateChange(initialState)


        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopBlinking()
        stopStateBlinking()
        glowAnimator?.cancel()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }
}