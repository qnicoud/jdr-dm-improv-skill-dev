package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.Settings

private const val DAY_SEPARATOR = ':'
private const val ID_SEPARATOR = ','

/** Format DataStore `daily_plan` : `<epochDay>:<id>,<id>,<id>`. */
fun encodePlan(plan: DailyPlan): String =
    "${plan.epochDay}$DAY_SEPARATOR${plan.exercises.joinToString(ID_SEPARATOR.toString()) { it.value }}"

/** Lecture stricte : toute valeur mal formée donne `null` (le plan est alors recalculé). */
fun decodePlan(encoded: String?): DailyPlan? {
    val value = encoded ?: return null
    val day = value.substringBefore(DAY_SEPARATOR).toLongOrNull() ?: return null
    val ids = value.substringAfter(DAY_SEPARATOR, "").split(ID_SEPARATOR).map { it.trim() }
    val valid = ids.size == DailyPlan.SIZE && ids.all { it.isNotEmpty() } && ids.toSet().size == ids.size
    return if (valid) DailyPlan(day, ids.map(::ExerciseId)) else null
}

/** Plan figé valable pour [today] : du jour, et dont tous les exercices sont encore dans [catalog]. */
fun Settings.frozenPlanFor(
    today: Long,
    catalog: List<ExerciseId>,
): DailyPlan? = frozenPlan?.takeIf { it.epochDay == today && it.exercises.all { id -> id in catalog } }
