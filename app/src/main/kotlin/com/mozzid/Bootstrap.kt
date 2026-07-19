package com.mozzid

import android.content.Context
import com.mozzid.data.classifier.MockSpeciesClassifier
import com.mozzid.data.local.DemoSeeder
import com.mozzid.data.local.MozzDatabase
import com.mozzid.data.local.RoomDetectionRepository
import com.mozzid.data.audio.MicAudioRecorder
import com.mozzid.data.location.FusedLocationService
import com.mozzid.data.permission.PermissionBridge
import com.mozzid.data.species.SpeciesCatalog
import com.mozzid.data.sync.NoopSyncService
import com.mozzid.domain.classifier.SpeciesClassifier
import com.mozzid.domain.repository.AudioRecorderService
import com.mozzid.domain.repository.DetectionRepository
import com.mozzid.domain.repository.LocationService
import com.mozzid.domain.repository.SpeciesRepository
import com.mozzid.domain.sync.SyncService

/**
 * All wired-up singletons, assembled once in [MozzApplication] and read via the
 * app container. Swapping an implementation (Mock → TFLite classifier, Noop →
 * Firebase sync) is a one-line change here — nothing else moves. This is the single composition root (was `Bootstrap.create`
 * + the single Riverpod override in the Flutter build).
 */
class Bootstrap private constructor(
    val database: MozzDatabase,
    val detectionRepository: DetectionRepository,
    val speciesRepository: SpeciesRepository,
    val classifier: SpeciesClassifier,
    val audioRecorder: AudioRecorderService,
    val location: LocationService,
    val sync: SyncService,
    /** Bound to the foreground Activity so services can prompt for permissions. */
    val permissions: PermissionBridge,
) {
    companion object {
        suspend fun create(context: Context): Bootstrap {
            val app = context.applicationContext
            val db = MozzDatabase.open(app)
            DemoSeeder.seedIfEmpty(db.detectionDao())

            val species = SpeciesCatalog()
            val classifier = MockSpeciesClassifier(species)
            classifier.load()

            val permissions = PermissionBridge()

            return Bootstrap(
                database = db,
                detectionRepository = RoomDetectionRepository(db.detectionDao()),
                speciesRepository = species,
                classifier = classifier,
                audioRecorder = MicAudioRecorder(app, permissions),
                location = FusedLocationService(app, permissions),
                sync = NoopSyncService,
                permissions = permissions,
            )
        }
    }
}
