package com.example.navya_2.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.example.navya_2.R
import com.example.navya_2.feature.ambientlight.view.AmbientLightFragment
import com.example.navya_2.feature.blindspot.view.BlindSpotFragment
import com.example.navya_2.feature.car.view.CarFragment
import com.example.navya_2.feature.voiceassistant.view.VoiceAssistantFragment

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private lateinit var voiceMicButton: ImageButton
    private lateinit var ambientLightButton: ImageButton
    private var blindSpotFragment: BlindSpotFragment? = null
    private var voiceAssistantFragment: VoiceAssistantFragment? = null
    private val viewModel: AppViewModel by viewModels { AppViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppViewModelFactory.context = applicationContext
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initializeViews()
        setupObservers()
        setupFragments(savedInstanceState)
        checkAndRequestPermissions()
        hideSystemBars()
    }

    private fun initializeViews() {
        voiceMicButton = findViewById(R.id.voice_mic_button)
        ambientLightButton = findViewById(R.id.ambient_light_button)

        voiceMicButton.setOnClickListener { viewModel.toggleVoiceAssistant() }
        ambientLightButton.setOnClickListener { viewModel.toggleAmbientLight() }
    }

    private fun setupObservers() {
        viewModel.isVoiceAssistantVisible.observe(this) { isVisible ->
            updateVoiceAssistantVisibility(isVisible)
        }
        viewModel.isAmbientLightVisible.observe(this) { isVisible ->
            updateAmbientLightVisibility(isVisible)
        }
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.fragment_container_view, CarFragment())
                add(R.id.camera_feed_fragment_container, BlindSpotFragment(), "BlindSpotFragmentTag")
                add(R.id.camera_feed_fragment_container, VoiceAssistantFragment(), "VoiceAssistantFragmentTag")
                show(supportFragmentManager.findFragmentByTag("BlindSpotFragmentTag")!!)
                hide(supportFragmentManager.findFragmentByTag("VoiceAssistantFragmentTag")!!)
            }
        }

        blindSpotFragment = supportFragmentManager.findFragmentByTag("BlindSpotFragmentTag") as? BlindSpotFragment
        voiceAssistantFragment = supportFragmentManager.findFragmentByTag("VoiceAssistantFragmentTag") as? VoiceAssistantFragment
    }

    private fun updateVoiceAssistantVisibility(isVisible: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
        if (isVisible) {
            voiceMicButton.setImageResource(R.drawable.ic_stop)
            voiceMicButton.setBackgroundResource(R.drawable.button_red_background)
            voiceMicButton.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
            blindSpotFragment?.let { transaction.hide(it) }
            voiceAssistantFragment?.let { transaction.show(it) }
        } else {
            voiceMicButton.setImageResource(R.drawable.ic_micsvg)
            voiceMicButton.setBackgroundResource(R.drawable.circular_button_background)
            voiceMicButton.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            voiceAssistantFragment?.let { transaction.hide(it) }
            blindSpotFragment?.let { transaction.show(it) }
        }
        transaction.commit()
    }

    private fun updateAmbientLightVisibility(isVisible: Boolean) {
        if (isVisible) {
            AmbientLightFragment.newInstance().show(supportFragmentManager, "AmbientLightFragment")
        } else {
            supportFragmentManager.findFragmentByTag("AmbientLightFragment")?.let {
                (it as androidx.fragment.app.DialogFragment).dismiss()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun hideSystemBars() {
        if (packageManager.hasSystemFeature("android.hardware.type.automotive")) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && !allPermissionsGranted()) {
            Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup if needed (e.g., cancel animations)
        voiceMicButton.animate().cancel()
        ambientLightButton.animate().cancel()
    }
}