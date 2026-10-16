package io.github.qnicoud.dmgym.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * État de l'accueil. [sessionCount] reste à 0 tant que l'historique (lot L6) n'est pas branché.
 */
data class HomeUiState(
    val sessionCount: Int = 0,
    val exercises: List<ExerciseSummary> = emptyList(),
) {
    val isEmpty: Boolean get() = sessionCount == 0
}

class HomeViewModel(
    catalog: ExerciseCatalog,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        catalog.exercises
            .map { HomeUiState(exercises = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
