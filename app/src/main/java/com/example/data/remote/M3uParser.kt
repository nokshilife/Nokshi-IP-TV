package com.example.data.remote

import android.util.Log
import com.example.data.model.Channel
import com.example.data.model.ChannelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.util.concurrent.TimeUnit

class M3uParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAndParseM3u(url: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            Log.d("M3uParser", "Fetching M3U from: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NokshiIPTV/1.0 (Android; Leanback)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("M3U ডাউনলোড ব্যর্থ (HTTP ${response.code})"))
            }

            val body = response.body?.string() ?: ""
            val channels = parseM3uContent(body)
            Log.d("M3uParser", "Parsed ${channels.size} channels from M3U")
            Result.success(channels)
        } catch (e: Exception) {
            Log.e("M3uParser", "Error fetching/parsing M3U: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun parseM3uContent(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(content))
        var currentLine: String? = reader.readLine()

        var currentName = ""
        var currentLogo: String? = null
        var currentGroup = "General"
        var idCounter = 10000 // Offset for M3U channels to avoid clash with YouTube IDs

        while (currentLine != null) {
            val line = currentLine.trim()
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                currentGroup = extractAttribute(line, "group-title") ?: "General"
                currentLogo = extractAttribute(line, "tvg-logo")
                val tvgName = extractAttribute(line, "tvg-name")

                // Extract name after the last comma
                val commaIndex = line.lastIndexOf(',')
                currentName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    tvgName ?: "Live Stream"
                }

                if (currentName.isEmpty()) {
                    currentName = tvgName ?: "Channel ${channels.size + 1}"
                }
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                // This is the stream URL
                if (currentName.isNotEmpty()) {
                    channels.add(
                        Channel(
                            id = idCounter++,
                            type = ChannelType.M3U,
                            name = currentName,
                            streamUrl = line,
                            channelUrl = null,
                            poster = currentLogo,
                            groupTitle = currentGroup.ifEmpty { "General" },
                            category = 1,
                            isActive = true,
                            displayOrder = channels.size,
                            isFavorite = false
                        )
                    )
                    // Reset
                    currentName = ""
                    currentLogo = null
                    currentGroup = "General"
                }
            }
            currentLine = reader.readLine()
        }
        return channels
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val regex = Regex("""$attribute="([^"]*)"""", RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groupValues?.get(1)?.trim()
    }
}
