package io.github.qnicoud.dmgym.core.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class LocaleFormatsTest {
    private val fr = Locale.FRENCH
    private val en = Locale.ENGLISH
    private val date = LocalDate.of(2026, 10, 12)

    @Test
    fun `day and month follow the language`() {
        assertEquals("12 octobre", LocaleFormats.dayMonth(date, fr))
        assertEquals("October 12", LocaleFormats.dayMonth(date, en))
    }

    @Test
    fun `french 24 hour clock keeps the h separator`() {
        assertEquals("14 h 05", LocaleFormats.clock(LocalTime.of(14, 5), fr, is24Hour = true))
        assertEquals("0 h 00", LocaleFormats.clock(LocalTime.MIDNIGHT, fr, is24Hour = true))
    }

    @Test
    fun `english clock is 24 or 12 hour depending on the phone setting`() {
        assertEquals("14:05", LocaleFormats.clock(LocalTime.of(14, 5), en, is24Hour = true))
        assertEquals("2:05 PM", LocaleFormats.clock(LocalTime.of(14, 5), en, is24Hour = false))
    }

    @Test
    fun `french 12 hour clock uses the localized am pm`() {
        assertEquals("6:30 PM", LocaleFormats.clock(LocalTime.of(18, 30), Locale.ENGLISH, is24Hour = false))
    }

    @Test
    fun `weekday names and initials follow the language`() {
        assertEquals("lundi", LocaleFormats.weekday(DayOfWeek.MONDAY, fr))
        assertEquals("Monday", LocaleFormats.weekday(DayOfWeek.MONDAY, en))
        assertEquals("L", LocaleFormats.weekdayInitial(DayOfWeek.MONDAY, fr))
        assertEquals("M", LocaleFormats.weekdayInitial(DayOfWeek.MONDAY, en))
    }

    @Test
    fun `a language without a dedicated pattern uses the localized default`() {
        assertEquals("12. Oktober", LocaleFormats.dayMonth(date, Locale.GERMAN))
    }

    @Test
    fun `the phone default locale never leaks into the result`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.JAPAN)
            assertEquals("12 octobre", LocaleFormats.dayMonth(date, fr))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
