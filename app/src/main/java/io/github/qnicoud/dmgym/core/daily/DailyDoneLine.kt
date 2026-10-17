package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus

/** Une ligne du récapitulatif de fin de séance (S07). Sans session terminée du jour, [done] vaut faux. */
data class DailyDoneLine(
    val id: ExerciseId,
    val done: Boolean,
    val durationSec: Int = 0,
    /** Niveau donné au bilan (1 à 3), `null` s'il n'y en a pas. */
    val rating: Int? = null,
    val isGem: Boolean = false,
)

/**
 * Une ligne par exercice de la séance. [sessions] est classé de la plus récente à la plus ancienne (historique) :
 * si un exercice a été refait dans la journée, la dernière session terminée fait foi.
 */
fun dailyDoneLines(
    exercises: List<ExerciseId>,
    today: Long,
    sessions: List<SessionRecord>,
): List<DailyDoneLine> {
    val completed = sessions.filter { it.localEpochDay == today && it.status == SessionStatus.COMPLETED }
    return exercises.map { id ->
        completed.firstOrNull { it.exerciseId == id.value }?.let {
            DailyDoneLine(id, true, it.activeDurationSec, it.selfRating, it.isGem)
        } ?: DailyDoneLine(id, false)
    }
}
