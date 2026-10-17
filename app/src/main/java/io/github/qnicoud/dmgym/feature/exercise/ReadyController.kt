package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.timer.DurationBounds
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Écran Prêt : durée, « sans minuteur », options et écriture différée de ces choix dans les réglages. */
internal class ReadyController(
    private val definition: ExerciseDefinition,
    private val model: MutableStateFlow<ExerciseModel>,
    private val writer: ReadySettingsWriter,
    private val drawing: DrawCoordinator,
    private val detection: DetectionCoordinator,
    private val scope: CoroutineScope,
    private val dispatch: (ExerciseAction) -> Unit,
) {
    /** Change une option ; avant le départ, les tirages qui en dépendent (mode, contre-stéréotype) sont refaits. */
    fun changeOption(
        key: String,
        value: Boolean,
    ) {
        dispatch(ExerciseAction.OptionChanged(key, value))
        if (model.value.session.phase == Phase.Ready) {
            scope.launch { if (drawing.redrawForOption(key)) detection.prepare() }
        }
    }

    fun increaseDuration() = step { bounds, current -> bounds.increase(current) }

    fun decreaseDuration() = step { bounds, current -> bounds.decrease(current) }

    private fun step(move: (DurationBounds, Duration) -> Duration) {
        val idle = model.value.timer as? TimerState.Idle ?: return
        val bounds = durationBoundsOf(definition) ?: return
        if (model.value.session.phase != Phase.Ready) return
        val duration = move(bounds, idle.durationMs.milliseconds)
        model.update { it.copy(timer = TimerState.Idle(duration.inWholeMilliseconds)) }
        writer.durationChanged()
    }

    fun changeUntimed(untimed: Boolean) {
        if (durationBoundsOf(definition) == null || model.value.session.phase != Phase.Ready) return
        model.update { it.copy(untimed = untimed) }
        writer.untimedChanged()
    }

    /** Écrit sans attendre le regroupement des appuis. */
    fun flush() = writer.flush()

    fun flushOnClear() = writer.flushOnClear()
}
