package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Channel
import com.example.data.model.ChannelType

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: Int,
    val type: String, // YOUTUBE or M3U
    val name: String,
    val streamUrl: String,
    val channelUrl: String?,
    val poster: String?,
    val groupTitle: String,
    val category: Int,
    val isActive: Boolean,
    val displayOrder: Int,
    val isFavorite: Boolean = false
) {
    fun toDomain(): Channel = Channel(
        id = id,
        type = if (type == "YOUTUBE") ChannelType.YOUTUBE else ChannelType.M3U,
        name = name,
        streamUrl = streamUrl,
        channelUrl = channelUrl,
        poster = poster,
        groupTitle = groupTitle,
        category = category,
        isActive = isActive,
        displayOrder = displayOrder,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(channel: Channel): ChannelEntity = ChannelEntity(
            id = channel.id,
            type = channel.type.name,
            name = channel.name,
            streamUrl = channel.streamUrl,
            channelUrl = channel.channelUrl,
            poster = channel.poster,
            groupTitle = channel.groupTitle,
            category = channel.category,
            isActive = channel.isActive,
            displayOrder = channel.displayOrder,
            isFavorite = channel.isFavorite
        )
    }
}
