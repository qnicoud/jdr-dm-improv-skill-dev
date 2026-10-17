package io.github.qnicoud.dmgym.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.streak.DayStatus
import io.github.qnicoud.dmgym.core.streak.StreakSummary
import io.github.qnicoud.dmgym.core.streak.computeStreak
import io.github.qnicoud.dmgym.core.streak.recentDays
import io.github.qnicoud.dmgym.core.streak.weekStart
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

data class ExerciseCount(
    val id: ExerciseId,
    val count: Int,
)

/**
 * État de Progrès (S10). Tant que [isLoading] est vrai, ni le compteur ni l'état vide ne sont affichés :
 * « 0 session » n'est une vraie valeur qu'une fois l'historique lu.
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    val today: Long = 0,
    val sessions: Int = 0,
    val sessionsThisWeek: Int = 0,
    val streak: StreakSummary = StreakSummary(0, 0, 0, 0, null, false),
    val recent: List<DayStatus> = emptyList(),
    val perExercise: List<ExerciseCount> = emptyList(),
    val gems: Int = 0,
    val totalSeconds: Long = 0,
) {
    val isEmpty: Boolean get() = !isLoading && sessions == 0

    /** « Nouvelle série : 1 jour » après une rupture, tant que la meilleure série reste plus longue. */
    val isStreakRestart: Boolean get() = streak.current == 1 && streak.best > 1
}

/** Calcule Progrès depuis l'historique ; `today` est relu à la reprise de l'écran, sans tic à minuit (architecture §9). */
class StatsViewModel(
    history: HistoryRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today = MutableStateFlow(clock.localEpochDay())

    val uiState: StateFlow<StatsUiState> =
        combine(
            history.observeSessionCount(),
            history.observePracticedDays(),
            history.observeHistory(),
            today,
        ) { count, days, sessions, day -> build(count, days.toSet(), sessions, day) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    /** À appeler à chaque reprise : le jour a pu changer (minuit, fuseau) pendant que l'écran était caché. */
    fun refreshToday() {
        today.value = clock.localEpochDay()
    }

    private fun build(
        count: Int,
        days: Set<Long>,
        sessions: List<SessionRecord>,
        day: Long,
    ): StatsUiState {
        val streak = computeStreak(days, count, day)
        return StatsUiState(
            isLoading = false,
            today = day,
            sessions = count,
            sessionsThisWeek = sessions.count { it.localEpochDay in weekStart(day)..day },
            streak = streak,
            recent = recentDays(streak, days, day),
            perExercise = rank(sessions),
            gems = sessions.count { it.isGem },
            totalSeconds = sessions.sumOf { it.activeDurationSec.toLong() },
        )
    }

    /** Du plus pratiqué au moins pratiqué (ordre de l'accueil à égalité) ; les exercices à zéro ne sont pas listés. */
    private fun rank(sessions: List<SessionRecord>): List<ExerciseCount> =
        ExerciseId.ALL
            .map { id -> ExerciseCount(id, sessions.count { it.exerciseId == id.value }) }
            .filter { it.count > 0 }
            .sortedByDescending { it.count }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
