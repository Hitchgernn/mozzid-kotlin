package com.mozzid

import android.app.Application
import kotlinx.coroutines.CompletableDeferred

/**
 * Owns the single [Bootstrap] composition root. Assembled asynchronously (the DB
 * open + demo seed are suspend calls); UI awaits [bootstrap].
 */
class MozzApplication : Application() {
    val bootstrap = CompletableDeferred<Bootstrap>()

    override fun onCreate() {
        super.onCreate()
        // Build the graph off the main thread via a coroutine started in MainActivity;
        // here we only hold the deferred so any entry point can await it.
    }
}
