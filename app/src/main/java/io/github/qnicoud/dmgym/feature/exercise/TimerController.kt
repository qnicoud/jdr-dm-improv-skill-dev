package io.github.qnicoud.dmgym.feature.exercise

import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.durationMs
import io.github.qnicoud.dmgym.core.exercise.toLongs
import io.github.qnicoud.dmgym.core.timer.TickResult
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Minuteur de la séance : tics, pause et reprise, instantané dans le [SavedStateHandle] et planification des
 * événements. Il lit et écrit le minuteur du [model] ; les annonces passent par [announcer].
 */
internal class TimerController(
    private val engine: TimerEngine,
    private val deps: ExerciseDependencies,
    private val handle: SavedStateHandle,
    private val model: MutableStateFlow<ExerciseModel>,
    private val scheduled: ScheduledEventRunner,
    private val announcer: TimerAnnouncer,
) {
    /** Un tic de l'horloge : fait avancer le minuteur, les annonces et les événements planifiés. */
    fun onTick(now: Long) {
        val m = model.value
        if (m.session.phase != Phase.Active) return
        val result = engine.tick(m.timer, now)
        if (result.state != m.timer) update(result.state)
        announcer.onExpired(result.expired)
        scheduled.onTick(result.state, now)
        announcer.announce(result.state, now)
    }

    fun pause() = change { engine.pause(it, deps.monotonicClock.now()) }

    fun resume() = change { TickResult(engine.resume(it, deps.monotonicClock.now())) }

    /** Lance le chrono ([plannedMs] `null` : sans minuteur). */
    fun start(
        plannedMs: Long?,
        now: Long,
    ) {
        val timer = if (plannedMs == null) engine.startUntimed(now) else engine.start(plannedMs, now)
        scheduled.reset(plannedMs, startElapsedMs = 0)
        update(timer)
    }

    /** Fige le chrono ; l'éventuel signal de fin est volontairement ignoré (terminer n'est pas une échéance). */
    fun freeze() = update(engine.pause(model.value.timer, deps.monotonicClock.now()).state)

    /** Reprend le chrono d'une séance restaurée, sans rejouer ce qui a déjà été annoncé ni déclenché. */
    fun restore(timer: TimerState) {
        val now = deps.monotonicClock.now()
        scheduled.reset(timer.durationMs(), engine.elapsedMs(timer, now))
        announcer.resumeAt(timer, now)
        update(timer)
    }

    /** Temps actif et dépassement à enregistrer. */
    fun times(): SessionTimes {
        val state = model.value.timer
        val now = deps.monotonicClock.now()
        return SessionTimes(activeMs = engine.elapsedMs(state, now), overtimeMs = engine.overtimeMs(state, now))
    }

    private fun change(transform: (TimerState) -> TickResult) {
        val m = model.value
        if (m.session.phase != Phase.Active || (m.timer is TimerState.Idle)) return
        val result = transform(m.timer)
        update(result.state)
        announcer.onExpired(result.expired)
    }

    private fun update(state: TimerState) {
        model.update { it.copy(timer = state) }
        val snapshot = engine.snapshot(state, deps.wallClock.millis(), deps.monotonicClock.now())
        handle[HandleKeys.TIMER] = snapshot.toLongs()
    }
}
