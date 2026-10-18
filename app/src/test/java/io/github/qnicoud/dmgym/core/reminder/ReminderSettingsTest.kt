package io.github.qnicoud.dmgym.core.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime

class ReminderSettingsTest {
    @Test
    fun `defaults are monday wednesday friday at 18h30 and off`() {
        val s = ReminderSettings()
        assertFalse(s.enabled)
        assertEquals(1110, s.minuteOfDay)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), s.days)
        assertEquals(LocalTime.of(18, 30), s.time)
    }

    @Test
    fun `days encode as ISO numbers and decode back`() {
        assertEquals("1,3,5", encodeDays(ReminderSettings.DEFAULT_DAYS))
        assertEquals(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), decodeDays("7,1"))
    }

    @Test
    fun `corrupt or empty days decode to the default`() {
        assertEquals(ReminderSettings.DEFAULT_DAYS, decodeDays(null))
        assertEquals(ReminderSettings.DEFAULT_DAYS, decodeDays(""))
        assertEquals(ReminderSettings.DEFAULT_DAYS, decodeDays("x,9,0"))
        assertEquals(setOf(DayOfWeek.TUESDAY), decodeDays("2,9"))
    }

    @Test
    fun `minute of day is bounded`() {
        assertEquals(ReminderSettings.DEFAULT_MINUTE, boundedMinute(-1))
        assertEquals(ReminderSettings.DEFAULT_MINUTE, boundedMinute(1440))
        assertEquals(0, boundedMinute(0))
        assertEquals(1439, boundedMinute(1439))
        assertEquals(ReminderSettings.DEFAULT_MINUTE, boundedMinute(null))
    }

    @Test
    fun `schedule mirrors the settings`() {
        val s = ReminderSettings(enabled = true, minuteOfDay = 60, days = setOf(DayOfWeek.TUESDAY))
        assertEquals(ReminderSchedule(true, LocalTime.of(1, 0), setOf(DayOfWeek.TUESDAY)), s.toSchedule())
    }
}
