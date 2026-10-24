package com.example.player

import android.app.Activity
import android.content.ComponentName
import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.data.local.PreferencesManager
import com.example.service.MediaPlaybackService

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    streamUrl: String,
    channelName: String = "",
    logoUrl: String? = null,
    modifier: Modifier = Modifier,
    onPlaybackStarted: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onProcessLog: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStarted by rememberUpdatedState(onPlaybackStarted)
    val currentOnError by rememberUpdatedState(onPlaybackError)
    val currentOnProcessLog by rememberUpdatedState(onProcessLog)

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Connect UI to Foreground MediaPlaybackService via Media3 MediaController
    DisposableEffect(context) {
        val appContext = context.applicationContext
        MediaPlaybackService.ensureServiceStarted(appContext)
        currentOnProcessLog("[FgService] MediaPlaybackService ফোরগ্রাউন্ড সেশন সংযোগ হচ্ছে...")

        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MediaPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

        var attachedController: MediaController? = null
        var tracksLogged = false
        var lastWidth = 0
        var lastHeight = 0

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                tracksLogged = false
                lastWidth = 0
                lastHeight = 0
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val ctrl = attachedController ?: return
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        currentOnProcessLog("[Exo.BUFFERING] M3U8 HLS সেগমেন্ট ও নেটওয়ার্ক বাফার লোড হচ্ছে...")
                    }
                    Player.STATE_READY -> {
                        val hasVideoTrack = ctrl.currentTracks.groups.any { group ->
                            group.type == C.TRACK_TYPE_VIDEO && group.isSelected
                        }
                        if (!hasVideoTrack && ctrl.playWhenReady) {
                            ctrl.volume = 1.0f
                            currentOnProcessLog("[Audio Stream] অডিও-অনলি স্ট্রিম প্রস্তুত (Foreground Service সক্রিয়)")
                            currentOnStarted()
                        } else {
                            currentOnProcessLog("[Exo.READY] ডিকোডার বাফার প্রস্তুত (Foreground MediaSession সচল)")
                            if (ctrl.isPlaying) {
                                // Ensure splash dismisses even when resuming an already-active background session
                                currentOnStarted()
                            }
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
                attachedController?.volume = 1.0f
                currentOnProcessLog("[GPU Render] প্রথম ভিডিও ফ্রেম রেন্ডার সম্পন্ন — স্প্ল্যাশ ব্যানার অপসারণ")
                currentOnStarted()
            }

            override fun onPlayerError(error: PlaybackException) {
                currentOnProcessLog("[Exo.Error] ${error.errorCodeName}: ${error.message ?: "স্ট্রিম ত্রুটি"}")
                currentOnError(error.message ?: "M3U8 playback error")
            }
        }

        controllerFuture.addListener(
            {
                try {
                    val ctrl = controllerFuture.get()
                    attachedController = ctrl
                    ctrl.addListener(listener)
                    mediaController = ctrl
                    currentOnProcessLog("[MediaSession] ফোরগ্রাউন্ড MediaController যুক্ত হয়েছে (Screen-Off Playback প্রস্তুত)")
                } catch (e: Exception) {
                    currentOnProcessLog("[ServiceError] MediaController সংযোগ ব্যর্থ: ${e.message}")
                    currentOnError(e.message ?: "MediaPlaybackService connection error")
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )

        onDispose {
            playerViewRef?.player = null
            attachedController?.removeListener(listener)
            mediaController = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    // Load or seamlessly attach to the active M3U stream inside MediaPlaybackService
    LaunchedEffect(mediaController, streamUrl, channelName, logoUrl) {
        val ctrl = mediaController ?: return@LaunchedEffect
        if (streamUrl.isEmpty()) return@LaunchedEffect

        MediaPlaybackService.ensureServiceStarted(context)
        ctrl.volume = 1.0f

        val currentMediaId = ctrl.currentMediaItem?.mediaId
        val isSameStreamAlreadyActive = currentMediaId == streamUrl &&
            (ctrl.playbackState == Player.STATE_READY || ctrl.playbackState == Player.STATE_BUFFERING)

        if (isSameStreamAlreadyActive) {
            if (!ctrl.playWhenReady) {
                ctrl.playWhenReady = true
            }
            ctrl.play()
            if (ctrl.playbackState == Player.STATE_READY) {
                currentOnProcessLog("[SessionHandoff] চলমান M3U স্ট্রিম পুনঃসংযুক্ত — নিরবচ্ছিন্ন প্লেব্যাক সচল")
                currentOnStarted()
            }
        } else {
            val shortHost = try {
                Uri.parse(streamUrl).host ?: "stream-host"
            } catch (_: Exception) {
                "stream-host"
            }
            currentOnProcessLog("[HLS MediaItem] কানেক্ট হচ্ছে: $shortHost — ফোরগ্রাউন্ড সার্ভিসে স্ট্রিম শুরু...")

            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(channelName.ifBlank { "Nokshi IP TV Live" })
                .setArtist("Nokshi IP TV • লাইভ স্ট্রিম")

            if (!logoUrl.isNullOrBlank()) {
                try {
                    metadataBuilder.setArtworkUri(Uri.parse(logoUrl))
                } catch (_: Exception) {}
            }

            val mediaItem = MediaItem.Builder()
                .setMediaId(streamUrl)
                .setUri(streamUrl)
                .setMediaMetadata(metadataBuilder.build())
                .build()

            ctrl.setMediaItem(mediaItem)
            ctrl.playWhenReady = true
            ctrl.prepare()
            ctrl.play()
        }
    }

    // Detach video SurfaceView when screen turns off / app goes to background so MediaPlaybackService
    // continues audio streaming in Foreground without SurfaceView destruction stalls; re-attach on return!
    DisposableEffect(lifecycleOwner, mediaController) {
        val prefs = PreferencesManager(context)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    val ctrl = mediaController
                    if (ctrl != null && playerViewRef?.player !== ctrl) {
                        playerViewRef?.player = ctrl
                        currentOnProcessLog("[SurfaceAttach] ভিডিও সারফেস পুনরায় যুক্ত হয়েছে")
                    }
                    if (ctrl != null && !prefs.isBackgroundAudioEnabled && streamUrl.isNotEmpty()) {
                        ctrl.playWhenReady = true
                        ctrl.play()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    val isChangingConfig = (context as? Activity)?.isChangingConfigurations == true
                    if (!isChangingConfig) {
                        if (prefs.isBackgroundAudioEnabled) {
                            // Detach surface so ExoPlayer in MediaPlaybackService decodes audio smoothly with screen off
                            playerViewRef?.player = null
                            currentOnProcessLog("[BgPlayback] স্ক্রিন অফ/ব্যাকগ্রাউন্ড সক্রিয় — Foreground Service প্লেব্যাক সচল রাখছে")
                        } else {
                            mediaController?.pause()
                            currentOnProcessLog("[BgPaused] ব্যাকগ্রাউন্ড অডিও বন্ধ থাকায় স্ট্রিম বিরত রাখা হয়েছে")
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                    playerViewRef = this
                    player = mediaController
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { view ->
                playerViewRef = view
                val isStarted = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                if (isStarted && view.player !== mediaController) {
                    view.player = mediaController
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
