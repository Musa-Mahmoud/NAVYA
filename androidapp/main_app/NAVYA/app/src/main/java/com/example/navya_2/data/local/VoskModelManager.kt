package com.example.navya_2.data.local

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class VoskModelManager(private val context: Context) {
    private var model: Model? = null
    private val modelName = "vosk-model-small-en-us-0.15"
    private val TAG = "VoskModelManager"

    fun initializeModel(): Model? {
        val modelPath = File(context.filesDir, modelName).absolutePath
        unpackModelToInternal()
        return try {
            model = Model(modelPath)
            Log.d(TAG, "Model loaded successfully from $modelPath")
            model
        } catch (e: IOException) {
            Log.e(TAG, "Model initialization failed: ${e.message}")
            null
        }
    }

    private fun unpackModelToInternal() {
        val assetManager = context.assets
        val modelDir = File(context.filesDir, modelName)
        if (!modelDir.exists()) {
            try {
                modelDir.mkdirs()
                copyAssetFolder(assetManager, modelName, modelDir.absolutePath)
                Log.d(TAG, "Model unpacked to ${modelDir.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to unpack model: ${e.message}")
            }
        }
    }

    private fun copyAssetFolder(assetManager: AssetManager, srcPath: String, dstPath: String) {
        try {
            val files = assetManager.list(srcPath) ?: return
            val dstDir = File(dstPath)
            dstDir.mkdirs()
            for (fileName in files) {
                val srcFilePath = if (srcPath.isEmpty()) fileName else "$srcPath/$fileName"
                val dstFilePath = File(dstDir, fileName)
                val subFiles = assetManager.list(srcFilePath)
                if (subFiles?.isNotEmpty() == true) {
                    copyAssetFolder(assetManager, srcFilePath, dstFilePath.absolutePath)
                } else {
                    assetManager.open(srcFilePath).use { inputStream ->
                        FileOutputStream(dstFilePath).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy asset folder $srcPath: ${e.message}")
        }
    }

    fun close() {
        model?.close()
        model = null
        Log.d(TAG, "Model closed")
    }
}