package io.github.qnicoud.dmgym.feature.stats

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.streak.DayMark
import io.github.qnicoud.dmgym.core.streak.DayStatus
import io.github.qnicoud.dmgym.core.streak.StreakSummary
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

object StatsTags {
    const val SESSIONS = "stats_sessions"
    const val THIS_WEEK = "stats_this_week"
    const val STREAK = "stats_streak"
    const val GRACE = "stats_grace"
    const val BEST = "stats_best"
    const val RECENT_DAYS = "stats_recent_days"
    const val EMPTY = "stats_empty"
    const val EMPTY_ACTION = "stats_empty_action"
    const val GEMS = "stats_gems"
    const val GEMS_LINK = "stats_gems_link"
    const val TIME = "stats_time"

    fun day(epochDay: Long) = "stats_day_$epochDay"

    fun bar(id: ExerciseId) = "stats_bar_${id.value}"
}

/** Point d'entrée de la route Stats : branche le ViewModel et relit le jour à chaque reprise (architecture §9). */
@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
    onChooseExercise: () -> Unit,
    onShowGems: () -> Unit,
    onShowExercise: (ExerciseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }
    StatsScreen(state, onChooseExercise, onShowGems, onShowExercise, modifier)
}

/** Progrès (S10) : sessions d'abord (D32), puis série, meilleure série, répartition, bijoux et temps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    onChooseExercise: () -> Unit,
    onShowGems: () -> Unit,
    onShowExercise: (ExerciseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title), Modifier.semantics { heading() }) },
            )
        },
    ) { padding ->
        if (state.isEmpty) {
            EmptyStats(onChooseExercise, Modifier.padding(padding))
        } else if (!state.isLoading) {
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "sessions") { SessionsCard(state) }
                item(key = "streak") { StreakCard(state) }
                if (state.perExercise.isNotEmpty()) {
                    item(
                        key = "by_exercise",
                    ) { ExerciseBars(state.perExercise, onShowExercise) }
                }
                item(key = "extras") { Extras(state, onShowGems) }
            }
        }
    }
}

@Composable
private fun EmptyStats(
    onChooseExercise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp).testTag(StatsTags.EMPTY),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.stats_empty_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.stats_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onChooseExercise, modifier = Modifier.testTag(StatsTags.EMPTY_ACTION)) {
            Text(stringResource(R.string.stats_empty_action))
        }
    }
}

@Composable
private fun SessionsCard(state: StatsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = pluralStringResource(R.plurals.home_sessions_count, state.sessions, state.sessions),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.testTag(StatsTags.SESSIONS),
            )
            Text(
                text = pluralStringResource(R.plurals.stats_this_week, state.sessionsThisWeek, state.sessionsThisWeek),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(StatsTags.THIS_WEEK),
            )
        }
    }
}

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun StatsScreenPreview() {
    DmGymTheme {
        StatsScreen(PreviewState, onChooseExercise = {}, onShowGems = {}, onShowExercise = {})
    }
}

@Preview(name = "Vide", showBackground = true)
@Composable
private fun StatsEmptyPreview() {
    DmGymTheme {
        StatsScreen(StatsUiState(isLoading = false), onChooseExercise = {}, onShowGems = {}, onShowExercise = {})
    }
}

private val PreviewState =
    StatsUiState(
        isLoading = false,
        today = 20_000,
        sessions = 47,
        sessionsThisWeek = 6,
        streak = StreakSummary(47, 30, 5, 12, 19_999, true, setOf(19_999)),
        recent = (19_994L..20_000L).map { DayStatus(it, if (it == 19_999L) DayMark.GRACE else DayMark.PRACTICED) },
        perExercise = listOf(ExerciseCount(ExerciseId.CRAZY_EIGHTS, 12), ExerciseCount(ExerciseId.CONSTRAINT_BOX, 9)),
        gems = 9,
        totalSeconds = 18_720,
    )
