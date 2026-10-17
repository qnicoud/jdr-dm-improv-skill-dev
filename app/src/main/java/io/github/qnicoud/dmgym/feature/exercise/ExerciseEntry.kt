package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.qnicoud.dmgym.app.AppContainer
import io.github.qnicoud.dmgym.app.navigation.Exercise
import io.github.qnicoud.dmgym.core.daily.DailyStep
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.ui.components.PlaceholderScreen
import io.github.qnicoud.dmgym.ui.components.nameRes

/**
 * Entrée de navigation de l'écran d'exercice. Tant qu'un exercice n'a pas de définition (lots L8 et L10),
 * l'écran provisoire reste affiché. [daily] place l'exercice dans la séance du jour (`null` hors séance) ;
 * [onNextExercise] et [onDailyDone] enchaînent depuis le bilan.
 */
@Composable
fun ExerciseEntry(
    route: Exercise,
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    daily: DailyStep? = null,
    onNextExercise: (ExerciseId) -> Unit = {},
    onDailyDone: () -> Unit = {},
) {
    val definition = container.exerciseRegistry[route.exerciseId]
    if (definition == null) {
        PlaceholderScreen(title = stringResource(route.exerciseId.nameRes), onBack = onBack, modifier = modifier)
        return
    }
    val viewModel =
        viewModel {
            ExerciseViewModel(
                definition = definition,
                deps = container.exerciseDependencies,
                handle = createSavedStateHandle(),
                launch = ExerciseLaunch(route.resumeSessionId, route.fromDaily),
            )
        }
    ExerciseRoute(viewModel, onBack, modifier, daily, onNextExercise, onDailyDone, route.fromDaily && daily == null)
}
