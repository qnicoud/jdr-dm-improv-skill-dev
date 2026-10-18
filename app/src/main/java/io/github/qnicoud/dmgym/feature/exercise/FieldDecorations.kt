package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.IconKey
import io.github.qnicoud.dmgym.core.exercise.TimerUi
import io.github.qnicoud.dmgym.core.exercise.minuteFraction
import io.github.qnicoud.dmgym.ui.components.CategoryIcon

private const val MS_PER_SECOND = 1_000L

/**
 * Icône d'un champ d'après son [IconKey] (même table que les catégories de Connect the Story). `null` si la clé est
 * absente ou sans icône : le libellé texte suffit, l'icône n'est jamais seule porteuse du sens.
 */
internal fun fieldIcon(key: IconKey?): ImageVector? = CategoryIcon.fromCategory(key)?.icon

/** Libellé d'un champ, précédé de son icône décorative s'il y en a une. */
@Composable
internal fun FieldLabel(
    text: String,
    icon: ImageVector?,
) {
    if (icon == null) {
        Text(text)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text)
    }
}

/**
 * Repère discret de l'étape en cours (D58) : une fine barre qui se remplit sur [periodMs] (durée / étapes). Il informe seulement,
 * il ne déclenche jamais d'avance automatique. Lit [timer] dans sa propre portée, comme la barre du minuteur.
 */
@Composable
internal fun RhythmMarker(
    timer: () -> TimerUi,
    periodMs: Long,
    modifier: Modifier = Modifier,
) {
    // Le temps est lu dans les lambdas de dessin et de sémantique : un tic ne recompose pas ce composable.
    val resources = LocalResources.current
    val total = (periodMs / MS_PER_SECOND).toInt()
    LinearProgressIndicator(
        progress = { minuteFraction(timer().elapsedMs, periodMs) },
        modifier =
            modifier.testTag(ExerciseTags.RHYTHM_MARKER).semantics {
                val seconds = ((timer().elapsedMs % periodMs) / MS_PER_SECOND).toInt()
                contentDescription = resources.getString(R.string.rhythm_marker_description, seconds, total)
            },
    )
}
