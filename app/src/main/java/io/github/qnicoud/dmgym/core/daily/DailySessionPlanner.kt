package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import kotlin.random.Random

/**
 * Planificateur pur de la séance du jour (ADR 0011, D20). La graine est la date : même jour, même séance,
 * sans rien stocker. Choix dans l'ordre : zone faible, échauffement, cœur.
 */
object DailySessionPlanner {
    private const val WARM_UP = 0
    private const val CORE = 1
    private const val WEAK = 2

    /**
     * @param previous séance de la veille : on évite le même exercice à la même position, sauf si c'est impossible.
     * @throws IllegalArgumentException si moins de trois exercices sont disponibles.
     */
    fun plan(
        today: Long,
        stats: PracticeStats,
        catalog: List<ExerciseId>,
        options: DailyOptions,
        previous: DailyPlan? = null,
    ): DailyPlan {
        val pool = catalog.distinct().filter { options.includeOnePage || it != ExerciseId.ONE_PAGE }
        require(pool.size >= DailyPlan.SIZE) { "at least ${DailyPlan.SIZE} exercises are needed, got ${pool.size}" }
        val random = Random(today)
        val before = previous?.exercises.orEmpty()

        val weak = pick(listOf(leastPracticed(pool, stats)), before.getOrNull(WEAK), random)
        val warmUps = (pool - weak).filter { it in ExerciseId.DAILY_WARM_UPS }
        val warm = pick(listOf(warmUps, pool - weak), before.getOrNull(WARM_UP), random)
        val rest = pool - weak - warm
        val core = pick(listOf(rest.filter { it !in ExerciseId.DAILY_WARM_UPS }, rest), before.getOrNull(CORE), random)
        return DailyPlan(today, listOf(warm, core, weak))
    }

    private fun leastPracticed(
        pool: List<ExerciseId>,
        stats: PracticeStats,
    ): List<ExerciseId> {
        val min = pool.minOf { stats.countOf(it) }
        return pool.filter { stats.countOf(it) == min }
    }

    /** Premier niveau non vide, en écartant [avoid] quand un autre choix existe. */
    private fun pick(
        tiers: List<List<ExerciseId>>,
        avoid: ExerciseId?,
        random: Random,
    ): ExerciseId {
        val tier = tiers.first { it.isNotEmpty() }
        val options = tier.filter { it != avoid }.ifEmpty { tier }
        return options[random.nextInt(options.size)]
    }
}
