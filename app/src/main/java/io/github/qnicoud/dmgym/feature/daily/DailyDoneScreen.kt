package io.github.qnicoud.dmgym.feature.daily

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.daily.DailyDoneLine
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.exercise.shortDuration
import io.github.qnicoud.dmgym.feature.history.Level
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

object DailyDoneTags {
    const val TITLE = "daily_done_title"
    const val MESSAGE = "daily_done_message"
    const val HOME = "daily_done_home"
    const val REDO = "daily_done_redo"
    const val SESSIONS = "daily_done_sessions"
    const val STREAK = "daily_done_streak"

    fun line(index: Int) = "daily_done_line_$index"
}

/** Point d'entrée de la route DailyDone : branche le ViewModel. */
@Composable
fun DailyDoneRoute(
    viewModel: DailyDoneViewModel,
    onHome: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DailyDoneScreen(state, onHome, onRedo, onBack, modifier)
}

/**
 * Fin de séance du jour (S07) : les exercices avec leur niveau et leur bijou, un message qui encourage sans
 * jamais reprocher ce qui n'a pas été fait, les compteurs, et la sortie vers l'accueil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDoneScreen(
    state: DailyDoneUiState,
    onHome: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(DmIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.daily_done_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }.testTag(DailyDoneTags.TITLE),
            )
            Text(
                stringResource(if (state.allDone) R.string.daily_done_all else R.string.daily_done_partial),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(DailyDoneTags.MESSAGE),
            )
            state.lines.forEachIndexed { index, line -> DoneLine(line, Modifier.testTag(DailyDoneTags.line(index))) }
            Counters(state)
            Button(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(DailyDoneTags.HOME),
            ) { Text(stringResource(R.string.daily_done_home)) }
            TextButton(
                onClick = onRedo,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(DailyDoneTags.REDO),
            ) { Text(stringResource(R.string.daily_done_redo)) }
        }
    }
}

/** Une ligne : un seul élément pour TalkBack (« Constraint Box, 5 min, Solide, bijou »). */
@Composable
private fun DoneLine(
    line: DailyDoneLine,
    modifier: Modifier = Modifier,
) {
    val level = Level.of(line.rating)
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(DmIcons.forExercise(line.id), contentDescription = null, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(line.id.nameRes), style = MaterialTheme.typography.titleMedium)
                val detail =
                    if (line.done) {
                        shortDuration(line.durationSec * MS_PER_SECOND)
                    } else {
                        stringResource(R.string.daily_done_not_done)
                    }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (level != null) {
                Icon(level.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(level.labelRes), style = MaterialTheme.typography.labelLarge)
            }
            if (line.isGem) {
                Icon(ExerciseIcons.Diamond, contentDescription = stringResource(R.string.gem_kept))
            }
        }
    }
}

@Composable
private fun Counters(state: DailyDoneUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            pluralStringResource(R.plurals.home_sessions_count, state.sessionCount, state.sessionCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(DailyDoneTags.SESSIONS),
        )
        if (state.streakDays > 0) {
            Text(
                pluralStringResource(R.plurals.stats_streak, state.streakDays, state.streakDays),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(DailyDoneTags.STREAK),
            )
        }
    }
}

private const val MS_PER_SECOND = 1_000L

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DailyDonePreview() {
    DmGymTheme {
        DailyDoneScreen(PreviewState, onHome = {}, onRedo = {}, onBack = {})
    }
}

private val PreviewState =
    DailyDoneUiState(
        isLoading = false,
        lines =
            listOf(
                DailyDoneLine(ExerciseId.CRAZY_EIGHTS, true, 480, rating = 2, isGem = true),
                DailyDoneLine(ExerciseId.CONSTRAINT_BOX, true, 300, rating = null),
                DailyDoneLine(ExerciseId.DECISION_POINT, false),
            ),
        sessionCount = 48,
        streakDays = 6,
    )
