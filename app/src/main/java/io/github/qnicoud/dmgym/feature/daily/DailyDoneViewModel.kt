package io.github.qnicoud.dmgym.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.daily.DailyDoneLine
import io.github.qnicoud.dmgym.core.daily.DailyPlanResolver
import io.github.qnicoud.dmgym.core.daily.DailyPlanSource
import io.github.qnicoud.dmgym.core.daily.dailyDoneLines
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.streak.computeStreak
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

/**
 * Fin de séance du jour (S07). Tant que [isLoading] est vrai, ni le récapitulatif ni les compteurs ne sont
 * affichés : « 0 session » n'est une vraie valeur qu'une fois l'historique lu.
 */
data class DailyDoneUiState(
    val isLoading: Boolean = true,
    val lines: List<DailyDoneLine> = emptyList(),
    val sessionCount: Int = 0,
    val streakDays: Int = 0,
) {
    val doneCount: Int get() = lines.count { it.done }
    val allDone: Boolean get() = lines.isNotEmpty() && lines.all { it.done }
}

/** Récapitulatif de la séance du jour : mêmes exercices que S03, niveau et bijou lus dans l'historique. */
class DailyDoneViewModel(
    history: HistoryRepository,
    settings: SettingsRepository,
    clock: Clock,
    planSource: DailyPlanSource = DailyPlanSource(history),
) : ViewModel() {
    private val today = MutableStateFlow(clock.localEpochDay())

    val uiState: StateFlow<DailyDoneUiState> =
        combine(
            DailyPlanResolver(settings, planSource).observe(today),
            history.observeHistory(),
            history.observeSessionCount(),
            history.observePracticedDays(),
        ) { plan, sessions, count, days ->
            DailyDoneUiState(
                isLoading = false,
                lines = dailyDoneLines(plan.exercises, plan.epochDay, sessions),
                sessionCount = count,
                streakDays = computeStreak(days.toSet(), count, plan.epochDay).current,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DailyDoneUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
