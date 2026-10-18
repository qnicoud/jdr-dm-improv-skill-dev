package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.boundFields

/**
 * Écran Prêt d'un exercice à champs liés (Échec qui avance) : la phrase d'aide, puis la définition de chaque type
 * tiré. Rien pendant le jeu : les champs portent déjà leur libellé et leur indication.
 */
@Composable
internal fun BoundTypesHelp(state: ExerciseUiState) {
    val layout = state.definition.layout as? FieldLayout.Fixed ?: return
    val bound = layout.boundFields(state.session.draws).filterNotNull()
    if (bound.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        layout.boundHint?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(ExerciseExtraTags.BOUND_HINT),
            )
        }
        bound.forEach { type ->
            val help = layout.helpByCode[type.code] ?: return@forEach
            Column(modifier = Modifier.testTag(ExerciseExtraTags.BOUND_HELP)) {
                Text(type.name, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(help), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
