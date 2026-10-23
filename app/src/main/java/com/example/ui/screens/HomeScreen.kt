package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ChannelType
import com.example.data.model.ServerSyncState
import com.example.player.ExoPlayerView
import com.example.player.YouTubePlayerView
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.components.FlashBanner
import com.example.ui.components.PinVerificationDialog
import com.example.ui.components.PlaylistSettingsDialog
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TvFocusCard
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val activeTab by viewModel.activeTab.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val isFlashBannerVisible by viewModel.isFlashBannerVisible.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val serverSyncState by viewModel.serverSyncState.collectAsState()
    val sleepTimerSeconds by viewModel.sleepTimerSeconds.collectAsState()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isPinDialogOpen by viewModel.isPinDialogOpen.collectAsState()
    val isSleepTimerDialogOpen by viewModel.isSleepTimerDialogOpen.collectAsState()
    val isExitDialogOpen by viewModel.isExitDialogOpen.collectAsState()

    val pinLoading by viewModel.pinLoading.collectAsState()
    val pinError by viewModel.pinError.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val settingsStatus by viewModel.settingsStatus.collectAsState()
    val settingsLoading by viewModel.settingsLoading.collectAsState()

    val visibleChannels = remember(
        activeTab,
        selectedGroup,
        viewModel.youtubeChannels.collectAsState().value,
        viewModel.m3uChannels.collectAsState().value,
        viewModel.favoriteChannels.collectAsState().value
    ) {
        viewModel.getVisibleChannels()
    }

    // Extract categories/groups for current tab
    val availableGroups = remember(activeTab, viewModel.youtubeChannels.collectAsState().value, viewModel.m3uChannels.collectAsState().value) {
        val base = when (activeTab) {
            "YOUTUBE" -> viewModel.youtubeChannels.value
            "M3U" -> viewModel.m3uChannels.value
            else -> viewModel.favoriteChannels.value
        }
        val groups = base.map { it.groupTitle }.distinct().filter { it.isNotEmpty() }
        listOf("All") + groups
    }

    // Manage screen orientation when fullscreen toggles
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Back handling: If in fullscreen, exit fullscreen. If on home, show exit dialog.
    BackHandler {
        if (isFullscreen) {
            viewModel.setFullscreen(false)
        } else {
            viewModel.openExitDialog()
        }
    }

    // Swipe state on player for mobile channel switching
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1715))
    ) {
        if (isFullscreen) {
            // Fullscreen View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            dragOffset += delta
                        },
                        onDragStopped = {
                            if (dragOffset > 100f) {
                                viewModel.playPreviousChannel()
                            } else if (dragOffset < -100f) {
                                viewModel.playNextChannel()
                            }
                            dragOffset = 0f
                        }
                    )
            ) {
                // Video Player
                currentChannel?.let { ch ->
                    if (ch.type == ChannelType.YOUTUBE) {
                        val videoId = ch.getYouTubeVideoId() ?: ""
                        YouTubePlayerView(
                            videoId = videoId,
                            modifier = Modifier.fillMaxSize(),
                            onPlaybackStarted = { viewModel.onPlaybackStarted() },
                            onPlaybackError = { err -> viewModel.onPlaybackError(err) }
                        )
                    } else {
                        ExoPlayerView(
                            streamUrl = ch.streamUrl,
                            modifier = Modifier.fillMaxSize(),
                            onPlaybackStarted = { viewModel.onPlaybackStarted() },
                            onPlaybackError = { err -> viewModel.onPlaybackError(err) },
                            onDoubleTap = { viewModel.toggleFullscreen() }
                        )
                    }
                }

                // Flash Banner
                FlashBanner(
                    isVisible = isFlashBannerVisible,
                    channelName = currentChannel?.name ?: "",
                    modifier = Modifier.fillMaxSize()
                )

                // Fullscreen Exit Button
                IconButton(
                    onClick = { viewModel.setFullscreen(false) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0x88000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        } else {
            // Standard Mobile & Android TV View
            Scaffold(
                containerColor = Color(0xFF0F1715),
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF141E1C),
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "YOUTUBE",
                            onClick = { viewModel.setActiveTab("YOUTUBE") },
                            icon = { Icon(Icons.Default.Videocam, contentDescription = "YouTube") },
                            label = { Text("YouTube লাইভ", fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFF5252),
                                selectedTextColor = Color(0xFFFF5252),
                                indicatorColor = Color(0xFF2A1F1F),
                                unselectedIconColor = Color(0xFF88A29B),
                                unselectedTextColor = Color(0xFF88A29B)
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == "M3U",
                            onClick = { viewModel.setActiveTab("M3U") },
                            icon = { Icon(Icons.Default.Tv, contentDescription = "M3U") },
                            label = { Text("M3U প্লেলিস্ট", fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00E676),
                                selectedTextColor = Color(0xFF00E676),
                                indicatorColor = Color(0xFF1B2E26),
                                unselectedIconColor = Color(0xFF88A29B),
                                unselectedTextColor = Color(0xFF88A29B)
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == "FAVORITES",
                            onClick = { viewModel.setActiveTab("FAVORITES") },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                            label = { Text("প্রিয় চ্যানেল", fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFFD600),
                                selectedTextColor = Color(0xFFFFD600),
                                indicatorColor = Color(0xFF332D19),
                                unselectedIconColor = Color(0xFF88A29B),
                                unselectedTextColor = Color(0xFF88A29B)
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Header Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141E1C))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Nokshi IP TV",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "বাংলাদেশের টিভি, আপনার হাতের মুঠোয়",
                                color = Color(0xFF80CBC4),
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Sleep Timer Button
                        IconButton(onClick = { viewModel.openSleepTimerDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerSeconds > 0) Color(0xFFFFB300) else Color(0xFFB0BEC5)
                            )
                        }

                        // Refresh / Sync Button
                        IconButton(onClick = { viewModel.checkServer() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFF80CBC4)
                            )
                        }

                        // Settings Button
                        IconButton(onClick = { viewModel.openSettings() }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFFFF9800)
                            )
                        }
                    }

                    // Server sync status notice
                    AnimatedVisibility(visible = serverSyncState !is ServerSyncState.Idle) {
                        Surface(
                            color = when (serverSyncState) {
                                is ServerSyncState.Updating -> Color(0xFFE65100)
                                is ServerSyncState.Success -> Color(0xFF1B5E20)
                                else -> Color(0xFF263238)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                if (serverSyncState is ServerSyncState.Updating || serverSyncState is ServerSyncState.Checking) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val text = when (val s = serverSyncState) {
                                    is ServerSyncState.Checking -> s.message
                                    is ServerSyncState.Updating -> s.message
                                    is ServerSyncState.Success -> s.message
                                    is ServerSyncState.Notice -> s.message
                                    else -> ""
                                }
                                Text(
                                    text = text,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Video Player Container (16:9 ratio)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    dragOffset += delta
                                },
                                onDragStopped = {
                                    if (dragOffset > 80f) {
                                        viewModel.playPreviousChannel()
                                    } else if (dragOffset < -80f) {
                                        viewModel.playNextChannel()
                                    }
                                    dragOffset = 0f
                                }
                            )
                    ) {
                        currentChannel?.let { ch ->
                            if (ch.type == ChannelType.YOUTUBE) {
                                val videoId = ch.getYouTubeVideoId() ?: ""
                                YouTubePlayerView(
                                    videoId = videoId,
                                    modifier = Modifier.fillMaxSize(),
                                    onPlaybackStarted = { viewModel.onPlaybackStarted() },
                                    onPlaybackError = { err -> viewModel.onPlaybackError(err) }
                                )
                            } else {
                                ExoPlayerView(
                                    streamUrl = ch.streamUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    onPlaybackStarted = { viewModel.onPlaybackStarted() },
                                    onPlaybackError = { err -> viewModel.onPlaybackError(err) },
                                    onDoubleTap = { viewModel.toggleFullscreen() }
                                )
                            }
                        } ?: run {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "কোনো চ্যানেল নির্বাচিত নেই",
                                    color = Color(0xFF88A29B),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Flash Banner
                        FlashBanner(
                            isVisible = isFlashBannerVisible,
                            channelName = currentChannel?.name ?: "",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Gradient Overlay for title and fullscreen button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xCC000000), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentChannel?.name ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.toggleFullscreen() }) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Category Filter Chips
                    if (availableGroups.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableGroups) { group ->
                                val isSelected = if (selectedGroup == null) group == "All" else selectedGroup == group
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setSelectedGroup(if (group == "All") null else group)
                                    },
                                    label = {
                                        Text(
                                            text = if (group == "All") "সকল" else group,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF9800),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF1B2825),
                                        labelColor = Color(0xFFE0F2F1)
                                    )
                                )
                            }
                        }
                    }

                    // Channel Grid
                    Box(modifier = Modifier.weight(1f)) {
                        if (visibleChannels.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color(0xFF3E5751),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (activeTab == "M3U") "কোনো M3U চ্যানেল লোড করা নেই" else "কোনো চ্যানেল পাওয়া যায়নি",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (activeTab == "M3U") "সেটিংস থেকে বিল্ট-ইন M3U বা কাস্টম প্লেলিস্ট লোড করুন।" else "পুনরায় চেষ্টা করুন।",
                                    color = Color(0xFF88A29B),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 130.dp),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(visibleChannels, key = { it.id }) { ch ->
                                    TvFocusCard(
                                        channel = ch,
                                        isSelected = currentChannel?.id == ch.id,
                                        onClick = { viewModel.selectChannel(ch) },
                                        onToggleFavorite = { viewModel.toggleFavorite(ch) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (isSettingsOpen) {
            PlaylistSettingsDialog(
                isBuiltInPinVerified = viewModel.repository.preferences.isBuiltInPinVerified,
                playlists = playlists,
                isBackgroundAudioEnabled = viewModel.repository.preferences.isBackgroundAudioEnabled,
                isLoading = settingsLoading,
                statusMessage = settingsStatus,
                onLoadBuiltInM3u = { viewModel.loadBuiltInM3u() },
                onLoadCustomM3u = { url, name -> viewModel.loadCustomM3u(url, name) },
                onLoadXtream = { server, user, pass -> viewModel.loadXtream(server, user, pass) },
                onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                onToggleBackgroundAudio = { viewModel.setBackgroundAudio(it) },
                onOpenSleepTimer = {
                    viewModel.closeSettings()
                    viewModel.openSleepTimerDialog()
                },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        if (isPinDialogOpen) {
            PinVerificationDialog(
                isLoading = pinLoading,
                errorMessage = pinError,
                onVerify = { pin -> viewModel.verifyPinAndLoadBuiltIn(pin) },
                onDismiss = { viewModel.closePinDialog() }
            )
        }

        if (isSleepTimerDialogOpen) {
            SleepTimerDialog(
                isActive = sleepTimerSeconds > 0,
                remainingSeconds = sleepTimerSeconds,
                onSetTimer = { min -> viewModel.startSleepTimer(min) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.closeSleepTimerDialog() }
            )
        }

        if (isExitDialogOpen) {
            ExitConfirmationDialog(
                onConfirmExit = {
                    viewModel.closeExitDialog()
                    activity?.finishAffinity()
                },
                onDismiss = { viewModel.closeExitDialog() }
            )
        }
    }
}
