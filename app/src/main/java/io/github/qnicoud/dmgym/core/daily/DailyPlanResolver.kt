package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

/**
 * Séance du jour telle que l'utilisateur la voit : le plan déterministe de [planSource] avec les remplacements
 * du jour mémorisés dans les réglages. Partagé par l'aperçu de la séance (S03) et sa fin (S07).
 */
class DailyPlanResolver(
    private val settings: SettingsRepository,
    private val planSource: DailyPlanSource,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(today: Flow<Long>): Flow<DailyPlan> {
        val onePage = settings.settings.map { it.dailyIncludeOnePage }.distinctUntilChanged()
        val base =
            combine(today, onePage) { day, includeOnePage -> day to DailyOptions(includeOnePage) }
                .mapLatest { (day, options) -> planSource.planFor(day, ExerciseId.ALL, options) }
        return combine(base, settings.settings) { plan, current ->
            plan.withReplacements(current.replacementsFor(plan.epochDay))
        }
    }
}
