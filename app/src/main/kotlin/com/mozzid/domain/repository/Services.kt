package com.mozzid.domain.repository

/** A captured GPS fix, or null fields when unavailable. */
data class GeoFix(val latitude: Double, val longitude: Double)

/** Mic capture boundary. Implemented over AudioRecord/MediaRecorder in data/. */
interface AudioRecorderService {
    suspend fun hasPermission(): Boolean
    suspend fun requestPermission(): Boolean
    suspend fun start()
    /** Stops and returns the recorded file path + duration in millis. */
    suspend fun stop(): Pair<String, Long>
}

/** Best-effort location boundary. Returns null when denied/unavailable. */
interface LocationService {
    suspend fun currentFix(): GeoFix?
}
