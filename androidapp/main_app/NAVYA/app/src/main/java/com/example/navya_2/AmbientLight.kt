package com.example.navya_2

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class AmbientLight : Fragment() {
    private lateinit var colorPickerView: ColorPickerView
    private lateinit var brightnessSlider: Slider
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private var userPickedColor: Int = Color.WHITE
    private var currentDangerLevel = DangerLevel.SAFE
    private var brightness = 255
    private lateinit var prefs: SharedPreferences
    private val TAG = "AmbientLight"
    private val propertyId = 557842693
    private val areaId = 0

    enum class DangerLevel { SAFE, MEDIUM, DANGER }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        when (key) {
            SharedState.KEY_DISTANCE_SAFETY_STATE -> {
                val newState = sharedPrefs.getInt(SharedState.KEY_DISTANCE_SAFETY_STATE, DistanceState.FAR)
                val level = when (newState) {
                    DistanceState.CLOSE -> DangerLevel.DANGER
                    DistanceState.NEAR -> DangerLevel.MEDIUM
                    DistanceState.FAR -> DangerLevel.SAFE
                    else -> DangerLevel.SAFE
                }
                Log.d(TAG, "Safety state changed to: $level")
                updateDangerLevel(level)
            }
            SharedState.KEY_SWITCH_STATE -> {
                val switchState = sharedPrefs.getInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_INVALID)
                if (switchState != SwitchState.SWITCH_CENTER) {
                    (activity as? FragmentSwitchListener)?.switchToCameraFeedFragment()
                        ?: Log.w(TAG, "Activity does not implement FragmentSwitchListener")
                    Log.d(TAG, "Switch state changed to $switchState, switching to CameraFeedFragment")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        car = Car.createCar(requireContext(), null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
            if (ready) {
                carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                Log.d(TAG, "CarPropertyManager initialized")
                updateDangerLevel(getInitialDangerLevel())
            } else {
                Log.e(TAG, "Car service connection failed")
                showToast("Car service unavailable")
            }
        }

        // Check initial switch state
        val initialSwitchState = prefs.getInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_INVALID)
        if (initialSwitchState != SwitchState.SWITCH_CENTER) {
            (activity as? FragmentSwitchListener)?.switchToCameraFeedFragment()
                ?: Log.w(TAG, "Activity does not implement FragmentSwitchListener")
            Log.d(TAG, "Initial switch state $initialSwitchState, switching to CameraFeedFragment")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ambient_light, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        colorPickerView = view.findViewById(R.id.colorPickerView)
        brightnessSlider = view.findViewById(R.id.brightnessSlider)

        // Configure slider range for brightness (0-255)
        brightnessSlider.valueFrom = 0f
        brightnessSlider.valueTo = 255f
        brightnessSlider.value = 255f // Default to max brightness
        userPickedColor = loadSavedColor()

        colorPickerView.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: com.skydoves.colorpickerview.ColorEnvelope, fromUser: Boolean) {
                userPickedColor = envelope.color
                saveColorToPreferences(userPickedColor)
                if (currentDangerLevel == DangerLevel.SAFE) {
                    setVhalProperty(userPickedColor)
                } else {
                    showToast("Color will apply when safe")
                }
            }
        })

        brightnessSlider.addOnChangeListener { _, value, _ ->
            brightness = value.toInt()
            Log.d(TAG, "Brightness set to: $brightness")
            if (currentDangerLevel == DangerLevel.SAFE) {
                setVhalProperty(userPickedColor)
            }
        }

        updateDangerLevel(getInitialDangerLevel())
    }

    private fun getInitialDangerLevel(): DangerLevel {
        val initialState = prefs.getInt(SharedState.KEY_DISTANCE_SAFETY_STATE, DistanceState.FAR)
        return when (initialState) {
            DistanceState.CLOSE -> DangerLevel.DANGER
            DistanceState.NEAR -> DangerLevel.MEDIUM
            DistanceState.FAR -> DangerLevel.SAFE
            else -> DangerLevel.SAFE
        }
    }

    private fun saveColorToPreferences(color: Int) {
        prefs.edit { putInt(SharedState.KEY_AMBIENT_COLOR, color) }
        Log.d(TAG, "Saved color: $color")
    }

    private fun loadSavedColor(): Int {
        return prefs.getInt(SharedState.KEY_AMBIENT_COLOR, Color.WHITE)
    }

    private fun setVhalProperty(color: Int) {
        carPropertyManager?.let { manager ->
            try {
                if (manager.isPropertyAvailable(propertyId, areaId)) {
                    val red = Color.red(color)
                    val green = Color.green(color)
                    val blue = Color.blue(color)
                    val packedColor = (brightness shl 24) or (blue shl 16) or (green shl 8) or red
                    manager.setIntProperty(propertyId, areaId, packedColor)
                    Log.d(TAG, "Set VHAL property: RGB=$packedColor (R=$red, G=$green, B=$blue, Bright=$brightness)")
                } else {
                    Log.w(TAG, "VHAL property $propertyId unavailable")
                    showToast("Ambient light control unavailable")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting VHAL property: ${e.message}", e)
                showToast("Error setting ambient light")
            }
        } ?: run {
            Log.w(TAG, "CarPropertyManager not initialized")
            showToast("Car service not initialized")
        }
    }

    private fun updateDangerLevel(level: DangerLevel) {
        if (currentDangerLevel == level) return
        currentDangerLevel = level
        Log.d(TAG, "Danger level updated to: $level")
        when (level) {
            DangerLevel.SAFE -> setVhalProperty(userPickedColor)
            DangerLevel.MEDIUM -> setVhalProperty(Color.YELLOW)
            DangerLevel.DANGER -> setVhalProperty(Color.RED)
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        car?.disconnect()
        car = null
        carPropertyManager = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = AmbientLight()
    }
}