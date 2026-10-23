package com.example.data.model

enum class PlaylistType {
    BUILT_IN_M3U,
    USER_M3U,
    XTREAM
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val type: PlaylistType,
    val url: String = "",
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null,
    val isSelected: Boolean = false
)
