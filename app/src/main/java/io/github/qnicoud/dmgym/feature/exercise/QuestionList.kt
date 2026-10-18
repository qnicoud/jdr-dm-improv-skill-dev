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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.revealedQuestions
import io.github.qnicoud.dmgym.core.exercise.visibleFields
import kotlinx.coroutines.flow.drop

/** Identifiants de test propres aux questions successives (Core Image Threading). */
object QuestionTags {
    const val PREVIOUS = "exercise_previous_question"
    const val LAST_FINISH = "exercise_last_question_finish"
    const val SKIP = "exercise_skip_question"
    const val SOFT_COUNTER = "exercise_soft_counter"
    const val CURRENT = "exercise_current_question"
    const val TOGGLE_LIST = "exercise_questions_toggle"
    const val LIST = "exercise_questions_list"
    const val READY_LIST = "exercise_questions_ready"
    const val READY_HIDDEN = "exercise_questions_ready_hidden"

    fun item(index: Int) = "exercise_question_$index"
}

/**
 * Ne garde dans l'état, pour la zone épinglée sous le minuteur, que les tirages de questions à y montrer : aucun en
 * disposition séquentielle ordinaire (ils chassent le champ de saisie quand le clavier s'ouvre), la seule question
 * courante quand les tirages sont révélés un à un.
 */
internal fun ExerciseUiState.withoutQuestionDraws(): ExerciseUiState {
    val layout = definition.layout as? FieldLayout.Sequential ?: return this
    val others = session.draws.filter { it.slotKey != layout.questionsSlot }
    val pinned = if (layout.revealProgressively) layout.revealedQuestions(session) else emptyList()
    return copy(session = session.copy(draws = others + pinned))
}

/** Vrai quand les tirages de la disposition séquentielle ne sont montrés qu'un à un (aucune relance, aucune liste). */
internal val ExerciseDefinition.revealsOneByOne: Boolean
    get() = (layout as? FieldLayout.Sequential)?.revealProgressively == true

/** Questions numérotées de l'écran Prêt, lisibles avant de lancer l'exercice (ou seulement leur nombre). */
@Composable
internal fun ReadyQuestions(
    state: ExerciseUiState,
    modifier: Modifier = Modifier,
) {
    val layout = state.definition.layout as? FieldLayout.Sequential ?: return
    val questions = state.session.drawsOf(layout.questionsSlot)
    if (questions.isEmpty()) return
    if (layout.revealProgressively) {
        Text(
            stringResource(R.string.questions_ready_hidden, questions.size),
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.testTag(QuestionTags.READY_HIDDEN),
        )
        return
    }
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
    val specs = layout.visibleFields(session.options)
    val keys = specs.map { AnswerKey(it.key, index) }
    val states = keys.map { handlers.fieldState(it) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(index) { showAll = false }
    LaunchedEffect(states) {
        snapshotFlow { states.map { it.text.toString() } }.drop(1).collect { showAll = false }
    }
    QuestionHeading(layout, questions.getOrNull(index), index, questions.size)
    specs.forEachIndexed { i, spec ->
        val position = if (specs.size > 1) i + 1 to specs.size else null
        val keyboard = if (i == specs.lastIndex) lastFieldKeyboard(index, questions.size, handlers) else null
        Field(spec, keys[i], position, session, handlers, if (i == 0) firstFieldFocus else null, keyboard = keyboard)
    }
    QuestionNavigation(index, questions.size, handlers)
    if (index < questions.size - 1 && states.all { it.text.isBlank() }) {
        TextButton(
            onClick = handlers.onNextQuestion,
            modifier = Modifier.heightIn(min = 48.dp).testTag(QuestionTags.SKIP),
        ) { Text(stringResource(R.string.question_skip)) }
    }
    if (layout.revealProgressively) return
    TextButton(
        onClick = { showAll = !showAll },
        modifier = Modifier.heightIn(min = 48.dp).testTag(QuestionTags.TOGGLE_LIST),
    ) { Text(stringResource(if (showAll) R.string.questions_hide_all else R.string.questions_show_all)) }
    if (showAll) NumberedQuestions(questions, Modifier.testTag(QuestionTags.LIST))
}

/**
 * Dernier champ d'une question : Suivant passe à la question suivante ; sur la dernière, Terminé ferme le clavier sans
 * finir l'exercice.
 */
private fun lastFieldKeyboard(
    index: Int,
    count: Int,
    handlers: FieldHandlers,
): FieldKeyboard =
    if (index < count - 1) FieldKeyboard(ImeAction.Next, handlers.onNextQuestion) else FieldKeyboard(ImeAction.Done)

/**
 * Titre de la question : « i/n — texte », ou [FieldLayout.Sequential.itemLabel] seul quand le texte est déjà épinglé
 * sous le minuteur (tirages révélés un à un), ou libellé puis texte quand l'exercice a son propre libellé.
 */
@Composable
private fun QuestionHeading(
    layout: FieldLayout.Sequential,
    question: Draw?,
    index: Int,
    count: Int,
) {
    if (question == null) return
    val tag = Modifier.testTag(QuestionTags.CURRENT)
    val label = layout.itemLabel?.let { stringResource(it, index + 1, count) }
    when {
        layout.revealProgressively -> {
            val text = label ?: stringResource(R.string.question_position, index + 1, count)
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = tag)
        }

        label != null -> {
            Column(modifier = tag) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(question.text, style = MaterialTheme.typography.titleLarge)
            }
        }

        else -> {
            val text = stringResource(R.string.question_numbered, index + 1, count, question.text)
            Text(text, style = MaterialTheme.typography.titleLarge, modifier = tag)
        }
    }
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
