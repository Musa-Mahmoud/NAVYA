package com.example.navya_2.feature.blindspot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.navya_2.data.local.SharedPrefsManager
import com.example.navya_2.data.repository.BlindSpotRepository
import com.example.navya_2.data.vhal.VhalManager
import com.example.navya_2.ui.AppViewModelFactory
import com.example.navya_2.util.ObjectDetectorHelper

class BlindSpotViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlindSpotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlindSpotViewModel(
                AppViewModelFactory.context,
                BlindSpotRepository(
                    AppViewModelFactory.context,
                    VhalManager(AppViewModelFactory.context),
                    SharedPrefsManager(AppViewModelFactory.context)
                ),
                ObjectDetectorHelper(AppViewModelFactory.context, numThreads = 1, useGpu = false)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}