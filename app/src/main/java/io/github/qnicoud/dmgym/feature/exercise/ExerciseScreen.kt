package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.components.PlaceholderScreen
import io.github.qnicoud.dmgym.ui.components.nameRes

/** Écran provisoire de l'exercice ; l'écran générique arrive au lot L5. */
@Composable
fun ExerciseScreen(
    exerciseId: ExerciseId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(title = stringResource(exerciseId.nameRes), onBack = onBack, modifier = modifier)
}
