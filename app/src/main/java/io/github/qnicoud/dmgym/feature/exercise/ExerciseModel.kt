package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.progressOf
import io.github.qnicoud.dmgym.core.exercise.timerUi
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState

/** État interne du ViewModel, projeté en [ExerciseUiState] à chaque tic. */
internal data class ExerciseModel(
    val session: ExerciseSession,
    val timer: TimerState,
    val untimed: Boolean = false,
    val loading: Boolean = true,
    val drawFailed: Boolean = false,
    val save: SaveStatus = SaveStatus.Idle,
    val extraGroups: Int = 0,
)

internal fun ExerciseModel.toUiState(
    definition: ExerciseDefinition,
    engine: TimerEngine,
    now: Long,
): ExerciseUiState {
    val load =
        when {
            loading -> LoadStatus.Loading
            drawFailed -> LoadStatus.DrawFailed
            else -> LoadStatus.Ready
        }
    val answeredGroups = (session.answers.keys.maxOfOrNull { it.groupIndex } ?: 0) + 1
    return ExerciseUiState(
        definition = definition,
        session = session,
        timer = timerUi(timer, engine, now, untimedReady = untimed),
        load = load,
        progress = progressOf(definition, session),
        save = save,
        groupCount = maxOf(extraGroups + 1, answeredGroups),
    )
}
