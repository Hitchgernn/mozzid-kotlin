package com.mozzid.domain.repository

import com.mozzid.domain.model.Detection
import com.mozzid.domain.model.Species
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for detections. Implemented over SQLite (Room); the domain
 * and presentation layers never see the database.
 */
interface DetectionRepository {
    suspend fun all(): List<Detection>
    suspend fun add(detection: Detection): Detection
    suspend fun remove(id: Long)
    suspend fun clear()

    /** Live stream so History updates when a detection is saved. */
    fun watch(): Flow<List<Detection>>
}

/**
 * Read-only catalogue of known species. Backed by an on-device static table
 * today; could later hydrate from a downloaded model bundle.
 */
interface SpeciesRepository {
    fun all(): List<Species>
    fun byId(id: String): Species?

    /** The species ids the classifier can currently emit, in catalogue order. */
    val classifiableIds: List<String>
}
