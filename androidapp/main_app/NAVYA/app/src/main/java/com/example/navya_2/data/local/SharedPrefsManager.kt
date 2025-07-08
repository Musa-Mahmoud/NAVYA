//package com.example.navya_2.data.local
//
//import android.content.Context
//import android.content.SharedPreferences
//import androidx.core.content.edit
//import com.example.navya_2.util.SharedState
//import com.example.navya_2.util.DistanceState
//import com.example.navya_2.util.SwitchState
//
//class SharedPrefsManager(context: Context) {
//    private val prefs: SharedPreferences = context.getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
//
//    fun saveSwitchState(state: Int) {
//        prefs.edit { putInt(SharedState.KEY_SWITCH_STATE, state) }
//    }
//
//    fun getSwitchState(): Int = prefs.getInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_INVALID)
//
//    fun saveDistanceState(state: Int) {
//        prefs.edit { putInt(SharedState.KEY_DISTANCE_SAFETY_STATE, state) }
//    }
//
//    fun getDistanceState(): Int = prefs.getInt(SharedState.KEY_DISTANCE_SAFETY_STATE, DistanceState.FAR)
//
//    fun saveAmbientColor(color: Int) {
//        prefs.edit { putInt(SharedState.KEY_AMBIENT_COLOR, color) }
//    }
//
//    fun getAmbientColor(): Int = prefs.getInt(SharedState.KEY_AMBIENT_COLOR, android.graphics.Color.WHITE)
//
//    fun saveDangerLevel(level: String) {
//        prefs.edit { putString(SharedState.KEY_STATE, level) }
//    }
//
//    fun getDangerLevel(): String = prefs.getString(SharedState.KEY_STATE, "safe") ?: "safe"
//}