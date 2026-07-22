package com.mozzid.data.sync

import android.content.Context
import com.mozzid.domain.sync.SyncService

/**
 * Factory for creating [SyncService].
 * Detects if Firebase is available on device; returns [FirebaseSyncService] if configured,
 * otherwise falls back to [NoopSyncService] for offline-only operation.
 */
object SyncServiceFactory {
    fun create(context: Context): SyncService {
        return try {
            val service = FirebaseSyncService(context)
            if (service.isEnabled) service else NoopSyncService
        } catch (_: Throwable) {
            NoopSyncService
        }
    }
}
