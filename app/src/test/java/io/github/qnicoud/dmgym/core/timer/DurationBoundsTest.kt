package io.github.qnicoud.dmgym.core.timer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationBoundsTest {
    @Test
    fun `bounds are half to ten times the default`() {
        val bounds = DurationBounds.of(8.minutes)

        assertEquals(4.minutes, bounds.min)
        assertEquals(80.minutes, bounds.max)
    }

    @Test
    fun `minimum never goes under 15 seconds`() {
        val bounds = DurationBounds.of(30.seconds)

        assertEquals(15.seconds, bounds.min)
        assertEquals(5.minutes, bounds.max)
    }

    @Test
    fun `clamp keeps values inside the bounds`() {
        val bounds = DurationBounds.of(5.minutes)

        assertEquals(150.seconds, bounds.clamp(10.seconds))
        assertEquals(50.minutes, bounds.clamp(120.minutes))
        assertEquals(6.minutes, bounds.clamp(6.minutes))
        assertTrue(bounds.contains(5.minutes))
        assertFalse(bounds.contains(1.seconds))
    }

    @Test
    fun `step is 15 seconds up to two minutes and one minute beyond`() {
        val bounds = DurationBounds.of(5.minutes)

        assertEquals(15.seconds, bounds.stepFor(90.seconds))
        assertEquals(15.seconds, bounds.stepFor(2.minutes))
        assertEquals(1.minutes, bounds.stepFor(3.minutes))
    }

    @Test
    fun `increase and decrease move by one step and stay clamped`() {
        val bounds = DurationBounds.of(1.minutes)

        assertEquals(75.seconds, bounds.increase(60.seconds))
        assertEquals(45.seconds, bounds.decrease(60.seconds))
        assertEquals(30.seconds, bounds.decrease(30.seconds))
        assertEquals(10.minutes, bounds.increase(10.minutes))
    }

    @Test
    fun `decrease from two minutes steps by 15 seconds and increase beyond steps by a minute`() {
        assertEquals(105.seconds, DurationBounds.of(2.minutes).decrease(2.minutes))
        assertEquals(4.minutes, DurationBounds.of(2.minutes).increase(3.minutes))
    }
}
