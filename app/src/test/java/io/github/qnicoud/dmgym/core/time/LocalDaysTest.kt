package io.github.qnicoud.dmgym.core.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class LocalDaysTest {
    private val instant = Instant.parse("2026-10-16T22:30:00Z")

    @Test
    fun `epoch day follows the local zone around midnight`() {
        val utc = Clock.fixed(instant, ZoneId.of("UTC"))
        val paris = Clock.fixed(instant, ZoneId.of("Europe/Paris"))

        assertEquals(utc.localEpochDay() + 1, paris.localEpochDay())
    }

    @Test
    fun `epoch day of the epoch is zero`() {
        assertEquals(0L, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")).localEpochDay())
    }
}
