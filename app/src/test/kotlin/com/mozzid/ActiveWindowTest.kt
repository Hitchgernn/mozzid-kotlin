package com.mozzid

import com.mozzid.domain.model.ActiveWindow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The result screen's active-time cross-check depends entirely on this, and it is
 * the one place the app tells the user its own identification looks doubtful — so
 * both directions matter, not just the daytime-biter-at-night case.
 */
class ActiveWindowTest {

    @Test
    fun `day biter is active through daylight hours`() {
        assertTrue(ActiveWindow.DAY.includesHour(6))
        assertTrue(ActiveWindow.DAY.includesHour(12))
        assertTrue(ActiveWindow.DAY.includesHour(18))
    }

    @Test
    fun `day biter is not active in the small hours`() {
        assertFalse(ActiveWindow.DAY.includesHour(2))
        assertFalse(ActiveWindow.DAY.includesHour(23))
    }

    @Test
    fun `night biter wraps past midnight`() {
        assertTrue(ActiveWindow.NIGHT.includesHour(23))
        assertTrue(ActiveWindow.NIGHT.includesHour(0))
        assertTrue(ActiveWindow.NIGHT.includesHour(5))
    }

    @Test
    fun `night biter heard at midday is flagged`() {
        assertFalse(ActiveWindow.NIGHT.includesHour(9))
        assertFalse(ActiveWindow.NIGHT.includesHour(12))
    }

    @Test
    fun `dusk to dawn spans a wider window than night`() {
        assertTrue(ActiveWindow.DUSK_TO_DAWN.includesHour(17))
        assertTrue(ActiveWindow.DUSK_TO_DAWN.includesHour(6))
        assertFalse(ActiveWindow.DUSK_TO_DAWN.includesHour(12))
    }

    @Test
    fun `every hour of the day resolves for every window`() {
        ActiveWindow.entries.forEach { window ->
            (0..23).forEach { hour -> window.includesHour(hour) }
        }
    }
}
