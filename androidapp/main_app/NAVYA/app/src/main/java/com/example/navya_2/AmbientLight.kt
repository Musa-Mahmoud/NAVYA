package com.example.navya_2

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import androidx.core.content.edit

class AmbientLight : DialogFragment() {

    private lateinit var colorPickerView: ColorPickerView
    private lateinit var colorPreview: View
    private lateinit var brightnessSlider: Slider
    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private var userPickedColor: Int = Color.WHITE
    private var currentDangerLevel = DangerLevel.SAFE
    private var brightness = 255

    private val TAG = "AmbientLight"
    private val propertyId = 557842693
    private val areaId = 0

    enum class DangerLevel { SAFE, MEDIUM, DANGER }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        if (key == SharedState.KEY_STATE) {
            val newState = sharedPrefs.getString(SharedState.KEY_STATE, "safe") ?: "safe"
            val level = when (newState.lowercase()) {
                "safe" -> DangerLevel.SAFE
                "medium" -> DangerLevel.MEDIUM
                "danger" -> DangerLevel.DANGER
                else -> DangerLevel.SAFE
            }
            Log.d(TAG, "Preference changed: KEY_STATE = $newState, resolved level = $level")
            updateDangerLevel(level)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)

        val prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        car = Car.createCar(requireContext(), null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
            if (ready) {
                carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                Log.d(TAG, "CarPropertyManager initialized")

                val state = prefs.getString(SharedState.KEY_STATE, "safe") ?: "safe"
                val level = when (state.lowercase()) {
                    "safe" -> DangerLevel.SAFE
                    "medium" -> DangerLevel.MEDIUM
                    "danger" -> DangerLevel.DANGER
                    else -> DangerLevel.SAFE
                }
                updateDangerLevel(level)

            } else {
                Log.e(TAG, "Failed to connect to Car service")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(R.drawable.rounded_rectangle_background)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.5).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setDimAmount(0.5f)
        return inflater.inflate(R.layout.fragment_ambient_light, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        colorPickerView = view.findViewById(R.id.colorPickerView)
        colorPreview = view.findViewById(R.id.color_preview)
        brightnessSlider = view.findViewById(R.id.brightnessSlider)

        userPickedColor = loadSavedColor()
        colorPreview.setBackgroundColor(userPickedColor)

        colorPickerView.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: com.skydoves.colorpickerview.ColorEnvelope, fromUser: Boolean) {
                userPickedColor = envelope.color
                colorPreview.setBackgroundColor(userPickedColor)
                saveColorToPreferences(userPickedColor)

                if (currentDangerLevel == DangerLevel.SAFE) {
                    setVhalProperty(userPickedColor)
                } else {
                    Toast.makeText(requireContext(), "Color will apply when safe.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        brightnessSlider.value = 50f
        brightnessSlider.addOnChangeListener { _, value, _ ->
            brightness = value.toInt()
            Log.d(TAG, "Brightness = $brightness")
            if (currentDangerLevel == DangerLevel.SAFE) {
                setVhalProperty(userPickedColor)
            }
        }
    }

    private fun saveColorToPreferences(color: Int) {
        val prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(SharedState.KEY_AMBIENT_COLOR, color) }
        Log.d(TAG, "Saved color to preferences: $color")
    }

    private fun loadSavedColor(): Int {
        val prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(SharedState.KEY_AMBIENT_COLOR, Color.WHITE)
    }

    private fun setVhalProperty(color: Int) {
        try {
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)

            val packedColor = (brightness shl 24) or (blue shl 16) or (green shl 8) or red
            Log.d(TAG, "Packed RGB = $packedColor (R=$red, G=$green, B=$blue, Bright=$brightness)")

            if (::carPropertyManager.isInitialized && carPropertyManager.isPropertyAvailable(propertyId, areaId)) {
                carPropertyManager.setIntProperty(propertyId, areaId, packedColor)
                Log.d(TAG, "VHAL property set successfully.")
            } else {
                Log.e(TAG, "VHAL property not available or manager not initialized.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting VHAL property: ${e.message}")
        }
    }

    fun updateDangerLevel(level: DangerLevel) {
        currentDangerLevel = level
        Log.d(TAG, "Danger level changed to: $level")
        when (level) {
            DangerLevel.SAFE -> setVhalProperty(userPickedColor)
            DangerLevel.MEDIUM -> {val packedColor = (100 shl 24) or (0 shl 16) or (255 shl 8) or 255
                ; setVhalProperty(packedColor)}
            DangerLevel.DANGER -> {
                val packedColor = (100 shl 24) or (0 shl 16) or (0 shl 8) or 255
                ; setVhalProperty(packedColor)}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        car.disconnect()
        setVhalProperty(Color.BLACK)
    }

    companion object {
        @JvmStatic
        fun newInstance() = AmbientLight()
    }
}
