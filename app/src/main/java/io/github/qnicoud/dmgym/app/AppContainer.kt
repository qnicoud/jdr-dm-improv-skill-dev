package io.github.qnicoud.dmgym.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.qnicoud.dmgym.feature.home.DefaultExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.ExerciseCatalog
import io.github.qnicoud.dmgym.feature.home.HomeViewModel

/**
 * Injection manuelle (ADR 0005). Seul endroit qui connaît les implémentations ; les classes métier
 * reçoivent leurs dépendances par le constructeur. Les autres ports (historique, réglages, tables…)
 * s'ajoutent ici au fil des lots.
 */
interface AppContainer {
    val exerciseCatalog: ExerciseCatalog

    val homeViewModelFactory: ViewModelProvider.Factory
        get() = viewModelFactory { initializer { HomeViewModel(exerciseCatalog) } }
}

class DefaultAppContainer : AppContainer {
    override val exerciseCatalog: ExerciseCatalog by lazy { DefaultExerciseCatalog() }
}
