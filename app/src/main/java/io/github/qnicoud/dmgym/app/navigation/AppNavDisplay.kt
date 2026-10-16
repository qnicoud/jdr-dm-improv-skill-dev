package io.github.qnicoud.dmgym.app.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.qnicoud.dmgym.app.AppContainer
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.daily.DailyScreen
import io.github.qnicoud.dmgym.feature.exercise.ExerciseScreen
import io.github.qnicoud.dmgym.feature.history.HistoryScreen
import io.github.qnicoud.dmgym.feature.history.SessionDetailScreen
import io.github.qnicoud.dmgym.feature.home.HomeRoute
import io.github.qnicoud.dmgym.feature.settings.SettingsScreen
import io.github.qnicoud.dmgym.feature.stats.StatsScreen

/**
 * Affiche la pile de retour (ADR 0003). Chaque entrée a son `ViewModelStore` et son état sauvegardé ;
 * le retour prédictif passe par `onBack`. La barre d'onglets n'apparaît que sur les trois écrans racines.
 */
@Composable
fun AppNavDisplay(
    backStack: List<NavKey>,
    navigator: AppNavigator,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val currentTab = navigator.currentTab()
    Scaffold(
        modifier = modifier,
        bottomBar = { currentTab?.let { AppNavigationBar(it, navigator::selectTab) } },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            onBack = { navigator.back() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider = appEntryProvider(navigator, container),
        )
    }
}

@Composable
private fun appEntryProvider(
    navigator: AppNavigator,
    container: AppContainer,
): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        entry<Home> {
            HomeRoute(
                viewModel = viewModel(factory = container.homeViewModelFactory),
                onExerciseClick = { id: ExerciseId -> navigator.openExercise(id.key) },
                onSettingsClick = navigator::openSettings,
            )
        }
        entry<Exercise> { route -> ExerciseScreen(route.exerciseId, onBack = { navigator.back() }) }
        entry<Daily> { DailyScreen(onBack = { navigator.back() }) }
        entry<History> { HistoryScreen() }
        entry<SessionDetail> { SessionDetailScreen(onBack = { navigator.back() }) }
        entry<Stats> { StatsScreen() }
        entry<Settings> { SettingsScreen(onBack = { navigator.back() }) }
    }
