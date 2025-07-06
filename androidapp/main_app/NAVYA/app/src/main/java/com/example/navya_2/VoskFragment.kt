package com.example.navya_2

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.*
import android.media.MediaPlayer
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Enum to manage UI display states
enum class DisplayState {
    Idle, Listening, PlayingSound, Text
}

// Manager class for VHAL operations
class VhalManager(private val context: Context) {
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null

    companion object {
        private const val TAG = "VhalManager"
        private const val HVAC_PROPERTY_ID = 557842694
        private const val AREA_ID = 0x01000000
    }

    init {
        try {
            car = Car.createCar(context)
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            Log.d(TAG, "VHAL initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VHAL: ${e.message}")
        }
    }

    fun setAcStatus(status: Int) {
        try {
            carPropertyManager?.let { manager ->
                if (manager.isPropertyAvailable(HVAC_PROPERTY_ID, AREA_ID)) {
                    manager.setIntProperty(HVAC_PROPERTY_ID, AREA_ID, if (status == 1) 1 else 0)
                    Log.d(TAG, "Set HVAC property to ${if (status == 1) "ON" else "OFF"}")
                } else {
                    Log.e(TAG, "HVAC property $HVAC_PROPERTY_ID is not available for area $AREA_ID")
                }
            } ?: Log.e(TAG, "CarPropertyManager not initialized")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when setting VHAL property: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid property or value: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting VHAL property: ${e.message}")
        }
    }

    fun cleanup() {
        car?.disconnect()
        Log.d(TAG, "VHAL disconnected")
    }
}

class NavyaVoiceViewModel(private val context: Context) : ViewModel() {
    private val _recognizedText = MutableLiveData("")
    val recognizedText: LiveData<String> = _recognizedText

    private val _isListening = MutableLiveData(false)
    val isListening: LiveData<Boolean> = _isListening

    private val _displayState = MutableLiveData(DisplayState.Idle)
    val displayState: LiveData<DisplayState> = _displayState

    private val stopPhrases = listOf(
        "hi navya", "light on", "light off", "ac on", "ac off", "ac of", "stop",
        "temperature down", "left camera", "right camera", "hi veachle",
        "hello car", "hello navya", "hello vehicle", "temperature up", "hi", "hello"
    )

    private var mediaPlayer: MediaPlayer? = null

    fun updateRecognizedText(text: String, state: DisplayState, vhalManager: VhalManager?) {
        val recognized = extractRecognizedText(text)
        val normalized = recognized.lowercase()

        Log.d(TAG, "Normalized text: $normalized")

        if (state == DisplayState.Listening || state == DisplayState.Text || state == DisplayState.PlayingSound) {
            _recognizedText.value = recognized
        } else {
            _recognizedText.value = text
        }

        if ((state == DisplayState.Listening || state == DisplayState.Text) && stopPhrases.contains(normalized)) {
            _isListening.value = false
            _displayState.value = DisplayState.PlayingSound
            when (normalized) {
                "ac on" -> {
                    vhalManager?.setAcStatus(1)
                    playSound(R.raw.ac_on)
                }
                "ac off", "ac of" -> {
                    vhalManager?.setAcStatus(0)
                    playSound(R.raw.ac_off)
                }
                "temperature down" -> {
                    playSound(R.raw.temp_down)
                }
                "temperature up" -> {
                    playSound(R.raw.temp_up)
                }
                "light off", "light of" -> {
                    vhalManager?.setAcStatus(0)
                    playSound(R.raw.light_off)
                }
                "light on" -> {
                    vhalManager?.setAcStatus(1)
                    playSound(R.raw.light_on)
                }
                "hi car", "hello car", "hi vehicle","hello","hi" -> {
                    playSound(R.raw.hey_car)
                }
                "stop" -> {
                    _displayState.value = DisplayState.Text // No sound, transition directly
                }
                "right camera on" -> {
                    playSound(R.raw.right_camera)
                }
                "right camera off" , "right camera of" -> {
                    playSound(R.raw.right_camera)
                }
                "left camera on" -> {
                    playSound(R.raw.left_camera)
                }
                "left camera off","left camera of" -> {
                    playSound(R.raw.left_camera)
                }
                "danger wa" -> {
                    playSound(R.raw.danger_wa)
                }
            }

            Log.d(TAG, "Stop phrase '$normalized' detected, stopping listening")
        } else {
            _displayState.value = state
        }
    }

    private fun playSound(resourceId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, resourceId)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
                _displayState.value = DisplayState.Text
                Log.d(TAG, "Sound playback completed, transitioning to Text state")
            }
            mediaPlayer?.start()
            Log.d(TAG, "Playing sound resource: $resourceId")
        } catch (e: Exception) {
            _displayState.value = DisplayState.Text
            Log.e(TAG, "Error playing sound: ${e.message}")
        }
    }

    fun toggleListening() {
        val isListening = !(_isListening.value ?: false)
        _isListening.value = isListening
        _displayState.value = if (isListening) DisplayState.Listening else DisplayState.Text
        Log.d(TAG, "Listening toggled: $isListening")
    }

    fun isStopPhrase(text: String): Boolean {
        val normalized = text.lowercase().trim()
        return stopPhrases.any { it == normalized }
    }

    private fun extractRecognizedText(result: String): String {
        Log.d(TAG, "Raw hypothesis: $result")
        val regex = Regex("\"(?:text|partial)\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(result)?.groupValues?.get(1)?.trim() ?: ""
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "ViewModel cleared")
    }

    companion object {
        private const val TAG = "NavyaVoiceViewModel"
        class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NavyaVoiceViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return NavyaVoiceViewModel(context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waveCount = 20
    private val amplitudes = FloatArray(waveCount) { Random.nextFloat() } // ✅ Initial values
    private val phaseShifts = FloatArray(waveCount) { Random.nextFloat() * 2f * Math.PI.toFloat() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 6f
        color = Color.GREEN // fallback color
        isAntiAlias = true
    }

    private val gradientColors = intArrayOf(
        ContextCompat.getColor(context, R.color.dark_surface_lighter),
        ContextCompat.getColor(context, R.color.white)
    )

    private val animators = List(waveCount) { index ->
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (600 + Random.nextInt(400)).toLong()
            startDelay = index * 30L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                amplitudes[index] = it.animatedValue as Float
                phaseShifts[index] += 0.04f
                invalidate()
            }
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // ✅ avoid passing Paint
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animators.forEach { it.start() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animators.forEach { it.cancel() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val centerY = h / 2f
        val maxAmp = h * 0.3f

        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            gradientColors, null,
            Shader.TileMode.CLAMP
        )

        val path = Path().apply {
            moveTo(0f, centerY)
            val step = w / (waveCount - 1)
            for (i in 0 until waveCount) {
                val x = i * step
                val amp = maxAmp * amplitudes[i]
                val y = centerY + amp * sin(phaseShifts[i] + (x / w * 2 * PI.toFloat()))
                lineTo(x, y)
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        canvas.drawPath(path, paint)
    }
}
class VoskDialogFragment : Fragment(), RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var vhalManager: VhalManager? = null
    private val viewModel: NavyaVoiceViewModel by activityViewModels { AppViewModelFactory }



    private lateinit var titleText: TextView
    private lateinit var waveformView: WaveformView
    private lateinit var recognizedTextView: TextView

    companion object {
        private const val TAG = "VoskFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_vosk, container, false)

        titleText = view.findViewById(R.id.title_text)
        waveformView = view.findViewById(R.id.waveform_view)
        recognizedTextView = view.findViewById(R.id.recognized_text)

        vhalManager = VhalManager(requireContext())
        viewModel.isListening.observe(viewLifecycleOwner) { isListening ->
            Log.d(TAG, "isListening changed: $isListening")
            if (isListening) {
                startRecognition()
                Log.d(TAG, "Starting recognition")
            } else {
                speechService?.stop()
                Log.d(TAG, "SpeechService stopped")
            }
        }

        viewModel.displayState.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "DisplayState changed: $state")
            when (state) {
                DisplayState.Idle -> {
                    waveformView.visibility = View.GONE
                    recognizedTextView.visibility = View.GONE
                }
                DisplayState.Listening -> {
                    waveformView.visibility = View.VISIBLE
                    recognizedTextView.visibility = View.GONE
                    waveformView.invalidate()
                    Log.d(TAG, "asasfasf changed: $state")

                }
                DisplayState.PlayingSound -> {
                    waveformView.visibility = View.VISIBLE
                    recognizedTextView.visibility = if (viewModel.recognizedText.value?.isNotBlank() == true) View.VISIBLE else View.GONE
                    waveformView.invalidate()
                }
                DisplayState.Text -> {
                    waveformView.visibility = View.GONE
                    recognizedTextView.visibility = if (viewModel.recognizedText.value?.isNotBlank() == true) View.VISIBLE else View.GONE
                }
            }
        }

        viewModel.recognizedText.observe(viewLifecycleOwner) { text ->
            Log.d(TAG, "Recognized text: $text")
            recognizedTextView.text = text
            if (viewModel.isStopPhrase(text) && (viewModel.displayState.value == DisplayState.Text || viewModel.displayState.value == DisplayState.PlayingSound)) {
                val glow = AlphaAnimation(0f, 0.5f).apply {
                    duration = 300
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            val fadeOut = AlphaAnimation(0.5f, 0f).apply { duration = 600 }
                            recognizedTextView.startAnimation(fadeOut)
                        }
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                }
                recognizedTextView.startAnimation(glow)
            }
        }

        checkPermissionAndInit()
        return view
    }

    private fun checkPermissionAndInit() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting RECORD_AUDIO permission")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            initModel()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "RECORD_AUDIO permission granted")
            initModel()
        } else {
            viewModel.updateRecognizedText("Permission denied", DisplayState.Text, vhalManager)
            Log.e(TAG, "Record audio permission denied")
        }
    }

    private fun unpackModelToInternal(context: Context, modelName: String) {
        val assetManager = context.assets
        val modelDir = File(context.filesDir, modelName)
        if (!modelDir.exists()) {
            try {
                modelDir.mkdirs()
                copyAssetFolder(assetManager, modelName, modelDir.absolutePath)
                Log.d(TAG, "Model unpacked to ${modelDir.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to unpack model: ${e.message}")
                viewModel.updateRecognizedText("Failed to unpack model: ${e.message}", DisplayState.Text, vhalManager)
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

    private fun initModel() {
        val modelPath = File(requireContext().filesDir, "vosk-model-small-en-us-0.15").absolutePath
        unpackModelToInternal(requireContext(), "vosk-model-small-en-us-0.15")
        try {
            model = Model(modelPath)
            Log.d(TAG, "Model loaded successfully from $modelPath")
        } catch (e: IOException) {
            Log.e(TAG, "Model initialization failed: ${e.message}")
            viewModel.updateRecognizedText("Model initialization failed: ${e.message}", DisplayState.Text, vhalManager)
        }
    }

    private fun startRecognition() {
        model?.let { model ->
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f).apply {
                startListening(this@VoskDialogFragment)
                Log.d(TAG, "SpeechService started")
            }
        } ?: run {
            viewModel.updateRecognizedText("Model not loaded", DisplayState.Text, vhalManager)
            Log.e(TAG, "Cannot start recognition, model is null")
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        viewModel.updateRecognizedText(hypothesis ?: "Processing...", DisplayState.Listening, vhalManager)
    }

    override fun onResult(hypothesis: String?) {
        viewModel.updateRecognizedText(hypothesis ?: "Processing...", DisplayState.Listening, vhalManager)
    }

    override fun onFinalResult(hypothesis: String?) {
        viewModel.updateRecognizedText(hypothesis ?: "No text recognized", DisplayState.Text, vhalManager)
    }

    override fun onError(exception: Exception?) {
        viewModel.updateRecognizedText("Error: ${exception?.message}", DisplayState.Text, vhalManager)
        Log.e(TAG, "Recognition error: ${exception?.message}")
    }

    override fun onTimeout() {
        viewModel.updateRecognizedText("Recognition timed out", DisplayState.Text, vhalManager)
        Log.d(TAG, "Recognition timeout")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechService?.stop()
        speechService = null
        model?.close()
        model = null
        vhalManager?.cleanup()
        vhalManager = null
        Log.d(TAG, "Fragment destroyed")
    }
}