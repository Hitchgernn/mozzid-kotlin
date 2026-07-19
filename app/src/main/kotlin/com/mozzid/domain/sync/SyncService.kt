package com.mozzid.domain.sync

import com.mozzid.domain.model.Detection

/**
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │  BACKEND SEAM — SyncService (optional, Firebase)                        │
 * │                                                                          │
 * │  The app is fully functional offline; sync is additive. This interface   │
 * │  is the only place the app talks to a backend. The default binding is     │
 * │  NoopSyncService (does nothing), so nothing depends on Firebase.          │
 * │                                                                          │
 * │  To enable cross-device sync + aggregate maps + model updates:            │
 * │   1. Add firebase-bom + firestore + auth, drop in google-services.json.   │
 * │   2. Implement FirebaseSyncService against this interface.                │
 * │   3. Swap the binding in Bootstrap. No UI/domain changes.                 │
 * └────────────────────────────────────────────────────────────────────────┘
 */
interface SyncService {
    val isEnabled: Boolean

    /** Push a newly-saved detection upstream (best-effort). Never blocks the UI. */
    suspend fun pushDetection(detection: Detection)

    /** Pull remote aggregates for the map. No-op when disabled. */
    suspend fun pullAggregates()
}
