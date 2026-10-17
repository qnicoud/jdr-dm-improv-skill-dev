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
internal fun ActiveBarActions(
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
