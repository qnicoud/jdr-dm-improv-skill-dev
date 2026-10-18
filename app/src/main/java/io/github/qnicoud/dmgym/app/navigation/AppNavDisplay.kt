package io.github.qnicoud.dmgym.app.navigation

import android.os.Build
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.qnicoud.dmgym.app.AppContainer
import io.github.qnicoud.dmgym.core.daily.DailyStep
import io.github.qnicoud.dmgym.core.daily.dailyStep
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.feature.daily.DailyDoneRoute
import io.github.qnicoud.dmgym.feature.daily.DailyDoneViewModel
import io.github.qnicoud.dmgym.feature.daily.DailyRoute
import io.github.qnicoud.dmgym.feature.daily.DailyViewModel
import io.github.qnicoud.dmgym.feature.exercise.ExerciseEntry
import io.github.qnicoud.dmgym.feature.history.HistoryFilter
import io.github.qnicoud.dmgym.feature.history.HistoryRoute
import io.github.qnicoud.dmgym.feature.history.HistoryViewModel
import io.github.qnicoud.dmgym.feature.history.SessionDetailRoute
import io.github.qnicoud.dmgym.feature.history.SessionDetailViewModel
import io.github.qnicoud.dmgym.feature.home.HomeRoute
import io.github.qnicoud.dmgym.feature.home.HomeViewModel
import io.github.qnicoud.dmgym.feature.settings.AboutRoute
import io.github.qnicoud.dmgym.feature.settings.ReminderRoute
import io.github.qnicoud.dmgym.feature.settings.ReminderViewModel
import io.github.qnicoud.dmgym.feature.settings.SettingsRoute
import io.github.qnicoud.dmgym.feature.settings.SettingsViewModel
import io.github.qnicoud.dmgym.feature.stats.StatsRoute
import io.github.qnicoud.dmgym.feature.stats.StatsViewModel
import io.github.qnicoud.dmgym.ui.theme.LocalReduceMotion

/**
 * Affiche la pile de retour (ADR 0003). Chaque entrée a son `ViewModelStore` et son état sauvegardé ;
 * le retour prédictif passe par `onBack`. La barre d'onglets n'apparaît que sur les trois écrans racines.
 */
@Composable
fun AppNavDisplay(
    navigator: AppNavigator,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val currentTab = navigator.currentTab()
    val provider = remember(navigator, container) { appEntryProvider(navigator, container) }
    Scaffold(
        modifier = modifier,
        bottomBar = { currentTab?.let { AppNavigationBar(it, navigator::selectTab) } },
    ) { padding ->
        val decorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            )
        val content = Modifier.padding(padding).consumeWindowInsets(padding)
        if (LocalReduceMotion.current) {
            val instant = { ContentTransform(EnterTransition.None, ExitTransition.None) }
            NavDisplay(
                backStack = navigator.backStack,
                modifier = content,
                onBack = { navigator.back() },
                entryDecorators = decorators,
                transitionSpec = { instant() },
                popTransitionSpec = { instant() },
                predictivePopTransitionSpec = { instant() },
                entryProvider = provider,
            )
        } else {
            NavDisplay(
                backStack = navigator.backStack,
                modifier = content,
                onBack = { navigator.back() },
                entryDecorators = decorators,
                entryProvider = provider,
            )
        }
    }
}

private fun appEntryProvider(
    navigator: AppNavigator,
    container: AppContainer,
): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        entry<Home> {
            HomeRoute(
                viewModel = viewModel { HomeViewModel(container.exerciseCatalog, container.historyRepository) },
                dailyViewModel = viewModel { container.dailyViewModel() },
                onExerciseClick = { navigator.openExercise(it) },
                onDailyClick = navigator::openDaily,
                onSettingsClick = navigator::openSettings,
                onResumeClick = { navigator.openExercise(it.exerciseId, resumeSessionId = it.sessionId) },
                clock = container.clock,
            )
        }
        entry<Exercise> { route ->
            ExerciseEntry(
                route,
                container,
                onBack = { navigator.back() },
                daily = if (route.fromDaily) dailyStepOf(route.exerciseId, container) else null,
                onNextExercise = navigator::openNextDailyExercise,
                onDailyDone = navigator::finishDailySession,
            )
        }
        entry<Daily> {
            DailyRoute(
                viewModel = viewModel { container.dailyViewModel() },
                onBack = { navigator.back() },
                onOpenExercise = { navigator.openExercise(it, fromDaily = true) },
            )
        }
        entry<DailyDone> {
            DailyDoneRoute(
                viewModel =
                    viewModel {
                        DailyDoneViewModel(
                            container.historyRepository,
                            container.settingsRepository,
                            container.clock,
                        )
                    },
                onHome = { navigator.selectTab(Home) },
                onRedo = { navigator.back() },
                onBack = { navigator.back() },
            )
        }
        entry<History> { route ->
            HistoryRoute(
                viewModel =
                    viewModel {
                        HistoryViewModel(
                            container.historyRepository,
                            container.clock,
                            route.filter,
                            createSavedStateHandle(),
                        )
                    },
                onOpenSession = navigator::openSessionDetail,
                onChooseExercise = { navigator.selectTab(Home) },
                onSettingsClick = navigator::openSettings,
            )
        }
        entry<SessionDetail> { route ->
            SessionDetailRoute(
                viewModel =
                    viewModel {
                        SessionDetailViewModel(
                            route.sessionId,
                            container.historyRepository,
                            container.exerciseRegistry,
                            container.tableRepository,
                        )
                    },
                onBack = { navigator.back() },
                onRedo = { navigator.openExercise(it) },
            )
        }
        entry<Stats> {
            StatsRoute(
                viewModel = viewModel { StatsViewModel(container.historyRepository, container.clock) },
                onChooseExercise = { navigator.selectTab(Home) },
                onShowGems = { navigator.openHistory(HistoryFilter.Gems) },
                onShowExercise = { navigator.openHistory(HistoryFilter.exercise(it)) },
                onSettingsClick = navigator::openSettings,
            )
        }
        entry<Settings> {
            SettingsRoute(
                viewModel =
                    viewModel {
                        SettingsViewModel(
                            container.settingsRepository,
                            Build.VERSION.SDK_INT,
                            container.endSignal,
                            container.sessionArchive,
                        )
                    },
                onBack = { navigator.back() },
                onOpenAbout = navigator::openAbout,
                reminder = {
                    ReminderRoute(
                        viewModel =
                            viewModel {
                                ReminderViewModel(
                                    container.settingsRepository,
                                    container.historyRepository,
                                    container.reminderDelivery,
                                    container.notificationPermissionGate,
                                    container.clock,
                                )
                            },
                    )
                },
            )
        }
        entry<About> { AboutRoute(onBack = { navigator.back() }) }
    }

/** Place de l'exercice dans la séance du jour, lue dans l'état de S03 ; `null` tant qu'il se charge. */
@Composable
private fun dailyStepOf(
    exercise: ExerciseId,
    container: AppContainer,
): DailyStep? {
    val viewModel = viewModel { container.dailyViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    return remember(state, exercise) {
        if (state.isLoading) null else dailyStep(state.items.map { it.id }, state.items.map { it.progress }, exercise)
    }
}

private fun AppContainer.dailyViewModel() =
    DailyViewModel(historyRepository, settingsRepository, exerciseCatalog, clock)
