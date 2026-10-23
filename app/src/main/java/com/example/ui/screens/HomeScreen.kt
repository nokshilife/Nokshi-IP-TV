package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.draw.scale
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
        viewModel.youtubeChannels.collectAsState().value,
        viewModel.m3uChannels.collectAsState().value,
        viewModel.favoriteChannels.collectAsState().value
    ) {
        viewModel.getVisibleChannels()
    }

    val fullScreenFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    // User requirement: "যেকোন চ্যানেল চেঞ্জের পর ফুল স্ক্রিন বাটন সেট ফোকাসে থাকবে যাতে সহজে এক প্রেসে ফুল স্ক্রিন করতে পারি।"
    LaunchedEffect(currentChannel?.id) {
        if (currentChannel != null && !isFullscreen) {
            kotlinx.coroutines.delay(200)
            try {
                fullScreenFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    // User requirement: "m3u তে যেহেতু অনেক চ্যানেল থাকবে তাই Lazy Load টেকনোলোজি ব্যবহার করবেন।"
    LaunchedEffect(gridState.firstVisibleItemIndex, visibleChannels.size) {
        if (activeTab == "M3U") {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= visibleChannels.size - 10) {
                viewModel.loadMoreM3uChannels()
            }
        }
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
                    androidx.compose.runtime.key(ch.id) {
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
        } else if (isLandscape) {
            // Android TV & Landscape Two-Pane Layout
            // Left: Player + Channel Info + TV Controls (Full Screen, Favorite, Settings, Sleep, Refresh)
            // Right: Tabs + Categories + Channel Grid (Both fully visible & D-pad navigable on TV!)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1715))
            ) {
                // Left Pane (Video Player & TV Controls)
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .padding(12.dp)
                ) {
                    // Mini Header: App Logo + Title + Server Sync / Live Indicator
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
                        Column {
                            Text(
                                text = "Nokshi IP TV",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Android TV Live",
                                color = Color(0xFF80CBC4),
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (currentChannel?.type == ChannelType.YOUTUBE) Color(0xFFE53935) else Color(0xFF00897B)
                        ) {
                            Text(
                                text = if (currentChannel?.type == ChannelType.YOUTUBE) "YT LIVE" else "IPTV",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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
                            androidx.compose.runtime.key(ch.id) {
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

                    // Channel Name & Category Details
                    Text(
                        text = currentChannel?.name ?: "কোনো চ্যানেল নির্বাচিত নেই",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentChannel?.groupTitle?.takeIf { it.isNotEmpty() } ?: "Nokshi IP TV",
                        color = Color(0xFF80CBC4),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // TV Action Buttons Row (Full Screen, Favorite, Settings, Sleep Timer, Refresh)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Prominent Full Screen Button (Auto-focused on channel change!)
                        FullScreenButton(
                            onClick = { viewModel.setFullscreen(true) },
                            focusRequester = fullScreenFocusRequester
                        )

                        // Favorite Button
                        currentChannel?.let { ch ->
                            TvActionButton(
                                onClick = { viewModel.toggleFavorite(ch) },
                                icon = if (ch.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                tint = if (ch.isFavorite) Color(0xFFFFD600) else Color.White,
                                contentDescription = "Favorite",
                                label = if (ch.isFavorite) "পছন্দ" else "ফেভারিট"
                            )
                        }

                        // Settings Button (Directly accessible on Android TV!)
                        TvActionButton(
                            onClick = { viewModel.openSettings() },
                            icon = Icons.Default.Settings,
                            tint = Color(0xFFFF9800),
                            contentDescription = "সেটিংস",
                            label = "সেটিংস"
                        )

                        // Sleep Timer Button
                        TvActionButton(
                            onClick = { viewModel.openSleepTimerDialog() },
                            icon = Icons.Default.Bedtime,
                            tint = if (sleepTimerSeconds > 0) Color(0xFFFFB300) else Color(0xFFB0BEC5),
                            contentDescription = "স্লিপ",
                            label = if (sleepTimerSeconds > 0) "${sleepTimerSeconds / 60}m" else "স্লিপ"
                        )

                        // Refresh / Sync Button
                        TvActionButton(
                            onClick = { viewModel.checkServer(forceUpdate = true) },
                            icon = Icons.Default.Refresh,
                            tint = Color(0xFF80CBC4),
                            contentDescription = "রিফ্রেশ",
                            label = "রিফ্রেশ"
                        )
                    }
                }

                // Right Pane: Top Tabs (YouTube / M3U / Favorites) + Categories + Channel Grid
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
                            modifier = Modifier.weight(1f)
                        )
                        TvTabButton(
                            title = "M3U প্লেলিস্ট",
                            isSelected = activeTab == "M3U",
                            icon = Icons.Default.Tv,
                            accentColor = Color(0xFF00E676),
                            onClick = { viewModel.setActiveTab("M3U") },
                            modifier = Modifier.weight(1f)
                        )
                        TvTabButton(
                            title = "প্রিয় চ্যানেল",
                            isSelected = activeTab == "FAVORITES",
                            icon = Icons.Default.Star,
                            accentColor = Color(0xFFFFD600),
                            onClick = { viewModel.setActiveTab("FAVORITES") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Category Filter Chips (With amber/brown focus border requested by user)
                    if (availableGroups.size > 1) {
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

                    // Channel Grid (Visible right beside the player on TV, lazy loaded!)
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

                        // Sleep Timer Button
                        IconButton(onClick = { viewModel.openSleepTimerDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerSeconds > 0) Color(0xFFFFB300) else Color(0xFFB0BEC5)
                            )
                        }

                        // Refresh / Sync Button (Forces check_youtube_live.php and updates channel data)
                        IconButton(onClick = { viewModel.checkServer(forceUpdate = true) }) {
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
                            androidx.compose.runtime.key(ch.id) {
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

                    // Dedicated Under-Player Control Bar requested by user
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
                            // Channel Details
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (currentChannel?.type == ChannelType.YOUTUBE) Color(0xFFE53935) else Color(0xFF00897B),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = if (currentChannel?.type == ChannelType.YOUTUBE) "YT LIVE" else "IPTV",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = currentChannel?.name ?: "কোনো চ্যানেল নির্বাচিত নেই",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentChannel?.groupTitle?.takeIf { it.isNotEmpty() } ?: "Nokshi IP TV",
                                        color = Color(0xFF80CBC4),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action buttons: Favorite & Big Full Screen button
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

                                // User explicitly requested Full Screen button below YouTube Player
                                // Styled for touch & TV D-pad remote focus highlight
                                FullScreenButton(
                                    onClick = { viewModel.setFullscreen(true) },
                                    focusRequester = fullScreenFocusRequester
                                )
                            }
                        }
                    }

                    // Category Filter Chips (With amber/brown focus border requested by user)
                    if (availableGroups.size > 1) {
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
private fun FullScreenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
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
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
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

// User requirement 4:
// "M3U প্লেলিস্টের আন্ডারে থাকা বিভিন্ন গ্রুপের মাঝে নেভিগেট করতে গেলে কোন গ্রুপ সিলেক্ট করতে যাচ্ছি তা বোঝা যায়না। অন্য থাম্বনেইলের মতো বাদামী বর্ডার দিয়ে দেন।"
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

    // Distinct Amber / Brown border on D-pad TV focus, matching channel thumbnails
    val borderColor = when {
        isFocused -> Color(0xFFFF9800) // Vibrant TV focus amber / brown
        isSelected -> Color(0xFFFFB300)
        else -> Color(0x33FFFFFF)
    }

    val borderWidth = if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 1.dp

    val containerColor = when {
        isFocused && isSelected -> Color(0xFFFF9800)
        isFocused -> Color(0xFF2D2319) // Warm dark brown focus container
        isSelected -> Color(0xFFFF9800)
        else -> Color(0xFF1B2825)
    }

    val textColor = when {
        isSelected -> Color.Black
        isFocused -> Color(0xFFFFD54F) // Bright amber text on focus
        else -> Color(0xFFE0F2F1)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .scale(scale)
            .focusable(interactionSource = interactionSource)
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
            .focusable(interactionSource = interactionSource)
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
            .focusable(interactionSource = interactionSource)
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


