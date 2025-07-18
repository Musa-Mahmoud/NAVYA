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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class CarFragment : Fragment() {

    private lateinit var carLeft: ImageView
    private lateinit var carRight: ImageView
    private lateinit var carSafe: ImageView
    private lateinit var carMedium: ImageView
    private lateinit var carDanger: ImageView
    private lateinit var glowView: ImageView

    private lateinit var roadGif: ImageView // ✅ added

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
        hideRoadGif() // ✅ ensure hiding when state changes
    }

    private fun handleStateChange(state: Int) {
        stopStateBlinking()
        when (state) {
            DistanceState.FAR -> {
                carSafe.visibility = View.GONE
                carMedium.visibility = View.GONE
                carDanger.visibility = View.GONE
                showRoadGif() // ✅ show GIF in safe mode
            }
            DistanceState.NEAR -> {
                carSafe.visibility = View.GONE
                carMedium.visibility = View.VISIBLE
                carDanger.visibility = View.GONE
                hideRoadGif() // ✅ hide GIF in other modes
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
                hideRoadGif() // ✅ hide GIF in other modes
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

    private fun showRoadGif() {
        roadGif.visibility = View.VISIBLE
        Glide.with(this)
            .asGif()
            .load(R.raw.moving_road2)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(roadGif)
    }

    private fun hideRoadGif() {
        roadGif.visibility = View.GONE
        Glide.with(this).clear(roadGif)
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
            SharedState.KEY_AMBIENT_COLOR -> {
                val color = prefs.getInt(SharedState.KEY_AMBIENT_COLOR, Color.CYAN)
                applyGlowColor(color)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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
        roadGif = view.findViewById(R.id.road_gif) // ✅ initialized

        val initialState = prefs.getInt(SharedState.KEY_DISTANCE_SAFETY_STATE, DistanceState.FAR)
        handleStateChange(initialState)
        applyGlowColor(prefs.getInt(SharedState.KEY_AMBIENT_COLOR, Color.CYAN))

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