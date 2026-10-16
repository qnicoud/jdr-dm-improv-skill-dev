package io.github.qnicoud.dmgym.core.timer

/** États du minuteur (ADR 0006). Tous les instants sont en millisecondes sur l'horloge monotone. */
sealed interface TimerState {
    data class Idle(
        val durationMs: Long,
    ) : TimerState

    data class Running(
        val endAtElapsed: Long,
        val durationMs: Long,
    ) : TimerState

    /** Pause d'un décompte : seul le temps restant est conservé. */
    data class Paused(
        val remainingMs: Long,
        val durationMs: Long,
    ) : TimerState

    data class Overtime(
        val endAtElapsed: Long,
        val durationMs: Long,
    ) : TimerState

    data class Untimed(
        val startedAtElapsed: Long,
    ) : TimerState

    data class UntimedPaused(
        val elapsedMs: Long,
    ) : TimerState
}

/** Événement émis par le moteur, une seule fois. */
sealed interface TimerEvent {
    /** `fresh` est faux si l'échéance a été constatée plus de 2 s après coup : pas de signal. */
    data class Expired(
        val endAtElapsed: Long,
        val fresh: Boolean,
    ) : TimerEvent
}

data class TickResult(
    val state: TimerState,
    val expired: TimerEvent.Expired? = null,
)
