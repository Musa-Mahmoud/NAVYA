package com.example.navya_2

import android.content.Context
import android.content.SharedPreferences
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

    private lateinit var buttonLeft: View
    private lateinit var buttonRight: View
    private lateinit var buttonOff: View

    private lateinit var prefs: SharedPreferences

    private var blinkJob: Job? = null
    private var stateBlinkJob: Job? = null
    private var dangerPlayer: MediaPlayer? = null

    private fun startBlinking(view: ImageView) {
        stopBlinking()
        blinkJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                view.visibility = if (view.isVisible) View.GONE else View.VISIBLE
                delay(500)
            }
        }
    }

    private fun stopBlinking() {
        blinkJob?.cancel()
        blinkJob = null
        carLeft.visibility = View.GONE
        carRight.visibility = View.GONE
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_car, container, false)

        prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

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

        val initialState = prefs.getInt(SharedState.KEY_DISTANCE_SAFETY_STATE, DistanceState.FAR)
        handleStateChange(initialState)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopBlinking()
        stopStateBlinking()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }
}