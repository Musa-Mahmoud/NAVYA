package com.example.navya_2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class CameraFeedFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_camera_feed, container, false)

        val buttonSafe = view.findViewById<MaterialButton>(R.id.button_safe)
        val buttonMedium = view.findViewById<MaterialButton>(R.id.button_medium)
        val buttonDanger = view.findViewById<MaterialButton>(R.id.button_danger)

        val prefs = requireContext().getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)

        buttonSafe.setOnClickListener {
            prefs.edit { putString(SharedState.KEY_STATE, "safe") }
        }
        buttonMedium.setOnClickListener {
            prefs.edit { putString(SharedState.KEY_STATE, "medium") }
        }
        buttonDanger.setOnClickListener {
            prefs.edit { putString(SharedState.KEY_STATE, "danger") }
        }

        return view
    }
}