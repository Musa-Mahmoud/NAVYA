//package com.iti.camera2
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.RectF
//import android.util.Log
//import com.iti.camera2.dto.Detection
//import com.iti.camera2.dto.DistanceCategory
//import org.tensorflow.lite.DataType
//import org.tensorflow.lite.Interpreter
//import org.tensorflow.lite.support.common.FileUtil
//import org.tensorflow.lite.support.common.ops.NormalizeOp
//import org.tensorflow.lite.support.image.TensorImage
//import org.tensorflow.lite.support.image.ImageProcessor
//import org.tensorflow.lite.support.image.ops.ResizeOp
//import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
//import java.nio.ByteBuffer
//import kotlin.math.log10
//
//class DepthEstimator(context: Context) {
//    private val interpreter: Interpreter
//
//    init {
//        val model = FileUtil.loadMappedFile(context, "midas-tflite-v2-1-small-lite-v1.tflite")
//        val options = Interpreter.Options().apply {
////            setUseNNAPI(true)
//            setAllowFp16PrecisionForFp32(true)
//        }
//        interpreter = Interpreter(model, options)
//        // Log input tensor details
//        val inputTensor = interpreter.getInputTensor(0)
//        Log.d("DepthEstimator", "Input shape: ${inputTensor.shape().contentToString()}")
//        Log.d("DepthEstimator", "Input data type: ${inputTensor.dataType()}")
//    }
//
//    fun estimateDistance(bitmap: Bitmap, detection: Detection): Float {
//        // Preprocess image for depth model
//        val depthInput = preprocessImage(bitmap)
//
//        // Run depth estimation
//        val depthOutput = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { FloatArray(1) } } }
//        interpreter.run(depthInput.buffer, depthOutput)
//
//        // Convert depth to distance
//        return calculateObjectDistance(
//            depthOutput[0],
//            detection.location,
//            bitmap.width,
//            bitmap.height,
//            detection.label
//        )
//    }
//
//    private fun preprocessImage(bitmap: Bitmap): TensorImage {
//        // Create TensorImage with FLOAT32 data type
//        val tensorImage = TensorImage(DataType.FLOAT32)
//        tensorImage.load(bitmap)
//
//        // Create processor to resize and normalize
//        val imageProcessor = ImageProcessor.Builder()
//            .add(ResizeWithCropOrPadOp(INPUT_SIZE, INPUT_SIZE)) // Center crop
//            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
//            .add(NormalizeOp(0f, 255f)) // Normalize to [0, 1]
//            .build()
//
//        return imageProcessor.process(tensorImage)
//    }
//
//    private fun calculateObjectDistance(
//        depthMap: Array<Array<FloatArray>>,
//        location: RectF,
//        imageWidth: Int,
//        imageHeight: Int,
//        objectLabel: String
//    ): Float {
//        // Convert bounding box to depth map coordinates
//        val scaleX = INPUT_SIZE / imageWidth.toFloat()
//        val scaleY = INPUT_SIZE / imageHeight.toFloat()
//
//        val startX = (location.left * scaleX).toInt().coerceIn(0, INPUT_SIZE - 1)
//        val startY = (location.top * scaleY).toInt().coerceIn(0, INPUT_SIZE - 1)
//        val endX = (location.right * scaleX).toInt().coerceIn(0, INPUT_SIZE - 1)
//        val endY = (location.bottom * scaleY).toInt().coerceIn(0, INPUT_SIZE - 1)
//
//        // 1. Get baseline depth using minimum value in region
//        var minDepth = Float.MAX_VALUE
//        for (x in startX..endX) {
//            for (y in startY..endY) {
//                val depthValue = depthMap[y][x][0]
//                if (depthValue < minDepth) minDepth = depthValue
//            }
//        }
//
//        // 2. Calculate relative size factor
//        val boxArea = location.width() * location.height()
//        val imageArea = imageWidth * imageHeight
//        val relativeSize = boxArea / imageArea
//
//        // 3. Get expected size for this object type
//        val expectedSize = getExpectedSizeForObject(objectLabel)
//
//        // 4. Calculate size-based depth adjustment
//        val sizeFactor = expectedSize / relativeSize
//
//        // 5. Combine depth information with size information
//        val depth = minDepth * DEPTH_SCALE_FACTOR
//        val sizeAdjustedDepth = depth * sizeFactor
//
//        return sizeAdjustedDepth
//    }
//
//    fun getDistanceCategory(distance: Float): DistanceCategory {
//        return when {
//            distance < CLOSE_THRESHOLD -> DistanceCategory.CLOSE
//            distance < NEAR_THRESHOLD -> DistanceCategory.NEAR
//            else -> DistanceCategory.FAR
//        }
//    }
//
//    fun close() {
//        interpreter.close()
//    }
//
//    private fun getExpectedSizeForObject(label: String): Float {
//        // Define expected relative sizes for common objects
//        return when (label.lowercase()) {
//            "car", "truck", "bus" -> 0.15f  // Large objects
//            "person", "bicycle", "motorcycle" -> 0.05f  // Medium objects
//            "cat", "dog" -> 0.02f  // Small objects
//            else -> 0.07f  // Default for unknown objects
//        }
//    }
//
//    companion object {
//        private const val INPUT_SIZE = 256 // MiDaS small model input size
//
//        // Distance thresholds (in meters)
//        private const val CLOSE_THRESHOLD = 3.0f
//        private const val NEAR_THRESHOLD = 6.0f
//        private const val DEPTH_SCALE_FACTOR = 1.0f // Calibrate for your camera = 0.2594594595
//    }
//}