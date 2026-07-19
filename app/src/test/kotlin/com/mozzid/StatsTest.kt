package com.mozzid

import com.mozzid.domain.model.Detection
import com.mozzid.domain.stats.computeStats
import com.mozzid.domain.stats.formatWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class StatsTest {

    private fun at(hour: Int, speciesId: String): Detection {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, 0)
        return Detection(
            speciesId = speciesId,
            confidence = 80,
            wingbeatHz = 500,
            timestampMillis = c.timeInMillis,
        )
    }

    @Test
    fun `empty log yields EMPTY stats`() {
        val s = computeStats(emptyList())
        assertEquals(0, s.total)
        assertNull(s.peakWindow)
    }

    @Test
    fun `breakdown counts and percents, highest first`() {
        val log = listOf(
            at(10, "aedes"), at(11, "aedes"), at(23, "aedes"), at(22, "culex"),
        )
        val s = computeStats(log)
        assertEquals(4, s.total)
        assertEquals("aedes", s.breakdown.first().speciesId)
        assertEquals(3, s.breakdown.first().count)
        assertEquals(75, s.breakdown.first().percent)
        assertEquals(25, s.breakdown[1].percent)
    }

    @Test
    fun `peak window is the fullest 2-hour bucket`() {
        // Two detections in the 22:00-00:00 bucket, one at 10:00.
        val log = listOf(at(23, "aedes"), at(22, "culex"), at(10, "aedes"))
        val s = computeStats(log)
        assertEquals("10PM–12AM", s.peakWindow)
    }

    @Test
    fun `formatWindow renders 12-hour labels`() {
        assertEquals("10PM–12AM", formatWindow(22))
        assertEquals("12AM–2AM", formatWindow(0))
        assertEquals("10AM–12PM", formatWindow(10))
    }
}
