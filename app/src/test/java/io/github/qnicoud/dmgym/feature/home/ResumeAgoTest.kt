package io.github.qnicoud.dmgym.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResumeAgoTest {
    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun `under a minute is now`() {
        assertEquals(Ago.Now, agoOf(lastActivity = 0, now = 59_999))
    }

    @Test
    fun `a clock that went backwards is now`() {
        assertEquals(Ago.Now, agoOf(lastActivity = 10 * minute, now = 0))
    }

    @Test
    fun `minutes are floored up to the hour`() {
        assertEquals(Ago.Minutes(1), agoOf(0, minute))
        assertEquals(Ago.Minutes(12), agoOf(0, 12 * minute + 30_000))
        assertEquals(Ago.Minutes(59), agoOf(0, hour - 1))
    }

    @Test
    fun `hours are floored`() {
        assertEquals(Ago.Hours(1), agoOf(0, hour))
        assertEquals(Ago.Hours(23), agoOf(0, 23 * hour + 59 * minute))
    }
}
