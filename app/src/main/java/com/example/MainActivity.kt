package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen active during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Observe Sleep Timer completion
        lifecycleScope.launch {
            // Drop initial 0
            viewModel.sleepTimerSeconds.drop(1).collect { seconds ->
                if (seconds == 0) {
                    finishAffinity()
                }
            }
        }

        setContent {
            MyApplicationTheme {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Android TV remote controls handling
        val isFullscreen = viewModel.isFullscreen.value

        when (keyCode) {
            // Left button toggles fullscreen on TV
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!isFullscreen && viewModel.currentChannel.value != null) {
                    viewModel.setFullscreen(true)
                    return true
                }
            }

            // In Fullscreen, Channel Up / Down or DPAD Up / Down switches channels
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                if (isFullscreen) {
                    viewModel.playNextChannel()
                    return true
                }
            }

            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
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
