package com.example.data.remote

import android.util.Log
import com.example.data.model.Channel
import com.example.data.model.ChannelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class XtreamService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchXtreamStreams(
        serverUrl: String,
        user: String,
        pass: String
    ): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val cleanServer = serverUrl.trim().removeSuffix("/")
            // 1. Fetch live categories map
            val categoriesUrl = "$cleanServer/player_api.php?username=$user&password=$pass&action=get_live_categories"
            val categoryMap = mutableMapOf<String, String>()

            try {
                val catReq = Request.Builder().url(categoriesUrl).build()
                val catResp = client.newCall(catReq).execute()
                if (catResp.isSuccessful) {
                    val catJson = JSONArray(catResp.body?.string() ?: "[]")
                    for (i in 0 until catJson.length()) {
                        val obj = catJson.getJSONObject(i)
                        val id = obj.optString("category_id")
                        val name = obj.optString("category_name")
                        categoryMap[id] = name
                    }
                }
            } catch (e: Exception) {
                Log.w("XtreamService", "Could not fetch categories: ${e.message}")
            }

            // 2. Fetch live streams
            val streamsUrl = "$cleanServer/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val req = Request.Builder().url(streamsUrl).build()
            val resp = client.newCall(req).execute()

            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Xtream সার্ভার রেসপন্স ত্রুটি (HTTP ${resp.code})"))
            }

            val body = resp.body?.string() ?: "[]"
            val jsonArray = JSONArray(body)
            val channels = mutableListOf<Channel>()
            var idCounter = 50000

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val streamId = obj.optInt("stream_id")
                val name = obj.optString("name", "Channel")
                val streamIcon = obj.optString("stream_icon").takeIf { it.isNotEmpty() }
                val catId = obj.optString("category_id")
                val group = categoryMap[catId] ?: "Xtream Live"

                val streamUrl = "$cleanServer/live/$user/$pass/$streamId.m3u8"

                channels.add(
                    Channel(
                        id = idCounter++,
                        type = ChannelType.M3U,
                        name = name,
                        streamUrl = streamUrl,
                        channelUrl = null,
                        poster = streamIcon,
                        groupTitle = group,
                        category = 1,
                        isActive = true,
                        displayOrder = i,
                        isFavorite = false
                    )
                )
            }

            Log.d("XtreamService", "Loaded ${channels.size} channels from Xtream server")
            Result.success(channels)
        } catch (e: Exception) {
            Log.e("XtreamService", "Error loading Xtream streams: ${e.message}", e)
            Result.failure(e)
        }
    }
}
