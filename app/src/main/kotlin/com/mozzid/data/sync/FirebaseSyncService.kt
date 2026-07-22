package com.mozzid.data.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.mozzid.domain.model.Detection
import com.mozzid.domain.sync.SyncService
import kotlinx.coroutines.tasks.await

/**
 * SyncService implementation backed by Firebase Cloud Firestore.
 * Pushes detection records to Firestore when configured with google-services.json;
 * falls back cleanly to offline no-op behavior if Firebase is uninitialized.
 */
class FirebaseSyncService(context: Context) : SyncService {

    private val db: FirebaseFirestore? by lazy {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            if (app != null) FirebaseFirestore.getInstance(app) else null
        } catch (_: Exception) {
            null
        }
    }

    override val isEnabled: Boolean
        get() = db != null

    override suspend fun pushDetection(detection: Detection) {
        val firestore = db ?: return
        val record = mapOf(
            "id" to detection.id,
            "speciesId" to detection.speciesId,
            "confidence" to detection.confidence,
            "wingbeatHz" to detection.wingbeatHz,
            "timestampMillis" to detection.timestampMillis,
            "latitude" to detection.latitude,
            "longitude" to detection.longitude,
            "locationLabel" to detection.locationLabel,
        )

        runCatching {
            firestore.collection("detections")
                .add(record)
                .await()
        }
    }

    override suspend fun pullAggregates() {
        val firestore = db ?: return
        runCatching {
            firestore.collection("detections")
                .limit(50)
                .get()
                .await()
        }
    }
}
