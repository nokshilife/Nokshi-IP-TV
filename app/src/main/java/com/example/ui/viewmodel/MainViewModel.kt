package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Channel
import com.example.data.model.ChannelType
import com.example.data.model.Playlist
import com.example.data.model.ServerSyncState
import com.example.data.repository.ChannelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class BackendLogLevel {
    INFO, WORKING, SUCCESS, WARNING
}

data class BackendLogEntry(
    val id: Long,
    val time: String,
    val message: String,
    val level: BackendLogLevel = BackendLogLevel.INFO
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChannelRepository(application)

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Dhaka")
    }
    private var logSequence = 0L

    private val _backendLogs = MutableStateFlow<List<BackendLogEntry>>(emptyList())
    val backendLogs: StateFlow<List<BackendLogEntry>> = _backendLogs.asStateFlow()

    private val _activeBackendStatus = MutableStateFlow("অ্যাপ চালু হচ্ছে...")
    val activeBackendStatus: StateFlow<String> = _activeBackendStatus.asStateFlow()

    private val _isBackendBusy = MutableStateFlow(true)
    val isBackendBusy: StateFlow<Boolean> = _isBackendBusy.asStateFlow()

    // App startup full-screen splash image state (disappears once app & initial channel list are ready)
    private val _isAppStartupSplashVisible = MutableStateFlow(true)
    val isAppStartupSplashVisible: StateFlow<Boolean> = _isAppStartupSplashVisible.asStateFlow()

    private fun appendBackendLog(
        message: String,
        level: BackendLogLevel = BackendLogLevel.INFO,
        isBusy: Boolean? = null
    ) {
        val now = synchronized(timeFormatter) { timeFormatter.format(Date()) }
        val entry = BackendLogEntry(
            id = ++logSequence,
            time = now,
            message = message,
            level = level
        )
        _activeBackendStatus.value = message
        if (isBusy != null) {
            _isBackendBusy.value = isBusy
        }
        _backendLogs.value = (listOf(entry) + _backendLogs.value).take(25)
    }

    // User requirement: "ইউটিউব চ্যানেল display_order অনুযায়ী sort করবেন।"
    val youtubeChannels: StateFlow<List<Channel>> = repository.getYouTubeChannels()
        .map { list -> list.sortedWith(compareBy({ it.displayOrder }, { it.id })) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val m3uChannels: StateFlow<List<Channel>> = repository.getM3uChannels()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = repository.getFavoriteChannels()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Lazy load paging for M3U channels
    private val _m3uVisibleLimit = MutableStateFlow(80)
    val m3uVisibleLimit: StateFlow<Int> = _m3uVisibleLimit.asStateFlow()

    fun loadMoreM3uChannels() {
        _m3uVisibleLimit.value += 60
    }

    private val _activeTab = MutableStateFlow("YOUTUBE") // YOUTUBE, M3U, FAVORITES
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _isFlashBannerVisible = MutableStateFlow(true)
    val isFlashBannerVisible: StateFlow<Boolean> = _isFlashBannerVisible.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _serverSyncState = MutableStateFlow<ServerSyncState>(ServerSyncState.Idle)
    val serverSyncState: StateFlow<ServerSyncState> = _serverSyncState.asStateFlow()

    // Sleep Timer
    private val _sleepTimerSeconds = MutableStateFlow(0)
    val sleepTimerSeconds: StateFlow<Int> = _sleepTimerSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Dialogs
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isPinDialogOpen = MutableStateFlow(false)
    val isPinDialogOpen: StateFlow<Boolean> = _isPinDialogOpen.asStateFlow()

    private val _isSleepTimerDialogOpen = MutableStateFlow(false)
    val isSleepTimerDialogOpen: StateFlow<Boolean> = _isSleepTimerDialogOpen.asStateFlow()

    private val _isExitDialogOpen = MutableStateFlow(false)
    val isExitDialogOpen: StateFlow<Boolean> = _isExitDialogOpen.asStateFlow()

    private val _pinLoading = MutableStateFlow(false)
    val pinLoading: StateFlow<Boolean> = _pinLoading.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _settingsStatus = MutableStateFlow<String?>(null)
    val settingsStatus: StateFlow<String?> = _settingsStatus.asStateFlow()

    private val _settingsLoading = MutableStateFlow(false)
    val settingsLoading: StateFlow<Boolean> = _settingsLoading.asStateFlow()

    private var isPlaybackStarted = false
    private var loadingWatchdogJob: Job? = null

    init {
        _activeTab.value = repository.preferences.lastSelectedTab
        appendBackendLog("অ্যাপ চালু হয়েছে — লোকাল চ্যানেল ডাটাবেজ প্রস্তুত হচ্ছে...", BackendLogLevel.WORKING, isBusy = true)

        viewModelScope.launch {
            val count = repository.initializeChannelsIfEmpty()
            appendBackendLog("লোকাল ডাটাবেজ প্রস্তুত ($count টি চ্যানেল পাওয়া গেছে)", BackendLogLevel.INFO, isBusy = true)

            // Wait briefly if Room Flow is emitting initial items, then dismiss full-screen startup splash
            var waitedMs = 0
            while (youtubeChannels.value.isEmpty() && m3uChannels.value.isEmpty() && waitedMs < 1800) {
                delay(100)
                waitedMs += 100
            }
            delay(350)
            _isAppStartupSplashVisible.value = false

            // Run async server check & MySQL display_order sync
            checkServer(forceUpdate = false)
        }

        // Auto select first channel when channels load if none selected
        viewModelScope.launch {
            youtubeChannels.collect { list ->
                if (_activeTab.value == "YOUTUBE") {
                    val cur = _currentChannel.value
                    if (cur == null && list.isNotEmpty()) {
                        selectChannel(list.first())
                    } else if (cur != null) {
                        // Refresh current channel metadata (updated streamUrl/name/poster after server sync) without interrupting active stream if URL unchanged
                        val updated = list.find { it.id == cur.id }
                        if (updated != null && updated.streamUrl != cur.streamUrl) {
                            selectChannel(updated)
                        } else if (updated != null) {
                            _currentChannel.value = updated
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            m3uChannels.collect { list ->
                if (_activeTab.value == "M3U" && _currentChannel.value == null && list.isNotEmpty()) {
                    selectChannel(list.first())
                }
            }
        }
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
        repository.preferences.lastSelectedTab = tab
        _selectedGroup.value = null
        _m3uVisibleLimit.value = 80

        val tabLabel = when (tab) {
            "YOUTUBE" -> "YouTube লাইভ"
            "M3U" -> "M3U প্লেলিস্ট"
            "FAVORITES" -> "প্রিয় চ্যানেল"
            else -> tab
        }
        appendBackendLog("ট্যাব পরিবর্তন: $tabLabel নির্বাচন করা হয়েছে", BackendLogLevel.INFO)

        val channels = when (tab) {
            "YOUTUBE" -> youtubeChannels.value
            "M3U" -> m3uChannels.value
            "FAVORITES" -> favoriteChannels.value
            else -> emptyList()
        }
        if (channels.isNotEmpty()) {
            selectChannel(channels.first())
        }
    }

    fun setSelectedGroup(group: String?) {
        _selectedGroup.value = group
        _m3uVisibleLimit.value = 80
        appendBackendLog("গ্রুপ ফিল্টার: ${group ?: "সকল চ্যানেল"}", BackendLogLevel.INFO)
    }

    fun selectChannel(channel: Channel) {
        if (_currentChannel.value?.id == channel.id &&
            _currentChannel.value?.streamUrl == channel.streamUrl &&
            isPlaybackStarted
        ) {
            return
        }

        _currentChannel.value = channel
        // Keep splash image (FlashBanner) visible until actual video frames start playing!
        _isFlashBannerVisible.value = true
        isPlaybackStarted = false

        val streamKind = if (channel.type == ChannelType.YOUTUBE) "YouTube Live" else "M3U8 HLS"
        appendBackendLog(
            "চ্যানেল লোড হচ্ছে: ${channel.name} [$streamKind]...",
            BackendLogLevel.WORKING,
            isBusy = true
        )

        // Give stream up to 12 seconds to start playing before switching to next channel.
        // Once playback starts, isPlaybackStarted = true cancels this watchdog so mid-stream buffering never switches.
        loadingWatchdogJob?.cancel()
        loadingWatchdogJob = viewModelScope.launch {
            delay(12000)
            if (!isPlaybackStarted) {
                Log.w("MainViewModel", "Channel ${channel.name} failed to load within 12s. Auto-switching.")
                appendBackendLog(
                    "${channel.name} নির্দিষ্ট সময়ে লোড হয়নি — পরবর্তী চ্যানেলে সুইচ করা হচ্ছে...",
                    BackendLogLevel.WARNING,
                    isBusy = true
                )
                _serverSyncState.value = ServerSyncState.Notice("${channel.name} লোড হতে বিলম্বিত, পরবর্তী চ্যানেলে যাওয়া হচ্ছে...")
                playNextChannel()
            }
        }
    }

    fun onPlaybackStarted() {
        if (isPlaybackStarted && !_isFlashBannerVisible.value) return
        isPlaybackStarted = true
        loadingWatchdogJob?.cancel()
        _isFlashBannerVisible.value = false

        val chName = _currentChannel.value?.name ?: "চ্যানেল"
        appendBackendLog(
            "লাইভ চলছে: $chName (ভিডিও ও সাউন্ড সচল)",
            BackendLogLevel.SUCCESS,
            isBusy = false
        )
    }

    fun onPlaybackError(error: String) {
        Log.e("MainViewModel", "Playback error for current channel: $error")
        val chName = _currentChannel.value?.name ?: "চ্যানেল"
        if (!isPlaybackStarted) {
            appendBackendLog(
                "$chName স্ট্রিম ত্রুটি ($error) — পরবর্তী চ্যানেলে যাওয়া হচ্ছে",
                BackendLogLevel.WARNING,
                isBusy = true
            )
            handlePlaybackFailure()
        } else {
            // Once a channel has started playing, never auto-switch on mid-stream hiccup
            appendBackendLog(
                "$chName সাময়িক বাফারিং হচ্ছে...",
                BackendLogLevel.WARNING,
                isBusy = false
            )
            _serverSyncState.value = ServerSyncState.Notice("ভিডিওটি বাফারিং বা সাময়িক বিলম্বিত হচ্ছে...")
        }
    }

    private fun handlePlaybackFailure() {
        loadingWatchdogJob?.cancel()
        playNextChannel()
    }

    fun playNextChannel() {
        val list = getVisibleChannels()
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentChannel.value?.id }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < list.size) currentIndex + 1 else 0
        selectChannel(list[nextIndex])
    }

    fun playPreviousChannel() {
        val list = getVisibleChannels()
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentChannel.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        selectChannel(list[prevIndex])
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val newFav = !channel.isFavorite
            repository.toggleFavorite(channel.id, newFav)
            if (_currentChannel.value?.id == channel.id) {
                _currentChannel.value = channel.copy(isFavorite = newFav)
            }
            appendBackendLog(
                if (newFav) "ফেভারিট তালিকায় যুক্ত: ${channel.name}" else "ফেভারিট থেকে সরানো হয়েছে: ${channel.name}",
                BackendLogLevel.INFO
            )
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    fun openPinDialog() {
        _pinError.value = null
        _isPinDialogOpen.value = true
    }
    fun closePinDialog() { _isPinDialogOpen.value = false }

    fun openSleepTimerDialog() { _isSleepTimerDialogOpen.value = true }
    fun closeSleepTimerDialog() { _isSleepTimerDialogOpen.value = false }

    fun openExitDialog() { _isExitDialogOpen.value = true }
    fun closeExitDialog() { _isExitDialogOpen.value = false }

    fun verifyPinAndLoadBuiltIn(pin: String) {
        viewModelScope.launch {
            _pinLoading.value = true
            _pinError.value = null
            appendBackendLog("সার্ভারে ওয়ান-টাইম পিন যাচাই করা হচ্ছে...", BackendLogLevel.WORKING, isBusy = true)

            val result = repository.verifyPin(pin)
            _pinLoading.value = false

            if (result.isSuccess && result.getOrNull() == true) {
                _isPinDialogOpen.value = false
                _settingsStatus.value = "পিন সফলভাবে যাচাই করা হয়েছে! বিল্ট-ইন M3U লোড হচ্ছে..."
                appendBackendLog("পিন যাচাই সফল! বিল্ট-ইন M3U ডাউনলোড শুরু...", BackendLogLevel.SUCCESS, isBusy = true)
                loadBuiltInM3u()
            } else {
                val err = result.exceptionOrNull()?.message ?: "ভুল পিন নম্বর!"
                _pinError.value = err
                appendBackendLog("পিন যাচাই ব্যর্থ: $err", BackendLogLevel.WARNING, isBusy = false)
            }
        }
    }

    fun loadBuiltInM3u() {
        if (!repository.preferences.isBuiltInPinVerified) {
            openPinDialog()
            return
        }

        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsStatus.value = "বিল্ট-ইন M3U ডাউনলোড হচ্ছে..."
            appendBackendLog("বিল্ট-ইন M3U প্লেলিস্ট সার্ভার থেকে ডাউনলোড হচ্ছে...", BackendLogLevel.WORKING, isBusy = true)
            val result = repository.loadBuiltInM3u()
            _settingsLoading.value = false

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _settingsStatus.value = "সফলভাবে $count টি চ্যানেল লোড হয়েছে!"
                appendBackendLog("বিল্ট-ইন M3U লোড সম্পন্ন ($count টি চ্যানেল)", BackendLogLevel.SUCCESS, isBusy = false)
                setActiveTab("M3U")
            } else {
                val err = result.exceptionOrNull()?.message ?: "M3U লোড ব্যর্থ"
                _settingsStatus.value = err
                appendBackendLog("M3U ডাউনলোড ব্যর্থ: $err", BackendLogLevel.WARNING, isBusy = false)
            }
        }
    }

    fun loadCustomM3u(url: String, name: String) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsStatus.value = "কাস্টম M3U ডাউনলোড হচ্ছে..."
            appendBackendLog("কাস্টম M3U ($name) ডাউনলোড হচ্ছে...", BackendLogLevel.WORKING, isBusy = true)
            val result = repository.loadCustomM3u(url, name)
            _settingsLoading.value = false

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _settingsStatus.value = "সফলভাবে $count টি চ্যানেল লোড হয়েছে!"
                appendBackendLog("কাস্টম M3U লোড সম্পন্ন ($count টি চ্যানেল)", BackendLogLevel.SUCCESS, isBusy = false)
                setActiveTab("M3U")
            } else {
                val err = result.exceptionOrNull()?.message ?: "M3U লোড ব্যর্থ"
                _settingsStatus.value = err
                appendBackendLog("কাস্টম M3U ত্রুটি: $err", BackendLogLevel.WARNING, isBusy = false)
            }
        }
    }

    fun loadXtream(server: String, user: String, pass: String) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsStatus.value = "Xtream সার্ভার কানেক্ট হচ্ছে..."
            appendBackendLog("Xtream সার্ভার সংযোগ করা হচ্ছে...", BackendLogLevel.WORKING, isBusy = true)
            val result = repository.loadXtream(server, user, pass)
            _settingsLoading.value = false

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _settingsStatus.value = "সফলভাবে $count টি Xtream চ্যানেল লোড হয়েছে!"
                appendBackendLog("Xtream লোড সম্পন্ন ($count টি চ্যানেল)", BackendLogLevel.SUCCESS, isBusy = false)
                setActiveTab("M3U")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Xtream কানেকশন ব্যর্থ"
                _settingsStatus.value = err
                appendBackendLog("Xtream সংযোগ ব্যর্থ: $err", BackendLogLevel.WARNING, isBusy = false)
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
            appendBackendLog("প্লেলিস্ট মুছে ফেলা হয়েছে", BackendLogLevel.INFO)
        }
    }

    fun setBackgroundAudio(enabled: Boolean) {
        repository.preferences.isBackgroundAudioEnabled = enabled
        appendBackendLog(
            if (enabled) "ব্যাকগ্রাউন্ড অডিও পাওয়ার ম্যানেজমেন্ট চালু" else "ব্যাকগ্রাউন্ড অডিও বন্ধ",
            BackendLogLevel.INFO
        )
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalSeconds = minutes * 60
        _sleepTimerSeconds.value = totalSeconds
        appendBackendLog("স্লিপ টাইমার চালু করা হয়েছে: $minutes মিনিট", BackendLogLevel.INFO)

        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerSeconds.value > 0) {
                delay(1000)
                _sleepTimerSeconds.value -= 1
            }
        }
    }

    fun cancelSleepTimer() {
        if (_sleepTimerSeconds.value > 0) {
            appendBackendLog("স্লিপ টাইমার বাতিল করা হয়েছে", BackendLogLevel.INFO)
        }
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSeconds.value = 0
    }

    fun checkServer(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            val initMsg = if (forceUpdate) {
                "সার্ভার ফোর্স আপডেট ও লাইভ স্ট্রিম চেক শুরু..."
            } else {
                "সার্ভার তারিখ (reDirectLog.txt) ও চ্যানেল সিঙ্ক যাচাই হচ্ছে..."
            }
            _serverSyncState.value = ServerSyncState.Checking(initMsg)
            appendBackendLog(initMsg, BackendLogLevel.WORKING, isBusy = true)

            val result = repository.checkServerSync(forceUpdate) { msg ->
                _serverSyncState.value = ServerSyncState.Updating(msg)
                appendBackendLog(msg, BackendLogLevel.WORKING, isBusy = true)
            }
            if (result.isSuccess) {
                val (statusText, syncedCount) = result.getOrNull() ?: ("হালনাগাদ সম্পন্ন" to 0)
                val summary = if (syncedCount > 0) {
                    "$statusText | MySQL সিঙ্ক: $syncedCount টি চ্যানেল (display_order)"
                } else {
                    statusText
                }
                _serverSyncState.value = ServerSyncState.Success(summary)
                appendBackendLog(summary, BackendLogLevel.SUCCESS, isBusy = !isPlaybackStarted)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "অফলাইন/ক্যাশে ডেটা দিয়ে চলছে"
                _serverSyncState.value = ServerSyncState.Notice(errMsg)
                appendBackendLog("সার্ভার নোটিশ: $errMsg", BackendLogLevel.WARNING, isBusy = !isPlaybackStarted)
            }
            delay(4000)
            if (_serverSyncState.value !is ServerSyncState.Updating) {
                _serverSyncState.value = ServerSyncState.Idle
            }
        }
    }

    fun getVisibleChannels(): List<Channel> {
        val baseList = when (_activeTab.value) {
            "YOUTUBE" -> youtubeChannels.value.sortedWith(compareBy({ it.displayOrder }, { it.id }))
            "M3U" -> m3uChannels.value
            "FAVORITES" -> favoriteChannels.value
            else -> emptyList()
        }
        val group = _selectedGroup.value
        val filtered = if (group.isNullOrEmpty() || group == "All") {
            baseList
        } else {
            baseList.filter { it.groupTitle.equals(group, ignoreCase = true) }
        }
        return if (_activeTab.value == "YOUTUBE") {
            filtered.sortedWith(compareBy({ it.displayOrder }, { it.id }))
        } else if (_activeTab.value == "M3U") {
            filtered.take(_m3uVisibleLimit.value)
        } else {
            filtered
        }
    }
}
