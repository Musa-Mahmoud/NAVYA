package com.example.navya_2.feature.voiceassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.navya_2.data.local.VoskModelManager
import com.example.navya_2.data.repository.VoiceAssistantRepository
import com.example.navya_2.data.vhal.VhalManager
import com.example.navya_2.ui.AppViewModelFactory

class VoiceAssistantViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceAssistantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoiceAssistantViewModel(
                VoiceAssistantRepository(
                    AppViewModelFactory.context,
                    VhalManager(AppViewModelFactory.context),
                    VoskModelManager(AppViewModelFactory.context)
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}