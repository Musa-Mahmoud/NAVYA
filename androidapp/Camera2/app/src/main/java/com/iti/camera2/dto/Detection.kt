package com.iti.camera2.dto

import android.graphics.RectF

data class Detection(
    val label: String,
    val score: Float,
    val location: RectF,
//    val distance: Float? = null,         // Estimated distance in meters
//    val category: DistanceCategory? = null // Distance category
)