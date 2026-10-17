package io.github.qnicoud.dmgym.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons

private const val PREVIEW_LINES = 2
private const val DASH_ON = 8f
private const val DASH_OFF = 6f

/**
 * Ligne de l'historique : exercice, heure, aperçu de deux lignes et badges (bijou, niveau, « Inachevée »).
 * Les badges portent un mot ou une icône : aucune information ne passe par la couleur seule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCard(
    item: SessionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 88.dp).testTag(HistoryTags.card(item.id)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(DmIcons.forExercise(item.exerciseId), contentDescription = null, Modifier.size(24.dp))
                Text(stringResource(item.exerciseId.nameRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "· " + clockTime(item.startedAtEpochMillis, item.zoneId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.preview?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Badges(item)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Badges(item: SessionItem) {
    val level = Level.of(item.selfRating)
    if (!item.isGem && level == null && !item.unfinished) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (item.isGem) GemBadge()
        level?.let { Badge(stringResource(it.labelRes), it.icon) }
        if (item.unfinished) Badge(stringResource(R.string.history_unfinished), icon = null, dashed = true)
    }
}

/** Bijou : gemme pleine et libellé lu par TalkBack (la gemme seule ne dirait rien). */
@Composable
private fun GemBadge() {
    Surface(shape = RoundedCornerShape(percent = 50), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                ExerciseIcons.Diamond,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(R.string.gem_kept),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun Badge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    dashed: Boolean = false,
) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier =
            Modifier
                .drawBehind {
                    val stroke =
                        Stroke(
                            width = 1.dp.toPx(),
                            pathEffect =
                                if (dashed) {
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(DASH_ON, DASH_OFF),
                                    )
                                } else {
                                    null
                                },
                        )
                    drawRoundRect(outline, cornerRadius = CornerRadius(size.height / 2), style = stroke)
                }.padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
