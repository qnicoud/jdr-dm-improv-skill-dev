package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.HistoryRepository

/**
 * Calcule la séance du jour à partir de l'historique **antérieur à aujourd'hui** uniquement, ce qui la rend
 * stable dans la journée. La séance de la veille est recalculée pour appliquer la règle « pas le même exercice
 * à la même position » (architecture §10).
 */
class DailyPlanSource(
    private val history: HistoryRepository,
) {
    suspend fun planFor(
        today: Long,
        catalog: List<ExerciseId>,
        options: DailyOptions,
    ): DailyPlan {
        val yesterday = DailySessionPlanner.plan(today - 1, statsBefore(today - 1), catalog, options)
        return DailySessionPlanner.plan(today, statsBefore(today), catalog, options, yesterday)
    }

    private suspend fun statsBefore(day: Long): PracticeStats {
        val counts = history.countSessionsByExercise(day - PracticeStats.WINDOW_DAYS, day - 1)
        return PracticeStats(counts.associate { ExerciseId(it.exerciseId) to it.count })
    }
}
