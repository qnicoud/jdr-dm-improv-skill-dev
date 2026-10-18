package io.github.qnicoud.dmgym.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.app.FakeAppContainer
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.app.navigation.AppNavDisplay
import io.github.qnicoud.dmgym.app.navigation.AppNavigator
import io.github.qnicoud.dmgym.app.navigation.Home
import io.github.qnicoud.dmgym.core.daily.DailyDoneLine
import io.github.qnicoud.dmgym.core.daily.ExerciseProgress
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.definitions.ConnectStory
import io.github.qnicoud.dmgym.core.exercise.definitions.ConstraintBox
import io.github.qnicoud.dmgym.core.exercise.definitions.CoreImage
import io.github.qnicoud.dmgym.core.exercise.definitions.CrazyEights
import io.github.qnicoud.dmgym.core.exercise.definitions.DecisionPoint
import io.github.qnicoud.dmgym.core.exercise.definitions.OnePage
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.exercise.definitions.StatusNumbers
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import io.github.qnicoud.dmgym.core.streak.DayMark
import io.github.qnicoud.dmgym.core.streak.DayStatus
import io.github.qnicoud.dmgym.core.streak.StreakSummary
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.feature.daily.DailyDoneScreen
import io.github.qnicoud.dmgym.feature.daily.DailyDoneUiState
import io.github.qnicoud.dmgym.feature.daily.DailyItem
import io.github.qnicoud.dmgym.feature.daily.DailyScreen
import io.github.qnicoud.dmgym.feature.daily.DailyUiState
import io.github.qnicoud.dmgym.feature.exercise.ExerciseDependencies
import io.github.qnicoud.dmgym.feature.exercise.ExerciseLaunch
import io.github.qnicoud.dmgym.feature.exercise.ExerciseRoute
import io.github.qnicoud.dmgym.feature.exercise.ExerciseTags
import io.github.qnicoud.dmgym.feature.exercise.ExerciseViewModel
import io.github.qnicoud.dmgym.feature.exercise.FakeEndSignal
import io.github.qnicoud.dmgym.feature.exercise.MutableClock
import io.github.qnicoud.dmgym.feature.exercise.RecordingHistoryRepository
import io.github.qnicoud.dmgym.feature.history.AnswerLine
import io.github.qnicoud.dmgym.feature.history.DayGroup
import io.github.qnicoud.dmgym.feature.history.DayLabel
import io.github.qnicoud.dmgym.feature.history.DetailActions
import io.github.qnicoud.dmgym.feature.history.DrawLine
import io.github.qnicoud.dmgym.feature.history.HistoryScreen
import io.github.qnicoud.dmgym.feature.history.HistoryUiState
import io.github.qnicoud.dmgym.feature.history.RubricLine
import io.github.qnicoud.dmgym.feature.history.SessionDetailScreen
import io.github.qnicoud.dmgym.feature.history.SessionDetailUi
import io.github.qnicoud.dmgym.feature.history.SessionDetailUiState
import io.github.qnicoud.dmgym.feature.history.SessionItem
import io.github.qnicoud.dmgym.feature.home.ExerciseSummary
import io.github.qnicoud.dmgym.feature.settings.AboutScreen
import io.github.qnicoud.dmgym.feature.settings.DurationRow
import io.github.qnicoud.dmgym.feature.settings.SettingsActions
import io.github.qnicoud.dmgym.feature.settings.SettingsScreen
import io.github.qnicoud.dmgym.feature.settings.SettingsUiState
import io.github.qnicoud.dmgym.feature.stats.ExerciseCount
import io.github.qnicoud.dmgym.feature.stats.StatsScreen
import io.github.qnicoud.dmgym.feature.stats.StatsUiState
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Socle commun des audits d'accessibilité : fenêtre haute (10 000 dp) pour que tous les nœuds soient placés, passes à
 * 1x et 2x, et parcours d'un exercice (prêt, en cours, bilan).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h10000dp")
abstract class AuditTestBase {
    @get:Rule
    val compose = createComposeRule()

    protected var fontScale by mutableFloatStateOf(1f)

    protected fun show(content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DmGymTheme { content() }
            }
        }
        compose.waitForIdle()
    }

    /** Audite l'écran affiché à 1x puis à 2x. */
    protected fun audit(
        screen: String,
        requireHeading: Boolean = true,
    ) {
        val audit = AccessibilityAudit(compose)
        val problems = mutableListOf<String>()
        listOf(1f, 2f).forEach { scale ->
            fontScale = scale
            compose.waitForIdle()
            val at = "$screen @${scale}x"
            problems += audit.smallTargets().map { "$at : $it" }
            problems += audit.unlabelledClickables().map { "$at : $it" }
            problems += audit.unlabelledTextFields().map { "$at : $it" }
            problems += audit.statefulWithoutRole().map { "$at : $it" }
            if (scale == 1f) audit.smallVisualTargets().forEach { println("VISUEL<48dp $at : $it") }
            if (scale == 2f) problems += audit.clippedTexts().map { "$at : $it" }
            if (requireHeading && audit.headingCount() == 0) problems += "$at : aucun titre (heading)"
        }
        fontScale = 1f
        compose.waitForIdle()
        assertTrue("Défauts d'accessibilité :\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    protected fun launchExercise(
        definition: ExerciseDefinition,
        options: Map<String, Boolean> = emptyMap(),
    ) {
        val vm =
            ExerciseViewModel(
                definition = definition,
                deps =
                    ExerciseDependencies(
                        RealTables.repository,
                        RecordingHistoryRepository(),
                        FakeSettingsRepository(Settings()),
                        FakeEndSignal(),
                        FakeMonotonicClock(1_000_000),
                        MutableClock(),
                        Dispatchers.Unconfined,
                    ),
                handle = SavedStateHandle(),
                launch = ExerciseLaunch(seed = 11),
            )
        options.forEach { (key, on) -> vm.onOptionChanged(key, on) }
        show { ExerciseRoute(vm, onBack = {}) }
    }

    protected fun tap(tag: String) {
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    protected fun exerciseJourney(
        name: String,
        definition: ExerciseDefinition,
        options: Map<String, Boolean> = emptyMap(),
    ) {
        launchExercise(definition, options)
        audit("$name prêt")
        tap(ExerciseTags.START)
        audit("$name en cours")
        tap(ExerciseTags.FINISH)
        audit("$name bilan")
    }
}
