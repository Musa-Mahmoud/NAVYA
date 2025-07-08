//package com.example.navya_2.data.repository
//
//import android.content.Context
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import com.example.navya_2.data.dto.VoiceCommandDto
//import com.example.navya_2.data.local.VoskModelManager
//import com.example.navya_2.data.vhal.VhalManager
//import org.vosk.Model
//import org.vosk.Recognizer
//import org.vosk.android.SpeechService
//import java.io.IOException
//
//class VoiceAssistantRepository(
//    private val context: Context,
//    private val vhalManager: VhalManager,
//    private val voskModelManager: VoskModelManager
//) {
//    private val _voiceCommand = MutableLiveData<VoiceCommandDto>()
//    val voiceCommand: LiveData<VoiceCommandDto> = _voiceCommand
//    private val TAG = "VoiceAssistantRepository"
//
//    fun getVoskModel(): Model? {
//        return try {
//            voskModelManager.initializeModel()
//        } catch (e: IOException) {
//            _voiceCommand.postValue(VoiceCommandDto("Model initialization failed: ${e.message}", "Text"))
//            null
//        }
//    }
//
//    fun createRecognizer(model: Model): Pair<Recognizer, SpeechService> {
//        try {
//            val recognizer = Recognizer(model, 16000.0f)
//            val speechService = SpeechService(recognizer, 16000.0f)
//            return Pair(recognizer, speechService)
//        } catch (e: IOException) {
//            _voiceCommand.postValue(VoiceCommandDto("Recognizer creation failed: ${e.message}", "Text"))
//            throw e
//        }
//    }
//
//    fun setVoiceCommand(text: String, displayState: String) {
//        _voiceCommand.postValue(VoiceCommandDto(text, displayState))
//    }
//
//    fun setAcStatus(status: Int) {
//        vhalManager.setAcStatus(status)
//    }
//
//    fun cleanup() {
//        voskModelManager.close()
//    }
//}