package com.example.navya_2

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions
import androidx.core.graphics.scale

class ObjectDetectorHelper(
    private val context: Context,
    private val threshold: Float = 0.5f,
    private val maxResults: Int = 4,
    private val numThreads: Int = 2,
    private val useGpu: Boolean = false
) {
    private var detector: ObjectDetector? = null

    private fun ensureInitialized() {
        if (detector != null) return
        try {
            val baseOptions = BaseOptions.builder()
                .setNumThreads(numThreads)
                .apply { if (useGpu) useGpu() }
                .build()

            val options = ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                .build()

            detector = ObjectDetector.createFromFileAndOptions(
                context,
                "efficientdet-tflite-lite2-detection-metadata-v1.tflite",
                options
            )
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Failed to initialize detector", e)
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        ensureInitialized()
        return try {
            val resizedBitmap = bitmap.scale(INPUT_SIZE, INPUT_SIZE)
            val image = TensorImage.fromBitmap(resizedBitmap)
            detector?.detect(image) ?: emptyList()
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Error detecting objects", e)
            emptyList()
        }
    }

    fun close() {
        detector?.close()
    }

    companion object {
        const val INPUT_SIZE = 448
    }
}