package com.mozzid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DetectionEntity::class, SettingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MozzDatabase : RoomDatabase() {
    abstract fun detectionDao(): DetectionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        fun open(context: Context): MozzDatabase =
            Room.databaseBuilder(context, MozzDatabase::class.java, "mozzid.db")
                .build()
    }
}
