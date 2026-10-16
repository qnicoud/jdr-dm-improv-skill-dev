package io.github.qnicoud.dmgym.feature.home

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Un exercice tel que l'accueil le présente. [durationSeconds] vaut `null` pour « Sans minuteur ». */
data class ExerciseSummary(
    val id: ExerciseId,
    val durationSeconds: Int?,
)

/** Port de l'accueil : la liste des exercices avec leur durée réglée. */
interface ExerciseCatalog {
    val exercises: Flow<List<ExerciseSummary>>
}

/** Adaptateur sur les réglages : durée enregistrée, sinon durée par défaut ; « sans minuteur » prioritaire. */
class SettingsExerciseCatalog(
    settings: SettingsRepository,
) : ExerciseCatalog {
    override val exercises: Flow<List<ExerciseSummary>> =
        settings.settings.map { s ->
            ExerciseId.ALL.map { id ->
                val duration = s.durations[id] ?: ExerciseId.DEFAULT_DURATIONS.getValue(id)
                ExerciseSummary(id, if (id in s.untimed) null else duration.inWholeSeconds.toInt())
            }
        }
}
