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
    const val RHYTHM_MARKER = "exercise_rhythm_marker"
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
    const val PINNED_SUMMARY = "exercise_pinned_summary"
    const val PINNED_DETAILS = "exercise_pinned_details"

    fun check(key: DrawKey) = "exercise_check_${key.slotKey}_${key.position}"

    fun field(key: AnswerKey) = "exercise_field_${key.fieldKey}_${key.groupIndex}"

    fun level(value: Int) = "exercise_level_$value"

    fun rubric(
        key: String,
        value: Boolean,
    ) = "exercise_rubric_${key}_${if (value) "yes" else "no"}"
}
