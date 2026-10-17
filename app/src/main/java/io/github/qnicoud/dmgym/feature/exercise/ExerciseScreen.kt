package io.github.qnicoud.dmgym.feature.exercise

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.daily.DailyStep
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.CardSlot
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FREE_SUBJECT_KEY
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.platform.KeepScreenOn
import io.github.qnicoud.dmgym.ui.icons.DmIcons

/** Identifiants de test des éléments de l'écran d'exercice. */
object ExerciseTags {
    const val TIMER = "exercise_timer"
    const val PAUSE = "exercise_pause"
    const val START = "exercise_start"
    const val FINISH = "exercise_finish"
    const val DRAW_CARD = "exercise_draw_card"
    const val DRAW_ERROR = "exercise_draw_error"
    const val REROLL = "exercise_reroll"
    const val REROLL_COUNT = "exercise_reroll_count"
    const val FREE_SUBJECT = "exercise_free_subject"
    const val PROGRESS = "exercise_progress"
    const val NEXT_QUESTION = "exercise_next_question"
    const val ADD_GROUP = "exercise_add_group"
    const val SAVE_STATUS = "exercise_save_status"
    const val END_PANEL = "exercise_end_panel"
    const val NEXT_EXERCISE = "exercise_next_exercise"
    const val END_PROMPT = "exercise_end_prompt"
    const val END_HINT = "exercise_end_hint"
    const val DONE = "exercise_done"
    const val SKIP = "exercise_skip"
    const val RUBRIC_TOGGLE = "exercise_rubric_toggle"
    const val GEM = "exercise_gem"
    const val CHECKLIST_COUNT = "exercise_checklist_count"
    const val CHECKLIST_TOGGLE = "exercise_checklist_toggle"

    fun check(key: DrawKey) = "exercise_check_${key.slotKey}_${key.position}"

    fun field(key: AnswerKey) = "exercise_field_${key.fieldKey}_${key.groupIndex}"

    fun level(value: Int) = "exercise_level_$value"

    fun rubric(
        key: String,
        value: Boolean,
    ) = "exercise_rubric_${key}_${if (value) "yes" else "no"}"
}

/** Actions de l'écran, séparées du ViewModel pour garder le contenu sans état. */
class ExerciseActions(
    val onLeave: () -> Unit,
    val onStart: () -> Unit,
    val onFinish: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onReroll: (String?) -> Unit,
    val onRetry: () -> Unit,
    val onCheckToggled: (DrawKey) -> Unit,
    val fields: FieldHandlers,
    val assess: AssessActions,
    val onOptionChanged: (String, Boolean) -> Unit = { _, _ -> },
)

/** Point d'entrée avec état : branche le ViewModel, les événements ponctuels, l'écran allumé et la sortie. */
@Composable
fun ExerciseRoute(
    viewModel: ExerciseViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    daily: DailyStep? = null,
    onNextExercise: (ExerciseId) -> Unit = {},
    onDailyDone: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timer = viewModel.timerState.collectAsStateWithLifecycle()
    val leave = {
        viewModel.onLeave()
        onBack()
    }
    BackHandler(onBack = leave)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }
    KeepScreenOn(state.keepScreenOn)
    val snackbarHost = remember { SnackbarHostState() }
    EventsEffect(viewModel, snackbarHost)
    val next = daily?.next
    val onNext = {
        viewModel.onLeave()
        if (next != null) onNextExercise(next.id) else onDailyDone()
    }
    val actions =
        remember(viewModel, onBack, daily) {
            ExerciseActions(
                onLeave = leave,
                onStart = viewModel::onStart,
                onFinish = viewModel::onFinish,
                onPause = viewModel::onPause,
                onResume = viewModel::onResume,
                onReroll = viewModel::onReroll,
                onRetry = viewModel::onRetry,
                onCheckToggled = viewModel::onCheckToggled,
                fields =
                    FieldHandlers(
                        viewModel::fieldState,
                        viewModel::onFlagToggled,
                        viewModel::onNextQuestion,
                        viewModel::onAddGroup,
                        viewModel::onPreviousQuestion,
                        viewModel::onFinish,
                        viewModel::highlights,
                    ),
                assess = AssessActions(viewModel::onAssess, { leave() }, viewModel::fieldState, onNext),
                onOptionChanged = viewModel::onOptionChanged,
            )
        }
    ExerciseContent(state, { timer.value }, actions, modifier, snackbarHost, daily)
}

/** Contenu sans état de l'écran : prêt, en cours (y compris en temps additionnel) ou bilan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseContent(
    state: ExerciseUiState,
    timer: () -> TimerUi,
    actions: ExerciseActions,
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
    daily: DailyStep? = null,
) {
    val phase = state.session.phase
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { ExerciseTitle(state.definition.name, daily) },
                navigationIcon = {
                    IconButton(onClick = actions.onLeave) {
                        Icon(DmIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = { if (phase == Phase.Active) ActiveBarActions(state, timer, actions) },
            )
        },
        bottomBar = {
            if (phase == Phase.Ready && state.load != LoadStatus.Loading) {
                ReadyStartButton(state, timer, actions.onStart, Modifier.imePadding().padding(16.dp))
            }
            val layout = state.definition.layout
            if (phase == Phase.Active && layout is FieldLayout.None) {
                SceneDoneButton(stringResource(layout.finishLabel), actions.onFinish, Modifier.padding(16.dp))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).imePadding().fillMaxSize()) {
            when (phase) {
                Phase.Ready -> ReadyBody(state, timer, actions)
                Phase.Active -> ActiveBody(state, timer, actions)
                Phase.Ended -> EndBody(state, timer, actions, daily)
            }
        }
    }
}

@Composable
private fun ActiveBarActions(
    state: ExerciseUiState,
    timer: () -> TimerUi,
    actions: ExerciseActions,
) {
    val label = stringResource(R.string.exercise_finish)
    val overtime by remember { derivedStateOf { timer().kind == TimerKind.Overtime } }
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SaveIndicator(state.save)
        if (overtime) {
            Button(onClick = actions.onFinish, modifier = Modifier.testTag(ExerciseTags.FINISH)) { Text(label) }
        } else {
            FilledTonalButton(
                onClick = actions.onFinish,
                modifier = Modifier.testTag(ExerciseTags.FINISH),
            ) { Text(label) }
        }
        androidx.compose.foundation.layout
            .Spacer(Modifier.padding(end = 4.dp))
    }
}

@Composable
private fun SaveIndicator(status: SaveStatus) {
    val text =
        when (status) {
            SaveStatus.Idle -> return
            SaveStatus.Saved -> stringResource(R.string.exercise_saved)
            SaveStatus.Failed -> stringResource(R.string.exercise_save_failed)
        }
    Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag(ExerciseTags.SAVE_STATUS))
}

@Composable
private fun ReadyBody(
    state: ExerciseUiState,
    timer: () -> TimerUi,
    actions: ExerciseActions,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(state.definition.instructions), style = MaterialTheme.typography.bodyLarge)
        OptionsPanel(state.definition.options, state.session.options, actions.onOptionChanged)
        DrawArea(state, actions)
        ReminderCard(state.definition)
        LiveTimerBar(timer, canPause = false, onPause = actions.onPause, onResume = actions.onResume)
    }
}

@Composable
private fun DrawArea(
    state: ExerciseUiState,
    actions: ExerciseActions,
) {
    when (state.load) {
        LoadStatus.Loading -> {
            DrawCardSkeleton()
        }

        LoadStatus.DrawFailed -> {
            val free = actions.fields.fieldState(AnswerKey(FREE_SUBJECT_KEY))
            DrawErrorCard(state.definition.acceptsFreeSubject, free, actions.onRetry)
        }

        LoadStatus.Ready -> {
            ReadyDraw(state.withoutQuestionDraws(), actions)
            if (state.session.phase == Phase.Ready) ReadyQuestions(state)
        }
    }
}

@Composable
private fun ColumnScope.ActiveBody(
    state: ExerciseUiState,
    timer: () -> TimerUi,
    actions: ExerciseActions,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LiveTimerBar(timer, canPause = true, onPause = actions.onPause, onResume = actions.onResume)
        DrawArea(state, actions)
    }
    Column(
        modifier =
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReminderCard(state.definition)
        FieldList(state.definition, state.session, state.progress, state.groupCount, actions.fields, focus)
    }
}

@Composable
private fun EndBody(
    state: ExerciseUiState,
    timer: () -> TimerUi,
    actions: ExerciseActions,
    daily: DailyStep?,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        EndPanel(state.definition, state.session, timer().elapsedMs, actions.assess, daily = daily)
    }
}

/** Nom de l'exercice ; dans la séance du jour, « Séance du jour · 1 sur 3 » en dessous (parcours S04). */
@Composable
private fun ExerciseTitle(
    @StringRes name: Int,
    daily: DailyStep?,
) {
    Column(modifier = Modifier.semantics { heading() }) {
        Text(stringResource(name))
        if (daily != null) {
            Text(
                stringResource(R.string.exercise_daily_step, daily.number, daily.total),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
