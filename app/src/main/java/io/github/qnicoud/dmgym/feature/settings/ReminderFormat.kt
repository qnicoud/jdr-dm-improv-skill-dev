package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.core.i18n.LocaleFormats
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

/** « 18 h 30 » (24 h) ou « 6:30 PM » (12 h), selon le réglage d'heure du téléphone. */
internal fun formatReminderTime(
    minuteOfDay: Int,
    is24Hour: Boolean,
    locale: Locale,
): String =
    LocaleFormats.clock(LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR), locale, is24Hour)

/** Les jours dans l'ordre de la semaine (lundi d'abord), séparés par une virgule : « Lun, mer, ven ». */
internal fun joinDays(
    days: Set<DayOfWeek>,
    names: Map<DayOfWeek, String>,
): String =
    DayOfWeek.entries
        .filter { it in days }
        .mapIndexed { index, day -> names.getValue(day).let { if (index == 0) it else it.lowercase() } }
        .joinToString(", ")

private const val MINUTES_PER_HOUR = 60
