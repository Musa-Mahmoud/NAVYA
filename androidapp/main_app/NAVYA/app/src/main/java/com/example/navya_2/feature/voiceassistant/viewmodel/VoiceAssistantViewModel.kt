//package com.example.navya_2.feature.voiceassistant.viewmodel
//
//import android.content.Context
//import android.media.MediaPlayer
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import com.example.navya_2.R
//import com.example.navya_2.data.dto.VoiceCommandDto
//import com.example.navya_2.data.repository.VoiceAssistantRepository
//import com.example.navya_2.ui.AppViewModelFactory
//import org.vosk.Model
//import org.vosk.android.RecognitionListener
//import org.vosk.android.SpeechService
//
//class VoiceAssistantViewModel(
//    private val voiceAssistantRepository: VoiceAssistantRepository
//) : ViewModel() {
//    private val _isListening = MutableLiveData(false)
//    val isListening: LiveData<Boolean> = _isListening
//    val voiceCommand: LiveData<VoiceCommandDto> = voiceAssistantRepository.voiceCommand
//    private var mediaPlayer: MediaPlayer? = null
//    private var speechService: SpeechService? = null
//    private val stopPhrases = listOf(
//        "hi navya", "light on", "light off", "ac on", "ac off", "ac of", "stop",
//        "temperature down", "left camera", "right camera", "hi vehicle",
//        "hello car", "hello navya", "hello vehicle", "temperature up", "hi", "hello",
//        "right camera on", "right camera off", "left camera on", "left camera off", "danger wa"
//    )
//
//    fun initializeModel() {
//        val model = voiceAssistantRepository.getVoskModel()
//        if (model != null) {
//            _isListening.value = true
//        }
//    }
//
//    fun startRecognition(listener: RecognitionListener) {
//        try {
//            val model = voiceAssistantRepository.getVoskModel() ?: return
//            val (recognizer, service) = voiceAssistantRepository.createRecognizer(model)
//            speechService = service
//            service.startListening(listener)
//            voiceAssistantRepository.setVoiceCommand("Processing...", "Listening")
//            _isListening.value = true
//        } catch (e: Exception) {
//            voiceAssistantRepository.setVoiceCommand("Recognition failed: ${e.message}", "Text")
//            _isListening.value = false
//        }
//    }
//
//    fun stopRecognition() {
//        speechService?.stop()
//        speechService = null
//        voiceAssistantRepository.setVoiceCommand(voiceCommand.value?.text ?: "", "Text")
//        _isListening.value = false
//    }
//
//    fun updateRecognizedText(text: String, displayState: String) {
//        val normalized = text.lowercase().trim()
//        voiceAssistantRepository.setVoiceCommand(text, displayState)
//        if (stopPhrases.contains(normalized) && (displayState == "Text" || displayState == "PlayingSound")) {
//            when (normalized) {
//                "ac on" -> {
//                    voiceAssistantRepository.setAcStatus(1)
//                    playSound(R.raw.ac_on)
//                }
//                "ac off", "ac of" -> {
//                    voiceAssistantRepository.setAcStatus(0)
//                    playSound(R.raw.ac_off)
//                }
//                "temperature down" -> playSound(R.raw.temp_down)
//                "temperature up" -> playSound(R.raw.temp_up)
//                "light off", "light of" -> {
//                    voiceAssistantRepository.setAcStatus(0)
//                    playSound(R.raw.light_off)
//                }
//                "light on" -> {
//                    voiceAssistantRepository.setAcStatus(1)
//                    playSound(R.raw.light_on)
//                }
//                "hi car", "hello car", "hi vehicle", "hello", "hi" -> playSound(R.raw.hey_car)
//                "right camera on", "right camera off", "left camera on", "left camera off" -> playSound(R.raw.right_camera)
//                "danger wa" -> playSound(R.raw.danger_wa)
//            }
//            _isListening.value = false
//        }
//    }
//
//    private fun playSound(resourceId: Int) {
//        try {
//            mediaPlayer?.release()
//            mediaPlayer = MediaPlayer.create(AppViewModelFactory.context, resourceId)
//            mediaPlayer?.setOnCompletionListener {
//                it.release()
//                mediaPlayer = null
//                voiceAssistantRepository.setVoiceCommand(voiceCommand.value?.text ?: "", "Text")
//            }
//            mediaPlayer?.start()
//        } catch (e: Exception) {
//            voiceAssistantRepository.setVoiceCommand(voiceCommand.value?.text ?: "", "Text")
//        }
//    }
//
//    fun isStopPhrase(text: String): Boolean = stopPhrases.contains(text.lowercase().trim())
//
//    override fun onCleared() {
//        super.onCleared()
//        mediaPlayer?.release()
//        mediaPlayer = null
//        speechService?.stop()
//        speechService = null
//        voiceAssistantRepository.cleanup()
//    }
//}