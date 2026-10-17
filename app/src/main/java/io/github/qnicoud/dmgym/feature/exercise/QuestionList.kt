package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import kotlinx.coroutines.flow.drop

/** Identifiants de test propres aux questions successives (Core Image Threading). */
object QuestionTags {
    const val PREVIOUS = "exercise_previous_question"
    const val LAST_FINISH = "exercise_last_question_finish"
    const val SOFT_COUNTER = "exercise_soft_counter"
    const val CURRENT = "exercise_current_question"
    const val TOGGLE_LIST = "exercise_questions_toggle"
    const val LIST = "exercise_questions_list"
    const val READY_LIST = "exercise_questions_ready"

    fun item(index: Int) = "exercise_question_$index"
}

/**
 * Retire de l'état les tirages de questions d'une disposition séquentielle : ils ne vont pas dans la zone
 * épinglée sous le minuteur, où ils chassent le champ de saisie quand le clavier s'ouvre.
 */
internal fun ExerciseUiState.withoutQuestionDraws(): ExerciseUiState {
    val layout = definition.layout as? FieldLayout.Sequential ?: return this
    return copy(session = session.copy(draws = session.draws.filter { it.slotKey != layout.questionsSlot }))
}

/** Questions numérotées de l'écran Prêt, lisibles avant de lancer l'exercice. */
@Composable
internal fun ReadyQuestions(
    state: ExerciseUiState,
    modifier: Modifier = Modifier,
) {
    val layout = state.definition.layout as? FieldLayout.Sequential ?: return
    val questions = state.session.drawsOf(layout.questionsSlot)
    if (questions.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth().testTag(QuestionTags.READY_LIST),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.questions_list_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            NumberedQuestions(questions)
        }
    }
}

@Composable
private fun NumberedQuestions(
    questions: List<Draw>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        questions.forEachIndexed { i, q ->
            Text(
                stringResource(R.string.question_numbered, i + 1, questions.size, q.text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(QuestionTags.item(i)),
            )
        }
    }
}

/**
 * Une question à la fois, numérotée, au-dessus du champ. La liste complète se déplie à la demande, sans
 * fenêtre, et se referme dès que le joueur écrit ou change de question.
 */
@Composable
internal fun SequentialField(
    layout: FieldLayout.Sequential,
    session: ExerciseSession,
    handlers: FieldHandlers,
    firstFieldFocus: FocusRequester,
) {
    val questions = session.drawsOf(layout.questionsSlot)
    val index = session.currentQuestion.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
    val key = AnswerKey(layout.field.key, index)
    val answer = handlers.fieldState(key)
    var showAll by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(index) { showAll = false }
    LaunchedEffect(answer) {
        snapshotFlow { answer.text.toString() }.drop(1).collect { showAll = false }
    }
    questions.getOrNull(index)?.let {
        Text(
            stringResource(R.string.question_numbered, index + 1, questions.size, it.text),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(QuestionTags.CURRENT),
        )
    }
    Field(layout.field, key, null, session, handlers, firstFieldFocus)
    QuestionNavigation(index, questions.size, handlers)
    TextButton(
        onClick = { showAll = !showAll },
        modifier = Modifier.heightIn(min = 48.dp).testTag(QuestionTags.TOGGLE_LIST),
    ) { Text(stringResource(if (showAll) R.string.questions_hide_all else R.string.questions_show_all)) }
    if (showAll) NumberedQuestions(questions, Modifier.testTag(QuestionTags.LIST))
}

@Composable
private fun QuestionNavigation(
    index: Int,
    count: Int,
    handlers: FieldHandlers,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (index > 0) {
            TextButton(
                onClick = handlers.onPreviousQuestion,
                modifier = Modifier.heightIn(min = 48.dp).testTag(QuestionTags.PREVIOUS),
            ) { Text(stringResource(R.string.question_previous)) }
        }
        if (index < count - 1) {
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
