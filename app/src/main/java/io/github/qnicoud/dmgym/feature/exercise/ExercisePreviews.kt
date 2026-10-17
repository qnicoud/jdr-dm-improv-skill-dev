package io.github.qnicoud.dmgym.feature.exercise

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.ProgressRule
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlin.time.Duration.Companion.minutes

private val previewDefinition =
    ExerciseDefinition(
        id = ExerciseId.CRAZY_EIGHTS,
        name = R.string.exercise_crazy_eights,
        discipline = R.string.discipline_crazy_eights,
        instructions = R.string.draw_error_body,
        reminder = null,
        timer = TimerPolicy.Required(8.minutes),
        slots = emptyList(),
        layout = FieldLayout.Fixed(listOf(FieldSpec("a"), FieldSpec("b"))),
        rubric = listOf(RubricQuestion("q1", R.string.rubric_yes), RubricQuestion("q2", R.string.rubric_no)),
        progress = ProgressRule.FilledFields,
    )

private val previewSession =
    ExerciseSession(
        definitionId = previewDefinition.id,
        phase = Phase.Active,
        draws = listOf(Draw("p", 0, "problems", "x", "Un pont s’effondre au milieu de la fête")),
    )

private fun previewState() = ExerciseUiState(previewDefinition, previewSession, LoadStatus.Ready)

private val noOp = {}

private fun previewActions() =
    ExerciseActions(
        onLeave = noOp,
        onStart = noOp,
        onFinish = noOp,
        onPause = noOp,
        onResume = noOp,
        onReroll = {},
        onRetry = noOp,
        onCheckToggled = {},
        fields = FieldHandlers({ TextFieldState() }, {}, noOp, noOp),
        assess = AssessActions({ _, _, _ -> }, noOp),
    )

@Preview(name = "Minuteur, clair", showBackground = true)
@Preview(name = "Minuteur, sombre", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TimerStatesPreview() {
    DmGymTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(8.dp),
        ) {
            listOf(
                TimerUi(TimerKind.Ready, 480_000, 1f),
                TimerUi(TimerKind.Running, 270_000, 0.56f),
                TimerUi(TimerKind.LastMinute, 58_000, 0.12f),
                TimerUi(TimerKind.Overtime, 42_000, 0f),
                TimerUi(TimerKind.Paused, 130_000, 0.27f),
                TimerUi(TimerKind.Untimed, 0, 0f),
            ).forEach { TimerBar(it, canPause = true, onPause = {}, onResume = {}) }
        }
    }
}

@Preview(name = "En cours, clair", showBackground = true)
@Preview(name = "En cours, sombre", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActivePreview() {
    DmGymTheme { ExerciseContent(previewState(), { TimerUi(TimerKind.Overtime, 12_000, 0f) }, previewActions()) }
}

@Preview(name = "Bilan, clair", showBackground = true)
@Preview(name = "Bilan, sombre", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EndPreview() {
    val ended = previewSession.copy(phase = Phase.Ended, selfRating = 2)
    DmGymTheme {
        ExerciseContent(
            previewState().copy(session = ended),
            { TimerUi(TimerKind.Paused, 0, 0f, elapsedMs = 492_000) },
            previewActions(),
        )
    }
}

@Preview(name = "Champs", showBackground = true)
@Composable
private fun FieldsPreview() {
    DmGymTheme {
        FieldList(
            previewDefinition,
            previewSession,
            progress =
                io.github.qnicoud.dmgym.core.exercise
                    .Progress(1, 2),
            groupCount = 1,
            handlers = FieldHandlers({ _: AnswerKey -> TextFieldState("Une idée") }, {}, noOp, noOp),
            firstFieldFocus = remember { FocusRequester() },
        )
    }
}
