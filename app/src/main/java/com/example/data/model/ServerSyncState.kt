package com.example.data.model

sealed class ServerSyncState {
    object Idle : ServerSyncState()
    data class Checking(val message: String = "সার্ভার চেক করা হচ্ছে...") : ServerSyncState()
    data class Updating(
        val message: String = "চ্যানেল আপডেট হচ্ছে...",
        val secondsElapsed: Int = 0
    ) : ServerSyncState()
    data class Success(val message: String) : ServerSyncState()
    data class Notice(val message: String) : ServerSyncState()
}
