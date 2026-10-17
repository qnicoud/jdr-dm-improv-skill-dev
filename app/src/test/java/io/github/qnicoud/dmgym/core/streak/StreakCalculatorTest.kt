package io.github.qnicoud.dmgym.core.streak

import io.github.qnicoud.dmgym.core.time.localEpochDay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StreakCalculatorTest {
    // 2026-10-14 est un mercredi ; la semaine (lundi) commence le 12.
    private val wednesday = LocalDate.of(2026, 10, 14).toEpochDay()

    private fun streak(
        vararg offsets: Int,
        today: Long = wednesday,
    ) = computeStreak(offsets.map { today + it }.toSet(), offsets.size, today)

    private fun dayAt(
        local: String,
        zone: ZoneId,
    ): Long = Clock.fixed(LocalDateTime.parse(local).atZone(zone).toInstant(), zone).localEpochDay()

    @Test
    fun `empty history gives a zero summary`() {
        val s = streak()
        assertEquals(0, s.totalSessions)
        assertEquals(0, s.practicedDays)
        assertEquals(0, s.current)
        assertEquals(0, s.best)
        assertNull(s.graceUsedOn)
        assertFalse(s.todayDone)
        assertTrue(s.graceDays.isEmpty())
        assertFalse(s.graceUsedThisWeek(wednesday))
    }

    @Test
    fun `two sessions the same day count once`() {
        val s = computeStreak(setOf(wednesday), sessions = 2, today = wednesday)
        assertEquals(2, s.totalSessions)
        assertEquals(1, s.practicedDays)
        assertEquals(1, s.current)
        assertTrue(s.todayDone)
    }

    @Test
    fun `consecutive days build the streak`() {
        val s = streak(-2, -1, 0)
        assertEquals(3, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun `today not done yet does not break the streak`() {
        val s = streak(-3, -2, -1)
        assertEquals(3, s.current)
        assertFalse(s.todayDone)
        assertNull(s.graceUsedOn)
    }

    @Test
    fun `a single gap is covered by the grace day and not counted`() {
        val s = streak(-3, -2, 0) // mardi (-1) manquant
        assertEquals(3, s.current)
        assertEquals(wednesday - 1, s.graceUsedOn)
        assertEquals(setOf(wednesday - 1), s.graceDays)
        assertTrue(s.graceUsedThisWeek(wednesday))
    }

    @Test
    fun `two gaps in the same week end the streak`() {
        val friday = wednesday + 2
        val s = computeStreak(setOf(wednesday - 2, wednesday, friday), 3, friday) // lundi, mercredi, vendredi
        assertEquals(1, s.current)
        assertEquals(2, s.best)
        assertNull(s.graceUsedOn)
    }

    @Test
    fun `grace is reloaded on monday`() {
        // samedi 17, trou dimanche 18, lundi 19, mardi 20, trou mercredi 21, jeudi 22
        val days = setOf(wednesday + 3, wednesday + 5, wednesday + 6, wednesday + 8)
        val s = computeStreak(days, 4, wednesday + 8)
        assertEquals(4, s.current)
        assertEquals(setOf(wednesday + 4, wednesday + 7), s.graceDays)
        assertEquals(wednesday + 7, s.graceUsedOn)
        assertTrue(s.graceUsedThisWeek(wednesday + 8))
    }

    @Test
    fun `the grace badge goes back to available the next monday`() {
        val s = streak(-3, -2, 0)
        assertTrue(s.graceUsedThisWeek(wednesday + 4)) // dimanche
        assertFalse(s.graceUsedThisWeek(wednesday + 5)) // lundi suivant
    }

    @Test
    fun `gap of two days ends the streak and best is kept`() {
        val s = streak(-10, -9, -8, -7, -6, -5, 0)
        assertEquals(1, s.current)
        assertEquals(6, s.best)
        assertEquals(7, s.practicedDays)
    }

    @Test
    fun `yesterday missed and today not done is still in grace when available`() {
        val s = streak(-3, -2)
        assertEquals(2, s.current)
        assertEquals(wednesday - 1, s.graceUsedOn)
    }

    @Test
    fun `yesterday missed after the weekly grace was used resets current but keeps best`() {
        val friday = wednesday + 2
        // lundi 12 et mercredi 14 (grâce le 13), jeudi 15 manquant, vendredi 16 pas encore fait
        val s = computeStreak(setOf(wednesday - 2, wednesday), 2, friday)
        assertEquals(0, s.current)
        assertEquals(2, s.best)
    }

    @Test
    fun `two missed days leave current at zero`() {
        val s = streak(-5, -4)
        assertEquals(0, s.current)
        assertEquals(2, s.best)
    }

    @Test
    fun `future days are ignored when the clock is set back`() {
        val s = computeStreak(setOf(wednesday - 1, wednesday + 3, wednesday + 4), 3, wednesday)
        assertEquals(1, s.current)
        assertEquals(3, s.practicedDays)
        assertEquals(2, s.best)
        assertEquals(3, s.totalSessions)
        assertFalse(s.todayDone)
    }

    @Test
    fun `daylight saving changes keep the days consecutive`() {
        val paris = ZoneId.of("Europe/Paris")
        listOf("2026-03-28T23:30:00", "2026-03-29T00:30:00", "2026-03-29T23:30:00", "2026-03-30T00:30:00")
            .map { dayAt(it, paris) }
            .zipWithNext { a, b -> assertTrue(b - a <= 1L) }
        val march = listOf("2026-03-28T23:50:00", "2026-03-29T03:30:00", "2026-03-30T00:10:00").map { dayAt(it, paris) }
        assertEquals(3, computeStreak(march.toSet(), 3, march.last()).current)
        val october =
            listOf(
                "2026-10-24T23:50:00",
                "2026-10-25T01:30:00",
                "2026-10-26T00:10:00",
            ).map { dayAt(it, paris) }
        assertEquals(3, computeStreak(october.toSet(), 3, october.last()).current)
    }

    @Test
    fun `a time zone change only shifts the stored days`() {
        val instant = Instant.parse("2026-10-14T16:00:00Z")
        val paris = Clock.fixed(instant, ZoneId.of("Europe/Paris")).localEpochDay()
        val tokyo = Clock.fixed(instant, ZoneId.of("Asia/Tokyo")).localEpochDay()
        assertEquals(paris + 1, tokyo)
        assertEquals(2, computeStreak(setOf(paris, tokyo), 2, tokyo).current)
        assertEquals(1, computeStreak(setOf(paris, paris), 2, paris).practicedDays)
    }

    @Test
    fun `week start is monday`() {
        assertEquals(wednesday - 2, weekStart(wednesday))
        assertEquals(wednesday - 2, weekStart(wednesday - 2))
        assertEquals(wednesday - 2, weekStart(wednesday + 4))
        assertEquals(wednesday + 5, weekStart(wednesday + 5))
    }

    @Test
    fun `recent days are a sliding row of seven with today on the right`() {
        val days = setOf(wednesday - 5, wednesday - 3, wednesday - 2, wednesday)
        val s = computeStreak(days, 4, wednesday)
        val row = recentDays(s, days, wednesday)
        assertEquals(7, row.size)
        assertEquals(wednesday - 6, row.first().epochDay)
        assertEquals(wednesday, row.last().epochDay)
        assertEquals(
            listOf(
                DayMark.NONE,
                DayMark.PRACTICED,
                DayMark.GRACE,
                DayMark.PRACTICED,
                DayMark.PRACTICED,
                DayMark.GRACE,
                DayMark.PRACTICED,
            ),
            row.map { it.mark },
        )
    }
}
