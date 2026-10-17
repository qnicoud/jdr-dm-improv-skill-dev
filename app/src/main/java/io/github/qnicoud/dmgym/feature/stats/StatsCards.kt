package io.github.qnicoud.dmgym.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.icons.DmIcons

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private val BarHeight = 12.dp

/** Série, jour de grâce, rangée des 7 jours et meilleure série. Aucune couleur d'alerte, même rompue (D15). */
@Composable
fun StreakCard(state: StatsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StreakHeadline(state)
            if (state.streak.current > 0) {
                Text(
                    text = graceText(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(StatsTags.GRACE),
                )
            }
            RecentDaysRow(state.recent, state.today)
            Text(
                text = pluralStringResource(R.plurals.stats_best_streak, state.streak.best, state.streak.best),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(StatsTags.BEST),
            )
        }
    }
}

@Composable
private fun StreakHeadline(state: StatsUiState) {
    val current = state.streak.current
    val text =
        when {
            current == 0 -> stringResource(R.string.stats_streak_waiting)
            state.isStreakRestart -> pluralStringResource(R.plurals.stats_streak_restart, current, current)
            else -> pluralStringResource(R.plurals.stats_streak, current, current)
        }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(DmIcons.Fire, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text(
            text = text,
            style = if (current == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(StatsTags.STREAK),
        )
    }
}

@Composable
private fun graceText(state: StatsUiState): String =
    if (state.streak.graceUsedThisWeek(state.today)) {
        stringResource(R.string.stats_grace_used)
    } else {
        stringResource(R.string.stats_grace_available)
    }

/** Répartition par exercice : la liste vaut le graphique (texte alternatif pour TalkBack) et personne n'est pointé du doigt. */
@Composable
fun ExerciseBars(counts: List<ExerciseCount>) {
    val max = counts.maxOf { it.count }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.stats_by_exercise),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        counts.forEach { ExerciseBar(it, max) }
    }
}

@Composable
private fun ExerciseBar(
    entry: ExerciseCount,
    max: Int,
) {
    val name = stringResource(entry.id.nameRes)
    val description = pluralStringResource(R.plurals.stats_exercise_count, entry.count, name, entry.count)
    Column(
        Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }.testTag(StatsTags.bar(entry.id)),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).clearAndSetSemantics {},
            )
            Text(
                entry.count.toString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(percent = 50)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(entry.count.toFloat() / max)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(percent = 50)),
            )
        }
    }
}

/** Bijoux et temps cumulé ; masqués tant qu'ils valent zéro (pas de zéro écrasant). */
@Composable
fun Extras(
    state: StatsUiState,
    onShowGems: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.gems > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    DmIcons.Gem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = pluralStringResource(R.plurals.stats_gems, state.gems, state.gems),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(StatsTags.GEMS),
                )
                TextButton(onClick = onShowGems, modifier = Modifier.testTag(StatsTags.GEMS_LINK)) {
                    Text(stringResource(R.string.stats_gems_link))
                }
            }
        }
        if (state.totalSeconds >= SECONDS_PER_MINUTE) {
            Text(
                text = timeText(state.totalSeconds),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(StatsTags.TIME),
            )
        }
    }
}

@Composable
private fun timeText(totalSeconds: Long): String {
    val totalMinutes = totalSeconds / SECONDS_PER_MINUTE
    val hours = totalMinutes / MINUTES_PER_HOUR
    return if (hours > 0) {
        stringResource(R.string.stats_time_hours, hours.toInt(), (totalMinutes % MINUTES_PER_HOUR).toInt())
    } else {
        stringResource(R.string.stats_time_minutes, totalMinutes.toInt())
    }
}
