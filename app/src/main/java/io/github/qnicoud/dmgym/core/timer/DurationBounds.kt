package io.github.qnicoud.dmgym.core.timer

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Bornes de durée réglable d'un exercice : `[max(15 s, défaut/2) ; 10 × défaut]` (ADR 0006). */
data class DurationBounds(
    val min: Duration,
    val max: Duration,
) {
    fun clamp(value: Duration): Duration = value.coerceIn(min, max)

    operator fun contains(value: Duration): Boolean = value in min..max

    /** Pas de réglage : 15 s jusqu'à 2 min incluses, 1 min au-delà. */
    fun stepFor(value: Duration): Duration = if (value <= STEP_THRESHOLD) SMALL_STEP else LARGE_STEP

    fun increase(value: Duration): Duration = clamp(value + stepFor(value))

    fun decrease(value: Duration): Duration = clamp(value - stepFor(value))

    companion object {
        private val MIN_FLOOR = 15.seconds
        private val STEP_THRESHOLD = 2.minutes
        private val SMALL_STEP = 15.seconds
        private val LARGE_STEP = 1.minutes
        private const val MAX_FACTOR = 10

        fun of(default: Duration): DurationBounds {
            require(default.isPositive()) { "default duration must be positive" }
            return DurationBounds(min = maxOf(MIN_FLOOR, default / 2), max = default * MAX_FACTOR)
        }
    }
}
