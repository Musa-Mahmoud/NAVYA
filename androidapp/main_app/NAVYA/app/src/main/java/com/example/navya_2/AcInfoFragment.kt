package com.example.navya_2

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.animation.ValueAnimator
import android.util.Log

class AcInfoFragment : Fragment() {
    private lateinit var frontAcWind: ImageView
    private lateinit var rearAcWind: ImageView
    private lateinit var innerTempText: TextView
    private lateinit var prefs: SharedPreferences
    private var syncedWindAnimator: ValueAnimator? = null

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            SharedState.KEY_AC_STATE, SharedState.KEY_REAR_AC_STATE -> updateAcState()
            SharedState.KEY_INNER_TEMP -> updateInnerTemp()
        }
    }

    companion object {
        private const val TAG = "AcInfoFragment"
        fun newInstance() = AcInfoFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ac_info, container, false)
        AppViewModelFactory.init(requireContext())

        innerTempText = view.findViewById(R.id.inner_temp)
        frontAcWind = view.findViewById(R.id.front_ac_wind)
        rearAcWind = view.findViewById(R.id.rear_ac_wind)

        prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)

        updateInnerTemp()
        updateAcState()
        return view
    }

    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateInnerTemp()
        updateAcState()
        Log.d(TAG, "onResume: Registered SharedPreferences listener")
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        stopSyncedWindAnimations()
        Log.d(TAG, "onPause: Unregistered SharedPreferences listener")
    }

    private fun updateInnerTemp() {
        val temp = prefs.getInt(SharedState.KEY_INNER_TEMP, 22)
        innerTempText.text = "$temp°C"
        Log.d(TAG, "Updated inner temp to $temp°C")
    }

    private fun updateAcState() {
        val isAcOn = prefs.getBoolean(SharedState.KEY_AC_STATE, false)
        val isRearAcOn = prefs.getBoolean(SharedState.KEY_REAR_AC_STATE, false)

        frontAcWind.visibility = if (isAcOn) View.VISIBLE else View.INVISIBLE
        rearAcWind.visibility = if (isRearAcOn) View.VISIBLE else View.INVISIBLE

        if (isAcOn || isRearAcOn) {
            startSyncedWindAnimations()
        } else {
            stopSyncedWindAnimations()
        }
        Log.d(TAG, "Updated AC state - Front AC: $isAcOn, Rear AC: $isRearAcOn")
    }

    private fun startSyncedWindAnimations() {
        if (syncedWindAnimator != null) return // Prevent duplicate animators

        syncedWindAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L // Total cycle duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedFraction
                val translationY = -20f + (30f * progress) // -20 to +10
                val alpha = if (progress < 0.75f) {
                    progress / 0.75f // Fade in
                } else {
                    1f - ((progress - 0.75f) / 0.25f) // Fade out
                }

                // Apply to visible winds only
                if (frontAcWind.visibility == View.VISIBLE) {
                    frontAcWind.translationY = translationY
                    frontAcWind.alpha = alpha
                }
                if (rearAcWind.visibility == View.VISIBLE) {
                    rearAcWind.translationY = translationY
                    rearAcWind.alpha = alpha
                }
            }

            start()
            Log.d(TAG, "Started wind animations")
        }
    }

    private fun stopSyncedWindAnimations() {
        syncedWindAnimator?.cancel()
        syncedWindAnimator = null

        // Reset visuals
        frontAcWind.alpha = 0f
        frontAcWind.translationY = -20f
        rearAcWind.alpha = 0f
        rearAcWind.translationY = -20f
        Log.d(TAG, "Stopped wind animations")
    }
}