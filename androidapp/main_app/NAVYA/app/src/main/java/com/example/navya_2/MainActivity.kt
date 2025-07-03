package com.example.navya_2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, CarFragment())
            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.camera_feed_fragment_container, CameraFeedFragment())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.square_fragment_container_1, VoskFragment())
            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.square_fragment_container_2, AmbientLight())
            .commit()}
}