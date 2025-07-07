package com.example.navya_2.feature.car.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.navya_2.data.local.SharedPrefsManager
import com.example.navya_2.data.repository.BlindSpotRepository
import com.example.navya_2.data.vhal.VhalManager
import com.example.navya_2.ui.AppViewModelFactory

class CarViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarViewModel(
                BlindSpotRepository(
                    AppViewModelFactory.context,
                    VhalManager(AppViewModelFactory.context),
                    SharedPrefsManager(AppViewModelFactory.context)
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    companion object {
        val Factory = CarViewModelFactory()
    }
}