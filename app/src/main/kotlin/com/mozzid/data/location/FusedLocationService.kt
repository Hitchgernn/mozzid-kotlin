package com.mozzid.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mozzid.data.permission.PermissionBridge
import com.mozzid.domain.repository.GeoFix
import com.mozzid.domain.repository.LocationService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * GPS via Play Services' fused provider.
 *
 * Best-effort and non-throwing by contract: on denial, a disabled provider, a
 * timeout, or any Play Services error it returns null so the detection still
 * saves with null coordinates. A location is a nice-to-have on a detection —
 * never a reason to lose one.
 */
class FusedLocationService(
    context: Context,
    private val permissions: PermissionBridge,
) : LocationService {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    override suspend fun hasPermission(): Boolean = isGranted()

    /** Asks once if not already granted. Coarse is enough for the history map. */
    override suspend fun requestPermission(): Boolean =
        isGranted() || permissions.request(LOCATION_PERMISSIONS)

    private fun isGranted(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // guarded by the hasPermission() check below
    override suspend fun currentFix(): GeoFix? {
        if (!isGranted()) return null

        val request = CurrentLocationRequest.Builder()
            // Medium accuracy: a city-block-level fix is all the map needs, and it
            // resolves far faster than a high-accuracy GPS lock.
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setDurationMillis(FIX_TIMEOUT_MILLIS)
            .setMaxUpdateAgeMillis(MAX_FIX_AGE_MILLIS)
            .build()

        return runCatching {
            suspendCancellableCoroutine { cont ->
                client.getCurrentLocation(request, null)
                    .addOnSuccessListener { location ->
                        if (!cont.isActive) return@addOnSuccessListener
                        cont.resume(
                            location?.let { GeoFix(it.latitude, it.longitude) },
                        )
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
            }
        }.getOrNull()
    }

    private companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        const val FIX_TIMEOUT_MILLIS = 8_000L
        const val MAX_FIX_AGE_MILLIS = 10 * 60 * 1_000L  // 10 min — accepts emulator cached fix
    }
}
