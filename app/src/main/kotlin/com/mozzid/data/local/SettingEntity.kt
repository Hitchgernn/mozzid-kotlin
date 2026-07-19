package com.mozzid.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Key/value settings row (language, theme, accent, toggles, onboarding flag). */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    suspend fun all(): List<SettingEntity>

    /** Live view, so a settings change repaints/relabels the app immediately. */
    @Query("SELECT * FROM settings")
    fun watchAll(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: SettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(settings: List<SettingEntity>)
}
