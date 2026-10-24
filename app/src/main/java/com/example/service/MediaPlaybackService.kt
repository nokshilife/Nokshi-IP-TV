package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reconnectJob: Job? = null
    private var hasReachedReadyForCurrentItem = false
    private var autoReconnectAttempts = 0

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()

        // Configure Media3 Foreground Notification Provider for continuous screen-off / background playback
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_stat_tv)
            }
        setMediaNotificationProvider(notificationProvider)

        // CPU Partial WakeLock + High-Performance / Low-Latency Wi-Fi Lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NokshiIPTV:MediaPlaybackWakeLock"
        ).apply {
            setReferenceCounted(false)
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiLockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifiManager?.createWifiLock(wifiLockMode, "NokshiIPTV:MediaWifiLock")?.apply {
            setReferenceCounted(false)
        }

        // Resilient HTTP + HLS configuration for continuous mobile background streaming
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)
            .setUserAgent("NokshiIPTV/1.0 (Linux; Android)")

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 8))

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 4_000
            )
            .setBackBuffer(10_000, false)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                playWhenReady = true
                volume = 1.0f
                addListener(servicePlayerListener)
            }

        player = exoPlayer

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    private val servicePlayerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            reconnectJob?.cancel()
            hasReachedReadyForCurrentItem = false
            autoReconnectAttempts = 0
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateHardwareLocks()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateHardwareLocks()
            if (playbackState == Player.STATE_READY) {
                hasReachedReadyForCurrentItem = true
                autoReconnectAttempts = 0
                reconnectJob?.cancel()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val activePlayer = player ?: return
            if (activePlayer.mediaItemCount == 0) return

            // Always recover immediately from HLS live window drift
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                Log.w(TAG, "Recovering from ERROR_CODE_BEHIND_LIVE_WINDOW")
                activePlayer.seekToDefaultPosition()
                activePlayer.prepare()
                activePlayer.play()
                return
            }

            // If stream was already playing (e.g., screen off / background mobile network handoff),
            // automatically reconnect up to 6 times so background playback never drops unexpectedly.
            if (hasReachedReadyForCurrentItem && autoReconnectAttempts < MAX_AUTO_RECONNECT_ATTEMPTS) {
                autoReconnectAttempts++
                val backoffMs = 1500L * autoReconnectAttempts
                Log.w(TAG, "Background stream hiccup (${error.errorCodeName}), auto-reconnecting #$autoReconnectAttempts in ${backoffMs}ms")
                reconnectJob?.cancel()
                reconnectJob = serviceScope.launch {
                    delay(backoffMs)
                    val p = player ?: return@launch
                    if (p.mediaItemCount > 0 && p.playWhenReady) {
                        p.seekToDefaultPosition()
                        p.prepare()
                        p.play()
                    }
                }
            }
        }
    }

    private fun updateHardwareLocks() {
        val p = player
        val shouldHoldLocks = p != null &&
            p.playWhenReady &&
            (p.playbackState == Player.STATE_BUFFERING || p.playbackState == Player.STATE_READY)

        if (shouldHoldLocks) {
            acquireLocks()
        } else {
            releaseLocks()
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(12 * 60 * 60 * 1000L) // 12h safety cap, renewed on state changes
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock", e)
        }
        try {
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WifiLock", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PLAYBACK) {
            reconnectJob?.cancel()
            player?.apply {
                playWhenReady = false
                stop()
                clearMediaItems()
            }
            releaseLocks()
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val prefs = PreferencesManager(this)
        val p = player
        val keepPlayingInBackground = prefs.isBackgroundAudioEnabled &&
            p != null &&
            p.playWhenReady &&
            p.mediaItemCount > 0 &&
            (p.playbackState == Player.STATE_READY || p.playbackState == Player.STATE_BUFFERING)

        if (!keepPlayingInBackground) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        reconnectJob?.cancel()
        serviceScope.cancel()
        releaseLocks()
        wakeLock = null
        wifiLock = null

        mediaSession?.run {
            player.removeListener(servicePlayerListener)
            player.release()
            release()
            mediaSession = null
        }
        player = null
        isServiceRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nokshi IP TV Live Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for uninterrupted M3U live playback"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "MediaPlaybackService"
        private const val MAX_AUTO_RECONNECT_ATTEMPTS = 6
        const val CHANNEL_ID = "nokshi_iptv_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_PLAYBACK = "com.example.service.action.STOP_PLAYBACK"

        @Volatile
        private var isServiceRunning: Boolean = false

        fun ensureServiceStarted(context: Context) {
            try {
                val appContext = context.applicationContext
                val intent = Intent(appContext, MediaPlaybackService::class.java)
                appContext.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to pre-start MediaPlaybackService", e)
            }
        }

        fun stopPlayback(context: Context) {
            if (!isServiceRunning) return
            try {
                val appContext = context.applicationContext
                val stopIntent = Intent(appContext, MediaPlaybackService::class.java).apply {
                    action = ACTION_STOP_PLAYBACK
                }
                appContext.startService(stopIntent)
            } catch (e: Exception) {
                try {
                    context.applicationContext.stopService(
                        Intent(context.applicationContext, MediaPlaybackService::class.java)
                    )
                } catch (_: Exception) {}
            }
        }
    }
}
