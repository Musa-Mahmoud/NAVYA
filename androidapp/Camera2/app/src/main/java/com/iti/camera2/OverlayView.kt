package com.iti.camera2

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.text.format
import androidx.core.graphics.toColorInt

class OverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val detections = mutableListOf<Detection>()
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val bgPaint = Paint().apply {
        color = "#99000000".toColorInt()
        style = Paint.Style.FILL
    }

    fun setDetections(newDetections: List<Detection>) {
        detections.clear()
        detections.addAll(newDetections)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (detection in detections) {
            // Set different colors for different classes
            boxPaint.color = when (detection.label) {
                "person" -> Color.GREEN
                "car" -> Color.BLUE
                else -> Color.RED
            }

            // Draw bounding box
            canvas.drawRect(detection.location, boxPaint)

            // Draw label with confidence score
            val label = "${detection.label} (${"%.2f".format(detection.score)})"
            val textWidth = textPaint.measureText(label)
            val height = textPaint.fontMetrics.run { descent - ascent }

            canvas.drawRect(
                detection.location.left,
                detection.location.top,
                detection.location.left + textWidth + 20,
                detection.location.top + height + 20,
                bgPaint
            )

            canvas.drawText(
                label,
                detection.location.left + 10,
                detection.location.top + height + 10,
                textPaint
            )
        }
    }
}