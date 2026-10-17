package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.home.ExerciseSummary
import io.github.qnicoud.dmgym.feature.home.durationLabel
import io.github.qnicoud.dmgym.ui.components.disciplineRes
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons

/**
 * Feuille « Remplacer par… » (S03b) : les exercices hors séance, One-Page inclus (étiqueté « Préparation »).
 * Un choix remplace et ferme ; glisser ou toucher à l'extérieur annule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplaceSheet(
    replaced: ExerciseId,
    alternatives: List<ExerciseSummary>,
    onChoose: (ExerciseId) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag(DailyTags.SHEET)) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.daily_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.daily_sheet_subtitle, stringResource(replaced.nameRes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alternatives, key = { it.id.value }) { ChoiceRow(it, onClick = { onChoose(it.id) }) }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    summary: ExerciseSummary,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(DailyTags.choice(summary.id)),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(DmIcons.forExercise(summary.id), contentDescription = null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(summary.id.nameRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(summary.id.disciplineRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (summary.id == ExerciseId.ONE_PAGE) {
                    Text(
                        stringResource(R.string.exercise_tag_preparation),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(durationLabel(summary.durationSeconds), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
