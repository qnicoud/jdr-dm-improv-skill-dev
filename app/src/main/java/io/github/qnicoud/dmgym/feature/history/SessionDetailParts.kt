package io.github.qnicoud.dmgym.feature.history

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.components.CategoryIcon
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons
import io.github.qnicoud.dmgym.ui.icons.HistoryIcons

/** Tirage de la session : chaque élément avec « utilisé » ou « non utilisé » en toutes lettres quand il est suivi. */
@Composable
fun DrawSection(draws: List<DrawLine>) {
    if (draws.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth().testTag(DetailTags.DRAW_CARD),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.detail_draw_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            draws.forEachIndexed { index, line -> DrawRow(index, line) }
        }
    }
}

@Composable
private fun DrawRow(
    index: Int,
    line: DrawLine,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .semantics(mergeDescendants = true) {}
                .testTag(DetailTags.draw(index)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon.fromCategory(line.category)?.let {
            Icon(it.icon, contentDescription = stringResource(it.label), modifier = Modifier.size(24.dp))
        }
        Text(line.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        line.used?.let { UsedMark(it) }
    }
}

@Composable
private fun UsedMark(used: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            if (used) ExerciseIcons.Check else HistoryIcons.Circle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(if (used) R.string.detail_word_used else R.string.detail_word_unused),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Texte saisi, en lecture confortable : jamais éditable (D40). */
@Composable
fun AnswersSection(answers: List<AnswerLine>) {
    if (answers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.detail_answers_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        answers.forEachIndexed { index, answer -> AnswerBlock(index, answer) }
    }
}

@Composable
private fun AnswerBlock(
    index: Int,
    answer: AnswerLine,
) {
    val label =
        answer.fieldLabel?.let { stringResource(it) }
            ?: stringResource(R.string.detail_answer_default_label, answer.fieldIndex)
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag(DetailTags.answer(index)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val heading = answer.group?.let { groupHeading(answer, it) + " · " + label } ?: label
        Text(heading, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(answer.text, style = MaterialTheme.typography.bodyLarge)
        if (answer.flagged) {
            answer.flagLabel?.let { Text(stringResource(it), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun groupHeading(
    answer: AnswerLine,
    group: Int,
): String {
    val itemLabel = answer.groupLabel ?: return stringResource(R.string.detail_answer_group, group)
    return stringResource(itemLabel, group, answer.groupCount ?: group)
}

/** Niveau (3 paliers, une touche pour choisir, une seconde pour retirer) et bijou, modifiables à tout moment. */
@Composable
fun EvaluationSection(
    detail: SessionDetailUi,
    onLevelSelected: (Int) -> Unit,
    onGemToggled: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.detail_level_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Level.entries.forEach { LevelRow(it, detail.selfRating == it.value, onLevelSelected) }
        }
        if (detail.selfRating == null) {
            Text(
                stringResource(R.string.detail_no_rating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DetailTags.NO_RATING),
            )
        }
        GemChip(detail.isGem, onGemToggled)
    }
}

@Composable
private fun LevelRow(
    level: Level,
    selected: Boolean,
    onSelect: (Int) -> Unit,
) {
    val name = stringResource(level.labelRes)
    val description = stringResource(R.string.level_description, name, level.value)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(DetailTags.level(level.value))
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onSelect(level.value) },
                ).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(if (selected) ExerciseIcons.Check else level.icon, contentDescription = null)
            Text(name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GemChip(
    isGem: Boolean,
    onToggle: () -> Unit,
) {
    val label = stringResource(if (isGem) R.string.gem_kept else R.string.gem_keep)
    FilterChip(
        selected = isGem,
        onClick = onToggle,
        label = { Text(label) },
        leadingIcon = { Icon(ExerciseIcons.Diamond, contentDescription = null) },
        modifier = Modifier.heightIn(min = 48.dp).testTag(DetailTags.GEM).semantics { stateDescription = label },
    )
}

/** Réponses à la grille d'auto-évaluation, en lecture seule. */
@Composable
fun RubricSection(rubric: List<RubricLine>) {
    if (rubric.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.detail_rubric_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        rubric.forEach {
            val answer = stringResource(if (it.value) R.string.rubric_yes else R.string.rubric_no)
            Text(stringResource(R.string.detail_rubric_line, stringResource(it.question), answer))
        }
    }
}

/** Réponse choisie dans le bilan « Laquelle aurais-tu jouée en premier ? », en lecture seule. */
@Composable
fun FirstPickSection(
    text: String?,
    @StringRes title: Int = R.string.detail_first_pick_title,
) {
    if (text == null) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag(DetailTags.FIRST_PICK)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(text)
    }
}

@Composable
fun RedoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(DetailTags.REDO),
    ) {
        Icon(ExerciseIcons.Dice, contentDescription = null)
        Text(stringResource(R.string.detail_redo), modifier = Modifier.padding(start = 8.dp))
    }
}
