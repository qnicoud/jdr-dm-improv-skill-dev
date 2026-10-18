package io.github.qnicoud.dmgym.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import io.github.qnicoud.dmgym.feature.daily.DailyCard
import io.github.qnicoud.dmgym.feature.daily.DailyViewModel
import io.github.qnicoud.dmgym.ui.components.SettingsAction
import io.github.qnicoud.dmgym.ui.components.SettingsActionTag
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import java.time.Clock

object HomeTags {
    const val WELCOME = "home_welcome"
    const val SETTINGS_BUTTON = SettingsActionTag
    const val PREPARATION_TAG = "exercise_tag_preparation"
    const val NEW_TAG = "exercise_tag_new"
    const val DURATION_TAG = "exercise_tag_duration"

    fun card(id: ExerciseId) = "exercise_card_${id.value}"
}

/** Point d'entrée de la route Home : branche le ViewModel. */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    dailyViewModel: DailyViewModel,
    onExerciseClick: (ExerciseId) -> Unit,
    onDailyClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onResumeClick: (ResumeUi) -> Unit,
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val daily by dailyViewModel.uiState.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(clock.millis()) }
    LifecycleResumeEffect(dailyViewModel) {
        dailyViewModel.refreshToday()
        now = clock.millis()
        onPauseOrDispose {}
    }
    HomeScreen(
        state = state,
        onExerciseClick = onExerciseClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
        resumeSlot = state.resume?.let { resume -> { ResumeCard(resume, now, onResume = { onResumeClick(resume) }) } },
        dailySlot = if (daily.isLoading) null else ({ DailyCard(daily, onDailyClick) }),
    )
}

/**
 * Accueil (S02). Les emplacements [resumeSlot] (carte « Reprendre », [ResumeCard]) et [dailySlot] (« Séance du
 * jour », [DailyCard]) sont branchés par [HomeRoute].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onExerciseClick: (ExerciseId) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    resumeSlot: (@Composable () -> Unit)? = null,
    dailySlot: (@Composable () -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { HomeTopBar(onSettingsClick, scrollBehavior) },
    ) { padding ->
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
            if (!state.isLoading) item(key = "stats") { SessionsBanner(state.sessionCount) }
            if (state.isEmpty) item(key = "welcome") { Welcome() }
            resumeSlot?.let { item(key = "resume") { it() } }
            dailySlot?.let { item(key = "daily") { it() } }
            item(key = "header") {
                Text(
                    text = stringResource(R.string.home_all_exercises),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            items(state.exercises, key = { it.id.value }) { summary ->
                ExerciseCard(summary, onClick = { onExerciseClick(summary.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onSettingsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.home_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        actions = {
            SettingsAction(onClick = onSettingsClick)
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SessionsBanner(sessionCount: Int) {
    Text(
        text = pluralStringResource(R.plurals.home_sessions_count, sessionCount, sessionCount),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun Welcome() {
    Text(
        text = stringResource(R.string.home_welcome_empty),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.testTag(HomeTags.WELCOME).padding(bottom = 4.dp),
    )
}

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DmGymTheme {
        HomeScreen(
            state = HomeUiState(isLoading = false, sessionCount = 12, exercises = DefaultExercisesForPreview),
            onExerciseClick = {},
            onSettingsClick = {},
        )
    }
}

private val DefaultExercisesForPreview =
    listOf(
        ExerciseSummary(ExerciseId.CRAZY_EIGHTS, 480),
        ExerciseSummary(ExerciseId.DECISION_POINT, 30),
        ExerciseSummary(ExerciseId.ONE_PAGE, null),
    )
