package com.iti.camera2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.core.graphics.scale
import com.iti.camera2.dto.Detection

class ObjectDetector(context: Context) {
    private val interpreter: Interpreter

//    // Add depth estimator
//    private val depthEstimator = DepthEstimator(context)
    private val inputSize = 300 // Must be 300 for SSD MobileNet

    init {
        val model = FileUtil.loadMappedFile(context, "ssd-mobilenet-v1-tflite-metadata-v2.tflite")
        val options = Interpreter.Options()
        interpreter = Interpreter(model, options)
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val resized = bitmap.scale(inputSize, inputSize)
        val input = convertBitmapToByteBuffer(resized)

        // Output buffers for SSD MobileNet
        val outputLocations = Array(1) { Array(10) { FloatArray(4) } } // Bounding boxes
        val outputCategories = Array(1) { FloatArray(10) } // Class indices
        val outputScores = Array(1) { FloatArray(10) }    // Confidence scores
        val outputCount = FloatArray(1)                   // Detection count

        val outputs = mapOf<Int, Any>(
            0 to outputLocations,
            1 to outputCategories,
            2 to outputScores,
            3 to outputCount
        )

        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        val detectionCount = minOf(outputCount[0].toInt(), 3)
        val detections = mutableListOf<Detection>()
        val threshold = 0.5f

        for (i in 0 until detectionCount) {
            val score = outputScores[0][i]
            if (score < threshold) continue

            val classId = outputCategories[0][i].toInt()
            Log.d("classId", "Class ID: $classId")
            if (!desired(classId)) continue

            val location = outputLocations[0][i]

            val rect = RectF(
                location[1] * bitmap.width,  // xmin
                location[0] * bitmap.height, // ymin
                location[3] * bitmap.width,  // xmax
                location[2] * bitmap.height  // ymax
            )

            val label = labelMap[classId] ?: "Unknown"
            Log.d("classId", "label: $label")

            val detection = Detection(
                label,
                score,
                rect
            )
            // Estimate distance
//            val distance = depthEstimator.estimateDistance(bitmap, detection)
//            val category = depthEstimator.getDistanceCategory(distance)
//            Log.d("Depth", "distance: $distance")
//            Log.d("Depth", "category: $category")

            detections.add(detection
//                detection.copy(
//                    distance = distance,
//                    category = category
//                )
            )
        }

        return detections
    }

    private fun desired(classId: Int): Boolean {
        return (classId in 0..3 || classId in 5..7 || classId in 15..23)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // UINT8 buffer: 300x300x3 = 270,000 bytes
        val buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            // Extract RGB components (0-255)
            buffer.put((pixel shr 16 and 0xFF).toByte())    // Red
            buffer.put((pixel shr 8 and 0xFF).toByte())     // Green
            buffer.put((pixel and 0xFF).toByte())           // Blue
        }

        buffer.rewind()
        return buffer
    }

    fun close() {
        interpreter.close()
//        depthEstimator.close()
    }

    private val labelMap = mapOf(
        0 to "person",
        1 to "bicycle",
        2 to "car",
        3 to "motorcycle",
        4 to "airplane",
        5 to "bus",
        6 to "train",
        7 to "truck",
        8 to "boat",
        9 to "traffic light",
        10 to "fire hydrant",
        11 to "stop sign",
        12 to "parking meter",
        13 to "bench",
        14 to "bird",
        15 to "cat",
        16 to "dog",
        17 to "horse",
        18 to "sheep",
        19 to "cow",
        20 to "elephant",
        21 to "bear",
        22 to "zebra",
        23 to "giraffe",
        24 to "backpack",
        25 to "umbrella",
        26 to "handbag",
        27 to "tie",
        28 to "suitcase",
        29 to "frisbee",
        30 to "skis",
        31 to "snowboard",
        32 to "sports ball",
        33 to "kite",
        34 to "baseball bat",
        35 to "baseball glove",
        36 to "skateboard",
        37 to "surfboard",
        38 to "tennis racket",
        39 to "bottle",
        40 to "wine glass",
        41 to "cup",
        42 to "fork",
        43 to "knife",
        44 to "spoon",
        45 to "bowl",
        46 to "banana",
        47 to "apple",
        48 to "sandwich",
        49 to "orange",
        50 to "broccoli",
        51 to "carrot",
        52 to "hot dog",
        53 to "pizza",
        54 to "donut",
        55 to "cake",
        56 to "chair",
        57 to "couch",
        58 to "potted plant",
        59 to "bed",
        60 to "dining table",
        61 to "toilet",
        62 to "tv",
        63 to "laptop",
        64 to "mouse",
        65 to "remote",
        66 to "keyboard",
        67 to "cell phone",
        68 to "microwave",
        69 to "oven",
        70 to "toaster",
        71 to "sink",
        72 to "refrigerator",
        73 to "book",
        74 to "clock",
        75 to "vase",
        76 to "scissors",
        77 to "teddy bear",
        78 to "hair drier",
        79 to "toothbrush"
    )
}