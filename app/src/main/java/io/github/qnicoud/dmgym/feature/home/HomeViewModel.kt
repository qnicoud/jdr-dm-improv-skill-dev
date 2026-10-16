package io.github.qnicoud.dmgym.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * État de l'accueil. Tant que [isLoading] est vrai, ni le compteur ni l'état vide ne sont affichés :
 * « 0 session » n'est une vraie valeur qu'une fois l'historique lu.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val sessionCount: Int = 0,
    val exercises: List<ExerciseSummary> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && sessionCount == 0
}

class HomeViewModel(
    catalog: ExerciseCatalog,
    history: HistoryRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(catalog.exercises, history.observeSessionCount()) { exercises, count ->
            HomeUiState(isLoading = false, sessionCount = count, exercises = exercises)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
