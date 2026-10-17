package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.timer.TimerSnapshot

/**
 * Codec `TimerSnapshot` <-> `LongArray` (un `SavedStateHandle` ne sait pas ranger nos types scellés).
 * Format : `[type, a, b]`, `b` valant 0 quand il est inutile.
 */
fun TimerSnapshot.toLongs(): LongArray =
    when (this) {
        is TimerSnapshot.Idle -> longArrayOf(IDLE, durationMs, 0)
        is TimerSnapshot.Running -> longArrayOf(RUNNING, endAtWallMillis, durationMs)
        is TimerSnapshot.Paused -> longArrayOf(PAUSED, remainingMs, durationMs)
        is TimerSnapshot.Overtime -> longArrayOf(OVERTIME, endAtWallMillis, durationMs)
        is TimerSnapshot.OvertimePaused -> longArrayOf(OVERTIME_PAUSED, overtimeMs, durationMs)
        is TimerSnapshot.Untimed -> longArrayOf(UNTIMED, startedAtWallMillis, 0)
        is TimerSnapshot.UntimedPaused -> longArrayOf(UNTIMED_PAUSED, elapsedMs, 0)
    }

/** Inverse de [toLongs] ; `null` si [values] est absent ou illisible. */
fun timerSnapshotOf(values: LongArray?): TimerSnapshot? {
    if (values == null || values.size != SIZE) return null
    val (type, a, b) = values.toList()
    return when (type) {
        IDLE -> TimerSnapshot.Idle(a)
        RUNNING -> TimerSnapshot.Running(a, b)
        PAUSED -> TimerSnapshot.Paused(a, b)
        OVERTIME -> TimerSnapshot.Overtime(a, b)
        OVERTIME_PAUSED -> TimerSnapshot.OvertimePaused(a, b)
        UNTIMED -> TimerSnapshot.Untimed(a)
        UNTIMED_PAUSED -> TimerSnapshot.UntimedPaused(a)
        else -> null
    }
}

private const val SIZE = 3
private const val IDLE = 0L
private const val RUNNING = 1L
private const val PAUSED = 2L
private const val OVERTIME = 3L
private const val OVERTIME_PAUSED = 4L
private const val UNTIMED = 5L
private const val UNTIMED_PAUSED = 6L
