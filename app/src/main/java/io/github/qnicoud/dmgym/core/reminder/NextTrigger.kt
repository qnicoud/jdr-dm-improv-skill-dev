package io.github.qnicoud.dmgym.core.reminder

import java.time.Instant
import java.time.ZonedDateTime

private const val DAYS_IN_WEEK = 7L

/**
 * Prochaine occurrence **strictement future** d'un jour choisi, à l'heure choisie, dans le fuseau de [now].
 * Un trou de changement d'heure est décalé vers l'avant et une heure ambiguë prend le premier instant
 * (comportement de [ZonedDateTime.of]). `null` si le rappel est désactivé ou si aucun jour n'est choisi.
 */
fun nextTrigger(
    now: ZonedDateTime,
    schedule: ReminderSchedule,
): Instant? {
    if (!schedule.enabled || schedule.days.isEmpty()) return null
    val today = now.toLocalDate()
    return (0..DAYS_IN_WEEK)
        .map { today.plusDays(it) }
        .filter { it.dayOfWeek in schedule.days }
        .map { ZonedDateTime.of(it, schedule.time, now.zone).toInstant() }
        .firstOrNull { it > now.toInstant() }
}
