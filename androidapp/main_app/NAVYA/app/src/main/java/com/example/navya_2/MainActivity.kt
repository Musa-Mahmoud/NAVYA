package com.example.navya_2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        // Load your main functional fragments


        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, CarFragment())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.camera_feed_fragment_container, CameraFeedFragment())
            .commit()



        // Ambient icon is now handled via ImageButton, not a fragment
        val ambientButton = findViewById<ImageButton>(R.id.ambient_light_button)
        ambientButton.setOnClickListener {
            val dialog = AmbientLight.newInstance()
            dialog.show(supportFragmentManager, "AmbientLightDialog")
        }

        val micButton = findViewById<ImageButton>(R.id.voice_mic_button)
        micButton.setOnClickListener {
            val dialog = VoskDialogFragment()
            dialog.show(supportFragmentManager, "VoskDialog")
        }

        hideSystemBars()

        supportFragmentManager.beginTransaction()
            .replace(R.id.square_fragment_container_1, VoskFragment())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.square_fragment_container_2, AmbientLight())
            .commit()

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (allPermissionsGranted()) {
            // Permissions granted, fragments can proceed
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun hideSystemBars() {
        if (packageManager.hasSystemFeature("android.hardware.type.automotive")) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }
}