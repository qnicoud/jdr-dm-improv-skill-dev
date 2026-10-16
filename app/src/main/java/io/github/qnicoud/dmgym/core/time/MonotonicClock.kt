package io.github.qnicoud.dmgym.core.time

/** Horloge monotone (ADR 0006) : millisecondes depuis le démarrage de l'appareil, veille comprise. */
fun interface MonotonicClock {
    fun now(): Long
}
