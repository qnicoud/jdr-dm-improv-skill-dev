package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.Settings

private const val ENTRY_SEPARATOR = ";"
private const val PAIR_SEPARATOR = ':'

/** Format DataStore `pos:exerciseId;…` (ADR 0013). */
fun encodeReplacements(replacements: Map<Int, ExerciseId>): String =
    replacements.entries
        .sortedBy { it.key }
        .joinToString(ENTRY_SEPARATOR) { "${it.key}$PAIR_SEPARATOR${it.value.value}" }

/** Lecture tolérante : les entrées illisibles ou hors des trois positions sont ignorées. */
fun decodeReplacements(encoded: String?): Map<Int, ExerciseId> =
    encoded
        .orEmpty()
        .split(ENTRY_SEPARATOR)
        .mapNotNull(::decodeEntry)
        .toMap()

private fun decodeEntry(entry: String): Pair<Int, ExerciseId>? {
    val position = entry.substringBefore(PAIR_SEPARATOR).toIntOrNull()?.takeIf { it in 0 until DailyPlan.SIZE }
    val id = entry.substringAfter(PAIR_SEPARATOR, "").trim()
    return if (position != null && id.isNotEmpty()) position to ExerciseId(id) else null
}

/** Remplacements valables pour [today] ; ceux d'un autre jour sont périmés (effacés le lendemain). */
fun Settings.replacementsFor(today: Long): Map<Int, ExerciseId> {
    val stillValid = dailyEpochDay == today
    return if (stillValid) dailyReplacements else emptyMap()
}

fun Settings.withReplacement(
    today: Long,
    position: Int,
    exercise: ExerciseId,
): Settings = copy(dailyEpochDay = today, dailyReplacements = replacementsFor(today) + (position to exercise))

/** Applique les remplacements ; ceux qui doubleraient un autre exercice, ou inconnus, sont ignorés. */
fun DailyPlan.withReplacements(replacements: Map<Int, ExerciseId>): DailyPlan {
    val applied =
        replacements.entries.sortedBy { it.key }.fold(exercises) { current, (position, id) ->
            replaceAt(current, position, id)
        }
    return copy(exercises = applied)
}

private fun replaceAt(
    current: List<ExerciseId>,
    position: Int,
    id: ExerciseId,
): List<ExerciseId> =
    if (isValidReplacement(current, position, id)) current.toMutableList().apply { set(position, id) } else current

private fun isValidReplacement(
    current: List<ExerciseId>,
    position: Int,
    id: ExerciseId,
): Boolean {
    val duplicate = current.withIndex().any { it.index != position && it.value == id }
    return position in current.indices && id in ExerciseId.ALL && !duplicate
}
