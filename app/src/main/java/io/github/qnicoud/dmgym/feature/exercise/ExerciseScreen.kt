package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.components.PlaceholderScreen
import io.github.qnicoud.dmgym.ui.components.nameRes

/** Écran provisoire de l'exercice ; l'écran générique arrive au lot L5. */
@Composable
fun ExerciseScreen(
    exerciseId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = ExerciseId.fromKey(exerciseId)?.let { stringResource(it.nameRes) } ?: stringResource(R.string.app_name)
    PlaceholderScreen(title = title, onBack = onBack, modifier = modifier)
}
