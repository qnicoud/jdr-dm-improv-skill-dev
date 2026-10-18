package io.github.qnicoud.dmgym.feature.settings

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Format24 = DateTimeFormatter.ofPattern("H 'h' mm", Locale.FRENCH)
private val Format12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/** « 18 h 30 » (24 h) ou « 6:30 PM » (12 h), selon le réglage d'heure du téléphone. */
internal fun formatReminderTime(
    minuteOfDay: Int,
    is24Hour: Boolean,
): String {
    val time = LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)
    return time.format(if (is24Hour) Format24 else Format12)
}

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
