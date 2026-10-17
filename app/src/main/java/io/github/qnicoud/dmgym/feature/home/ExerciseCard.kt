package io.github.qnicoud.dmgym.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.components.disciplineRes
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons

private const val SECONDS_PER_MINUTE = 60

/** Carte d'exercice (design-system §5.1) : toute la carte est la cible tactile, lue comme un seul élément. */
@Composable
fun ExerciseCard(
    summary: ExerciseSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(summary.id.nameRes)
    val discipline = stringResource(summary.id.disciplineRes)
    val description =
        stringResource(R.string.exercise_card_description, name, discipline, spokenDuration(summary.durationSeconds))
    Surface(
        onClick = onClick,
        modifier =
            modifier.heightIn(min = 72.dp).testTag(HomeTags.card(summary.id)).semantics {
                contentDescription =
                    description
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExercisePastille(summary.id)
            CardBody(summary.id, name, discipline, summary.durationSeconds, Modifier.weight(1f))
            Icon(DmIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExercisePastille(id: ExerciseId) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(DmIcons.forExercise(id), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardBody(
    id: ExerciseId,
    name: String,
    discipline: String,
    durationSeconds: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(discipline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            if (id == ExerciseId.ONE_PAGE) PreparationTag()
            DurationChip(durationSeconds)
        }
    }
}

@Composable
private fun PreparationTag() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = stringResource(R.string.exercise_tag_preparation),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun DurationChip(durationSeconds: Int?) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (durationSeconds == null) DmIcons.TimerOff else DmIcons.Timer,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = durationLabel(durationSeconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
internal fun durationLabel(durationSeconds: Int?): String =
    when {
        durationSeconds == null -> {
            stringResource(R.string.duration_none)
        }

        durationSeconds % SECONDS_PER_MINUTE == 0 -> {
            stringResource(R.string.duration_minutes, durationSeconds / SECONDS_PER_MINUTE)
        }

        else -> {
            stringResource(R.string.duration_seconds, durationSeconds)
        }
    }

@Composable
internal fun spokenDuration(durationSeconds: Int?): String =
    when {
        durationSeconds == null -> {
            stringResource(R.string.duration_none)
        }

        durationSeconds % SECONDS_PER_MINUTE == 0 -> {
            val minutes = durationSeconds / SECONDS_PER_MINUTE
            pluralStringResource(R.plurals.duration_minutes_spoken, minutes, minutes)
        }

        else -> {
            pluralStringResource(R.plurals.duration_seconds_spoken, durationSeconds, durationSeconds)
        }
    }
