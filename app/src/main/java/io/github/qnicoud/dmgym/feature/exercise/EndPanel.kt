package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons

/** Actions du bilan : toutes facultatives, rien n'est bloquant (parcours §4.5). */
class AssessActions(
    val onAssess: (rating: Int?, isGem: Boolean, rubric: Map<String, Boolean>) -> Unit,
    val onDone: () -> Unit,
)

/** Bilan S06 : récap neutre, niveau en 3 paliers, grille repliée, bijou, sorties « Terminé » et « Passer ». */
@Composable
fun EndPanel(
    definition: ExerciseDefinition,
    session: ExerciseSession,
    elapsedMs: Long,
    actions: AssessActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp).testTag(ExerciseTags.END_PANEL),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(
                R.string.end_title,
            ),
            style = MaterialTheme.typography.headlineSmall,
            modifier =
                Modifier.semantics {
                    heading()
                },
        )
        Text(stringResource(R.string.end_recap, shortDuration(elapsedMs)), style = MaterialTheme.typography.bodyLarge)
        LevelPicker(session.selfRating) { actions.onAssess(it, session.isGem, session.rubric) }
        RubricGrid(definition, session) { actions.onAssess(session.selfRating, session.isGem, it) }
        GemToggle(session.isGem) { actions.onAssess(session.selfRating, it, session.rubric) }
        Button(
            onClick = actions.onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(ExerciseTags.DONE),
        ) {
            Text(stringResource(R.string.end_done))
        }
        TextButton(
            onClick = actions.onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(ExerciseTags.SKIP),
        ) {
            Text(stringResource(R.string.end_skip))
        }
    }
}

@Composable
private fun LevelPicker(
    rating: Int?,
    onChange: (Int?) -> Unit,
) {
    val levels =
        listOf(
            Triple(1, R.string.level_draft, ExerciseIcons.Edit),
            Triple(2, R.string.level_solid, ExerciseIcons.Build),
            Triple(3, R.string.level_ready, ExerciseIcons.Shield),
        )
    Text(stringResource(R.string.end_level_title), style = MaterialTheme.typography.titleMedium)
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (value, label, icon) ->
            val selected = rating == value
            val name = stringResource(label)
            val description = stringResource(R.string.level_description, name, value)
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag(ExerciseTags.level(value))
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onChange(if (selected) null else value) },
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
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(if (selected) ExerciseIcons.Check else icon, contentDescription = null)
                    Text(name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun RubricGrid(
    definition: ExerciseDefinition,
    session: ExerciseSession,
    onChange: (Map<String, Boolean>) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = {
        open = !open
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(ExerciseTags.RUBRIC_TOGGLE)) {
        Text(stringResource(R.string.end_rubric_title))
    }
    if (!open) return
    definition.rubric.forEach { question ->
        val answer = session.rubric[question.key]
        Text(stringResource(question.text), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(true to R.string.rubric_yes, false to R.string.rubric_no).forEach { (value, label) ->
                FilterChip(
                    selected = answer == value,
                    onClick = {
                        onChange(
                            if (answer ==
                                value
                            ) {
                                session.rubric - question.key
                            } else {
                                session.rubric + (question.key to value)
                            },
                        )
                    },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(ExerciseTags.rubric(question.key, value)),
                )
            }
        }
    }
}

@Composable
private fun GemToggle(
    isGem: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val label = stringResource(if (isGem) R.string.gem_kept else R.string.gem_keep)
    FilterChip(
        selected = isGem,
        onClick = { onChange(!isGem) },
        label = { Text(label) },
        leadingIcon = { Icon(ExerciseIcons.Diamond, contentDescription = null) },
        modifier = Modifier.heightIn(min = 48.dp).testTag(ExerciseTags.GEM).semantics { stateDescription = label },
    )
}
