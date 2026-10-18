package io.github.qnicoud.dmgym.core.reminder

import java.time.DayOfWeek
import java.time.LocalTime

/** Modèle de planification du rappel (ADR 0014). */
data class ReminderSchedule(
    val enabled: Boolean,
    val time: LocalTime,
    val days: Set<DayOfWeek>,
)

/**
 * Réglages du rappel (ADR 0014 §4). [enabled] est l'intention de l'utilisateur ; la livraison dépend en plus de la
 * permission. [resumedEpochDay] : jour de l'activation ou de la dernière reprise, point de départ de la pause.
 */
data class ReminderSettings(
    val enabled: Boolean = false,
    val minuteOfDay: Int = DEFAULT_MINUTE,
    val days: Set<DayOfWeek> = DEFAULT_DAYS,
    val resumedEpochDay: Long? = null,
) {
    val time: LocalTime get() = LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)

    fun toSchedule(): ReminderSchedule = ReminderSchedule(enabled, time, days)

    companion object {
        /** 18 h 30 (décision D59). */
        const val DEFAULT_MINUTE = 1110
        const val MINUTES_PER_DAY = 1440
        const val MINUTES_PER_HOUR = 60

        /** Lundi, mercredi, vendredi (décision D59). */
        val DEFAULT_DAYS: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    }
}

/** `"1,3,5"` (ISO, 1 = lundi). */
fun encodeDays(days: Set<DayOfWeek>): String = days.map { it.value }.sorted().joinToString(",")

/** Les valeurs invalides sont ignorées ; un résultat vide est remplacé par le défaut (ADR 0014 §4). */
fun decodeDays(raw: String?): Set<DayOfWeek> =
    raw
        ?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull()?.takeIf { n -> n in 1..DayOfWeek.entries.size } }
        ?.map { DayOfWeek.of(it) }
        ?.toSet()
        ?.takeIf { it.isNotEmpty() }
        ?: ReminderSettings.DEFAULT_DAYS

/** Minute du jour dans 0..1439, sinon le défaut. */
fun boundedMinute(raw: Int?): Int =
    raw?.takeIf { it in 0 until ReminderSettings.MINUTES_PER_DAY } ?: ReminderSettings.DEFAULT_MINUTE
