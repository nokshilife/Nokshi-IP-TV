package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Playlist
import com.example.data.model.PlaylistType

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val url: String,
    val serverUrl: String?,
    val username: String?,
    val password: String?,
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Playlist = Playlist(
        id = id,
        name = name,
        type = try { PlaylistType.valueOf(type) } catch (e: Exception) { PlaylistType.USER_M3U },
        url = url,
        serverUrl = serverUrl,
        username = username,
        password = password,
        isSelected = isSelected
    )

    companion object {
        fun fromDomain(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            type = playlist.type.name,
            url = playlist.url,
            serverUrl = playlist.serverUrl,
            username = playlist.username,
            password = playlist.password,
            isSelected = playlist.isSelected
        )
    }
}
