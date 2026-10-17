package io.github.qnicoud.dmgym.feature.exercise

import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.durationMs
import io.github.qnicoud.dmgym.core.exercise.milestonesReached
import io.github.qnicoud.dmgym.core.exercise.timerUi
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerEvent
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Jalons annoncés une seule fois et signal de fin joué une seule fois, même après une reprise (ADR 0006). */
internal class TimerAnnouncer(
    private val engine: TimerEngine,
    private val deps: ExerciseDependencies,
    private val handle: SavedStateHandle,
    private val scope: CoroutineScope,
    private val out: Channel<ExerciseEvent>,
) {
    private var reached: Set<Milestone> = emptySet()
    private var signalDelivered = handle.get<Boolean>(KEY_SIGNAL) ?: false

    /** Joue le signal de fin si l'échéance est fraîche et qu'il n'a pas déjà sonné. */
    fun onExpired(expired: TimerEvent.Expired?) {
        if (expired == null || !expired.fresh || signalDelivered) return
        signalDelivered = true
        handle[KEY_SIGNAL] = true
        scope.launch { deps.endSignal.play(deps.settings.settings.first()) }
        out.trySend(ExerciseEvent.Announce(Milestone.TimeUp))
    }

    fun announce(
        state: TimerState,
        now: Long,
    ) {
        val durationMs = state.durationMs() ?: return
        val fresh = milestonesReached(timerUi(state, engine, now, untimedReady = false), durationMs) - reached
        reached = reached + fresh
        fresh.forEach { out.trySend(ExerciseEvent.Announce(it)) }
    }

    /** Reprise : ce qui est déjà passé n'est plus annoncé, et le signal ne sonne pas pour un temps déjà dépassé. */
    fun resumeAt(
        state: TimerState,
        now: Long,
    ) {
        signalDelivered = signalDelivered || state is TimerState.Overtime || state is TimerState.OvertimePaused
        reached =
            state.durationMs()?.let { milestonesReached(timerUi(state, engine, now, false), it) }.orEmpty()
    }

    private companion object {
        const val KEY_SIGNAL = "signal_delivered"
    }
}
