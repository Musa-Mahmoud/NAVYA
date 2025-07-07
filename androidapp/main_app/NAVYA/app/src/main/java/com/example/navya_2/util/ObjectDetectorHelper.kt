package com.example.navya_2.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions
import java.io.IOException

class ObjectDetectorHelper(
    private val context: Context,
    private val threshold: Float = 0.5f,
    private val maxResults: Int = 4,
    private val numThreads: Int = 2,
    private val useGpu: Boolean = false
) {
    private var detector: ObjectDetector? = null

    companion object {
        const val INPUT_SIZE = 448
        private const val TAG = "ObjectDetectorHelper"
    }

    fun warmUp() {
        ensureInitialized()
    }

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
            Log.d(TAG, "ObjectDetector initialized successfully.")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load TFLite model: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid arguments for ObjectDetector setup: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ObjectDetector: ${e.message}", e)
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
        detector = null
        Log.d(TAG, "ObjectDetector closed.")
    }
}