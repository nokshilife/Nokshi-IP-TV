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
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
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
    onDoubleTap: () -> Unit = {},
    onProcessLog: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnStarted by rememberUpdatedState(onPlaybackStarted)
    val currentOnError by rememberUpdatedState(onPlaybackError)
    val currentOnProcessLog by rememberUpdatedState(onProcessLog)

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            volume = 1.0f
            setWakeMode(C.WAKE_MODE_NETWORK)
        }
    }

    DisposableEffect(exoPlayer) {
        currentOnProcessLog("[ExoPlayer] Media3 প্লেয়ার ইনিশিয়ালাইজড (WAKE_MODE_NETWORK সক্রিয়)")
        var tracksLogged = false
        var lastWidth = 0
        var lastHeight = 0

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        currentOnProcessLog("[Exo.BUFFERING] M3U8 HLS সেগমেন্ট ও নেটওয়ার্ক বাফার লোড হচ্ছে...")
                    }
                    Player.STATE_READY -> {
                        val hasVideoTrack = exoPlayer.currentTracks.groups.any { group ->
                            group.type == C.TRACK_TYPE_VIDEO && group.isSelected
                        }
                        if (!hasVideoTrack && exoPlayer.playWhenReady) {
                            exoPlayer.volume = 1.0f
                            currentOnProcessLog("[Audio Stream] অডিও-অনলি স্ট্রিম প্রস্তুত (Vol: 100%)")
                            currentOnStarted()
                        } else {
                            currentOnProcessLog("[Exo.READY] ডিকোডার বাফার প্রস্তুত (STATE_READY)")
                        }
                    }
                    Player.STATE_ENDED -> {
                        currentOnProcessLog("[Exo.ENDED] লাইভ স্ট্রিম সেশন সমাপ্ত")
                    }
                    else -> {}
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (!tracksLogged && tracks.groups.isNotEmpty()) {
                    val vCount = tracks.groups.count { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
                    val aCount = tracks.groups.count { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
                    if (vCount > 0 || aCount > 0) {
                        tracksLogged = true
                        currentOnProcessLog("[Tracks/Codec] ভিডিও ট্র্যাক: $vCount, অডিও ট্র্যাক: $aCount সিলেক্ট সম্পন্ন")
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0 &&
                    (videoSize.width != lastWidth || videoSize.height != lastHeight)
                ) {
                    lastWidth = videoSize.width
                    lastHeight = videoSize.height
                    currentOnProcessLog("[VideoDecoder] আউটপুট রেজোলিউশন: ${videoSize.width}x${videoSize.height}")
                }
            }

            override fun onRenderedFirstFrame() {
                exoPlayer.volume = 1.0f
                currentOnProcessLog("[GPU Render] প্রথম ভিডিও ফ্রেম রেন্ডার সম্পন্ন — স্প্ল্যাশ ব্যানার অপসারণ")
                currentOnStarted()
            }

            override fun onPlayerError(error: PlaybackException) {
                currentOnProcessLog("[Exo.Error] ${error.errorCodeName}: ${error.message ?: "স্ট্রিম ত্রুটি"}")
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
            val shortHost = try {
                android.net.Uri.parse(streamUrl).host ?: "stream-host"
            } catch (e: Exception) {
                "stream-host"
            }
            currentOnProcessLog("[HLS MediaItem] কানেক্ট হচ্ছে: $shortHost — ম্যানিফেস্ট প্রিপেয়ার শুরু...")
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
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
