package com.example.navya_2.feature.ambientlight.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.navya_2.R
import com.example.navya_2.data.dto.AmbientLightStateDto
import com.example.navya_2.feature.ambientlight.viewmodel.AmbientLightViewModel
import com.example.navya_2.feature.ambientlight.viewmodel.AmbientLightViewModelFactory
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class AmbientLightFragment : DialogFragment() {
    private lateinit var colorPickerView: ColorPickerView
    private lateinit var colorPreview: View
    private lateinit var brightnessSlider: Slider
    private val viewModel: AmbientLightViewModel by viewModels { AmbientLightViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
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
        super.onViewCreated(view, savedInstanceState)
        colorPickerView = view.findViewById(R.id.colorPickerView)
        colorPreview = view.findViewById(R.id.color_preview)
        brightnessSlider = view.findViewById(R.id.brightnessSlider)

        viewModel.ambientLightState.observe(viewLifecycleOwner) { state ->
            colorPreview.setBackgroundColor(state.color)
            brightnessSlider.value = state.brightness.toFloat()
            if (state.dangerLevel != "safe") {
                Toast.makeText(requireContext(), "Color will apply when safe.", Toast.LENGTH_SHORT).show()
            }
        }

        colorPickerView.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: com.skydoves.colorpickerview.ColorEnvelope, fromUser: Boolean) {
                viewModel.updateColor(envelope.color)
            }
        })

        brightnessSlider.addOnChangeListener { _, value, _ ->
            viewModel.updateBrightness(value.toInt())
        }

        viewModel.updateDangerLevelFromSharedPrefs()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AmbientLightFragment()
    }
}