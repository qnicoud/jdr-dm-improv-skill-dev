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

    /** Pause du temps additionnel : seul le dépassement déjà écoulé est conservé. */
    data class OvertimePaused(
        val overtimeMs: Long,
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

/**
 * Photographie d'un état sur l'heure murale, à placer dans le `SavedStateHandle` (ADR 0006) :
 * les instants monotones ne survivent pas à la mort du processus.
 */
sealed interface TimerSnapshot {
    data class Idle(
        val durationMs: Long,
    ) : TimerSnapshot

    data class Running(
        val endAtWallMillis: Long,
        val durationMs: Long,
    ) : TimerSnapshot

    data class Paused(
        val remainingMs: Long,
        val durationMs: Long,
    ) : TimerSnapshot

    data class Overtime(
        val endAtWallMillis: Long,
        val durationMs: Long,
    ) : TimerSnapshot

    data class OvertimePaused(
        val overtimeMs: Long,
        val durationMs: Long,
    ) : TimerSnapshot

    data class Untimed(
        val startedAtWallMillis: Long,
    ) : TimerSnapshot

    data class UntimedPaused(
        val elapsedMs: Long,
    ) : TimerSnapshot
}
