package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.DmIcons

private val DETAILS_MAX_HEIGHT = 140.dp
private const val COLLAPSED_ROTATION = 90f
private const val EXPANDED_ROTATION = -90f

/**
 * Zone de tirage en version compacte (paysage, fenêtre basse avec le clavier) : le tirage tient sur une ligne,
 * un chevron le déplie à la demande, dans une zone de hauteur bornée. Pendant le chargement ou en cas d'erreur
 * de tirage, la carte habituelle reste affichée.
 */
@Composable
internal fun CompactDrawArea(
    state: ExerciseUiState,
    actions: ExerciseActions,
) {
    val summary =
        state
            .withoutQuestionDraws()
            .session.draws
            .joinToString(" · ") { it.text }
    if (state.load != LoadStatus.Ready || summary.isEmpty()) {
        DrawArea(state, actions)
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val action = stringResource(if (expanded) R.string.pinned_summary_collapse else R.string.pinned_summary_expand)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClickLabel = action, role = Role.Button) { expanded = !expanded }
                .testTag(ExerciseTags.PINNED_SUMMARY),
    ) {
        Text(
            summary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Icon(
            DmIcons.Chevron,
            contentDescription = null,
            modifier = Modifier.rotate(if (expanded) EXPANDED_ROTATION else COLLAPSED_ROTATION),
        )
    }
    if (expanded) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = DETAILS_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState())
                    .testTag(ExerciseTags.PINNED_DETAILS),
        ) { DrawArea(state, actions) }
    }
}
