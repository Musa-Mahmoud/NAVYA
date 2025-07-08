package com.example.navya_2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@SuppressLint("StaticFieldLeak")
object AppViewModelFactory : ViewModelProvider.Factory {
    private lateinit var context: Context
    fun init(context: Context) { this.context = context.applicationContext }
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

    private var ambientFragment: AmbientLight? = null
    private var voskDialogFragment: VoskDialogFragment? = null
    private var cameraFeedFragment: CameraFeedFragment? = null
    private var acControlFragment: AcControlFragment? = null
    private var userSpaceFragment: UserSpace? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppViewModelFactory.init(applicationContext)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        voiceMicButton = findViewById(R.id.voice_mic_button)

        showLeftFragment(CarFragment())

        val prefs = getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
        val switchState = prefs.getInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_CENTER)

        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

        cameraFeedFragment = CameraFeedFragment()
        ambientFragment = AmbientLight()
        voskDialogFragment = VoskDialogFragment()
        acControlFragment = AcControlFragment.newInstance()
        userSpaceFragment = UserSpace()

        transaction.add(R.id.camera_feed_fragment_container, cameraFeedFragment!!, "CameraFeedFragmentTag")
        transaction.add(R.id.camera_feed_fragment_container, ambientFragment!!, "AmbientLightFragmentTag")
        transaction.add(R.id.camera_feed_fragment_container, voskDialogFragment!!, "VoskDialogFragmentTag")
        transaction.add(R.id.camera_feed_fragment_container, acControlFragment!!, "AcControlFragmentTag")
        transaction.add(R.id.camera_feed_fragment_container, userSpaceFragment!!, "UserSpaceFragmentTag")

        if (switchState == SwitchState.SWITCH_CENTER) {
            transaction.show(userSpaceFragment!!)
            transaction.hide(cameraFeedFragment!!)
            transaction.hide(ambientFragment!!)
            transaction.hide(voskDialogFragment!!)
            transaction.hide(acControlFragment!!)
        } else {
            transaction.show(cameraFeedFragment!!)
            transaction.hide(userSpaceFragment!!)
            transaction.hide(ambientFragment!!)
            transaction.hide(voskDialogFragment!!)
            transaction.hide(acControlFragment!!)
        }

        transaction.commit()

        findViewById<ImageButton>(R.id.ambient_light_button).setOnClickListener {
            handleFragmentSwitch(SWITCH_TARGET.AMBIENT)
        }

        findViewById<ImageButton>(R.id.ac_control_button).setOnClickListener {
            handleFragmentSwitch(SWITCH_TARGET.AC_CONTROL)
        }

        findViewById<ImageButton>(R.id.user_space_button).setOnClickListener {
            handleFragmentSwitch(SWITCH_TARGET.USERSPACE)
        }

        voiceMicButton.setOnClickListener {
            handleVoiceMicClick()
        }

        viewModel.isListening.observe(this) { isListening ->
            if (!isListening && voskDialogFragment?.isVisible == true) {
                switchToFragment(cameraFeedFragment!!)
                updateMicButtonIdle()
            }
        }

        checkAndRequestPermissions()
        hideSystemBars()
    }

    enum class SWITCH_TARGET { AMBIENT, AC_CONTROL, USERSPACE }

    private fun handleFragmentSwitch(target: SWITCH_TARGET) {
        val switchState = getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_CENTER)

        if (switchState != SwitchState.SWITCH_CENTER) {
            Toast.makeText(this, "Cannot change screen while turning.", Toast.LENGTH_SHORT).show()
            switchToFragment(cameraFeedFragment!!)
            return
        }

        when (target) {
            SWITCH_TARGET.AMBIENT -> switchToFragment(ambientFragment!!)
            SWITCH_TARGET.AC_CONTROL -> switchToFragment(acControlFragment!!)
            SWITCH_TARGET.USERSPACE -> switchToFragment(userSpaceFragment!!)
        }
    }

    private fun handleVoiceMicClick() {
        val switchState = getSharedPreferences(SharedState.PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(SharedState.KEY_SWITCH_STATE, SwitchState.SWITCH_CENTER)

        if (switchState != SwitchState.SWITCH_CENTER) {
            Toast.makeText(this, "Cannot use voice control while turning.", Toast.LENGTH_SHORT).show()
            switchToFragment(cameraFeedFragment!!)
            return
        }

        if (viewModel.isListening.value == true) {
            viewModel.toggleListening()
            updateMicButtonIdle()
            switchToFragment(userSpaceFragment!!)
        } else {
            viewModel.toggleListening()
            updateMicButtonActive()
            switchToFragment(voskDialogFragment!!)
        }
    }

    private fun switchToFragment(fragmentToShow: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

        listOf(cameraFeedFragment, ambientFragment, voskDialogFragment, acControlFragment, userSpaceFragment).forEach {
            if (it == fragmentToShow) transaction.show(it!!)
            else transaction.hide(it!!)
        }

        transaction.commit()
        syncLeftContainer(fragmentToShow)
    }

    private fun syncLeftContainer(currentTargetFragment: Fragment) {
        val shouldShowAcInfo = currentTargetFragment == acControlFragment
        val currentLeftFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)

        if (shouldShowAcInfo) {
            if (currentLeftFragment !is AcInfoFragment) {
                showLeftFragment(AcInfoFragment.newInstance())
            }
        } else {
            if (currentLeftFragment !is CarFragment) {
                showLeftFragment(CarFragment())
            }
        }
    }

    private fun showLeftFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container_view, fragment)
            .commit()
    }

    private fun updateMicButtonIdle() {
        voiceMicButton.setImageResource(R.drawable.ic_micsvg)
        voiceMicButton.setBackgroundResource(R.drawable.circular_button_background)
        voiceMicButton.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun updateMicButtonActive() {
        voiceMicButton.setImageResource(R.drawable.ic_stop)
        voiceMicButton.setBackgroundResource(R.drawable.button_red_background)
        voiceMicButton.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
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
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && !allPermissionsGranted()) {
            Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
