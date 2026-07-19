package com.mozzid.domain.stats

import com.mozzid.domain.model.Detection

/** Date-range filter for the history log. */
enum class DateRange { ALL, WEEK }

/**
 * The two history filters combined. [speciesId] == null (or "all") means all
 * species.
 */
data class LogFilter(
    val speciesId: String? = null,
    val range: DateRange = DateRange.ALL,
) {
    val isAllSpecies: Boolean get() = speciesId == null || speciesId == "all"
}

/**
 * Apply [filter] to [detections]. Pure and deterministic — [nowMillis] is injected
 * so the "this week" window is testable. Order is preserved.
 */
fun applyFilters(
    detections: List<Detection>,
    filter: LogFilter,
    nowMillis: Long,
): List<Detection> {
    val cutoff = nowMillis - 7L * 24 * 60 * 60 * 1000
    return detections.filter { d ->
        val speciesOk = filter.isAllSpecies || d.speciesId == filter.speciesId
        val rangeOk = filter.range == DateRange.ALL || d.timestampMillis > cutoff
        speciesOk && rangeOk
    }
}
