package com.example.navya_2.feature.blindspot.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.camera.view.PreviewView
import com.example.navya_2.R
import com.example.navya_2.feature.blindspot.viewmodel.BlindSpotViewModel
import com.example.navya_2.feature.blindspot.viewmodel.BlindSpotViewModelFactory
import com.example.navya_2.data.repository.BlindSpotRepository
import com.example.navya_2.util.ObjectDetectorHelper
import com.example.navya_2.util.SharedState

class BlindSpotFragment : Fragment() {

    private lateinit var viewModel: BlindSpotViewModel
    private lateinit var previewView: PreviewView
    private lateinit var instructionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera_feed, container, false)
        previewView = view.findViewById(R.id.preview_view)
        instructionText = view.findViewById(R.id.instruction_text)
        viewModel.setSurfaceProvider(previewView.surfaceProvider)
        viewModel.setLifecycleOwner(this)
        observeViewModel()
        viewModel.initializeCamera()
        return view
    }

    private fun setupViewModel() {
        val factory = BlindSpotViewModelFactory(
            BlindSpotRepository(
                requireContext(),
                ObjectDetectorHelper(requireContext())
            )
        )
        viewModel = ViewModelProvider(this, factory)[BlindSpotViewModel::class.java]
    }

    private fun observeViewModel() {
        viewModel.cameraState.observe(viewLifecycleOwner) { isOn ->
            if (isOn) {
                previewView.visibility = View.VISIBLE
                instructionText.visibility = View.GONE
            } else {
                previewView.visibility = View.GONE
                instructionText.visibility = View.VISIBLE
                instructionText.text = getString(R.string.camera_view)
            }
        }

        viewModel.detectionResult.observe(viewLifecycleOwner) { result ->
            instructionText.text = result ?: getString(R.string.camera_view)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopCamera()
    }
}