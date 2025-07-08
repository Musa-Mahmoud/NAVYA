//package com.example.navya_2.feature.ambientlight.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.example.navya_2.data.local.SharedPrefsManager
//import com.example.navya_2.data.repository.AmbientLightRepository
//import com.example.navya_2.data.vhal.VhalManager
//import com.example.navya_2.ui.AppViewModelFactory
//
//class AmbientLightViewModelFactory : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(AmbientLightViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return AmbientLightViewModel(
//                AmbientLightRepository(
//                    AppViewModelFactory.context,
//                    VhalManager(AppViewModelFactory.context),
//                    SharedPrefsManager(AppViewModelFactory.context)
//                )
//            ) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}