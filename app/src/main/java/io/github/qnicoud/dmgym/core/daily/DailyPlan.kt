package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId

/** Séance du jour : trois exercices distincts, dans l'ordre échauffement, cœur, zone faible. */
data class DailyPlan(
    val epochDay: Long,
    val exercises: List<ExerciseId>,
) {
    companion object {
        const val SIZE = 3
    }
}

/** Pratique des 14 jours **précédant** la séance (jamais celle du jour : le plan reste stable dans la journée). */
data class PracticeStats(
    val countsLast14Days: Map<ExerciseId, Int>,
) {
    fun countOf(id: ExerciseId): Int = countsLast14Days[id] ?: 0

    companion object {
        const val WINDOW_DAYS = 14L
    }
}

data class DailyOptions(
    val includeOnePage: Boolean = false,
)
