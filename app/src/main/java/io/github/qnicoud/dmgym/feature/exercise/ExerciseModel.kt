package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.core.exercise.defaultDuration
import io.github.qnicoud.dmgym.core.exercise.pausedTimerFrom
import io.github.qnicoud.dmgym.core.exercise.progressOf
import io.github.qnicoud.dmgym.core.exercise.timerUi
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlin.time.Duration.Companion.minutes

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

internal fun ExerciseModel.toUiState(definition: ExerciseDefinition): ExerciseUiState {
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
        keepScreenOn = keepScreenOn,
        load = load,
        progress = progressOf(definition, session),
        save = save,
        groupCount = maxOf(extraGroups + 1, answeredGroups),
    )
}

/** Écran maintenu allumé en décompte, en temps additionnel et au chronomètre, pas en pause (ADR 0006). */
private val ExerciseModel.keepScreenOn: Boolean
    get() =
        session.phase == Phase.Active &&
            (timer is TimerState.Running || timer is TimerState.Overtime || timer is TimerState.Untimed)

/** Projection du minuteur, recalculée à chaque tic indépendamment du reste de l'écran. */
internal fun ExerciseModel.toTimerUi(
    engine: TimerEngine,
    now: Long,
): TimerUi = timerUi(timer, engine, now, untimedReady = untimed)

private val FALLBACK_DURATION = 5.minutes

/** Durée du décompte en ms et mode « sans minuteur », d'après la politique de l'exercice et les réglages. */
internal fun timerConfig(
    definition: ExerciseDefinition,
    settings: Settings,
): Pair<Long, Boolean> {
    val duration =
        settings.durations[definition.id] ?: definition.timer.defaultDuration
            ?: ExerciseId.DEFAULT_DURATIONS[definition.id] ?: FALLBACK_DURATION
    return duration.inWholeMilliseconds to (definition.timer is TimerPolicy.None || definition.id in settings.untimed)
}

/** Minuteur en pause reconstruit depuis l'enregistrement quand aucun instantané n'a survécu. */
internal fun pausedTimerFor(record: SessionRecord): TimerState =
    pausedTimerFrom(record.plannedDurationSec, record.activeDurationSec)
