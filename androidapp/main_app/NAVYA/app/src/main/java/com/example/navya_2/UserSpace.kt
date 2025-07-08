package com.example.navya_2

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class UserSpace : Fragment() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    private lateinit var usernameText: TextView

    companion object {
        fun newInstance() = UserSpace()
    }
    private fun getUsername(): String {
        val userManager = requireContext().getSystemService(UserManager::class.java)
        return userManager?.userName ?: "Guest"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_user_space, container, false)

        timeText = view.findViewById(R.id.timeText)
        dateText = view.findViewById(R.id.dateText)
        usernameText = view.findViewById(R.id.usernameText)

        usernameText.text = getUsername()

        startClock()
        return view
    }


    private fun startClock() {
        updateRunnable = object : Runnable {
            override fun run() {
                val calendar = Calendar.getInstance()
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                timeText.text = timeFormat.format(calendar.time)
                dateText.text = dateFormat.format(calendar.time)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }
}
