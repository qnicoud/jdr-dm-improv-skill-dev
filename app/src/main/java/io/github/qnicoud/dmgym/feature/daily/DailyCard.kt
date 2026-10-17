package io.github.qnicoud.dmgym.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.ui.components.nameRes

object DailyCardTags {
    const val CARD = "home_daily_card"
    const val BUTTON = "home_daily_button"
    const val DONE = "home_daily_done"
}

/**
 * Carte « Séance du jour » de l'accueil (S02) : date, les 3 exercices (« · Fait » sur ceux réalisés), durée,
 * et un seul bouton plein. Séance terminée : « Séance du jour terminée · Refaire un exercice ».
 */
@Composable
fun DailyCard(
    state: DailyUiState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag(DailyCardTags.CARD),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.home_daily_kicker),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(dailyDateTitle(state.epochDay), style = MaterialTheme.typography.titleLarge)
            state.items.forEach { item ->
                val name = stringResource(item.id.nameRes)
                val done = item.progress == ExerciseProgress.DONE
                Text(
                    text = if (done) stringResource(R.string.home_daily_exercise_done, name) else name,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Summary(state)
            Button(onClick = onOpen, modifier = Modifier.testTag(DailyCardTags.BUTTON).padding(top = 8.dp)) {
                Text(stringResource(buttonLabel(state)))
            }
        }
    }
}

@Composable
private fun Summary(state: DailyUiState) {
    if (state.allDone) {
        Text(
            text = stringResource(R.string.home_daily_done),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(DailyCardTags.DONE),
        )
    } else {
        val remaining = state.items.count { it.progress != ExerciseProgress.DONE }
        Text(
            text = pluralStringResource(R.plurals.home_daily_summary_todo, remaining, state.totalMinutes, remaining),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun buttonLabel(state: DailyUiState): Int =
    when {
        state.allDone -> R.string.home_daily_redo
        state.started -> R.string.daily_continue
        else -> R.string.daily_start
    }
