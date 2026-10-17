package io.github.qnicoud.dmgym.feature.daily

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.home.ExerciseSummary
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

object DailyTags {
    const val TITLE = "daily_title"
    const val SUMMARY = "daily_summary"
    const val START = "daily_start"
    const val FINISHED = "daily_finished"
    const val SHEET = "daily_sheet"

    fun row(position: Int) = "daily_row_$position"

    fun info(position: Int) = "daily_info_$position"

    fun replace(position: Int) = "daily_replace_$position"

    fun redo(position: Int) = "daily_redo_$position"

    fun choice(id: ExerciseId) = "daily_choice_${id.value}"
}

/** Point d'entrée de la route Daily : branche le ViewModel et relit le jour à chaque reprise (D16). */
@Composable
fun DailyRoute(
    viewModel: DailyViewModel,
    onBack: () -> Unit,
    onOpenExercise: (ExerciseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }
    DailyScreen(state, onBack, onOpenExercise, viewModel::replace, modifier)
}

/**
 * Aperçu de la séance du jour (S03) et feuille « Remplacer par… » (S03b). La séance est suggérée, jamais imposée :
 * « Remplacer » n'existe que sur « À faire », et rien n'oblige à suivre l'ordre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    state: DailyUiState,
    onBack: () -> Unit,
    onOpenExercise: (ExerciseId) -> Unit,
    onReplace: (position: Int, with: ExerciseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var replacing by rememberSaveable { mutableStateOf<Int?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_title), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) { Icon(DmIcons.Back, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
        bottomBar = { if (!state.isLoading && !state.allDone) StartBar(state, onOpenExercise) },
    ) { padding ->
        if (!state.isLoading) {
            DailyList(state, padding, onOpenExercise, onRequestReplace = { replacing = it })
        }
    }
    replacing?.let { position ->
        state.items.getOrNull(position)?.let { item ->
            ReplaceSheet(
                replaced = item.id,
                alternatives = state.alternatives,
                onChoose = {
                    onReplace(position, it)
                    replacing = null
                },
                onDismiss = { replacing = null },
            )
        }
    }
}

@Composable
private fun DailyList(
    state: DailyUiState,
    padding: PaddingValues,
    onOpenExercise: (ExerciseId) -> Unit,
    onRequestReplace: (Int) -> Unit,
) {
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
        item(key = "header") { DailyHeader(state) }
        itemsIndexed(state.items, key = { _, item -> item.position }) { _, item ->
            DailyRow(
                item = item,
                canRedo = state.allDone,
                onReplace = { onRequestReplace(item.position) },
                onOpen = { onOpenExercise(item.id) },
            )
        }
    }
}

@Composable
private fun DailyHeader(state: DailyUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = dailyDateTitle(state.epochDay),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(DailyTags.TITLE),
        )
        Text(
            text = stringResource(R.string.daily_summary, state.totalMinutes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(DailyTags.SUMMARY),
        )
        if (state.allDone) {
            Text(
                text = stringResource(R.string.daily_finished),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(DailyTags.FINISHED),
            )
        }
    }
}

/** « Commencer » (rien de fait) ou « Continuer » : le seul bouton plein de l'écran, fixé en bas. */
@Composable
private fun StartBar(
    state: DailyUiState,
    onOpenExercise: (ExerciseId) -> Unit,
) {
    val next = state.nextExercise ?: return
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Button(onClick = { onOpenExercise(next) }, modifier = Modifier.fillMaxWidth().testTag(DailyTags.START)) {
            Text(stringResource(if (state.started) R.string.daily_continue else R.string.daily_start))
        }
    }
}

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DailyScreenPreview() {
    DmGymTheme {
        DailyScreen(PreviewState, onBack = {}, onOpenExercise = {}, onReplace = { _, _ -> })
    }
}

private val PreviewState =
    DailyUiState(
        isLoading = false,
        epochDay = 20_742,
        items =
            listOf(
                DailyItem(0, ExerciseId.CRAZY_EIGHTS, 480, ExerciseProgress.DONE),
                DailyItem(1, ExerciseId.CONSTRAINT_BOX, 300, ExerciseProgress.IN_PROGRESS),
                DailyItem(2, ExerciseId.DECISION_POINT, 30, ExerciseProgress.TODO),
            ),
        alternatives = listOf(ExerciseSummary(ExerciseId.CORE_IMAGE, 600), ExerciseSummary(ExerciseId.ONE_PAGE, null)),
        nextExercise = ExerciseId.CONSTRAINT_BOX,
        started = true,
    )
