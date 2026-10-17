package io.github.qnicoud.dmgym.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

/** Intitulé d'un groupe de jours : « Aujourd'hui », « Hier » ou une date (mise en forme par l'écran). */
sealed interface DayLabel {
    data object Today : DayLabel

    data object Yesterday : DayLabel

    data class Date(
        val epochDay: Long,
    ) : DayLabel
}

/** Une ligne de la liste. [unfinished] : session en cours restée sans activité plus de 24 h (D36). */
data class SessionItem(
    val id: Long,
    val exerciseId: ExerciseId,
    val startedAtEpochMillis: Long,
    val zoneId: String,
    val preview: String?,
    val isGem: Boolean,
    val selfRating: Int?,
    val unfinished: Boolean,
)

data class DayGroup(
    val label: DayLabel,
    val sessions: List<SessionItem>,
)

/**
 * État de l'historique. Tant que [isLoading] est vrai, ni l'état vide ni « aucun résultat » ne sont affichés.
 * [totalCount] distingue le carnet vide (aucune session) d'un filtre sans résultat.
 */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val filter: HistoryFilter = HistoryFilter.All,
    val groups: List<DayGroup> = emptyList(),
    val totalCount: Int = 0,
) {
    val isJournalEmpty: Boolean get() = !isLoading && totalCount == 0 && groups.isEmpty()
    val isFilterEmpty: Boolean get() = !isLoading && groups.isEmpty() && totalCount > 0
}

/** ViewModel de l'historique (S08) : liste par jour, filtrée par exercice ou par bijoux. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    history: HistoryRepository,
    private val clock: Clock,
    routeFilter: HistoryFilter = HistoryFilter.All,
    private val handle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    /** Le filtre choisi à l'écran l'emporte sur celui de la route, y compris après la mort du processus. */
    private val initialFilter = handle.get<String>(KEY_FILTER)?.let(::decodeFilter) ?: routeFilter
    private val filter = MutableStateFlow(initialFilter)

    val uiState: StateFlow<HistoryUiState> =
        combine(
            filter,
            filter.flatMapLatest { history.observeHistory(it.exerciseId?.value, it.gemsOnly) },
            history.observePreviews(),
            history.observeSessionCount(),
        ) { current, sessions, previews, total ->
            HistoryUiState(
                isLoading = false,
                filter = current,
                groups = groupByDay(sessions, previews, clock.localEpochDay()),
                totalCount = total,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            HistoryUiState(filter = initialFilter),
        )

    fun onFilterSelected(selected: HistoryFilter) {
        filter.value = selected
        handle[KEY_FILTER] = encodeFilter(selected)
    }

    fun onClearFilter() = onFilterSelected(HistoryFilter.All)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val KEY_FILTER = "filter"
        const val GEMS = "gems"
        const val EXERCISE_PREFIX = "exercise:"

        fun encodeFilter(filter: HistoryFilter): String =
            when {
                filter.gemsOnly -> GEMS
                filter.exerciseId != null -> EXERCISE_PREFIX + filter.exerciseId.value
                else -> ""
            }

        fun decodeFilter(raw: String): HistoryFilter =
            when {
                raw == GEMS -> HistoryFilter.Gems
                raw.startsWith(EXERCISE_PREFIX) -> HistoryFilter.exercise(ExerciseId(raw.removePrefix(EXERCISE_PREFIX)))
                else -> HistoryFilter.All
            }
    }
}

private fun groupByDay(
    sessions: List<SessionRecord>,
    previews: Map<Long, String>,
    today: Long,
): List<DayGroup> =
    sessions
        .groupBy { it.localEpochDay }
        .map { (day, list) -> DayGroup(dayLabel(day, today), list.map { it.toItem(previews[it.id]) }) }

private fun dayLabel(
    day: Long,
    today: Long,
): DayLabel =
    when (day) {
        today -> DayLabel.Today
        today - 1 -> DayLabel.Yesterday
        else -> DayLabel.Date(day)
    }

private fun SessionRecord.toItem(preview: String?) =
    SessionItem(
        id = id,
        exerciseId = ExerciseId(exerciseId),
        startedAtEpochMillis = startedAtEpochMillis,
        zoneId = zoneId,
        preview = preview,
        isGem = isGem,
        selfRating = selfRating,
        unfinished = status == SessionStatus.IN_PROGRESS,
    )
