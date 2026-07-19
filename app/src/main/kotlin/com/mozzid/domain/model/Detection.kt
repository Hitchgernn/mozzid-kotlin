package com.mozzid.domain.model

/**
 * A persisted detection: what was heard, how sure, when and where. Offline-first
 * record — every field is captured on-device and survives without a network.
 */
data class Detection(
    val id: Long? = null,
    val speciesId: String,
    val confidence: Int, // 0..100
    val wingbeatHz: Int,
    val timestampMillis: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String? = null,
)
