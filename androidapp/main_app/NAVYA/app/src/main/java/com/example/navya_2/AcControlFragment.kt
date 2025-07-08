package com.example.navya_2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import me.tankery.lib.circularseekbar.CircularSeekBar

class AcControlFragment : Fragment() {

    private lateinit var circularSeekBar: CircularSeekBar
    private lateinit var innerTempText: TextView
    private lateinit var centerIcon: ImageView
    private lateinit var acToggleButton: MaterialButton
    private lateinit var rearAcToggleButton: MaterialButton
    private lateinit var rearAcIcon: ImageView
    private var isAcOn = false
    private var isRearAcOn = false // Rear AC starts OFF
    private val prefs by lazy {
        requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_ac_control, container, false)

        // View bindings
        circularSeekBar = view.findViewById(R.id.circularSeekBar)
        innerTempText = view.findViewById(R.id.inner_temp)
        centerIcon = view.findViewById(R.id.center_icon)
        acToggleButton = view.findViewById(R.id.ac_toggle_button)
        rearAcToggleButton = view.findViewById(R.id.rear_ac_toggle_button)
        rearAcIcon = view.findViewById(R.id.rear_ac_icon)

        // Load states from SharedPreferences
        isAcOn = prefs.getBoolean(SharedState.KEY_AC_STATE, true)
        isRearAcOn = prefs.getBoolean(SharedState.KEY_REAR_AC_STATE, false)

        // Load and set saved temperature
        val savedTemp = prefs.getInt(SharedState.KEY_INNER_TEMP, 20)
        innerTempText.text = "Inner: ${savedTemp}°C"
        val progress = ((savedTemp - 10) / 20f) * 100f
        circularSeekBar.progress = progress

        // Set max
        circularSeekBar.max = 100f

        // Listener to update temperature live and save to prefs
        circularSeekBar.setOnSeekBarChangeListener(object :
            CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CircularSeekBar?, progress: Float, fromUser: Boolean) {
                val temperature = 10 + (progress / 100f) * 20 // 10°C–30°C
                innerTempText.text = "Inner: ${temperature.toInt()}°C"
                prefs.edit().putInt(SharedState.KEY_INNER_TEMP, temperature.toInt()).apply()
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {}
            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {}
        })

        // Initialize UI state
        setAcState(isAcOn)
        setRearAcState(isRearAcOn)

        // AC toggle listener
        acToggleButton.setOnClickListener {
            isAcOn = !isAcOn
            setAcState(isAcOn)
            prefs.edit().putBoolean(SharedState.KEY_AC_STATE, isAcOn).apply()
        }

        // Rear AC toggle listener
        rearAcToggleButton.setOnClickListener {
            isRearAcOn = !isRearAcOn
            setRearAcState(isRearAcOn)
            prefs.edit().putBoolean(SharedState.KEY_REAR_AC_STATE, isRearAcOn).apply()
        }

        return view
    }

    private fun setAcState(isOn: Boolean) {
        circularSeekBar.isEnabled = isOn
        centerIcon.isEnabled = isOn

        val alpha = if (isOn) 1.0f else 0.3f
        circularSeekBar.alpha = alpha
        centerIcon.alpha = alpha

        acToggleButton.text = if (isOn) "AC On" else "AC Off"
        val color = ContextCompat.getColor(requireContext(),
            if (isOn) R.color.ac_blue else R.color.ac_red)
        acToggleButton.setBackgroundColor(color)
    }

    private fun setRearAcState(isOn: Boolean) {
        rearAcIcon.alpha = if (isOn) 0.8f else 0.3f
        rearAcToggleButton.text = if (isOn) "Rear AC On" else "Rear AC Off"
        val color = ContextCompat.getColor(requireContext(),
            if (isOn) R.color.ac_blue else R.color.ac_red)
        rearAcToggleButton.setBackgroundColor(color)
    }

    companion object {
        @JvmStatic
        fun newInstance() = AcControlFragment()
    }
}
