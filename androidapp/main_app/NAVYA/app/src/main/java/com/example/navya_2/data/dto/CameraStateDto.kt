package com.example.navya_2.data.dto

data class CameraStateDto(
    val switchState: Int,
    val distanceState: Int,
    val closestDistance: Float,
    val closestLabel: String?,
    val isCameraOn: Boolean
)