package com.example.navya_2.feature.blindspot.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.navya_2.R
import com.example.navya_2.data.dto.CameraStateDto
import com.example.navya_2.feature.blindspot.viewmodel.BlindSpotViewModel
import com.example.navya_2.feature.blindspot.viewmodel.BlindSpotViewModelFactory
import com.example.navya_2.util.DistanceState
import com.example.navya_2.util.SwitchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlindSpotFragment : Fragment() {
    private lateinit var previewView: PreviewView
    private lateinit var instructionText: TextView
    private val viewModel: BlindSpotViewModel by viewModels { BlindSpotViewModelFactory() }

    companion object {
        private const val TAG = "BlindSpotFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.startSwitchPolling()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera_feed, container, false)
        previewView = view.findViewById(R.id.preview_view)
        instructionText = view.findViewById(R.id.instruction_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.cameraState.observe(viewLifecycleOwner) { state ->
            updateUI(state)
        }
    }

    private fun updateUI(state: CameraStateDto) {
        when (state.switchState) {
            SwitchState.SWITCH_LEFT, SwitchState.SWITCH_RIGHT -> {
                if (!state.isCameraOn) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            viewModel.startCamera(this@BlindSpotFragment, previewView)
                        } catch (e: Exception) {
                            instructionText.text = getString(R.string.camera_unavailable)
                            android.widget.Toast.makeText(context, "Failed to start camera: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            SwitchState.SWITCH_CENTER, SwitchState.SWITCH_INVALID -> {
                if (state.isCameraOn) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        viewModel.stopCamera()
                    }
                }
            }
        }

        if (state.isCameraOn) {
            instructionText.text = if (state.closestLabel != null) {
                "${state.closestLabel} (${"%.1f".format(state.closestDistance)}m)"
            } else {
                "No objects detected"
            }
            previewView.visibility = View.VISIBLE
            instructionText.visibility = View.VISIBLE
        } else {
            instructionText.text = getString(R.string.camera_view)
            previewView.visibility = View.GONE
            instructionText.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "Fragment destroyed")
    }
}