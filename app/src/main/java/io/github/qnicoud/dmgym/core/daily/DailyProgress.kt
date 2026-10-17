package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus

enum class ExerciseProgress { TODO, IN_PROGRESS, DONE }

/** Avancement de la séance, déduit de l'historique du jour (ADR 0011 : l'origine de la session n'importe pas). */
data class DailyProgress(
    val states: List<ExerciseProgress>,
    val exercises: List<ExerciseId>,
) {
    val allDone: Boolean get() = states.isNotEmpty() && states.all { it == ExerciseProgress.DONE }
    val started: Boolean get() = states.any { it != ExerciseProgress.TODO }

    /** Exercice à proposer pour « Commencer / Continuer » : celui en cours, sinon le premier à faire. */
    val nextExercise: ExerciseId?
        get() {
            val index =
                states.indexOf(ExerciseProgress.IN_PROGRESS).takeIf { it >= 0 } ?: states.indexOf(ExerciseProgress.TODO)
            return exercises.getOrNull(index)
        }
}

fun dailyProgress(
    exercises: List<ExerciseId>,
    today: Long,
    sessions: List<SessionRecord>,
): DailyProgress {
    val ofToday = sessions.filter { it.localEpochDay == today }
    val states =
        exercises.map { id ->
            val own = ofToday.filter { it.exerciseId == id.value }
            when {
                own.any { it.status == SessionStatus.COMPLETED } -> ExerciseProgress.DONE
                own.isNotEmpty() -> ExerciseProgress.IN_PROGRESS
                else -> ExerciseProgress.TODO
            }
        }
    return DailyProgress(states, exercises)
}
