package io.github.qnicoud.dmgym.core.timer

import kotlin.random.Random

/**
 * Moteur de minuteur pur (ADR 0006) : tout est calculé à partir d'un instant fourni, jamais décrémenté.
 * Sans état : chaque opération renvoie un nouvel état.
 */
class TimerEngine(
    private val staleAfterMs: Long = STALE_AFTER_MS,
) {
    fun idle(durationMs: Long): TimerState {
        require(durationMs > 0) { "duration must be positive" }
        return TimerState.Idle(durationMs)
    }

    fun start(
        durationMs: Long,
        now: Long,
    ): TimerState {
        require(durationMs > 0) { "duration must be positive" }
        return TimerState.Running(endAtElapsed = now + durationMs, durationMs = durationMs)
    }

    fun startUntimed(now: Long): TimerState = TimerState.Untimed(startedAtElapsed = now)

    fun tick(
        state: TimerState,
        now: Long,
    ): TickResult {
        if (state !is TimerState.Running || now < state.endAtElapsed) return TickResult(state)
        val expired = TimerEvent.Expired(state.endAtElapsed, fresh = now - state.endAtElapsed <= staleAfterMs)
        return TickResult(TimerState.Overtime(state.endAtElapsed, state.durationMs), expired)
    }

    fun pause(
        state: TimerState,
        now: Long,
    ): TimerState =
        when (state) {
            is TimerState.Running -> TimerState.Paused((state.endAtElapsed - now).coerceAtLeast(0), state.durationMs)
            is TimerState.Untimed -> TimerState.UntimedPaused(now - state.startedAtElapsed)
            else -> state
        }

    fun resume(
        state: TimerState,
        now: Long,
    ): TimerState =
        when (state) {
            is TimerState.Paused -> TimerState.Running(now + state.remainingMs, state.durationMs)
            is TimerState.UntimedPaused -> TimerState.Untimed(now - state.elapsedMs)
            else -> state
        }

    /**
     * Reconstruit l'état après la mort du processus : `endAtElapsed = elapsedNow + (endAtWall − wallNow)`.
     * Une échéance déjà dépassée donne directement `Overtime`, sans événement (signal périmé).
     */
    fun restore(
        endAtWallMillis: Long,
        durationMs: Long,
        wallNow: Long,
        elapsedNow: Long,
    ): TimerState {
        val endAtElapsed = elapsedNow + (endAtWallMillis - wallNow)
        return if (elapsedNow >= endAtElapsed) {
            TimerState.Overtime(endAtElapsed, durationMs)
        } else {
            TimerState.Running(endAtElapsed, durationMs)
        }
    }

    fun remainingMs(
        state: TimerState,
        now: Long,
    ): Long =
        when (state) {
            is TimerState.Idle -> state.durationMs
            is TimerState.Running -> (state.endAtElapsed - now).coerceAtLeast(0)
            is TimerState.Paused -> state.remainingMs
            is TimerState.Overtime, is TimerState.Untimed, is TimerState.UntimedPaused -> 0
        }

    fun overtimeMs(
        state: TimerState,
        now: Long,
    ): Long = if (state is TimerState.Overtime) (now - state.endAtElapsed).coerceAtLeast(0) else 0

    fun elapsedMs(
        state: TimerState,
        now: Long,
    ): Long =
        when (state) {
            is TimerState.Idle -> 0
            is TimerState.Running -> state.durationMs - remainingMs(state, now)
            is TimerState.Paused -> state.durationMs - state.remainingMs
            is TimerState.Overtime -> state.durationMs + overtimeMs(state, now)
            is TimerState.Untimed -> (now - state.startedAtElapsed).coerceAtLeast(0)
            is TimerState.UntimedPaused -> state.elapsedMs
        }

    /** Planifie les occurrences des événements pour une durée donnée, triées par décalage. */
    fun plan(
        events: List<TimedEvent>,
        durationMs: Long,
        random: Random,
    ): List<PlannedEvent> {
        if (durationMs <= 0) return emptyList()
        return events
            .flatMap { event ->
                when (event) {
                    is TimedEvent.Periodic -> {
                        generateSequence(event.intervalMs) { it + event.intervalMs }
                            .takeWhile { it < durationMs }
                            .map { PlannedEvent(event, it) }
                            .toList()
                    }

                    is TimedEvent.Once -> {
                        listOf(PlannedEvent(event, onceOffset(event, durationMs, random)))
                    }
                }
            }.sortedBy { it.offsetMs }
    }

    /**
     * Occurrences franchies entre deux évaluations. Les rappels périodiques manqués sont réduits à la
     * dernière occurrence (jamais rattrapés) ; un événement unique manqué est rendu, signalé `stale`.
     */
    fun due(
        plan: List<PlannedEvent>,
        previousElapsedMs: Long,
        elapsedMs: Long,
    ): List<FiredEvent> {
        val crossed = plan.filter { it.offsetMs > previousElapsedMs && it.offsetMs <= elapsedMs }
        val latestPeriodic =
            crossed
                .filter {
                    it.event is TimedEvent.Periodic
                }.groupBy { it.event }
                .mapValues { it.value.last() }
        return crossed
            .filter { it.event !is TimedEvent.Periodic || latestPeriodic[it.event] == it }
            .map { FiredEvent(it.event, it.offsetMs, stale = elapsedMs - it.offsetMs > staleAfterMs) }
    }

    private fun onceOffset(
        event: TimedEvent.Once,
        durationMs: Long,
        random: Random,
    ): Long {
        val fraction = event.fromFraction + random.nextDouble() * (event.toFraction - event.fromFraction)
        return (durationMs * fraction).toLong()
    }

    companion object {
        const val STALE_AFTER_MS = 2_000L
    }
}
