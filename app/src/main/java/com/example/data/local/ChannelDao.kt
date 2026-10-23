package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE type = :type AND isActive = 1 ORDER BY displayOrder ASC")
    fun getChannelsByType(type: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 AND isActive = 1 ORDER BY displayOrder ASC")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Int): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels WHERE type = :type")
    suspend fun getChannelCountByType(type: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: Int, isFavorite: Boolean)

    @Query("DELETE FROM channels WHERE type = :type")
    suspend fun deleteChannelsByType(type: String)

    @Query("DELETE FROM channels")
    suspend fun clearAll()
}
