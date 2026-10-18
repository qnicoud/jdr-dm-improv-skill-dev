package io.github.qnicoud.dmgym.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.SessionDetail
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * État de l'accueil. Tant que [isLoading] est vrai, ni le compteur ni l'état vide ne sont affichés :
 * « 0 session » n'est une vraie valeur qu'une fois l'historique lu.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val sessionCount: Int = 0,
    val exercises: List<ExerciseSummary> = emptyList(),
    /** Brouillon à reprendre (moins de 24 h, avec contenu), `null` s'il n'y en a pas. */
    val resume: ResumeUi? = null,
) {
    val isEmpty: Boolean get() = !isLoading && sessionCount == 0
}

/** Carte « Reprendre » (D36) : la session en cours et un extrait de ce qui a été écrit. */
data class ResumeUi(
    val sessionId: Long,
    val exerciseId: ExerciseId,
    val preview: String,
    val lastActivityAtEpochMillis: Long,
)

class HomeViewModel(
    catalog: ExerciseCatalog,
    history: HistoryRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            catalog.exercises,
            history.observeSessionCount(),
            history.observeResumable(),
            history.observeHistory().map { sessions -> sessions.map { it.exerciseId }.toSet() },
        ) { exercises, count, resumable, practised ->
            HomeUiState(
                isLoading = false,
                sessionCount = count,
                exercises = exercises.map { it.withNewTag(practised) },
                resume = resumable?.toResume(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun ExerciseSummary.withNewTag(practised: Set<String>): ExerciseSummary =
    copy(isNew = id in ExerciseId.NEW_IN_LATEST_RELEASE && id.value !in practised)

private fun SessionDetail.toResume(): ResumeUi {
    val text =
        answers
            .sortedWith(compareBy({ it.groupIndex }, { it.fieldKey }))
            .firstOrNull { it.text.isNotBlank() }
            ?.text
            ?.trim()
            .orEmpty()
    return ResumeUi(session.id, ExerciseId(session.exerciseId), text, session.lastActivityAtEpochMillis)
}
