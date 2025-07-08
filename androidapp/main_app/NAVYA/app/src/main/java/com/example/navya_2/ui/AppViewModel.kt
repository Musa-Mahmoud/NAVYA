//package com.example.navya_2.ui
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//
//class AppViewModel : ViewModel() {
//    private val _isVoiceAssistantVisible = MutableLiveData<Boolean>(false)
//    val isVoiceAssistantVisible: LiveData<Boolean> = _isVoiceAssistantVisible
//
//    private val _isAmbientLightVisible = MutableLiveData<Boolean>(false)
//    val isAmbientLightVisible: LiveData<Boolean> = _isAmbientLightVisible
//
//    fun toggleVoiceAssistant() {
//        _isVoiceAssistantVisible.value = !(_isVoiceAssistantVisible.value ?: false)
//    }
//
//    fun toggleAmbientLight() {
//        _isAmbientLightVisible.value = !(_isAmbientLightVisible.value ?: false)
//    }
//}