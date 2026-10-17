package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.DueEvent
import io.github.qnicoud.dmgym.core.exercise.EventPlan
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.ScheduledEvent
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.tables.NotEnoughEntriesException
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Déclenche les événements programmés (rappel périodique, bascule) au fil des tics du minuteur. */
internal class ScheduledEventRunner(
    private val engine: TimerEngine,
    private val events: List<ScheduledEvent>,
    private val random: Random,
    private val drawer: SlotDrawer,
    private val scope: CoroutineScope,
    private val out: Channel<ExerciseEvent>,
    private val dispatch: (ExerciseAction) -> Unit,
    private val options: () -> Map<String, Boolean>,
    private val firedEvents: () -> Set<String>,
) {
    private var plan = EventPlan(engine, emptyList(), null, random)
    private var lastElapsedMs: Long? = null

    /** Replanifie pour une durée de [durationMs] (`null` : sans minuteur) à partir de [startElapsedMs]. */
    fun reset(
        durationMs: Long?,
        startElapsedMs: Long,
    ) {
        plan = EventPlan(engine, events, durationMs, random)
        lastElapsedMs = startElapsedMs
    }

    /** Rien n'est déclenché pendant une pause ; un rappel manqué n'est jamais rattrapé. */
    fun onTick(
        state: TimerState,
        now: Long,
    ) {
        if (state is TimerState.Paused || state is TimerState.OvertimePaused ||
            state is TimerState.UntimedPaused
        ) {
            return
        }
        val elapsed = engine.elapsedMs(state, now)
        val previous = lastElapsedMs
        lastElapsedMs = elapsed
        plan.due(previous, elapsed).forEach { fire(it) }
    }

    private fun fire(due: DueEvent) {
        val event = due.event
        if (event.requiresOption?.let { options()[it] != true } == true) return
        when (val timing = event.timing) {
            is TimedEvent.Periodic -> if (!due.stale) out.trySend(ExerciseEvent.Reminder(event.id, timing.haptic))
            is TimedEvent.Once -> if (event.id !in firedEvents()) fireOnce(event, due.stale)
        }
    }

    private fun fireOnce(
        event: ScheduledEvent,
        stale: Boolean,
    ) {
        scope.launch {
            val draws =
                try {
                    event.draw?.let { drawer.draw(it) }.orEmpty()
                } catch (_: NotEnoughEntriesException) {
                    emptyList()
                } catch (_: TableLoadException) {
                    emptyList()
                }
            dispatch(ExerciseAction.EventFired(event.id, draws))
            if (!stale) draws.firstOrNull()?.let { out.trySend(ExerciseEvent.Shift(it.text)) }
        }
    }
}
