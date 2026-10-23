package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.Channel
import com.example.data.model.ChannelType
import com.example.data.model.Playlist
import com.example.data.model.PlaylistType
import com.example.data.remote.M3uParser
import com.example.data.remote.ServerCheckService
import com.example.data.remote.XtreamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

class ChannelRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val channelDao = db.channelDao()
    private val playlistDao = db.playlistDao()
    val preferences = PreferencesManager(context)

    private val serverCheckService = ServerCheckService()
    private val m3uParser = M3uParser()
    private val xtreamService = XtreamService()

    fun getYouTubeChannels(): Flow<List<Channel>> =
        channelDao.getChannelsByType(ChannelType.YOUTUBE.name).map { list ->
            list.map { it.toDomain() }
        }

    fun getM3uChannels(): Flow<List<Channel>> =
        channelDao.getChannelsByType(ChannelType.M3U.name).map { list ->
            list.map { it.toDomain() }
        }

    fun getFavoriteChannels(): Flow<List<Channel>> =
        channelDao.getFavoriteChannels().map { list ->
            list.map { it.toDomain() }
        }

    fun getPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun initializeChannelsIfEmpty() = withContext(Dispatchers.IO) {
        val ytCount = channelDao.getChannelCountByType(ChannelType.YOUTUBE.name)
        if (ytCount == 0) {
            Log.d("ChannelRepo", "Loading bundled default YouTube channels...")
            try {
                context.assets.open("default_channels.json").use { inputStream ->
                    val reader = InputStreamReader(inputStream, "UTF-8")
                    val jsonStr = reader.readText()
                    val jsonArray = JSONArray(jsonStr)
                    val entities = mutableListOf<ChannelEntity>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        entities.add(
                            ChannelEntity(
                                id = obj.getInt("id"),
                                type = ChannelType.YOUTUBE.name,
                                name = obj.getString("name"),
                                streamUrl = obj.getString("youtubeUrl"),
                                channelUrl = obj.optString("channelUrl"),
                                poster = obj.optString("poster").takeIf { it.isNotEmpty() && it != "null" },
                                groupTitle = "YouTube Live",
                                category = obj.getInt("category"),
                                isActive = obj.getBoolean("isActive"),
                                displayOrder = obj.getInt("displayOrder"),
                                isFavorite = false
                            )
                        )
                    }
                    channelDao.insertChannels(entities)
                    Log.d("ChannelRepo", "Inserted ${entities.size} default YouTube channels")
                }
            } catch (e: Exception) {
                Log.e("ChannelRepo", "Error loading bundled channels: ${e.message}", e)
            }
        }
    }

    suspend fun toggleFavorite(channelId: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        channelDao.setFavorite(channelId, isFavorite)
    }

    suspend fun verifyPin(pin: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = serverCheckService.verifyPinOnServer(pin)
        if (result.isSuccess && result.getOrNull() == true) {
            preferences.isBuiltInPinVerified = true
        }
        result
    }

    suspend fun loadBuiltInM3u(): Result<Int> = withContext(Dispatchers.IO) {
        if (!preferences.isBuiltInPinVerified) {
            return@withContext Result.failure(Exception("বিল্ট-ইন M3U লোড করার জন্য প্রথমে ওয়ান-টাইম পিন ভেরিফিকেশন প্রয়োজন"))
        }

        val result = m3uParser.fetchAndParseM3u(ServerCheckService.BUILT_IN_M3U_URL)
        if (result.isSuccess) {
            val channels = result.getOrNull() ?: emptyList()
            if (channels.isNotEmpty()) {
                channelDao.deleteChannelsByType(ChannelType.M3U.name)
                channelDao.insertChannels(channels.map { ChannelEntity.fromDomain(it) })
            }
            Result.success(channels.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("M3U লোড ব্যর্থ"))
        }
    }

    suspend fun loadCustomM3u(url: String, name: String = "Custom M3U"): Result<Int> = withContext(Dispatchers.IO) {
        val result = m3uParser.fetchAndParseM3u(url)
        if (result.isSuccess) {
            val channels = result.getOrNull() ?: emptyList()
            if (channels.isNotEmpty()) {
                channelDao.deleteChannelsByType(ChannelType.M3U.name)
                channelDao.insertChannels(channels.map { ChannelEntity.fromDomain(it) })
            }
            // Save playlist
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    type = PlaylistType.USER_M3U.name,
                    url = url,
                    serverUrl = null,
                    username = null,
                    password = null,
                    isSelected = true
                )
            )
            Result.success(channels.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("M3U লোড ব্যর্থ"))
        }
    }

    suspend fun loadXtream(
        serverUrl: String,
        username: String,
        password: String,
        name: String = "Xtream Live"
    ): Result<Int> = withContext(Dispatchers.IO) {
        val result = xtreamService.fetchXtreamStreams(serverUrl, username, password)
        if (result.isSuccess) {
            val channels = result.getOrNull() ?: emptyList()
            if (channels.isNotEmpty()) {
                channelDao.deleteChannelsByType(ChannelType.M3U.name)
                channelDao.insertChannels(channels.map { ChannelEntity.fromDomain(it) })
            }
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    type = PlaylistType.XTREAM.name,
                    url = "",
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    isSelected = true
                )
            )
            Result.success(channels.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Xtream লোড ব্যর্থ"))
        }
    }

    suspend fun checkServerSync(onUpdating: (String) -> Unit): Result<String> =
        serverCheckService.performServerCheck(onUpdating)

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(id)
    }
}
