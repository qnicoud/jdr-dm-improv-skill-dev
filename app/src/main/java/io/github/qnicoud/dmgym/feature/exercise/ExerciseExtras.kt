package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseOption
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.StatusLevel
import io.github.qnicoud.dmgym.core.exercise.statusLevel

/** Identifiants de test des éléments propres aux exercices guidés (options, rappel, carte de statut, bascule). */
object ExerciseExtraTags {
    const val OPTIONS = "exercise_options"
    const val REMINDER = "exercise_reminder"
    const val STATUS_CARD = "exercise_status_card"
    const val SHIFT_BANNER = "exercise_shift_banner"
    const val SCENE_DONE = "exercise_scene_done"
    const val END_NOTE = "exercise_end_note"

    fun option(key: String) = "exercise_option_$key"

    fun optionHelp(key: String) = "exercise_option_help_$key"
}

/** Interrupteurs propres à l'exercice, affichés avant le départ ; les valeurs sont enregistrées avec la session. */
@Composable
fun OptionsPanel(
    options: List<ExerciseOption>,
    values: Map<String, Boolean>,
    onChanged: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().testTag(ExerciseExtraTags.OPTIONS)) {
        Text(stringResource(R.string.options_title), style = MaterialTheme.typography.titleMedium)
        options.forEach { option ->
            val checked = values[option.key] ?: option.default
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(ExerciseExtraTags.option(option.key))
                        .toggleable(value = checked, role = Role.Switch, onValueChange = { onChanged(option.key, it) }),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(option.label), modifier = Modifier.weight(1f).padding(end = 16.dp))
                    Switch(checked = checked, onCheckedChange = null)
                }
                option.help?.let {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .padding(end = 16.dp, bottom = 8.dp)
                                .testTag(ExerciseExtraTags.optionHelp(option.key)),
                    )
                }
            }
        }
    }
}

/** Rappel repliable (Status Numbers : le statut n'est pas le rang). */
@Composable
fun ReminderCard(
    definition: ExerciseDefinition,
    modifier: Modifier = Modifier,
    startOpen: Boolean = true,
) {
    val reminder = definition.reminder ?: return
    var open by rememberSaveable { mutableStateOf(startOpen) }
    Card(
        modifier = modifier.fillMaxWidth().testTag(ExerciseExtraTags.REMINDER),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            TextButton(onClick = { open = !open }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(if (open) R.string.reminder_hide else R.string.reminder_show))
            }
            if (open) Text(stringResource(reminder), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Carte de statut : la valeur en grand, le libellé écrit (la valeur n'est jamais portée par la couleur seule). */
@Composable
fun StatusCardLine(
    draw: Draw,
    modifier: Modifier = Modifier,
) {
    val value = draw.text.toIntOrNull() ?: return
    val level = stringResource(statusLevel(value).labelRes())
    val label = stringResource(R.string.status_card_label, value, level)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(ExerciseExtraTags.STATUS_CARD)
                .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(draw.text, style = MaterialTheme.typography.displayLarge.copy(fontSize = CARD_VALUE_SP.sp))
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

private const val CARD_VALUE_SP = 84

private fun StatusLevel.labelRes(): Int =
    when (this) {
        StatusLevel.Low -> R.string.status_level_low
        StatusLevel.Medium -> R.string.status_level_medium
        StatusLevel.High -> R.string.status_level_high
    }

/** Bandeau de bascule : il reste affiché jusqu'à la fin ; région polie, sans clignotement. */
@Composable
fun ShiftBanner(
    draw: Draw,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(ExerciseExtraTags.SHIFT_BANNER)
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
    ) {
        Text(
            stringResource(R.string.shift_banner, draw.text),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Grand bouton qui termine une scène jouée à l'oral (aucune saisie pendant le jeu). */
@Composable
fun SceneDoneButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(ExerciseExtraTags.SCENE_DONE),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

/** Notes facultatives du bilan (Status Numbers) : ce que le MJ a remarqué pendant la scène. */
@Composable
fun EndNote(
    spec: FieldSpec,
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = state,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(ExerciseExtraTags.END_NOTE),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { spec.label?.let { Text(stringResource(it)) } },
        placeholder = { spec.hint?.let { Text(stringResource(it)) } },
        lineLimits =
            TextFieldLineLimits.MultiLine(
                minHeightInLines = spec.minLines,
                maxHeightInLines = maxOf(NOTE_MAX_LINES, spec.minLines),
            ),
    )
}

private const val NOTE_MAX_LINES = 6
