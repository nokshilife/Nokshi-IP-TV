package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.service.MediaPlaybackService
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BatteryOptimizationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.logPlayerProcessStep("[Notification] ফোরগ্রাউন্ড মিডিয়া নোটিফিকেশন পারমিশন অনুমোদিত")
        } else {
            viewModel.logPlayerProcessStep("[Notification] নোটিফিকেশন পারমিশন দেওয়া হয়নি (প্লেব্যাক চলবে)")
        }
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBatteryOptimizationState(logResult = true)
        requestNotificationPermissionIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen active during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Observe Sleep Timer completion -> stop background playback service and close app cleanly
        lifecycleScope.launch {
            viewModel.sleepTimerSeconds.drop(1).collect { seconds ->
                if (seconds == 0) {
                    MediaPlaybackService.stopPlayback(this@MainActivity)
                    finishAffinity()
                }
            }
        }

        // On cold start, prompt Android System Battery Optimization exemption (Unrestricted)
        // if not already excluded, followed by POST_NOTIFICATIONS on Android 13+.
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                delay(600)
                checkAndPromptStartupSystemPermissions()
            }
        }

        setContent {
            MyApplicationTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onRequestBatteryUnrestricted = {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(
                            context = this@MainActivity,
                            launcher = batteryOptimizationLauncher
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryOptimizationState(logResult = false)
    }

    private fun checkAndPromptStartupSystemPermissions() {
        val isUnrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
        if (!isUnrestricted) {
            viewModel.logPlayerProcessStep(
                "[PowerManager] অ্যাপকে Battery Optimization থেকে মুক্ত (Unrestricted) করার সিস্টেম প্রম্পট দেখানো হচ্ছে..."
            )
            val launched = BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(
                context = this,
                launcher = batteryOptimizationLauncher
            )
            if (!launched) {
                requestNotificationPermissionIfNeeded()
            }
        } else {
            viewModel.logPlayerProcessStep(
                "[PowerManager] ব্যাটারি অপ্টিমাইজেশন ইতিমধ্যে Unrestricted (মুক্ত) অবস্থায় আছে"
            )
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPerm) {
                try {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Android TV remote controls handling
        val isFullscreen = viewModel.isFullscreen.value

        when (keyCode) {
            // In Fullscreen mode, Channel Up / Down or DPAD Up / Down switches channels
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                viewModel.playNextChannel()
                return true
            }

            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                viewModel.playPreviousChannel()
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isFullscreen) {
                    viewModel.playNextChannel()
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isFullscreen) {
                    viewModel.playPreviousChannel()
                    return true
                }
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                viewModel.playNextChannel()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                viewModel.playPreviousChannel()
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }
}
