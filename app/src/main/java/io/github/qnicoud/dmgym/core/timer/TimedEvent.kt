package io.github.qnicoud.dmgym.core.timer

/** Événement programmé d'un exercice, relatif au début du minuteur (ADR 0006, point 9). */
sealed interface TimedEvent {
    /** Rappel toutes les `intervalMs` (ex. Crazy Eights, 60 s). */
    data class Periodic(
        val intervalMs: Long,
        val haptic: Boolean,
    ) : TimedEvent {
        init {
            require(intervalMs > 0) { "interval must be positive" }
        }
    }

    /** Événement unique, tiré entre `fromFraction` et `toFraction` de la durée (ex. bascule de Status). */
    data class Once(
        val fromFraction: Double,
        val toFraction: Double,
    ) : TimedEvent {
        init {
            require(fromFraction in 0.0..1.0 && toFraction in fromFraction..1.0) { "invalid fraction range" }
        }
    }
}

/** Occurrence planifiée : décalage en ms depuis le début du chrono. */
data class PlannedEvent(
    val event: TimedEvent,
    val offsetMs: Long,
)

/** Occurrence franchie ; `stale` vaut vrai si elle a été constatée plus de 2 s après son instant. */
data class FiredEvent(
    val event: TimedEvent,
    val offsetMs: Long,
    val stale: Boolean,
)
