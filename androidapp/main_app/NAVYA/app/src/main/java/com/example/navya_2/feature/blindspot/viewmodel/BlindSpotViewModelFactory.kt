package com.example.navya_2.feature.blindspot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.navya_2.data.repository.BlindSpotRepository

@Suppress("UNCHECKED_CAST")
class BlindSpotViewModelFactory(
    private val repository: BlindSpotRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(BlindSpotViewModel::class.java)) {
            BlindSpotViewModel(repository) as T
        } else {
            throw IllegalArgumentException("ViewModel class NOT found")
        }
    }
}