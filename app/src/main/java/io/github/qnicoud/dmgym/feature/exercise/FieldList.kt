package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.Progress

/** Ce dont la liste de champs a besoin du ViewModel, sans en dépendre. */
class FieldHandlers(
    val fieldState: (AnswerKey) -> TextFieldState,
    val onFlagToggled: (AnswerKey) -> Unit,
    val onNextQuestion: () -> Unit,
    val onAddGroup: () -> Unit,
)

/** Zone de saisie : champs fixes, champ unique, questions successives ou groupes répétables (ADR 0012). */
@Composable
fun FieldList(
    definition: ExerciseDefinition,
    session: ExerciseSession,
    progress: Progress?,
    groupCount: Int,
    handlers: FieldHandlers,
    firstFieldFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        progress?.let { ProgressLine(it) }
        when (val layout = definition.layout) {
            is FieldLayout.Fixed -> {
                FixedFields(layout, session, handlers, firstFieldFocus)
            }

            is FieldLayout.Single -> {
                Field(
                    layout.field,
                    AnswerKey(layout.field.key),
                    null,
                    session,
                    handlers,
                    firstFieldFocus,
                )
            }

            is FieldLayout.Sequential -> {
                SequentialField(layout, session, handlers, firstFieldFocus)
            }

            is FieldLayout.RepeatableGroup -> {
                Groups(layout, session, groupCount, handlers, firstFieldFocus)
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Progress) {
    val description = stringResource(R.string.exercise_progress_description, progress.done, progress.total)
    Text(
        text = stringResource(R.string.exercise_progress, progress.done, progress.total),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.testTag(ExerciseTags.PROGRESS).semantics { contentDescription = description },
    )
}

@Composable
private fun FixedFields(
    layout: FieldLayout.Fixed,
    session: ExerciseSession,
    handlers: FieldHandlers,
    firstFieldFocus: FocusRequester,
) {
    layout.fields.forEachIndexed { index, spec ->
        Field(
            spec = spec,
            key = AnswerKey(spec.key),
            position = index + 1 to layout.fields.size,
            session = session,
            handlers = handlers,
            focus = if (index == 0) firstFieldFocus else null,
        )
    }
}

@Composable
private fun SequentialField(
    layout: FieldLayout.Sequential,
    session: ExerciseSession,
    handlers: FieldHandlers,
    firstFieldFocus: FocusRequester,
) {
    val questions = session.drawsOf(layout.questionsSlot)
    val index = session.currentQuestion.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
    Text(
        stringResource(R.string.question_progress, index + 1, questions.size.coerceAtLeast(1)),
        style = MaterialTheme.typography.labelLarge,
    )
    questions.getOrNull(index)?.let { Text(it.text, style = MaterialTheme.typography.titleLarge) }
    Field(layout.field, AnswerKey(layout.field.key, index), null, session, handlers, firstFieldFocus)
    if (index < questions.size - 1) {
        OutlinedButton(
            onClick = handlers.onNextQuestion,
            modifier = Modifier.heightIn(min = 48.dp).testTag(ExerciseTags.NEXT_QUESTION),
        ) {
            Text(stringResource(R.string.question_next))
        }
    }
}

@Composable
private fun Groups(
    layout: FieldLayout.RepeatableGroup,
    session: ExerciseSession,
    groupCount: Int,
    handlers: FieldHandlers,
    firstFieldFocus: FocusRequester,
) {
    repeat(groupCount) { group ->
        Text(stringResource(R.string.group_title, group + 1), style = MaterialTheme.typography.titleMedium)
        layout.fields.forEachIndexed { index, spec ->
            Field(
                spec,
                AnswerKey(spec.key, group),
                null,
                session,
                handlers,
                if (group == 0 &&
                    index == 0
                ) {
                    firstFieldFocus
                } else {
                    null
                },
            )
        }
    }
    if (groupCount < layout.max) {
        OutlinedButton(
            onClick = handlers.onAddGroup,
            modifier = Modifier.heightIn(min = 48.dp).testTag(ExerciseTags.ADD_GROUP),
        ) {
            Text(stringResource(R.string.group_add))
        }
    }
}

@Composable
private fun Field(
    spec: FieldSpec,
    key: AnswerKey,
    position: Pair<Int, Int>?,
    session: ExerciseSession,
    handlers: FieldHandlers,
    focus: FocusRequester?,
) {
    val label =
        spec.label?.let { stringResource(it) } ?: stringResource(R.string.field_default_label, (position?.first ?: 1))
    val state = handlers.fieldState(key)
    val focusModifier = focus?.let { Modifier.focusRequester(it) } ?: Modifier
    OutlinedTextField(
        state = state,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(focusModifier)
                .testTag(ExerciseTags.field(key)),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(label) },
        placeholder = { Text(stringResource(spec.hint ?: R.string.field_default_hint)) },
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = spec.minLines, maxHeightInLines = MAX_LINES),
    )
    spec.flagLabel?.let { flag ->
        FlagRow(stringResource(flag), session.answers[key]?.flagged == true) { handlers.onFlagToggled(key) }
    }
}

@Composable
private fun FlagRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(start = 4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

private const val MAX_LINES = 6
