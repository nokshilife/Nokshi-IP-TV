package com.example.data.model

enum class ChannelType {
    YOUTUBE,
    M3U
}

data class Channel(
    val id: Int,
    val type: ChannelType,
    val name: String,
    val streamUrl: String,
    val channelUrl: String? = null,
    val poster: String? = null,
    val groupTitle: String = "General",
    val category: Int = 0,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val isFavorite: Boolean = false
) {
    /**
     * Extracts YouTube 11-char video ID from various YouTube URL formats
     */
    fun getYouTubeVideoId(): String? {
        if (type != ChannelType.YOUTUBE) return null
        val url = streamUrl.trim()
        val regex = Regex("""(?:v=|\/embed\/|\/watch\?v=|\/live\/|\.be\/)([a-zA-Z0-9_-]{11})""")
        val match = regex.find(url)
        return match?.groupValues?.get(1)
    }
}
