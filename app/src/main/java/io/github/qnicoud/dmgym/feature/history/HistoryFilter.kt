package io.github.qnicoud.dmgym.feature.history

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import kotlinx.serialization.Serializable

/**
 * Filtre de l'historique (S08) : « Tous » (aucun critère), « Bijoux », ou un exercice. Les deux critères
 * ne se combinent pas à l'écran : une puce à la fois.
 */
@Serializable
data class HistoryFilter(
    val exerciseId: ExerciseId? = null,
    val gemsOnly: Boolean = false,
) {
    companion object {
        val All = HistoryFilter()
        val Gems = HistoryFilter(gemsOnly = true)

        fun exercise(id: ExerciseId) = HistoryFilter(exerciseId = id)
    }
}
