package com.mozzid

import com.mozzid.domain.model.Detection
import com.mozzid.domain.stats.DateRange
import com.mozzid.domain.stats.LogFilter
import com.mozzid.domain.stats.applyFilters
import org.junit.Assert.assertEquals
import org.junit.Test

class LogFiltersTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun det(id: String, ageDays: Long) = Detection(
        speciesId = id,
        confidence = 80,
        wingbeatHz = 500,
        timestampMillis = now - ageDays * day,
    )

    private val log = listOf(
        det("aedes", 0), det("culex", 2), det("aedes", 10), det("anopheles", 30),
    )

    @Test
    fun `all species, all range keeps everything`() {
        val out = applyFilters(log, LogFilter(), now)
        assertEquals(4, out.size)
    }

    @Test
    fun `species filter keeps only matches`() {
        val out = applyFilters(log, LogFilter(speciesId = "aedes"), now)
        assertEquals(2, out.size)
    }

    @Test
    fun `week range drops older than 7 days`() {
        val out = applyFilters(log, LogFilter(range = DateRange.WEEK), now)
        assertEquals(2, out.size) // only the 0- and 2-day-old rows
    }

    @Test
    fun `all sentinel means all species`() {
        val out = applyFilters(log, LogFilter(speciesId = "all"), now)
        assertEquals(4, out.size)
    }
}
