package com.example.navya_2

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaPlayer
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
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
import kotlin.math.sin
import kotlin.random.Random

// Enum to manage UI display states
enum class DisplayState {
    Idle, Listening, Text
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

// ViewModel to manage voice recognition data and state
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
        "hello car", "hello navya", "hello veachle", "temperature up", "hi", "hello"
    )

    private var mediaPlayer: MediaPlayer? = null

    fun updateRecognizedText(text: String, state: DisplayState, vhalManager: VhalManager?) {
        val recognized = extractRecognizedText(text)
        val normalized = recognized.lowercase()

        Log.d(TAG, "Normalized text: $normalized")

        if (state == DisplayState.Listening || state == DisplayState.Text) {
            _recognizedText.value = recognized
        } else {
            _recognizedText.value = text
        }

        if ((state == DisplayState.Listening || state == DisplayState.Text) && stopPhrases.contains(normalized)) {
            _isListening.value = false
            _displayState.value = DisplayState.Text
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
                "hi car", "hello car", "hi veachle" -> {
                    playSound(R.raw.hey_car)
                }
                "stop" -> {
                    // No sound specified
                }
                "right camera" -> {
                    playSound(R.raw.right_camera)
                }
                "left camera" -> {
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
            }
            mediaPlayer?.start()
            Log.d(TAG, "Playing sound resource: $resourceId")
        } catch (e: Exception) {
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
    }
}

// Custom View for displaying animated waveform
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val waveCount = 20
    private val amplitudes = FloatArray(waveCount) { 0f }
    private val phaseShift = FloatArray(waveCount) { 0f }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 8f
        isAntiAlias = true
    }
    private val gradientColors = intArrayOf(
        ContextCompat.getColor(context, R.color.black),
        ContextCompat.getColor(context, R.color.white)
    )

    private val animators = List(waveCount) { index ->
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (600 + Random.nextInt(400)).toLong()
            startDelay = index * 20L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener {
                amplitudes[index] = it.animatedValue as Float
                phaseShift[index] += 0.05f
                invalidate()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animators.forEach { it.start() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animators.forEach { it.cancel() }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f
        val maxAmplitude = height * 0.3f

        paint.setShadowLayer(10f, 0f, 4f, Color.argb(80, 0, 0, 0))
        setLayerType(LAYER_TYPE_SOFTWARE, paint)

        val shader = LinearGradient(
            0f, 0f, width, 0f,
            gradientColors,
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader

        val path = Path()
        val step = width / waveCount
        path.moveTo(0f, centerY)

        for (i in 0 until waveCount) {
            val x = i * step
            val amplitude = maxAmplitude * amplitudes[i]
            val y = centerY + amplitude * sin(phaseShift[i].toDouble() + (x / width * 2 * Math.PI)).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        path.lineTo(width, centerY)
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        canvas.drawPath(path, paint)
    }
}

// Fragment to display voice recognition UI
class VoskDialogFragment : DialogFragment(), RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var vhalManager: VhalManager? = null
    private val viewModel: NavyaVoiceViewModel by viewModels { ViewModelFactory(requireContext().applicationContext) }

    private lateinit var titleText: TextView
    private lateinit var hintText: TextView
    private lateinit var waveformView: WaveformView
    private lateinit var recognizedTextView: TextView
    private lateinit var toggleButton: Button

    companion object {
        private const val TAG = "VoskFragment"
    }

    class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NavyaVoiceViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NavyaVoiceViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vosk, container, false)

        // Initialize views
        titleText = view.findViewById(R.id.title_text)
        hintText = view.findViewById(R.id.hint_text)
        waveformView = view.findViewById(R.id.waveform_view)
        recognizedTextView = view.findViewById(R.id.recognized_text)
        toggleButton = view.findViewById(R.id.toggle_button)

        // Initialize VHAL manager
        vhalManager = VhalManager(requireContext())

        // Set up button click listener
        toggleButton.setOnClickListener { viewModel.toggleListening() }

        // Observe LiveData
        viewModel.isListening.observe(viewLifecycleOwner) { isListening ->
            if (isListening) {
                startRecognition()
                toggleButton.setBackgroundResource(R.drawable.button_red_background)
                toggleButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
                toggleButton.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
            } else {
                speechService?.stop()
                toggleButton.setBackgroundResource(R.drawable.button_primary_background)
                toggleButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_micsvg, 0, 0, 0)
                toggleButton.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                Log.d(TAG, "SpeechService stopped")
            }
        }

        viewModel.displayState.observe(viewLifecycleOwner) { state ->
            when (state) {
                DisplayState.Idle -> {
                    hintText.visibility = View.VISIBLE
                    waveformView.visibility = View.GONE
                    recognizedTextView.visibility = View.GONE
                }
                DisplayState.Listening -> {
                    hintText.visibility = View.GONE
                    waveformView.visibility = View.VISIBLE
                    recognizedTextView.visibility = View.GONE
                }
                DisplayState.Text -> {
                    hintText.visibility = View.GONE
                    waveformView.visibility = View.GONE
                    recognizedTextView.visibility = if (viewModel.recognizedText.value?.isNotBlank() == true) View.VISIBLE else View.GONE
                }
            }
        }

        viewModel.recognizedText.observe(viewLifecycleOwner) { text ->
            recognizedTextView.text = text
            if (viewModel.isStopPhrase(text) && viewModel.displayState.value == DisplayState.Text) {
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(R.drawable.rounded_rectangle_background)
            setDimAmount(0.5f)
        }
    }

    private fun checkPermissionAndInit() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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
            initModel()
        } else {
            viewModel.updateRecognizedText("Permission denied", DisplayState.Text, vhalManager)
            Log.e(TAG, "Record audio permission denied")
        }
    }

    private fun initModel() {
        StorageService.unpack(
            requireContext(),
            "vosk-model-small-en-us-0.15",
            "model",
            { unpacked ->
                model = unpacked
                Log.d(TAG, "Model loaded successfully")
            },
            { e ->
                viewModel.updateRecognizedText("Model unpack failed: ${e.message}", DisplayState.Text, vhalManager)
                Log.e(TAG, "Model unpack error: ${e.message}")
            }
        )
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