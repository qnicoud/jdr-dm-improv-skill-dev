package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
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
    val onPreviousQuestion: () -> Unit = {},
    val onFinish: () -> Unit = {},
    /** Plages à surligner dans le champ surveillé par la détection ; `null` pour un exercice sans détection. */
    val highlights: ((String) -> List<IntRange>)? = null,
)

/** Zone de saisie : champs fixes, champ unique, questions successives ou groupes répétables (ADR 0012). */
@Composable
fun FieldList(
    definition: ExerciseDefinition,
    stored: ExerciseSession,
    progress: Progress?,
    groupCount: Int,
    handlers: FieldHandlers,
    firstFieldFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val session = stored.copy(options = definition.resolveOptions(stored.options))
    val highlight = rememberHighlight(definition.detection?.field, session.draws, handlers.highlights)
    CompositionLocalProvider(LocalFieldHighlight provides highlight) {
        FieldColumn(definition, session, progress, groupCount, handlers, firstFieldFocus, modifier)
    }
}

@Composable
private fun FieldColumn(
    definition: ExerciseDefinition,
    session: ExerciseSession,
    progress: Progress?,
    groupCount: Int,
    handlers: FieldHandlers,
    firstFieldFocus: FocusRequester,
    modifier: Modifier,
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

            is FieldLayout.None -> {
                Unit
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (index > 0) {
            TextButton(
                onClick = handlers.onPreviousQuestion,
                modifier = Modifier.heightIn(min = 48.dp).testTag(QuestionTags.PREVIOUS),
            ) { Text(stringResource(R.string.question_previous)) }
        }
        if (index < questions.size - 1) {
            FilledTonalButton(
                onClick = handlers.onNextQuestion,
                modifier = Modifier.heightIn(min = 48.dp).testTag(ExerciseTags.NEXT_QUESTION),
            ) { Text(stringResource(R.string.question_next)) }
        } else {
            Button(
                onClick = handlers.onFinish,
                modifier = Modifier.heightIn(min = 48.dp).testTag(QuestionTags.LAST_FINISH),
            ) { Text(stringResource(R.string.exercise_finish)) }
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
    val shown = { specs: List<FieldSpec> -> specs.filter { it.visibleWhen?.holds(session.options) ?: true } }
    val before = shown(layout.before)
    before.forEachIndexed { i, spec ->
        Field(spec, AnswerKey(spec.key), null, session, handlers, if (i == 0) firstFieldFocus else null)
    }
    repeat(groupCount) { group ->
        Text(
            stringResource(layout.title ?: R.string.group_title, group + 1),
            style = MaterialTheme.typography.titleMedium,
        )
        layout.fields.forEachIndexed { index, spec ->
            val focus = if (group == 0 && index == 0 && before.isEmpty()) firstFieldFocus else null
            Field(spec, AnswerKey(spec.key, group), null, session, handlers, focus)
        }
    }
    if (groupCount < layout.max) {
        OutlinedButton(
            onClick = handlers.onAddGroup,
            modifier = Modifier.heightIn(min = 48.dp).testTag(ExerciseTags.ADD_GROUP),
        ) {
            Text(stringResource(layout.addLabel ?: R.string.group_add))
        }
    }
    shown(layout.after).forEach { spec -> Field(spec, AnswerKey(spec.key), null, session, handlers, null) }
}

/** Identifiants de test propres à la navigation entre questions. */
object QuestionTags {
    const val PREVIOUS = "exercise_previous_question"
    const val LAST_FINISH = "exercise_last_question_finish"
    const val SOFT_COUNTER = "exercise_soft_counter"
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
        lineLimits =
            TextFieldLineLimits.MultiLine(
                minHeightInLines = spec.minLines,
                maxHeightInLines = maxOf(MAX_LINES, spec.minLines),
            ),
        supportingText = spec.softLimit?.let { limit -> { SoftCounter(state.text.length, limit) } },
        keyboardOptions = KeyboardOptions(imeAction = imeActionOf(position)),
        outputTransformation = LocalFieldHighlight.current(spec.key),
    )
    spec.flagLabel?.let { flag ->
        FlagRow(stringResource(flag), session.answers[key]?.flagged == true) { handlers.onFlagToggled(key) }
    }
}

/** Compteur souple : il informe, il ne bloque jamais la saisie. */
@Composable
private fun SoftCounter(
    length: Int,
    limit: Int,
) {
    Text(
        stringResource(R.string.field_soft_counter, length, limit),
        modifier = Modifier.testTag(QuestionTags.SOFT_COUNTER),
    )
}

/** Suivant d'un champ numéroté au suivant ; Terminé au dernier ferme le clavier sans finir l'exercice. */
private fun imeActionOf(position: Pair<Int, Int>?): ImeAction =
    when {
        position == null -> ImeAction.Default
        position.first < position.second -> ImeAction.Next
        else -> ImeAction.Done
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
