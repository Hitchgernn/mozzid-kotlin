package com.mozzid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {
    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun watchAll(): Flow<List<DetectionEntity>>

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    suspend fun all(): List<DetectionEntity>

    @Insert
    suspend fun insert(entity: DetectionEntity): Long

    @Query("DELETE FROM detections WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM detections")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM detections")
    suspend fun count(): Int
}
