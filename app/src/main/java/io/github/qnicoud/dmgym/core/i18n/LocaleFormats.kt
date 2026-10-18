package io.github.qnicoud.dmgym.core.i18n

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formats de date et d'heure qui dépendent de la langue (ADR 0016 §5). Fonctions pures : la [Locale] est toujours
 * passée par l'appelant, jamais lue dans le défaut de la JVM. Le français garde ses motifs « 12 octobre » et
 * « 14 h 05 » ; les autres langues prennent les motifs localisés du JDK.
 */
object LocaleFormats {
    private const val FR = "fr"

    /** « 12 octobre » (français), « October 12 » (anglais). */
    fun dayMonth(
        date: LocalDate,
        locale: Locale,
    ): String = date.format(DateTimeFormatter.ofPattern(dayMonthPattern(locale), locale))

    /** « 14 h 05 » (français, 24 h) ; « 14:05 » ou « 2:05 PM » ailleurs, selon [is24Hour] (réglage du téléphone). */
    fun clock(
        time: LocalTime,
        locale: Locale,
        is24Hour: Boolean = true,
    ): String = time.format(DateTimeFormatter.ofPattern(clockPattern(locale, is24Hour), locale))

    /** « lundi », « Monday ». */
    fun weekday(
        day: DayOfWeek,
        locale: Locale,
    ): String = day.getDisplayName(TextStyle.FULL, locale)

    /** Initiale en majuscule : « L », « M ». */
    fun weekdayInitial(
        day: DayOfWeek,
        locale: Locale,
    ): String = day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)

    private fun dayMonthPattern(locale: Locale): String =
        when (locale.language) {
            FR -> "d MMMM"
            else -> localizedDayMonth(locale)
        }

    private fun clockPattern(
        locale: Locale,
        is24Hour: Boolean,
    ): String =
        when {
            !is24Hour -> "h:mm a"
            locale.language == FR -> "H 'h' mm"
            else -> "H:mm"
        }

    /** Retire l'année du motif de date long de la langue : « d. MMMM y » devient « d. MMMM ». */
    private fun localizedDayMonth(locale: Locale): String {
        val full =
            DateTimeFormatterBuilder
                .getLocalizedDateTimePattern(FormatStyle.LONG, null, IsoChronology.INSTANCE, locale)
        return full
            .replace(Regex("""[\s,./-]*y+[\s,./-]*(?=$)"""), "")
            .replace(Regex("""^[\s,./-]*y+[\s,./-]*"""), "")
            .trim()
    }
}
