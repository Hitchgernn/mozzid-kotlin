package com.mozzid.data.location

import com.mozzid.domain.repository.AudioRecorderService
import com.mozzid.domain.repository.GeoFix
import com.mozzid.domain.repository.LocationService

/**
 * Placeholder recorder for the skeleton. The mock classifier ignores audio
 * content, so this lets the full record→analyze→result→save flow run end-to-end
 * before the real AudioRecord capture lands. Swap in Bootstrap.
 */
class StubAudioRecorder : AudioRecorderService {
    override suspend fun hasPermission(): Boolean = true
    override suspend fun requestPermission(): Boolean = true
    override suspend fun start() {}
    override suspend fun stop(): Pair<String, Long> = "" to 4000L
}

/** Placeholder location — returns no fix until FusedLocation is wired. */
class StubLocationService : LocationService {
    override suspend fun currentFix(): GeoFix? = null
}
