package io.github.qnicoud.dmgym.core.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerTest {
    private val paris = ZoneId.of("Europe/Paris")
    private val mwf = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

    private fun schedule(
        days: Set<DayOfWeek> = mwf,
        time: LocalTime = LocalTime.of(18, 30),
        enabled: Boolean = true,
    ) = ReminderSchedule(enabled, time, days)

    private fun at(
        local: String,
        zone: ZoneId = paris,
    ): ZonedDateTime = LocalDateTime.parse(local).atZone(zone)

    private fun next(
        now: ZonedDateTime,
        schedule: ReminderSchedule = schedule(),
    ): ZonedDateTime? = nextTrigger(now, schedule)?.atZone(now.zone)

    @Test
    fun `chosen day later today fires today`() {
        // 2026-10-14 est un mercredi.
        assertEquals(at("2026-10-14T18:30"), next(at("2026-10-14T09:00")))
    }

    @Test
    fun `time already passed today moves to the next chosen day`() {
        assertEquals(at("2026-10-16T18:30"), next(at("2026-10-14T19:00")))
    }

    @Test
    fun `occurrence is strictly in the future`() {
        assertEquals(at("2026-10-16T18:30"), next(at("2026-10-14T18:30")))
    }

    @Test
    fun `week wraps from friday to monday`() {
        assertEquals(at("2026-10-19T18:30"), next(at("2026-10-16T20:00")))
    }

    @Test
    fun `single day waits a full week when passed`() {
        val monday = schedule(days = setOf(DayOfWeek.MONDAY))
        assertEquals(at("2026-10-26T18:30"), next(at("2026-10-19T19:00"), monday))
    }

    @Test
    fun `no chosen day gives null`() {
        assertNull(nextTrigger(at("2026-10-14T09:00"), schedule(days = emptySet())))
    }

    @Test
    fun `disabled schedule gives null`() {
        assertNull(nextTrigger(at("2026-10-14T09:00"), schedule(enabled = false)))
    }

    @Test
    fun `spring forward gap is shifted forward`() {
        // 2027-03-28 (dimanche) : 02:30 n'existe pas à Paris, il devient 03:30.
        val sunday = schedule(days = setOf(DayOfWeek.SUNDAY), time = LocalTime.of(2, 30))
        assertEquals(at("2027-03-28T03:30"), next(at("2027-03-27T12:00"), sunday))
    }

    @Test
    fun `autumn overlap takes the first instant`() {
        // 2026-10-25 (dimanche) : 02:30 existe deux fois, on prend la première (UTC+2).
        val sunday = schedule(days = setOf(DayOfWeek.SUNDAY), time = LocalTime.of(2, 30))
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), nextTrigger(at("2026-10-24T12:00"), sunday))
    }

    @Test
    fun `time zone change uses the current zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertEquals(at("2026-10-14T18:30", tokyo), next(at("2026-10-14T09:00", tokyo)))
    }

    @Test
    fun `same instant gives different triggers in different zones`() {
        val now = at("2026-10-14T09:00")
        val inParis = nextTrigger(now, schedule())
        val inNewYork = nextTrigger(now.withZoneSameInstant(ZoneId.of("America/New_York")), schedule())
        assertNotEquals(inParis, inNewYork)
    }
}
