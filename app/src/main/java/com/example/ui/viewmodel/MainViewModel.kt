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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChannelRepository(application)

    val youtubeChannels: StateFlow<List<Channel>> = repository.getYouTubeChannels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val m3uChannels: StateFlow<List<Channel>> = repository.getM3uChannels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = repository.getFavoriteChannels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeTab = MutableStateFlow("YOUTUBE") // YOUTUBE, M3U, FAVORITES
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _isFlashBannerVisible = MutableStateFlow(false)
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

    private var bannerTimeoutJob: Job? = null
    private var consecutiveFailures = 0

    init {
        _activeTab.value = repository.preferences.lastSelectedTab

        viewModelScope.launch {
            // Initialize bundled channels
            repository.initializeChannelsIfEmpty()

            // Run async server check without blocking UI
            checkServer()
        }

        // Auto select first channel when channels load if none selected
        viewModelScope.launch {
            youtubeChannels.collect { list ->
                if (_activeTab.value == "YOUTUBE" && _currentChannel.value == null && list.isNotEmpty()) {
                    selectChannel(list.first())
                }
            }
        }
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
        repository.preferences.lastSelectedTab = tab
        _selectedGroup.value = null

        // Select first channel in new tab if available
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
    }

    private var isPlaybackStarted = false
    private var loadingWatchdogJob: Job? = null

    fun selectChannel(channel: Channel) {
        if (_currentChannel.value?.id == channel.id && !_isFlashBannerVisible.value) {
            return
        }

        _currentChannel.value = channel
        _isFlashBannerVisible.value = true
        isPlaybackStarted = false

        bannerTimeoutJob?.cancel()
        bannerTimeoutJob = viewModelScope.launch {
            // Flash banner displays for 1.5 seconds during transition
            delay(1500)
            _isFlashBannerVisible.value = false
        }

        // User requirement:
        // "যেকো ভিডিও লোড হওয়ার জন্য কমপক্ষে ৫ সেকেন্ড সময় পাবে। এর মধ্যে লোড হতে ব্যর্থ হলে পরবর্তি চ্যানেলে সুইচ করবে। কোন চ্যানেল একবার চালু হবার পর মাঝ পথে আটকে গেলে আর সুইচ করবে না।"
        loadingWatchdogJob?.cancel()
        loadingWatchdogJob = viewModelScope.launch {
            delay(6000) // At least 5 seconds window
            if (!isPlaybackStarted) {
                Log.w("MainViewModel", "Channel ${channel.name} failed to load within 6s. Auto-switching to next channel.")
                _serverSyncState.value = ServerSyncState.Notice("${channel.name} লোড হতে ব্যর্থ, পরবর্তী চ্যানেলে যাওয়া হচ্ছে...")
                playNextChannel()
            }
        }
    }

    fun onPlaybackStarted() {
        isPlaybackStarted = true
        loadingWatchdogJob?.cancel()
        bannerTimeoutJob?.cancel()
        _isFlashBannerVisible.value = false
        consecutiveFailures = 0
    }

    fun onPlaybackError(error: String) {
        Log.e("MainViewModel", "Playback error for current channel: $error")
        // User requirement: "কোন চ্যানেল একবার চালু হবার পর মাঝ পথে আটকে গেলে আর সুইচ করবে না।"
        if (!isPlaybackStarted) {
            handlePlaybackFailure()
        } else {
            // Channel had started playing before, so do NOT switch channel
            _serverSyncState.value = ServerSyncState.Notice("ভিডিওটি বাফারিং বা সাময়িক বিলম্বিত হচ্ছে...")
        }
    }

    private fun handlePlaybackFailure() {
        _isFlashBannerVisible.value = false
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

            val result = repository.verifyPin(pin)
            _pinLoading.value = false

            if (result.isSuccess && result.getOrNull() == true) {
                _isPinDialogOpen.value = false
                _settingsStatus.value = "পিন সফলভাবে যাচাই করা হয়েছে! বিল্ট-ইন M3U লোড হচ্ছে..."
                loadBuiltInM3u()
            } else {
                _pinError.value = result.exceptionOrNull()?.message ?: "ভুল পিন নম্বর!"
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
            val result = repository.loadBuiltInM3u()
            _settingsLoading.value = false

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _settingsStatus.value = "সফলভাবে $count টি চ্যানেল লোড হয়েছে!"
                setActiveTab("M3U")
            } else {
                _settingsStatus.value = result.exceptionOrNull()?.message ?: "M3U লোড ব্যর্থ"
            }
        }
    }

    fun loadCustomM3u(url: String, name: String) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsStatus.value = "কাস্টম M3U ডাউনলোড হচ্ছে..."
            val result = repository.loadCustomM3u(url, name)
            _settingsLoading.value = false

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _settingsStatus.value = "সফলভাবে $count টি চ্যানেল লোড হয়েছে!"
                setActiveTab("M3U")
            } else {
                _settingsStatus.value = result.exceptionOrNull()?.message ?: "M3U লোড ব্যর্থ"
            }
        }
    }

    fun loadXtream(server: String, user: String, pass: String) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsStatus.value = "Xtream সার্ভার কানেক্ট হচ্ছে..."
            val result = repository.loadXtream(server, user, pass)
            _settingsLoading.value = false

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _settingsStatus.value = "সফলভাবে $count টি Xtream চ্যানেল লোড হয়েছে!"
                setActiveTab("M3U")
            } else {
                _settingsStatus.value = result.exceptionOrNull()?.message ?: "Xtream কানেকশন ব্যর্থ"
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun setBackgroundAudio(enabled: Boolean) {
        repository.preferences.isBackgroundAudioEnabled = enabled
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalSeconds = minutes * 60
        _sleepTimerSeconds.value = totalSeconds

        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerSeconds.value > 0) {
                delay(1000)
                _sleepTimerSeconds.value -= 1
            }
            // Expired -> Will be handled in Activity via StateFlow collection
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSeconds.value = 0
    }

    fun checkServer(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            _serverSyncState.value = ServerSyncState.Checking("সার্ভার চেক: তারিখ ও লাইভ স্ট্রিম স্থিতি যাচাই করা হচ্ছে...")
            val result = repository.checkServerSync(forceUpdate) { msg ->
                _serverSyncState.value = ServerSyncState.Updating(msg)
            }
            if (result.isSuccess) {
                _serverSyncState.value = ServerSyncState.Success(result.getOrNull() ?: "হালনাগাদ সম্পন্ন")
            } else {
                _serverSyncState.value = ServerSyncState.Notice(result.exceptionOrNull()?.message ?: "অফলাইন/ক্যাশে ডেটা দিয়ে চলছে")
            }
            delay(4000)
            if (_serverSyncState.value !is ServerSyncState.Updating) {
                _serverSyncState.value = ServerSyncState.Idle
            }
        }
    }

    fun getVisibleChannels(): List<Channel> {
        val baseList = when (_activeTab.value) {
            "YOUTUBE" -> youtubeChannels.value
            "M3U" -> m3uChannels.value
            "FAVORITES" -> favoriteChannels.value
            else -> emptyList()
        }
        val group = _selectedGroup.value
        return if (group.isNullOrEmpty() || group == "All") {
            baseList
        } else {
            baseList.filter { it.groupTitle.equals(group, ignoreCase = true) }
        }
    }
}
