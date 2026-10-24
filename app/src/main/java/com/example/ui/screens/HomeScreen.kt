package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import com.example.ui.viewmodel.BackendLogEntry
import com.example.ui.viewmodel.BackendLogLevel
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val activeTab by viewModel.activeTab.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val isFlashBannerVisible by viewModel.isFlashBannerVisible.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val serverSyncState by viewModel.serverSyncState.collectAsState()
    val sleepTimerSeconds by viewModel.sleepTimerSeconds.collectAsState()
    val isAppStartupSplashVisible by viewModel.isAppStartupSplashVisible.collectAsState()

    val backendLogs by viewModel.backendLogs.collectAsState()
    val activeBackendStatus by viewModel.activeBackendStatus.collectAsState()
    val isBackendBusy by viewModel.isBackendBusy.collectAsState()

    val ytChannels by viewModel.youtubeChannels.collectAsState()
    val m3uChannels by viewModel.m3uChannels.collectAsState()
    val favChannels by viewModel.favoriteChannels.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isPinDialogOpen by viewModel.isPinDialogOpen.collectAsState()
    val isSleepTimerDialogOpen by viewModel.isSleepTimerDialogOpen.collectAsState()
    val isExitDialogOpen by viewModel.isExitDialogOpen.collectAsState()

    val pinLoading by viewModel.pinLoading.collectAsState()
    val pinError by viewModel.pinError.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val settingsStatus by viewModel.settingsStatus.collectAsState()
    val settingsLoading by viewModel.settingsLoading.collectAsState()
    val m3uVisibleLimit by viewModel.m3uVisibleLimit.collectAsState()

    val visibleChannels = remember(
        activeTab,
        selectedGroup,
        m3uVisibleLimit,
        ytChannels,
        m3uChannels,
        favChannels
    ) {
        val baseList = when (activeTab) {
            "YOUTUBE" -> ytChannels.sortedWith(compareBy({ it.displayOrder }, { it.id }))
            "M3U" -> m3uChannels
            "FAVORITES" -> favChannels
            else -> emptyList()
        }
        val filtered = if (selectedGroup.isNullOrEmpty() || selectedGroup == "All") {
            baseList
        } else {
            baseList.filter { it.groupTitle.equals(selectedGroup, ignoreCase = true) }
        }
        when (activeTab) {
            "YOUTUBE" -> filtered.sortedWith(compareBy({ it.displayOrder }, { it.id }))
            "M3U" -> filtered.take(m3uVisibleLimit)
            else -> filtered
        }
    }

    val fullScreenFocusRequester = remember { FocusRequester() }
    val channelFocusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val gridState = rememberLazyGridState()

    // Focus Full Screen button automatically whenever channel changes (after startup splash closes)
    LaunchedEffect(currentChannel?.id, isAppStartupSplashVisible) {
        if (!isAppStartupSplashVisible && currentChannel != null && !isFullscreen) {
            delay(180)
            try {
                fullScreenFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    // Lazy load pagination for M3U channels
    LaunchedEffect(gridState.firstVisibleItemIndex, visibleChannels.size) {
        if (activeTab == "M3U") {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= visibleChannels.size - 10) {
                viewModel.loadMoreM3uChannels()
            }
        }
    }

    // Extract categories/groups for current tab
    val availableGroups = remember(activeTab, ytChannels, m3uChannels, favChannels) {
        val base = when (activeTab) {
            "YOUTUBE" -> ytChannels
            "M3U" -> m3uChannels
            else -> favChannels
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
                currentChannel?.let { ch ->
                    androidx.compose.runtime.key(ch.id, ch.streamUrl) {
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
                }

                FlashBanner(
                    isVisible = isFlashBannerVisible,
                    channelName = currentChannel?.name ?: "",
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { viewModel.setFullscreen(false) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0x88000000), CircleShape)
                        .testTag("exit_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        } else if (isLandscape) {
            // Android TV & Landscape Two-Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1715))
            ) {
                // Left Pane (Header + Player + Channel Name + Action Bar + Backend Process Monitor)
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .padding(12.dp)
                ) {
                    // Clean Header: App Logo + Title ONLY (Crossed-out "Android TV Live" & "YT LIVE" removed)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nokshi IP TV",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // 16:9 Video Player Box inside Left Pane
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        currentChannel?.let { ch ->
                            androidx.compose.runtime.key(ch.id, ch.streamUrl) {
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

                        FlashBanner(
                            isVisible = isFlashBannerVisible,
                            channelName = currentChannel?.name ?: "",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Channel Name ONLY (Crossed-out "YouTube Live" subtitle removed)
                    Text(
                        text = currentChannel?.name ?: "কোনো চ্যানেল নির্বাচিত নেই",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // TV Action Buttons Row:
                    // Left: [ফেভারিট] [সেটিংস] [স্লিপ] ---- Spacer ---- Far Right: [ফুল স্ক্রিন]
                    // ("রিফ্রেশ" button removed as requested; Right D-pad on "ফুল স্ক্রিন" jumps to next channel card!)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Favorite Button
                        currentChannel?.let { ch ->
                            TvActionButton(
                                onClick = { viewModel.toggleFavorite(ch) },
                                icon = if (ch.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                tint = if (ch.isFavorite) Color(0xFFFFD600) else Color.White,
                                contentDescription = "Favorite",
                                label = if (ch.isFavorite) "পছন্দ" else "ফেভারিট",
                                modifier = Modifier.testTag("tv_favorite_button")
                            )
                        }

                        // Settings Button
                        TvActionButton(
                            onClick = { viewModel.openSettings() },
                            icon = Icons.Default.Settings,
                            tint = Color(0xFFFF9800),
                            contentDescription = "সেটিংস",
                            label = "সেটিংস",
                            modifier = Modifier.testTag("tv_settings_button")
                        )

                        // Sleep Timer Button
                        TvActionButton(
                            onClick = { viewModel.openSleepTimerDialog() },
                            icon = Icons.Default.Bedtime,
                            tint = if (sleepTimerSeconds > 0) Color(0xFFFFB300) else Color(0xFFB0BEC5),
                            contentDescription = "স্লিপ",
                            label = if (sleepTimerSeconds > 0) "${sleepTimerSeconds / 60}m" else "স্লিপ",
                            modifier = Modifier.testTag("tv_sleep_button")
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Far-Right Prominent Full Screen Button:
                        // Pressing Right D-pad jumps directly to the thumbnail card AFTER the currently playing channel!
                        FullScreenButton(
                            onClick = { viewModel.setFullscreen(true) },
                            focusRequester = fullScreenFocusRequester,
                            onRightKeyPressed = {
                                if (visibleChannels.isNotEmpty()) {
                                    val curIdx = visibleChannels.indexOfFirst { it.id == currentChannel?.id }
                                    val nextIdx = if (curIdx != -1 && curIdx + 1 < visibleChannels.size) curIdx + 1 else 0
                                    val targetChannel = visibleChannels[nextIdx]
                                    coroutineScope.launch {
                                        val isItemVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == nextIdx }
                                        if (!isItemVisible) {
                                            gridState.scrollToItem((nextIdx - 3).coerceAtLeast(0))
                                            delay(45)
                                        }
                                        try {
                                            channelFocusRequesters[targetChannel.id]?.requestFocus()
                                        } catch (e: Exception) {}
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                            modifier = Modifier.testTag("tv_fullscreen_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Backend Process Monitor Panel (Fills empty space below TV controls)
                    TvBackendProcessPanel(
                        activeStatus = activeBackendStatus,
                        isBusy = isBackendBusy,
                        logs = backendLogs,
                        sleepTimerSeconds = sleepTimerSeconds,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                // Right Pane: Top Tabs + Category Chips + Channel Grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF141E1C))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    // TV Navigation Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TvTabButton(
                            title = "YouTube লাইভ",
                            isSelected = activeTab == "YOUTUBE",
                            icon = Icons.Default.Videocam,
                            accentColor = Color(0xFFFF5252),
                            onClick = { viewModel.setActiveTab("YOUTUBE") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_youtube")
                        )
                        TvTabButton(
                            title = "M3U প্লেলিস্ট",
                            isSelected = activeTab == "M3U",
                            icon = Icons.Default.Tv,
                            accentColor = Color(0xFF00E676),
                            onClick = { viewModel.setActiveTab("M3U") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_m3u")
                        )
                        TvTabButton(
                            title = "প্রিয় চ্যানেল",
                            isSelected = activeTab == "FAVORITES",
                            icon = Icons.Default.Star,
                            accentColor = Color(0xFFFFD600),
                            onClick = { viewModel.setActiveTab("FAVORITES") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_favorites")
                        )
                    }

                    // Category Filter Chips (Only needed when multiple categories exist, e.g., M3U groups)
                    if (activeTab != "YOUTUBE" && availableGroups.size > 2) {
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableGroups) { group ->
                                val isSelected = if (selectedGroup == null) group == "All" else selectedGroup == group
                                TvGroupChip(
                                    text = if (group == "All") "সকল" else group,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.setSelectedGroup(if (group == "All") null else group)
                                    }
                                )
                            }
                        }
                    }

                    // Channel Grid
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (visibleChannels.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color(0xFF3E5751),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (activeTab == "M3U") "কোনো M3U চ্যানেল নেই" else "কোনো চ্যানেল পাওয়া যায়নি",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 115.dp),
                                state = gridState,
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tv_channel_grid")
                            ) {
                                items(visibleChannels, key = { it.id }) { ch ->
                                    val cardFocusRequester = remember(ch.id) {
                                        channelFocusRequesters.getOrPut(ch.id) { FocusRequester() }
                                    }
                                    TvFocusCard(
                                        channel = ch,
                                        isSelected = currentChannel?.id == ch.id,
                                        onClick = { viewModel.selectChannel(ch) },
                                        onToggleFavorite = { viewModel.toggleFavorite(ch) },
                                        focusRequester = cardFocusRequester,
                                        modifier = Modifier.testTag("channel_card_${ch.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Mobile Portrait View
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

                        IconButton(onClick = { viewModel.openSleepTimerDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerSeconds > 0) Color(0xFFFFB300) else Color(0xFFB0BEC5)
                            )
                        }

                        IconButton(onClick = { viewModel.checkServer(forceUpdate = true) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFF80CBC4)
                            )
                        }

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
                            androidx.compose.runtime.key(ch.id, ch.streamUrl) {
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
                    }

                    // Mobile Under-Player Control Bar
                    Surface(
                        color = Color(0xFF141E1C),
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentChannel?.name ?: "কোনো চ্যানেল নির্বাচিত নেই",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                currentChannel?.let { ch ->
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(ch) },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (ch.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "Favorite",
                                            tint = if (ch.isFavorite) Color(0xFFFFD600) else Color(0xFF88A29B),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                FullScreenButton(
                                    onClick = { viewModel.setFullscreen(true) },
                                    focusRequester = fullScreenFocusRequester,
                                    modifier = Modifier.testTag("mobile_fullscreen_button")
                                )
                            }
                        }
                    }

                    // Category Filter Chips (M3U groups)
                    if (activeTab != "YOUTUBE" && availableGroups.size > 2) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableGroups) { group ->
                                val isSelected = if (selectedGroup == null) group == "All" else selectedGroup == group
                                TvGroupChip(
                                    text = if (group == "All") "সকল" else group,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.setSelectedGroup(if (group == "All") null else group)
                                    }
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
                                state = gridState,
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

        // Full-Screen App Startup Splash Image Overlay (Eliminates blank startup screen)
        AnimatedVisibility(
            visible = isAppStartupSplashVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            StartupSplashOverlay(statusMessage = activeBackendStatus)
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
                onForceServerCheck = { viewModel.checkServer(forceUpdate = true) },
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

@Composable
private fun StartupSplashOverlay(
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A110F))
    ) {
        Image(
            painter = painterResource(id = R.drawable.nokshi_flash_banner),
            contentDescription = "Nokshi IP TV Splash",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33000000),
                            Color(0xDD08100E)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFF9800),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Nokshi IP TV",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusMessage,
                color = Color(0xFFFFCC80),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvBackendProcessPanel(
    activeStatus: String,
    isBusy: Boolean,
    logs: List<BackendLogEntry>,
    sleepTimerSeconds: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF131D1A),
        border = BorderStroke(1.dp, Color(0xFF233631)),
        modifier = modifier.testTag("tv_backend_process_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Header bar with live activity indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color(0xFF80CBC4),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ব্যাকএন্ড প্রসেস স্ট্যাটাস",
                    color = Color(0xFF80CBC4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (sleepTimerSeconds > 0) {
                    Text(
                        text = "স্লিপ: ${sleepTimerSeconds / 60}m ${sleepTimerSeconds % 60}s",
                        color = Color(0xFFFFCC80),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                if (isBusy) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF9800),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isBusy) "প্রসেসিং..." else "সচল",
                    color = if (isBusy) Color(0xFFFFB74D) else Color(0xFF00E676),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Highlighted current backend action
            Text(
                text = activeStatus,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(
                color = Color(0xFF20312C),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 5.dp)
            )

            // Recent live backend process logs (non-focusable so TV D-pad stays on controls/cards)
            LazyColumn(
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs.take(6), key = { it.id }) { item ->
                    val dotColor = when (item.level) {
                        BackendLogLevel.SUCCESS -> Color(0xFF00E676)
                        BackendLogLevel.WORKING -> Color(0xFFFFB300)
                        BackendLogLevel.WARNING -> Color(0xFFFF7043)
                        BackendLogLevel.INFO -> Color(0xFF80CBC4)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.time,
                            color = Color(0xFF68867E),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.message,
                            color = Color(0xFFB8CFC9),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onRightKeyPressed: (() -> Boolean)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        label = "fs_btn_scale"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isFocused) Color(0xFFFF9800) else Color(0xFF263238),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) Color(0xFFFFD54F) else Color(0xFF455A64)
        ),
        modifier = modifier
            .scale(scale)
            .onPreviewKeyEvent { keyEvent ->
                if (onRightKeyPressed != null &&
                    keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                ) {
                    onRightKeyPressed()
                } else {
                    false
                }
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Full Screen",
                tint = if (isFocused) Color.Black else Color(0xFFFFD54F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ফুল স্ক্রিন",
                color = if (isFocused) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TvGroupChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        label = "group_chip_scale"
    )

    val borderColor = when {
        isFocused -> Color(0xFFFF9800)
        isSelected -> Color(0xFFFFB300)
        else -> Color(0x33FFFFFF)
    }

    val borderWidth = if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 1.dp

    val containerColor = when {
        isFocused && isSelected -> Color(0xFFFF9800)
        isFocused -> Color(0xFF2D2319)
        isSelected -> Color(0xFFFF9800)
        else -> Color(0xFF1B2825)
    }

    val textColor = when {
        isSelected -> Color.Black
        isFocused -> Color(0xFFFFD54F)
        else -> Color(0xFFE0F2F1)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TvActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        label = "act_btn_scale"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isFocused) Color(0xFFFF9800) else Color(0xFF1E2D29),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color(0xFFFFD54F) else Color(0x33FFFFFF)
        ),
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color.Black else tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isFocused) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TvTabButton(
    title: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        label = "tab_btn_scale"
    )

    val bgColor = when {
        isFocused -> Color(0xFFFF9800)
        isSelected -> Color(0xFF263833)
        else -> Color(0xFF1B2624)
    }

    val contentColor = when {
        isFocused -> Color.Black
        isSelected -> accentColor
        else -> Color(0xFF88A29B)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
            color = if (isFocused) Color(0xFFFFD54F) else if (isSelected) accentColor else Color(0x22FFFFFF)
        ),
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
