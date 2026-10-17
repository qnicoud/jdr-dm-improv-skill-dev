package io.github.qnicoud.dmgym.core.time

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

class SystemZoneClockTest {
    private val original = TimeZone.getDefault()

    @AfterEach
    fun restoreZone() = TimeZone.setDefault(original)

    @Test
    fun `the zone follows the default zone of the device`() {
        val clock = SystemZoneClock()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        assertEquals(ZoneId.of("Asia/Tokyo"), clock.zone)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"))
        assertEquals(ZoneId.of("Europe/Paris"), clock.zone)
    }

    @Test
    fun `the local day changes with the zone`() {
        val clock = SystemZoneClock()
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        val far = clock.localEpochDay()
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Pago_Pago"))
        val behind = clock.localEpochDay()
        assertEquals(1L, far - behind)
    }

    @Test
    fun `the instant is the system instant and withZone gives a fixed zone clock`() {
        val clock = SystemZoneClock()
        assertTrue(Duration.between(clock.instant(), Instant.now()).abs() < Duration.ofSeconds(5))
        assertEquals(ZoneId.of("UTC"), clock.withZone(ZoneId.of("UTC")).zone)
    }
}
