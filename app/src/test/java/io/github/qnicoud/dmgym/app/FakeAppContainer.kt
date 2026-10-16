package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.feature.home.DefaultExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.ExerciseCatalog

/** Conteneur de test : aucune dépendance Android ni stockage. */
class FakeAppContainer(
    override val exerciseCatalog: ExerciseCatalog = DefaultExerciseCatalog(),
) : AppContainer
