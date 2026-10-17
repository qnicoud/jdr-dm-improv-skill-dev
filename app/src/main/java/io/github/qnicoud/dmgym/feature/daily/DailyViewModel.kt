package io.github.qnicoud.dmgym.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.daily.DailyOptions
import io.github.qnicoud.dmgym.core.daily.DailyPlan
import io.github.qnicoud.dmgym.core.daily.DailyPlanSource
import io.github.qnicoud.dmgym.core.daily.DailyProgress
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.core.daily.dailyProgress
import io.github.qnicoud.dmgym.core.daily.replacementsFor
import io.github.qnicoud.dmgym.core.daily.withReplacement
import io.github.qnicoud.dmgym.core.daily.withReplacements
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.time.localEpochDay
import io.github.qnicoud.dmgym.feature.home.ExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.ExerciseSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

/** Une ligne de la séance du jour (S03). [canReplace] : « Remplacer » n'existe que sur « À faire ». */
data class DailyItem(
    val position: Int,
    val id: ExerciseId,
    val durationSeconds: Int?,
    val progress: ExerciseProgress,
) {
    val canReplace: Boolean get() = progress == ExerciseProgress.TODO
}

/** État de la séance du jour ; partagé par l'écran S03 et la carte de l'accueil. */
data class DailyUiState(
    val isLoading: Boolean = true,
    val epochDay: Long = 0,
    val items: List<DailyItem> = emptyList(),
    /** Exercices hors séance, proposés par la feuille « Remplacer par… » (One-Page inclus). */
    val alternatives: List<ExerciseSummary> = emptyList(),
    val nextExercise: ExerciseId? = null,
    val started: Boolean = false,
    val allDone: Boolean = false,
) {
    /** Durée totale arrondie à la minute (« ≈ 14 min »). */
    val totalMinutes: Int get() = (items.sumOf { it.durationSeconds ?: 0 } + HALF_MINUTE) / SECONDS_PER_MINUTE

    private companion object {
        const val HALF_MINUTE = 30
        const val SECONDS_PER_MINUTE = 60
    }
}

/**
 * Séance du jour (ADR 0011, architecture §10) : plan déterministe, remplacements du jour dans les réglages
 * et avancement déduit de l'historique du jour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModel(
    history: HistoryRepository,
    private val settings: SettingsRepository,
    catalog: ExerciseCatalog,
    private val clock: Clock,
    planSource: DailyPlanSource = DailyPlanSource(history),
) : ViewModel() {
    private val today = MutableStateFlow(clock.localEpochDay())

    private val basePlan =
        combine(today, settings.settings.map { it.dailyIncludeOnePage }.distinctUntilChanged()) { day, onePage ->
            day to DailyOptions(includeOnePage = onePage)
        }.mapLatest { (day, options) -> planSource.planFor(day, ExerciseId.ALL, options) }

    val uiState: StateFlow<DailyUiState> =
        combine(
            basePlan,
            settings.settings,
            catalog.exercises,
            history.observeHistory(),
        ) { plan, current, exercises, sessions ->
            val resolved = plan.withReplacements(current.replacementsFor(plan.epochDay))
            toUiState(resolved, exercises, dailyProgress(resolved.exercises, plan.epochDay, sessions))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DailyUiState())

    /** À appeler à la reprise de l'écran : le jour a pu changer pendant qu'il était en arrière-plan (D16). */
    fun refreshToday() {
        today.value = clock.localEpochDay()
    }

    /** Remplace l'exercice de [position] par [exercise], si celui-ci est encore à faire et n'est pas déjà dans la séance. */
    fun replace(
        position: Int,
        exercise: ExerciseId,
    ) {
        val state = uiState.value
        val item = state.items.getOrNull(position) ?: return
        if (state.isLoading || !item.canReplace || state.items.any { it.id == exercise }) return
        viewModelScope.launch { settings.update { it.withReplacement(state.epochDay, position, exercise) } }
    }

    private fun toUiState(
        plan: DailyPlan,
        exercises: List<ExerciseSummary>,
        progress: DailyProgress,
    ): DailyUiState {
        val byId = exercises.associateBy { it.id }
        val items =
            plan.exercises.mapIndexed { index, id ->
                DailyItem(index, id, byId[id]?.durationSeconds, progress.states[index])
            }
        return DailyUiState(
            isLoading = false,
            epochDay = plan.epochDay,
            items = items,
            alternatives = exercises.filter { it.id !in plan.exercises },
            nextExercise = progress.nextExercise,
            started = progress.started,
            allDone = progress.allDone,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
