package com.example.navya_2

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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

    init {
        setupDetector()
    }

    private fun setupDetector() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setNumThreads(numThreads)

            // If you want to use GPU, you need to add the 'org.tensorflow:tensorflow-lite-gpu' dependency
            // and use baseOptionsBuilder.addDelegate(GpuDelegate()).
            // Otherwise, the default CPU backend (which often uses XNNPACK internally) will be used.
            if (useGpu) {
                // Example for GPU delegate (requires 'tensorflow-lite-gpu' dependency)
                // try {
                //     baseOptionsBuilder.addDelegate(GpuDelegate())
                //     Log.d(TAG, "GPU delegate added.")
                // } catch (e: Exception) {
                //     Log.e(TAG, "Failed to add GPU delegate: ${e.message}. Falling back to CPU.", e)
                // }
                Log.w(TAG, "GPU delegate requested but not explicitly configured. Using default CPU backend.")
            }

            val options = ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
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
        if (detector == null) {
            Log.e(TAG, "Detector is null, cannot perform detection.")
            return emptyList()
        }
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val tensorImage = TensorImage.fromBitmap(resizedBitmap)
        return detector?.detect(tensorImage) ?: emptyList()
    }

    fun close() {
        detector?.close()
        detector = null
        Log.d(TAG, "ObjectDetector closed.")
    }
}
