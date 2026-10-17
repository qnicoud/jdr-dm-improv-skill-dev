package io.github.qnicoud.dmgym.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.components.nameRes
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val PREVIEW_LINES = 2

/** Temps écoulé depuis la dernière activité du brouillon. */
sealed interface Ago {
    data object Now : Ago

    data class Minutes(
        val count: Int,
    ) : Ago

    data class Hours(
        val count: Int,
    ) : Ago
}

/** « À l'instant », « il y a 12 min », « il y a 3 h » ; une horloge qui recule compte comme « à l'instant ». */
fun agoOf(
    lastActivity: Long,
    now: Long,
): Ago {
    val minutes = ((now - lastActivity) / MS_PER_MINUTE).coerceAtLeast(0)
    return when {
        minutes < 1 -> Ago.Now
        minutes < MINUTES_PER_HOUR -> Ago.Minutes(minutes.toInt())
        else -> Ago.Hours((minutes / MINUTES_PER_HOUR).toInt())
    }
}

@Composable
private fun Ago.text(): String =
    when (this) {
        Ago.Now -> stringResource(R.string.resume_ago_now)
        is Ago.Minutes -> stringResource(R.string.resume_ago_minutes, count)
        is Ago.Hours -> stringResource(R.string.resume_ago_hours, count)
    }

object ResumeTags {
    const val CARD = "home_resume_card"
    const val ACTION = "home_resume_action"
}

/**
 * Carte « Reprendre » de l'accueil (S02) : exercice, brouillon, ancienneté et début du texte. Un seul bouton
 * rouvre l'exercice là où on s'était arrêté.
 */
@Composable
fun ResumeCard(
    resume: ResumeUi,
    nowMillis: Long,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(resume.exerciseId.nameRes)
    val meta =
        stringResource(R.string.home_resume_meta, name, agoOf(resume.lastActivityAtEpochMillis, nowMillis).text())
    val actionDescription = stringResource(R.string.home_resume_action_description, name)
    Card(
        modifier = modifier.fillMaxWidth().testTag(ResumeTags.CARD),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.home_resume_kicker),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(meta, style = MaterialTheme.typography.bodyMedium)
            Text(
                resume.preview,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            FilledTonalButton(
                onClick = onResume,
                modifier =
                    Modifier
                        .heightIn(min = 48.dp)
                        .testTag(ResumeTags.ACTION)
                        .semantics { contentDescription = actionDescription },
            ) {
                Text(stringResource(R.string.home_resume_action))
            }
        }
    }
}

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ResumeCardPreview() {
    DmGymTheme {
        ResumeCard(
            resume = ResumeUi(1, ExerciseId.CONSTRAINT_BOX, "Le bois du comptoir est tiède sous la paume…", 0),
            nowMillis = 12 * MS_PER_MINUTE,
            onResume = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
