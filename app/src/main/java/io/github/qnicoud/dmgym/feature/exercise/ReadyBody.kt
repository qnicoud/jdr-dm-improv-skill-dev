package io.github.qnicoud.dmgym.feature.exercise

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

@Composable
internal fun ReadyBody(
    state: ExerciseUiState,
    timer: () -> TimerUi,
    actions: ExerciseActions,
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val setting = state.timerSetting
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(state.definition.instructions), style = MaterialTheme.typography.bodyLarge)
        OptionsPanel(state.definition.options, state.session.options, actions.onOptionChanged)
        DrawArea(state, actions)
        ReminderCard(state.definition)
        LiveTimerBar(
            timer,
            canPause = false,
            onPause = actions.onPause,
            onResume = actions.onResume,
            onClick = if (setting != null) ({ sheetOpen = true }) else null,
        )
    }
    if (sheetOpen && setting != null) {
        DurationSheet(
            exerciseName = stringResource(state.definition.name),
            setting = setting,
            onDecrease = actions.onDurationDecrease,
            onIncrease = actions.onDurationIncrease,
            onUntimedChange = actions.onUntimedChanged,
            onDismiss = {
                sheetOpen = false
                actions.onDurationSheetClosed()
            },
        )
    }
}

@Composable
internal fun DrawArea(
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
