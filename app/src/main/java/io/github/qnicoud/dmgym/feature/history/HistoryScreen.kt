package io.github.qnicoud.dmgym.feature.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.components.SettingsAction
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

object HistoryTags {
    const val CHIPS = "history_chips"
    const val EMPTY = "history_empty"
    const val EMPTY_ACTION = "history_empty_action"
    const val FILTER_EMPTY = "history_filter_empty"
    const val CLEAR_FILTER = "history_clear_filter"

    fun card(id: Long) = "history_card_$id"

    fun chip(filter: HistoryFilter) =
        when {
            filter.gemsOnly -> "history_chip_gems"
            filter.exerciseId != null -> "history_chip_${filter.exerciseId.value}"
            else -> "history_chip_all"
        }
}

/** Point d'entrée de la route History : branche le ViewModel. */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onOpenSession: (Long) -> Unit,
    onChooseExercise: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onClearFilter = viewModel::onClearFilter,
        onOpenSession = onOpenSession,
        onChooseExercise = onChooseExercise,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

/** Historique (S08) : liste par jour, filtres en puces, états vide et « aucun résultat » (parcours §4.6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onFilterSelected: (HistoryFilter) -> Unit,
    onClearFilter: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onChooseExercise: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), Modifier.semantics { heading() }) },
                actions = { SettingsAction(onSettingsClick) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
            when {
                state.isLoading -> {
                    Unit
                }

                state.isJournalEmpty -> {
                    EmptyJournal(onChooseExercise)
                }

                else -> {
                    FilterChips(state.filter, onFilterSelected)
                    if (state.isFilterEmpty) {
                        EmptyFilter(onClearFilter)
                    } else {
                        SessionList(state.groups, padding.calculateBottomPadding(), onOpenSession)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    current: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit,
) {
    val filters = listOf(HistoryFilter.All, HistoryFilter.Gems) + ExerciseId.ALL.map(HistoryFilter::exercise)
    val label = stringResource(R.string.history_filters_label)
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag(HistoryTags.CHIPS).semantics { contentDescription = label },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters, key = HistoryTags::chip) { filter ->
            val selected = filter == current
            FilterChip(
                selected = selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label()) },
                leadingIcon = filter.icon(selected),
                modifier = Modifier.heightIn(min = 48.dp).testTag(HistoryTags.chip(filter)),
            )
        }
    }
}

@Composable
private fun HistoryFilter.label(): String =
    when {
        gemsOnly -> stringResource(R.string.history_filter_gems)
        exerciseId != null -> stringResource(exerciseId.nameRes)
        else -> stringResource(R.string.history_filter_all)
    }

/** Le filtre actif porte une coche (jamais la couleur seule) ; « Bijoux » garde sa gemme. */
private fun HistoryFilter.icon(selected: Boolean): (@Composable () -> Unit)? =
    when {
        selected -> ({ Icon(ExerciseIcons.Check, contentDescription = null) })
        gemsOnly -> ({ Icon(ExerciseIcons.Diamond, contentDescription = null) })
        else -> null
    }

@Composable
private fun SessionList(
    groups: List<DayGroup>,
    bottomPadding: Dp,
    onOpenSession: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            item(key = "day_${group.label}") {
                Text(
                    text = group.label.text(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                )
            }
            items(group.sessions, key = { it.id }) { SessionCard(it, onClick = { onOpenSession(it.id) }) }
        }
    }
}

@Composable
private fun EmptyJournal(onChooseExercise: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(HistoryTags.EMPTY),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onChooseExercise, modifier = Modifier.testTag(HistoryTags.EMPTY_ACTION)) {
            Text(stringResource(R.string.history_empty_action))
        }
    }
}

@Composable
private fun EmptyFilter(onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(HistoryTags.FILTER_EMPTY),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.history_filter_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClear, modifier = Modifier.testTag(HistoryTags.CLEAR_FILTER)) {
            Text(stringResource(R.string.history_filter_clear))
        }
    }
}

private val PreviewItems =
    listOf(
        SessionItem(
            1,
            ExerciseId.CRAZY_EIGHTS,
            1_700_000_000_000,
            "UTC",
            "Un PNJ vient leur demander de l’aide…",
            true,
            2,
            false,
        ),
        SessionItem(
            2,
            ExerciseId.CONSTRAINT_BOX,
            1_699_900_000_000,
            "UTC",
            "Le bois du comptoir est tiède…",
            false,
            3,
            false,
        ),
        SessionItem(3, ExerciseId.CONNECT_STORY, 1_699_000_000_000, "UTC", null, false, 1, true),
    )

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    DmGymTheme {
        HistoryScreen(
            state =
                HistoryUiState(
                    isLoading = false,
                    totalCount = 3,
                    groups =
                        listOf(
                            DayGroup(DayLabel.Today, PreviewItems.take(1)),
                            DayGroup(DayLabel.Yesterday, PreviewItems.drop(1)),
                        ),
                ),
            onFilterSelected = {},
            onClearFilter = {},
            onOpenSession = {},
            onChooseExercise = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "Vide", showBackground = true)
@Composable
private fun HistoryEmptyPreview() {
    DmGymTheme {
        HistoryScreen(HistoryUiState(isLoading = false), {}, {}, {}, {}, {})
    }
}
