package io.github.qnicoud.dmgym.feature.home

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Un exercice tel que l'accueil le présente. [durationSeconds] vaut `null` pour « Sans minuteur ». */
data class ExerciseSummary(
    val id: ExerciseId,
    val durationSeconds: Int?,
)

/**
 * Port de l'accueil : la liste des exercices avec leur durée réglée.
 * Provisoire : sera alimenté par `SettingsRepository` (lot L3) après fusion.
 */
interface ExerciseCatalog {
    val exercises: Flow<List<ExerciseSummary>>
}

/** Durées par défaut de la SPEC (docs/ux/parcours.md, DÉCISION 4). */
class DefaultExerciseCatalog : ExerciseCatalog {
    override val exercises: Flow<List<ExerciseSummary>> = flowOf(DEFAULTS)

    private companion object {
        val DEFAULTS =
            listOf(
                ExerciseSummary(ExerciseId.CRAZY_EIGHTS, 8 * 60),
                ExerciseSummary(ExerciseId.CONSTRAINT_BOX, 5 * 60),
                ExerciseSummary(ExerciseId.CORE_IMAGE, 10 * 60),
                ExerciseSummary(ExerciseId.STATUS_NUMBERS, 3 * 60),
                ExerciseSummary(ExerciseId.DECISION_POINT, 30),
                ExerciseSummary(ExerciseId.CONNECT_STORY, 10 * 60),
                ExerciseSummary(ExerciseId.ONE_PAGE, null),
            )
    }
}
