package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlin.math.ceil

/** Les six états visuels du minuteur (design-system §5.3). */
enum class TimerKind { Ready, Running, LastMinute, Overtime, Paused, Untimed }

/**
 * Ce que l'écran affiche du minuteur. [displayMs] est le temps restant (arrondi à la seconde supérieure),
 * le temps additionnel ou le temps écoulé selon [kind] ; [overtime] vaut vrai pour une pause en temps additionnel.
 */
data class TimerUi(
    val kind: TimerKind,
    val displayMs: Long,
    val fraction: Float,
    val overtime: Boolean = false,
    val elapsedMs: Long = 0,
)

/** Jalons annoncés une seule fois (TalkBack, polite). */
enum class Milestone { Started, Halfway, LastMinute, TimeUp }

private const val LAST_MINUTE_MS = 60_000L
private const val LAST_MINUTE_MIN_DURATION_MS = 180_000L
private const val HALFWAY_MIN_DURATION_MS = 360_000L
private const val SECOND_MS = 1_000L

/** Projette l'état du minuteur à l'instant [now]. [untimedReady] couvre « Sans minuteur » avant le départ. */
fun timerUi(
    state: TimerState,
    engine: TimerEngine,
    now: Long,
    untimedReady: Boolean,
): TimerUi {
    val elapsed = engine.elapsedMs(state, now)
    return when (state) {
        is TimerState.Idle -> {
            if (untimedReady) {
                TimerUi(TimerKind.Untimed, 0, 0f)
            } else {
                TimerUi(TimerKind.Ready, state.durationMs, 1f)
            }
        }

        is TimerState.Running -> {
            val remaining = engine.remainingMs(state, now)
            val kind = if (isLastMinute(remaining, state.durationMs)) TimerKind.LastMinute else TimerKind.Running
            TimerUi(kind, ceilToSecond(remaining), remaining.toFloat() / state.durationMs, elapsedMs = elapsed)
        }

        is TimerState.Paused -> {
            TimerUi(
                TimerKind.Paused,
                ceilToSecond(state.remainingMs),
                state.remainingMs.toFloat() / state.durationMs,
                elapsedMs = elapsed,
            )
        }

        is TimerState.Overtime -> {
            TimerUi(TimerKind.Overtime, engine.overtimeMs(state, now), 0f, elapsedMs = elapsed)
        }

        is TimerState.OvertimePaused -> {
            TimerUi(TimerKind.Paused, state.overtimeMs, 0f, overtime = true, elapsedMs = elapsed)
        }

        is TimerState.Untimed -> {
            TimerUi(TimerKind.Untimed, elapsed, 0f, elapsedMs = elapsed)
        }

        is TimerState.UntimedPaused -> {
            TimerUi(TimerKind.Paused, state.elapsedMs, 0f, elapsedMs = elapsed)
        }
    }
}

/** Jalons atteints par l'état affiché (hors [Milestone.Started] et [Milestone.TimeUp], émis par le ViewModel). */
fun milestonesReached(
    ui: TimerUi,
    durationMs: Long,
): Set<Milestone> =
    buildSet {
        if (ui.kind == TimerKind.LastMinute) add(Milestone.LastMinute)
        val counting = ui.kind in setOf(TimerKind.Running, TimerKind.LastMinute, TimerKind.Overtime)
        if (counting && durationMs >= HALFWAY_MIN_DURATION_MS && ui.elapsedMs >= durationMs / 2) add(Milestone.Halfway)
    }

private fun isLastMinute(
    remainingMs: Long,
    durationMs: Long,
): Boolean = durationMs >= LAST_MINUTE_MIN_DURATION_MS && remainingMs <= LAST_MINUTE_MS

private fun ceilToSecond(ms: Long): Long = (ceil(ms.toDouble() / SECOND_MS) * SECOND_MS).toLong()

/** Durée configurée de l'état, ou `null` s'il n'a pas de décompte (avant le départ, sans minuteur). */
fun TimerState.durationMs(): Long? =
    when (this) {
        is TimerState.Running -> durationMs
        is TimerState.Paused -> durationMs
        is TimerState.Overtime -> durationMs
        is TimerState.OvertimePaused -> durationMs
        else -> null
    }
