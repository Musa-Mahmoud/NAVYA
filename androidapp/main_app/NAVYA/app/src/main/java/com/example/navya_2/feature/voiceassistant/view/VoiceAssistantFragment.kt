//package com.example.navya_2.feature.voiceassistant.view
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.view.animation.AlphaAnimation
//import android.view.animation.Animation
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import com.example.navya_2.R
//import com.example.navya_2.feature.voiceassistant.viewmodel.VoiceAssistantViewModel
//import com.example.navya_2.feature.voiceassistant.viewmodel.VoiceAssistantViewModelFactory
//import org.vosk.android.RecognitionListener
//
//class VoiceAssistantFragment : Fragment(), RecognitionListener {
//    private lateinit var titleText: TextView
//    private lateinit var waveformView: WaveformView
//    private lateinit var recognizedTextView: TextView
//    private val viewModel: VoiceAssistantViewModel by viewModels { VoiceAssistantViewModelFactory() }
//    private val TAG = "VoiceAssistantFragment"
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_vosk, container, false)
//        titleText = view.findViewById(R.id.title_text)
//        waveformView = view.findViewById(R.id.waveform_view)
//        recognizedTextView = view.findViewById(R.id.recognized_text)
//        return view
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        checkPermissionAndInit()
//
//        viewModel.voiceCommand.observe(viewLifecycleOwner) { command ->
//            when (command.displayState) {
//                "Idle" -> {
//                    waveformView.visibility = View.GONE
//                    recognizedTextView.visibility = View.GONE
//                }
//                "Listening" -> {
//                    waveformView.visibility = View.VISIBLE
//                    recognizedTextView.visibility = View.GONE
//                    waveformView.invalidate()
//                }
//                "PlayingSound" -> {
//                    waveformView.visibility = View.VISIBLE
//                    recognizedTextView.visibility = if (command.text.isNotBlank()) View.VISIBLE else View.GONE
//                    waveformView.invalidate()
//                }
//                "Text" -> {
//                    waveformView.visibility = View.GONE
//                    recognizedTextView.visibility = if (command.text.isNotBlank()) View.VISIBLE else View.GONE
//                    if (viewModel.isStopPhrase(command.text)) {
//                        val glow = AlphaAnimation(0f, 0.5f).apply {
//                            duration = 300
//                            setAnimationListener(object : Animation.AnimationListener {
//                                override fun onAnimationStart(animation: Animation?) {}
//                                override fun onAnimationEnd(animation: Animation?) {
//                                    val fadeOut = AlphaAnimation(0.5f, 0f).apply { duration = 600 }
//                                    recognizedTextView.startAnimation(fadeOut)
//                                }
//                                override fun onAnimationRepeat(animation: Animation?) {}
//                            })
//                        }
//                        recognizedTextView.startAnimation(glow)
//                    }
//                }
//            }
//            recognizedTextView.text = command.text
//        }
//
//        viewModel.isListening.observe(viewLifecycleOwner) { isListening ->
//            if (isListening) {
//                viewModel.startRecognition(this)
//            } else {
//                viewModel.stopRecognition()
//            }
//        }
//    }
//
//    private fun checkPermissionAndInit() {
//        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
//        } else {
//            viewModel.initializeModel()
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            viewModel.initializeModel()
//        } else {
//            viewModel.updateRecognizedText("Permission denied", "Text")
//        }
//    }
//
//    override fun onPartialResult(hypothesis: String?) {
//        viewModel.updateRecognizedText(hypothesis ?: "Processing...", "Listening")
//    }
//
//    override fun onResult(hypothesis: String?) {
//        viewModel.updateRecognizedText(hypothesis ?: "Processing...", "Listening")
//    }
//
//    override fun onFinalResult(hypothesis: String?) {
//        viewModel.updateRecognizedText(hypothesis ?: "No text recognized", "Text")
//    }
//
//    override fun onError(exception: Exception?) {
//        viewModel.updateRecognizedText("Error: ${exception?.message}", "Text")
//    }
//
//    override fun onTimeout() {
//        viewModel.updateRecognizedText("Recognition timed out", "Text")
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//    }
//}