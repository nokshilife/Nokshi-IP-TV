package com.example.player

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    streamUrl: String,
    modifier: Modifier = Modifier,
    onPlaybackStarted: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {},
    onDoubleTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnStarted by rememberUpdatedState(onPlaybackStarted)
    val currentOnError by rememberUpdatedState(onPlaybackError)

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            volume = 1.0f
            // Power management: keep network & CPU awake for continuous background audio
            setWakeMode(C.WAKE_MODE_NETWORK)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                // Only dismiss splash banner once the first actual video frame is drawn on screen!
                exoPlayer.volume = 1.0f
                currentOnStarted()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // Fallback for audio-only radio/HLS streams that have no video track
                if (playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                    val hasVideoTrack = exoPlayer.currentTracks.groups.any { group ->
                        group.type == C.TRACK_TYPE_VIDEO && group.isSelected
                    }
                    if (!hasVideoTrack) {
                        exoPlayer.volume = 1.0f
                        currentOnStarted()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                currentOnError(error.message ?: "M3U8 playback error")
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotEmpty()) {
            exoPlayer.volume = 1.0f
            val mediaItem = MediaItem.fromUri(streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Requirement: Remove default controls for a fresh, clean player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
