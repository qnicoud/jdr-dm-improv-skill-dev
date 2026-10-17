package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.feature.home.durationLabel
import io.github.qnicoud.dmgym.feature.home.spokenDuration
import io.github.qnicoud.dmgym.ui.components.disciplineRes
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons

private val NumberSize = 36.dp

/**
 * Ligne de la séance : numéro (disque), nom, discipline et durée, état (icône + mot, jamais la teinte seule),
 * puis « Remplacer » (à faire) ou « Refaire » (séance terminée). À 200 %, les actions passent à la ligne.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyRow(
    item: DailyItem,
    canRedo: Boolean,
    onReplace: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(item.id.nameRes)
    val discipline = stringResource(item.id.disciplineRes)
    val state = item.progress.label()
    val replaceDescription = stringResource(R.string.daily_replace_description, name)
    val redoDescription = stringResource(R.string.daily_redo_description, name)
    val description =
        stringResource(
            R.string.daily_row_description,
            item.position + 1,
            name,
            discipline,
            spokenDuration(item.durationSeconds),
            state,
        )
    Surface(
        modifier = modifier.fillMaxWidth().testTag(DailyTags.row(item.position)),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier =
                    Modifier.testTag(DailyTags.info(item.position)).clearAndSetSemantics {
                        contentDescription =
                            description
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberDisc(item.position + 1, item.progress)
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$discipline · ${durationLabel(item.durationSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StateLabel(item.progress, state)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.canReplace) {
                    TextButton(
                        onClick = onReplace,
                        modifier =
                            Modifier.testTag(DailyTags.replace(item.position)).semantics {
                                contentDescription = replaceDescription
                            },
                    ) { Text(stringResource(R.string.daily_replace)) }
                }
                if (canRedo) {
                    TextButton(
                        onClick = onOpen,
                        modifier =
                            Modifier.testTag(DailyTags.redo(item.position)).semantics {
                                contentDescription = redoDescription
                            },
                    ) {
                        Text(stringResource(R.string.daily_redo))
                    }
                }
            }
        }
    }
}

@Composable
private fun StateLabel(
    progress: ExerciseProgress,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val icon: ImageVector? =
            when (progress) {
                ExerciseProgress.DONE -> DmIcons.Check
                ExerciseProgress.IN_PROGRESS -> DmIcons.Play
                ExerciseProgress.TODO -> null
            }
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Disque numéroté : à contour (à faire), rempli secondaryContainer (fait), rempli primary (en cours). */
@Composable
private fun NumberDisc(
    number: Int,
    progress: ExerciseProgress,
) {
    val colors = MaterialTheme.colorScheme
    val (fill, content) =
        when (progress) {
            ExerciseProgress.DONE -> colors.secondaryContainer to colors.onSecondaryContainer
            ExerciseProgress.IN_PROGRESS -> colors.primary to colors.onPrimary
            ExerciseProgress.TODO -> Color.Transparent to colors.onSurface
        }
    Surface(
        modifier = Modifier.size(NumberSize),
        shape = CircleShape,
        color = fill,
        contentColor = content,
        border = if (progress == ExerciseProgress.TODO) BorderStroke(1.dp, colors.outline) else null,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) { Text(number.toString(), style = MaterialTheme.typography.labelLarge) }
    }
}
