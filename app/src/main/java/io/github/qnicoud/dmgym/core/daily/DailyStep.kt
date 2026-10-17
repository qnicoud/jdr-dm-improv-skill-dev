package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId

/** Exercice suivant de la séance du jour ; [number] est sa place (à partir de 1) dans la séance. */
data class DailyNext(
    val id: ExerciseId,
    val number: Int,
)

/** Où en est l'exercice en cours dans la séance du jour : « 1 sur 3 », et quoi proposer ensuite. */
data class DailyStep(
    val number: Int,
    val total: Int,
    /** `null` : c'est le dernier exercice à faire, la séance se termine sur S07. */
    val next: DailyNext?,
)

/**
 * Place de [current] dans la séance. L'exercice suivant est le premier après lui qui n'est pas déjà fait ;
 * un exercice sauté plus haut dans la liste n'est pas reproposé. `null` si [current] n'est pas dans la séance.
 */
fun dailyStep(
    exercises: List<ExerciseId>,
    states: List<ExerciseProgress>,
    current: ExerciseId,
): DailyStep? {
    val index = exercises.indexOf(current).takeIf { it >= 0 } ?: return null
    val nextIndex = (index + 1 until exercises.size).firstOrNull { states.getOrNull(it) != ExerciseProgress.DONE }
    return DailyStep(
        number = index + 1,
        total = exercises.size,
        next = nextIndex?.let { DailyNext(exercises[it], it + 1) },
    )
}
