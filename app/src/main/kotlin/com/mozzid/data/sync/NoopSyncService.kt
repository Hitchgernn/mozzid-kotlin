package com.mozzid.data.sync

import com.mozzid.domain.model.Detection
import com.mozzid.domain.sync.SyncService

/** Default no-op binding — keeps the app 100% offline unless Firebase is wired. */
object NoopSyncService : SyncService {
    override val isEnabled: Boolean = false
    override suspend fun pushDetection(detection: Detection) {}
    override suspend fun pullAggregates() {}
}
