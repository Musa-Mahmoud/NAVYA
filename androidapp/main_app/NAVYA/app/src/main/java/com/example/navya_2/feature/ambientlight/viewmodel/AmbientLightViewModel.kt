//package com.example.navya_2.feature.ambientlight.viewmodel
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.navya_2.data.dto.AmbientLightStateDto
//import com.example.navya_2.data.repository.AmbientLightRepository
//import kotlinx.coroutines.launch
//
//class AmbientLightViewModel(
//    private val ambientLightRepository: AmbientLightRepository
//) : ViewModel() {
//    val ambientLightState: LiveData<AmbientLightStateDto> = ambientLightRepository.ambientLightState
//
//    fun updateColor(color: Int) {
//        val currentState = ambientLightRepository.getAmbientLightState()
//        ambientLightRepository.setAmbientLightProperty(color, currentState.brightness, currentState.dangerLevel)
//    }
//
//    fun updateBrightness(brightness: Int) {
//        val currentState = ambientLightRepository.getAmbientLightState()
//        ambientLightRepository.setAmbientLightProperty(currentState.color, brightness, currentState.dangerLevel)
//    }
//
//    fun updateDangerLevelFromSharedPrefs() {
//        viewModelScope.launch {
//            val currentState = ambientLightRepository.getAmbientLightState()
//            ambientLightRepository.setAmbientLightProperty(currentState.color, currentState.brightness, currentState.dangerLevel)
//        }
//    }
//}