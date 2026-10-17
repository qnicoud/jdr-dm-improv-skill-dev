package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.timer.PlannedEvent
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlin.random.Random

/** Occurrence franchie d'un [ScheduledEvent] ; [stale] vaut vrai si elle est constatée plus de 2 s après coup. */
data class DueEvent(
    val event: ScheduledEvent,
    val stale: Boolean,
)

/** Planning des événements programmés d'une séance (ADR 0006, point 9). Vide si l'exercice est sans minuteur. */
class EventPlan(
    private val engine: TimerEngine,
    events: List<ScheduledEvent>,
    durationMs: Long?,
    random: Random,
) {
    private val plans: List<Pair<ScheduledEvent, List<PlannedEvent>>> =
        if (durationMs == null) emptyList() else events.map { it to engine.plan(listOf(it.timing), durationMs, random) }

    /** Occurrences franchies entre deux évaluations ; rien tant que le chrono est en pause. */
    fun due(
        previousElapsedMs: Long?,
        elapsedMs: Long,
    ): List<DueEvent> =
        plans.flatMap { (event, plan) ->
            engine.due(plan, previousElapsedMs, elapsedMs).map { DueEvent(event, it.stale) }
        }
}

/**
 * État d'un minuteur reconstruit sans instantané (redémarrage de l'appareil, reprise depuis l'accueil) :
 * toujours en pause, à reprendre par l'utilisateur ; temps additionnel si l'échéance est passée.
 */
fun pausedTimerFrom(
    plannedSec: Int?,
    activeSec: Int,
): TimerState {
    val planned = plannedSec?.let { it * MS_PER_SEC }
    val active = activeSec * MS_PER_SEC
    return when {
        planned == null -> TimerState.UntimedPaused(active)
        active >= planned -> TimerState.OvertimePaused(active - planned, planned)
        else -> TimerState.Paused(planned - active, planned)
    }
}

private const val MS_PER_SEC = 1_000L
