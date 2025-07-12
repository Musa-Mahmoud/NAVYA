package com.example.navya_2

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import me.tankery.lib.circularseekbar.CircularSeekBar

class AcControlFragment : Fragment() {
    private lateinit var acToggleButton: Button
    private lateinit var rearAcToggleButton: Button
    private lateinit var inner_temp: TextView
    private lateinit var circularSeekBar: CircularSeekBar
    private var isAcOn = false
    private var isRearAcOn = false
    private lateinit var prefs: SharedPreferences
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    private val viewModel: NavyaVoiceViewModel by activityViewModels {
        NavyaVoiceViewModel.Companion.ViewModelFactory(requireContext().applicationContext)
    }

    companion object {
        private const val TAG = "AcControlFragment"
        private const val TEMP_MIN = 16 // Minimum temperature in °C
        private const val TEMP_MAX = 30 // Maximum temperature in °C
        private const val SEEK_BAR_MAX = 100f // CircularSeekBar max progress

        fun newInstance() = AcControlFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ac_control, container, false)

        // Initialize views
        acToggleButton = view.findViewById(R.id.ac_toggle_button)
        rearAcToggleButton = view.findViewById(R.id.rear_ac_toggle_button)
        circularSeekBar = view.findViewById(R.id.circularSeekBar)
        inner_temp = view.findViewById(R.id.inner_temp)
        // Initialize SharedPreferences
        prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)

        // Initialize UI from SharedPreferences
        isAcOn = prefs.getBoolean(SharedState.KEY_AC_STATE, false)
        isRearAcOn = prefs.getBoolean(SharedState.KEY_REAR_AC_STATE, false)
        val temp = prefs.getInt(SharedState.KEY_INNER_TEMP, 22)
        circularSeekBar.max = SEEK_BAR_MAX
        circularSeekBar.progress = tempToProgress(temp)
        inner_temp.text="Inner: ${temp}°C"
        setAcState(isAcOn)
        setRearAcState(isRearAcOn)
        Log.d(TAG, "Initial state - AC: $isAcOn, Rear AC: $isRearAcOn, Temp: $temp°C")

        // Set up SharedPreferences listener
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                SharedState.KEY_AC_STATE -> {
                    isAcOn = prefs.getBoolean(SharedState.KEY_AC_STATE, false)
                    setAcState(isAcOn)
                    Log.d(TAG, "AC state changed to $isAcOn")
                }
                SharedState.KEY_REAR_AC_STATE -> {
                    isRearAcOn = prefs.getBoolean(SharedState.KEY_REAR_AC_STATE, false)
                    setRearAcState(isRearAcOn)
                    Log.d(TAG, "Rear AC state changed to $isRearAcOn")
                }
                SharedState.KEY_INNER_TEMP -> {
                    val temp = prefs.getInt(SharedState.KEY_INNER_TEMP, 22)
                    circularSeekBar.progress = tempToProgress(temp)
                    inner_temp.text="Inner: ${temp}°C"
                    Log.d(TAG, "Temperature changed to $temp°C")
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // Set up manual toggle listeners
        acToggleButton.setOnClickListener {
            isAcOn = !isAcOn
            setAcState(isAcOn)
            prefs.edit().putBoolean(SharedState.KEY_AC_STATE, isAcOn).apply()
            Log.d(TAG, "AC toggled manually to $isAcOn")
        }

        rearAcToggleButton.setOnClickListener {
            isRearAcOn = !isRearAcOn
            setRearAcState(isRearAcOn)
            prefs.edit().putBoolean(SharedState.KEY_REAR_AC_STATE, isRearAcOn).apply()
            Log.d(TAG, "Rear AC toggled manually to $isRearAcOn")
        }

        // Set up CircularSeekBar listener for manual temperature changes
        circularSeekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar?, progress: Float, fromUser: Boolean) {
                if (fromUser) {
                    val temp = progressToTemp(progress)
                    inner_temp.text="Inner: ${temp}°C"
                    prefs.edit().putInt(SharedState.KEY_INNER_TEMP, temp).apply()
                    Log.d(TAG, "Temperature manually set to $temp°C")
                }
            }
            override fun onStartTrackingTouch(circularSeekBar: CircularSeekBar?) {}
            override fun onStopTrackingTouch(circularSeekBar: CircularSeekBar?) {}
        })

        return view
    }

    private fun setAcState(isOn: Boolean) {
        acToggleButton.text = if (isOn) "AC On" else "AC Off"
        acToggleButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), if (isOn) R.color.ac_blue else R.color.ac_red)
        )
        circularSeekBar.alpha = if (isOn) 1.0f else 0.5f
    }

    private fun setRearAcState(isOn: Boolean) {
        rearAcToggleButton.text = if (isOn) "Rear AC On" else "Rear AC Off"
        rearAcToggleButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), if (isOn) R.color.ac_blue else R.color.ac_red)
        )
    }

    private fun tempToProgress(temp: Int): Float {
        return ((temp - TEMP_MIN).toFloat() / (TEMP_MAX - TEMP_MIN)) * SEEK_BAR_MAX
    }

    private fun progressToTemp(progress: Float): Int {
        return (TEMP_MIN + (progress / SEEK_BAR_MAX * (TEMP_MAX - TEMP_MIN))).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        Log.d(TAG, "SharedPreferences listener unregistered")
    }
}