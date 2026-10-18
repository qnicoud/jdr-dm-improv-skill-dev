package io.github.qnicoud.dmgym.feature.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.util.Locale

class ReminderFormatTest {
    @Test
    fun `24 hour format is the French one`() {
        assertEquals("18 h 30", formatReminderTime(1110, is24Hour = true, Locale.FRENCH))
        assertEquals("7 h 05", formatReminderTime(425, is24Hour = true, Locale.FRENCH))
        assertEquals("0 h 00", formatReminderTime(0, is24Hour = true, Locale.FRENCH))
    }

    @Test
    fun `12 hour format follows the phone setting`() {
        assertEquals("6:30 PM", formatReminderTime(1110, is24Hour = false, Locale.FRENCH))
    }

    @Test
    fun `12 hour format in english`() {
        assertEquals("6:30 PM", formatReminderTime(1110, is24Hour = false, Locale.ENGLISH))
        assertEquals("12:05 AM", formatReminderTime(5, is24Hour = false, Locale.ENGLISH))
    }

    @Test
    fun `days are listed from monday, lowercase after the first`() {
        val names = DayOfWeek.entries.associateWith { it.name.take(3) }
        assertEquals("MON, wed, sun", joinDays(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), names))
    }
}
