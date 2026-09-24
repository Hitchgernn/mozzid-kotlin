package com.mozzid.data.local

import java.util.Calendar

/**
 * Seeds a handful of demo detections on first install so the map/stats have
 * something to show. Set [enabled] false to launch with an empty History.
 * Mirrors the original app's seed rows (Jakarta coords).
 */
object DemoSeeder {
    const val enabled = false

    suspend fun seedIfEmpty(dao: DetectionDao) {
        if (!enabled) return
        if (dao.count() > 0) return

        fun at(daysAgo: Int, hour: Int, minute: Int): Long {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        val rows = listOf(
            DetectionEntity(0, "aedes_aegypti", 87, 612, at(0, 23, 42), -6.2005, 106.8166, "Bedroom"),
            DetectionEntity(0, "culex_quinquefasciatus", 79, 372, at(0, 22, 5), -6.2011, 106.8172, "Balcony"),
            DetectionEntity(0, "aedes_albopictus", 91, 640, at(1, 6, 20), -6.1998, 106.8159, "Kitchen"),
            DetectionEntity(0, "anopheles_gambiae", 74, 510, at(2, 2, 15), -6.2021, 106.8181, "Bedroom"),
            DetectionEntity(0, "aedes_aegypti", 83, 588, at(4, 19, 50), -6.1989, 106.8150, "Garden"),
            DetectionEntity(0, "culex_pipiens", 68, 341, at(6, 21, 30), -6.2030, 106.8190, "Living rm"),
        )
        rows.forEach { dao.insert(it) }
    }
}
