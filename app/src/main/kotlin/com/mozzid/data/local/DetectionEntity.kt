package com.mozzid.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mozzid.domain.model.Detection

/** Room row for a detection. Mirrors the original sqflite `detections` table. */
@Entity(tableName = "detections")
data class DetectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "species_id") val speciesId: String,
    val confidence: Int,
    @ColumnInfo(name = "wingbeat_hz") val wingbeatHz: Int,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "location_label") val locationLabel: String?,
) {
    fun toModel(): Detection = Detection(
        id = id,
        speciesId = speciesId,
        confidence = confidence,
        wingbeatHz = wingbeatHz,
        timestampMillis = timestamp,
        latitude = latitude,
        longitude = longitude,
        locationLabel = locationLabel,
    )

    companion object {
        fun fromModel(d: Detection): DetectionEntity = DetectionEntity(
            id = d.id ?: 0,
            speciesId = d.speciesId,
            confidence = d.confidence,
            wingbeatHz = d.wingbeatHz,
            timestamp = d.timestampMillis,
            latitude = d.latitude,
            longitude = d.longitude,
            locationLabel = d.locationLabel,
        )
    }
}
