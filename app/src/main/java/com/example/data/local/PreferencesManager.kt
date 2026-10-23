package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nokshi_iptv_prefs", Context.MODE_PRIVATE)

    var isBuiltInPinVerified: Boolean
        get() = prefs.getBoolean(KEY_PIN_VERIFIED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_VERIFIED, value).apply()

    var lastSelectedTab: String
        get() = prefs.getString(KEY_LAST_TAB, "YOUTUBE") ?: "YOUTUBE"
        set(value) = prefs.edit().putString(KEY_LAST_TAB, value).apply()

    var lastYouTubeChannelId: Int
        get() = prefs.getInt(KEY_LAST_YT_CHANNEL, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_YT_CHANNEL, value).apply()

    var lastM3uChannelId: Int
        get() = prefs.getInt(KEY_LAST_M3U_CHANNEL, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_M3U_CHANNEL, value).apply()

    var isBackgroundAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_AUDIO, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_AUDIO, value).apply()

    var lastSyncDate: String
        get() = prefs.getString(KEY_LAST_SYNC_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_DATE, value).apply()

    fun clearPinVerification() {
        prefs.edit().remove(KEY_PIN_VERIFIED).apply()
    }

    companion object {
        private const val KEY_PIN_VERIFIED = "key_pin_verified"
        private const val KEY_LAST_TAB = "key_last_tab"
        private const val KEY_LAST_YT_CHANNEL = "key_last_yt_channel"
        private const val KEY_LAST_M3U_CHANNEL = "key_last_m3u_channel"
        private const val KEY_BACKGROUND_AUDIO = "key_background_audio"
        private const val KEY_LAST_SYNC_DATE = "key_last_sync_date"
    }
}
