package com.mozzid.data.permission

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Bridges a suspending `requestPermission()` to the Activity-scoped launcher that
 * actually shows the system dialog.
 *
 * Runtime permissions can only be requested from a live Activity, but the
 * services that need them are built in [com.mozzid.Bootstrap] long before one
 * exists — and `domain/` must stay free of Android types. So the Activity [bind]s
 * a launcher on start and [unbind]s on stop; data-layer services just call
 * [request] and suspend. With nothing bound (no Activity in the foreground) a
 * request resolves `false` rather than hanging.
 */
class PermissionBridge {

    /** Launches the system dialog and reports whether every permission was granted. */
    fun interface Launcher {
        fun launch(permissions: Array<String>, onResult: (Boolean) -> Unit)
    }

    private val mutex = Mutex()

    @Volatile
    private var launcher: Launcher? = null

    fun bind(launcher: Launcher) {
        this.launcher = launcher
    }

    fun unbind() {
        launcher = null
    }

    /**
     * Shows the system dialog and suspends until the user answers. Serialised —
     * Android ignores a second request while one is in flight, so overlapping
     * callers queue instead of silently dropping.
     */
    suspend fun request(permissions: Array<String>): Boolean = mutex.withLock {
        val active = launcher ?: return false
        suspendCancellableCoroutine { cont ->
            active.launch(permissions) { granted ->
                if (cont.isActive) cont.resume(granted)
            }
        }
    }
}
