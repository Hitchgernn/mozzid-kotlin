package com.mozzid.data.local

import com.mozzid.domain.model.Detection
import com.mozzid.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [DetectionRepository]. Room's reactive [Flow] query means History
 * updates immediately on save — no manual cache/broadcast needed (that was the
 * Dart in-memory-cache + StreamController pattern; Room gives it for free).
 */
class RoomDetectionRepository(private val dao: DetectionDao) : DetectionRepository {

    override suspend fun all(): List<Detection> = dao.all().map { it.toModel() }

    override suspend fun add(detection: Detection): Detection {
        val id = dao.insert(DetectionEntity.fromModel(detection))
        return detection.copy(id = id)
    }

    override suspend fun remove(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.deleteAll()

    override fun watch(): Flow<List<Detection>> =
        dao.watchAll().map { rows -> rows.map { it.toModel() } }
}
