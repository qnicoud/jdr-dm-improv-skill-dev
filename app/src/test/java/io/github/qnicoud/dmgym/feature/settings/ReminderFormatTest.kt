package io.github.qnicoud.dmgym.feature.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class ReminderFormatTest {
    @Test
    fun `24 hour format is the French one`() {
        assertEquals("18 h 30", formatReminderTime(1110, is24Hour = true))
        assertEquals("7 h 05", formatReminderTime(425, is24Hour = true))
        assertEquals("0 h 00", formatReminderTime(0, is24Hour = true))
    }

    @Test
    fun `12 hour format follows the phone setting`() {
        assertEquals("6:30 PM", formatReminderTime(1110, is24Hour = false))
    }

    @Test
    fun `days are listed from monday, lowercase after the first`() {
        val names = DayOfWeek.entries.associateWith { it.name.take(3) }
        assertEquals("MON, wed, sun", joinDays(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), names))
    }
}
