//package com.example.navya_2.feature.voiceassistant.view
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.util.AttributeSet
//import android.view.View
//import kotlin.random.Random
//
//class WaveformView @JvmOverloads constructor(
//    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//    private val paint = Paint().apply {
//        color = Color.BLUE
//        strokeWidth = 5f
//        style = Paint.Style.STROKE
//    }
//    private val amplitudes = mutableListOf<Float>()
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        val width = width.toFloat()
//        val height = height.toFloat()
//        val centerY = height / 2
//        val maxAmplitude = height / 4
//
//        if (amplitudes.isEmpty()) {
//            amplitudes.addAll(List(20) { Random.nextFloat() * maxAmplitude })
//        }
//
//        val step = width / amplitudes.size
//        for (i in amplitudes.indices) {
//            val x = i * step
//            canvas.drawLine(x, centerY - amplitudes[i], x, centerY + amplitudes[i], paint)
//        }
//    }
//
//    fun updateWaveform() {
//        amplitudes.clear()
//        amplitudes.addAll(List(20) { Random.nextFloat() * (height / 4) })
//        invalidate()
//    }
//}