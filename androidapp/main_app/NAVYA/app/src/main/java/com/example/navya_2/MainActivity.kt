package com.example.navya_2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.viewModels

import androidx.activity.viewModels


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@SuppressLint("StaticFieldLeak")
object AppViewModelFactory : ViewModelProvider.Factory {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NavyaVoiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NavyaVoiceViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private lateinit var voiceMicButton: ImageButton
    private val viewModel: NavyaVoiceViewModel by viewModels { AppViewModelFactory }

    private var voskDialogFragment: VoskDialogFragment? = null
    private var cameraFeedFragment: CameraFeedFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppViewModelFactory.init(applicationContext)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        voiceMicButton = findViewById(R.id.voice_mic_button)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, CarFragment())
            .commit()

        cameraFeedFragment = supportFragmentManager.findFragmentByTag("CameraFeedFragmentTag") as? CameraFeedFragment
        voskDialogFragment = supportFragmentManager.findFragmentByTag("VoskDialogFragmentTag") as? VoskDialogFragment

        val fragmentTransaction = supportFragmentManager.beginTransaction()

        if (cameraFeedFragment == null) {
            cameraFeedFragment = CameraFeedFragment()
            fragmentTransaction.add(R.id.camera_feed_fragment_container, cameraFeedFragment!!, "CameraFeedFragmentTag")
        }

        if (voskDialogFragment == null) {
            voskDialogFragment = VoskDialogFragment()
            fragmentTransaction.add(R.id.camera_feed_fragment_container, voskDialogFragment!!, "VoskDialogFragmentTag")
        }

        fragmentTransaction.show(cameraFeedFragment!!)
        fragmentTransaction.hide(voskDialogFragment!!)
        fragmentTransaction.commit()

        findViewById<ImageButton>(R.id.ambient_light_button).setOnClickListener {
            val dialog = AmbientLight.newInstance()
            dialog.show(supportFragmentManager, "AmbientLightDialog")
        }

        voiceMicButton.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            if (viewModel.isListening.value == true) {
                viewModel.toggleListening()
                voiceMicButton.setImageResource(R.drawable.ic_micsvg)
                voiceMicButton.setBackgroundResource(R.drawable.circular_button_background)
                voiceMicButton.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                voskDialogFragment?.let { transaction.hide(it) }
                cameraFeedFragment?.let { transaction.show(it) }
            } else {
                viewModel.toggleListening()
                voiceMicButton.setImageResource(R.drawable.ic_stop)
                voiceMicButton.setBackgroundResource(R.drawable.button_red_background)
                voiceMicButton.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
                cameraFeedFragment?.let { transaction.hide(it) }
                voskDialogFragment?.let { transaction.show(it) }
            }
            transaction.commit()
        }

        viewModel.isListening.observe(this) { isListening ->
            if (!isListening && voskDialogFragment?.isVisible == true) {
                val transaction = supportFragmentManager.beginTransaction()
                voskDialogFragment?.let { transaction.hide(it) }
                cameraFeedFragment?.let { transaction.show(it) }
                transaction.commit()

                voiceMicButton.setImageResource(R.drawable.ic_micsvg)
                voiceMicButton.setBackgroundResource(R.drawable.circular_button_background)
                voiceMicButton.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }
        }

        checkAndRequestPermissions()
        hideSystemBars()
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
}
