package com.mozzid.domain.stats

import com.mozzid.domain.model.Detection
import java.util.Calendar
import kotlin.math.roundToInt

/** Per-species share of the log, used for the breakdown bar + legend. */
data class SpeciesShare(
    val speciesId: String,
    val count: Int,
    val percent: Int, // whole-number percent of total, 0..100
)

/** Aggregate stats over a detection log. Pure value object; computed by [computeStats]. */
data class DetectionStats(
    val total: Int,
    val breakdown: List<SpeciesShare>, // highest count first
    val peakWindow: String?,           // e.g. "10PM–12AM", null when empty
) {
    companion object {
        val EMPTY = DetectionStats(total = 0, breakdown = emptyList(), peakWindow = null)
    }
}

/**
 * Compute [DetectionStats] from a log. Deterministic and side-effect free so it
 * can be unit-tested directly.
 */
fun computeStats(log: List<Detection>): DetectionStats {
    if (log.isEmpty()) return DetectionStats.EMPTY

    val counts = LinkedHashMap<String, Int>()
    val buckets = IntArray(12) // 12 × 2-hour windows
    val cal = Calendar.getInstance()

    for (d in log) {
        counts[d.speciesId] = (counts[d.speciesId] ?: 0) + 1
        cal.timeInMillis = d.timestampMillis
        buckets[cal.get(Calendar.HOUR_OF_DAY) / 2] += 1
    }

    val total = log.size
    val breakdown = counts.entries
        .map { SpeciesShare(it.key, it.value, ((it.value.toDouble() / total) * 100).roundToInt()) }
        .sortedByDescending { it.count }

    var peakBucket = 0
    for (i in 1 until buckets.size) {
        if (buckets[i] > buckets[peakBucket]) peakBucket = i
    }

    return DetectionStats(total, breakdown, formatWindow(peakBucket * 2))
}

/** Format a 2-hour window starting at [startHour] (0..22, even) as "10PM–12AM". */
fun formatWindow(startHour: Int): String {
    val end = (startHour + 2) % 24
    return "${hour12(startHour)}–${hour12(end)}"
}

private fun hour12(h: Int): String {
    val period = if (h < 12) "AM" else "PM"
    val base = if (h % 12 == 0) 12 else h % 12
    return "$base$period"
}
