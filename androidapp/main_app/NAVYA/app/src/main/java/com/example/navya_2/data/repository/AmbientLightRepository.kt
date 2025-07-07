package com.example.navya_2.data.repository

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.navya_2.data.dto.AmbientLightStateDto
import com.example.navya_2.data.local.SharedPrefsManager
import com.example.navya_2.data.vhal.VhalManager

class AmbientLightRepository(
    private val context: Context,
    private val vhalManager: VhalManager,
    private val sharedPrefsManager: SharedPrefsManager
) {
    private val _ambientLightState = MutableLiveData<AmbientLightStateDto>()
    val ambientLightState: LiveData<AmbientLightStateDto> = _ambientLightState

    init {
        val savedColor = sharedPrefsManager.getAmbientColor()
        val savedDangerLevel = sharedPrefsManager.getDangerLevel()
        _ambientLightState.value = AmbientLightStateDto(
            color = savedColor,
            brightness = 255,
            dangerLevel = savedDangerLevel
        )
    }

    fun setAmbientLightProperty(color: Int, brightness: Int, dangerLevel: String) {
        try {
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            val packedColor = when (dangerLevel.lowercase()) {
                "safe" -> (brightness shl 24) or (blue shl 16) or (green shl 8) or red
                "medium" -> (brightness shl 24) or (255 shl 8) or 255
                "danger" -> (brightness shl 24) or 255
                else -> (brightness shl 24) or (blue shl 16) or (green shl 8) or red
            }
            vhalManager.setAmbientLightProperty(packedColor, brightness)
            sharedPrefsManager.saveAmbientColor(color)
            sharedPrefsManager.saveDangerLevel(dangerLevel)
            _ambientLightState.postValue(
                _ambientLightState.value?.copy(
                    color = color,
                    brightness = brightness,
                    dangerLevel = dangerLevel
                ) ?: AmbientLightStateDto(color, brightness, dangerLevel)
            )
        } catch (e: Exception) {
            android.util.Log.e("AmbientLightRepository", "Error setting ambient light: ${e.message}")
        }
    }

    fun getAmbientLightState(): AmbientLightStateDto {
        return _ambientLightState.value ?: AmbientLightStateDto(
            color = sharedPrefsManager.getAmbientColor(),
            brightness = 255,
            dangerLevel = sharedPrefsManager.getDangerLevel()
        )
    }
}