package io.github.qnicoud.dmgym.core.settings

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import kotlin.time.Duration

/** Réglages utilisateur (ADR 0013). Une durée absente de [durations] signifie « durée par défaut ». */
data class Settings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val durations: Map<ExerciseId, Duration> = emptyMap(),
    val untimed: Set<ExerciseId> = emptySet(),
    val dailyIncludeOnePage: Boolean = false,
    /** Vibration chaque minute pour Crazy Eights (désactivée par défaut, décision UX 14). */
    val crazyEightsMinuteHaptic: Boolean = false,
)
