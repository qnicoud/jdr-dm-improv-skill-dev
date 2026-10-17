package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons

/** Carte de tirage : éléments tirés, relance comptée et, si l'exercice détecte des mots, cases « utilisé ». */
@Composable
fun DrawCard(
    draws: List<Draw>,
    rerollCount: Int,
    showReroll: Boolean,
    showChecks: Boolean,
    onReroll: () -> Unit,
    onCheckToggled: (DrawKey) -> Unit,
    modifier: Modifier = Modifier,
    /** Slots affichés comme une carte de statut en grand (Status Numbers). */
    cardSlots: Set<String> = emptySet(),
) {
    val (cards, others) = draws.partition { it.slotKey in cardSlots }
    val spoken = stringResource(R.string.draw_description, others.joinToString(", ") { it.text })
    Card(
        modifier = modifier.fillMaxWidth().testTag(ExerciseTags.DRAW_CARD),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // La carte de statut porte son propre libellé : elle reste hors de la description fusionnée du tirage.
            cards.forEach { StatusCardLine(it) }
            if (others.isNotEmpty()) {
                Column(modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) {
                    others.forEach { DrawLine(it, showChecks, onCheckToggled) }
                }
            }
            if (showReroll) RerollRow(rerollCount, onReroll)
        }
    }
}

@Composable
private fun DrawLine(
    draw: Draw,
    showCheck: Boolean,
    onCheckToggled: (DrawKey) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
        Text(draw.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (showCheck) {
            Checkbox(
                checked = draw.check?.checked == true,
                onCheckedChange = { onCheckToggled(DrawKey(draw.slotKey, draw.position)) },
                modifier = Modifier.semantics { contentDescription = draw.text },
            )
        }
    }
}

@Composable
private fun RerollRow(
    rerollCount: Int,
    onReroll: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = onReroll, modifier = Modifier.heightIn(min = 48.dp).testTag(ExerciseTags.REROLL)) {
            Icon(ExerciseIcons.Dice, contentDescription = null)
            Text(stringResource(R.string.draw_reroll), modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            pluralStringResource(R.plurals.draw_reroll_count, rerollCount, rerollCount),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag(ExerciseTags.REROLL_COUNT),
        )
    }
}

/** État de chargement : un squelette de carte, jamais d'écran blanc. */
@Composable
fun DrawCardSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.exercise_loading)
    Card(
        modifier = modifier.fillMaxWidth().heightIn(min = 96.dp).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {}
}

/** Carte d'erreur (état X) : le détail technique reste dans Réglages > À propos. */
@Composable
fun DrawErrorCard(
    acceptsFreeSubject: Boolean,
    freeSubject: TextFieldState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag(ExerciseTags.DRAW_ERROR),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.draw_error_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.draw_error_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.draw_retry))
            }
            if (acceptsFreeSubject) {
                OutlinedTextField(
                    state = freeSubject,
                    label = { Text(stringResource(R.string.draw_free_subject)) },
                    placeholder = { Text(stringResource(R.string.draw_free_subject_hint)) },
                    modifier = Modifier.fillMaxWidth().testTag(ExerciseTags.FREE_SUBJECT),
                )
            }
        }
    }
}

/** Bouton principal de l'écran « Prêt ». */
@Composable
fun StartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(ExerciseTags.START)) {
        Text(stringResource(R.string.exercise_start), style = MaterialTheme.typography.titleMedium)
    }
}
