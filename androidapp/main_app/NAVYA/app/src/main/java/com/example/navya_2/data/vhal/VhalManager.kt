//package com.example.navya_2.data.vhal
//
//import android.car.Car
//import android.car.hardware.property.CarPropertyManager
//import android.content.Context
//import android.graphics.Color
//import android.util.Log
//import com.example.navya_2.util.SwitchState
//
//class VhalManager(private val context: Context) {
//    private var car: Car? = null
//    private var carPropertyManager: CarPropertyManager? = null
//    private val TAG = "VhalManager"
//
//    companion object {
//        private const val SWITCH_PROPERTY_ID = 557842692
//        private const val AMBIENT_PROPERTY_ID = 557842693
//        private const val HVAC_PROPERTY_ID = 557842694
//        private const val AREA_ID = 0
//        private const val HVAC_AREA_ID = 0x01000000
//    }
//
//    init {
//        try {
//            car = Car.createCar(context)
//            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
//            Log.d(TAG, "VHAL initialized successfully")
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to initialize VHAL: ${e.message}")
//        }
//    }
//
//    fun readSwitchState(): Int {
//        return try {
//            val prop = carPropertyManager?.getProperty(Integer::class.java, SWITCH_PROPERTY_ID, AREA_ID)
//            prop?.value?.toInt() ?: SwitchState.SWITCH_INVALID
//        } catch (e: Exception) {
//            Log.e(TAG, "Error reading switch: ${e.message}")
//            SwitchState.SWITCH_INVALID
//        }
//    }
//
//    fun setAmbientLightProperty(color: Int, brightness: Int) {
//        try {
//            val red = Color.red(color)
//            val green = Color.green(color)
//            val blue = Color.blue(color)
//            val packedColor = (brightness shl 24) or (blue shl 16) or (green shl 8) or red
//            carPropertyManager?.let {
//                if (it.isPropertyAvailable(AMBIENT_PROPERTY_ID, AREA_ID)) {
//                    it.setIntProperty(AMBIENT_PROPERTY_ID, AREA_ID, packedColor)
//                    Log.d(TAG, "Set ambient light to $packedColor")
//                } else {
//                    Log.e(TAG, "Ambient property not available")
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error setting ambient property: ${e.message}")
//        }
//    }
//
//    fun setAcStatus(status: Int) {
//        try {
//            carPropertyManager?.let {
//                if (it.isPropertyAvailable(HVAC_PROPERTY_ID, HVAC_AREA_ID)) {
//                    it.setIntProperty(HVAC_PROPERTY_ID, HVAC_AREA_ID, if (status == 1) 1 else 0)
//                    Log.d(TAG, "Set HVAC to ${if (status == 1) "ON" else "OFF"}")
//                } else {
//                    Log.e(TAG, "HVAC property not available")
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error setting HVAC property: ${e.message}")
//        }
//    }
//
//    fun cleanup() {
//        car?.disconnect()
//        Log.d(TAG, "VHAL disconnected")
//    }
//}