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
 * Audit d'accessibilité de la recette RC1 (design system §7) sur les écrans principaux, largeur 360 dp. La fenêtre
 * est haute (5000 dp) pour que tous les nœuds soient placés et mesurables : cibles de 48 dp, libellés des éléments
 * cliquables, titres, et textes non coupés à `fontScale = 2`. Le contrôle « zone épinglée sur 640 dp avec clavier »
 * reste manuel (checklist RC1).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h5000dp")
class AccessibilityAuditTest {
    @get:Rule
    val compose = createComposeRule()

    private var fontScale by mutableFloatStateOf(1f)

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DmGymTheme { content() }
            }
        }
        compose.waitForIdle()
    }

    /** Audite l'écran affiché à 1x puis à 2x. */
    private fun audit(
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

    // --- Accueil -----------------------------------------------------------------------------------------------

    @Test
    fun `home screen passes the audit`() {
        val navigator = AppNavigator(NavBackStack<NavKey>(Home))
        val container = FakeAppContainer()
        show { AppNavDisplay(navigator = navigator, container = container) }
        audit("accueil")
    }

    // --- Exercices : prêt, en cours, bilan ---------------------------------------------------------------------

    private fun launchExercise(definition: ExerciseDefinition) {
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
        show { ExerciseRoute(vm, onBack = {}) }
    }

    private fun tap(tag: String) {
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun exerciseJourney(
        name: String,
        definition: ExerciseDefinition,
    ) {
        launchExercise(definition)
        audit("$name prêt")
        tap(ExerciseTags.START)
        audit("$name en cours")
        tap(ExerciseTags.FINISH)
        audit("$name bilan")
    }

    @Test
    fun `crazy eights ready, running and end pass the audit`() = exerciseJourney("Crazy Eights", CrazyEights.definition)

    @Test
    fun `constraint box ready, running and end pass the audit`() =
        exerciseJourney("Constraint Box", ConstraintBox.definition)

    @Test
    fun `connect the story ready, running and end pass the audit`() =
        exerciseJourney("Connect the Story", ConnectStory.definition)

    @Test
    fun `core image ready, running and end pass the audit`() = exerciseJourney("Core Image", CoreImage.definition)

    @Test
    fun `status numbers ready, running and end pass the audit`() =
        exerciseJourney("Status Numbers", StatusNumbers.definition)

    @Test
    fun `decision point ready, running and end pass the audit`() =
        exerciseJourney("Decision Point", DecisionPoint.definition)

    @Test
    fun `one page ready, running and end pass the audit`() = exerciseJourney("One-Page", OnePage.definition)

    // --- Historique, détail, progrès ---------------------------------------------------------------------------

    @Test
    fun `history screen passes the audit`() {
        fun item(
            id: Long,
            exercise: ExerciseId = ExerciseId.CONSTRAINT_BOX,
            unfinished: Boolean = false,
        ) = SessionItem(id, exercise, 1_700_000_000_000, "UTC", "Le bois du comptoir", id == 1L, 2, unfinished)
        val state =
            HistoryUiState(
                isLoading = false,
                totalCount = 3,
                groups =
                    listOf(
                        DayGroup(DayLabel.Today, listOf(item(1))),
                        DayGroup(DayLabel.Yesterday, listOf(item(2, unfinished = true), item(3, ExerciseId.ONE_PAGE))),
                    ),
            )
        show {
            HistoryScreen(
                state = state,
                onFilterSelected = {},
                onClearFilter = {},
                onOpenSession = {},
                onChooseExercise = {},
                onSettingsClick = {},
            )
        }
        audit("historique")
    }

    @Test
    fun `session detail screen passes the audit`() {
        val detail =
            SessionDetailUi(
                sessionId = 7,
                exerciseId = ExerciseId.CONSTRAINT_BOX,
                startedAtEpochMillis = 1_700_000_000_000,
                zoneId = "UTC",
                activeSec = 300,
                overtimeSec = 42,
                unfinished = false,
                draws = listOf(DrawLine("tiède", true), DrawLine("moisi", false), DrawLine("Auberge de relais", null)),
                answers =
                    listOf(
                        AnswerLine(null, 1, null, "Le bois du comptoir est tiède sous la paume.", false, null),
                        AnswerLine(null, 2, null, "Personne ne lève la tête.", true, R.string.gem_kept),
                    ),
                rubric = listOf(RubricLine(R.string.rubric_yes, true)),
                selfRating = 3,
                isGem = false,
                shareText = "texte",
            )
        val actions = DetailActions({}, {}, {}, {}, {}, {}, {}, {})
        show {
            SessionDetailScreen(
                state = SessionDetailUiState(isLoading = false, detail = detail),
                actions = actions,
            )
        }
        audit("détail de session")
    }

    @Test
    fun `stats screen passes the audit`() {
        val today = LocalDate.of(2026, 10, 14).toEpochDay()
        val marks = listOf(DayMark.PRACTICED, DayMark.PRACTICED, DayMark.NONE, DayMark.PRACTICED, DayMark.PRACTICED)
        val state =
            StatsUiState(
                isLoading = false,
                today = today,
                sessions = 47,
                sessionsThisWeek = 6,
                streak = StreakSummary(47, 30, 5, 12, null, true),
                recent = marks.mapIndexed { i, mark -> DayStatus(today - 4 + i, mark) },
                perExercise = listOf(ExerciseCount(ExerciseId.CRAZY_EIGHTS, 12), ExerciseCount(ExerciseId.ONE_PAGE, 1)),
                gems = 9,
                totalSeconds = 5 * 3_600L + 12 * 60,
            )
        show { StatsScreen(state, onChooseExercise = {}, onShowGems = {}, onShowExercise = {}, onSettingsClick = {}) }
        audit("progrès")
    }

    // --- Séance du jour, réglages, à propos --------------------------------------------------------------------

    @Test
    fun `daily screen passes the audit`() {
        val state =
            DailyUiState(
                isLoading = false,
                epochDay = LocalDate.of(2026, 10, 16).toEpochDay(),
                items =
                    listOf(
                        DailyItem(0, ExerciseId.CRAZY_EIGHTS, 480, ExerciseProgress.DONE),
                        DailyItem(1, ExerciseId.CONSTRAINT_BOX, 300, ExerciseProgress.IN_PROGRESS),
                        DailyItem(2, ExerciseId.DECISION_POINT, 30, ExerciseProgress.TODO),
                    ),
                alternatives =
                    listOf(ExerciseSummary(ExerciseId.CORE_IMAGE, 600), ExerciseSummary(ExerciseId.ONE_PAGE, null)),
                nextExercise = ExerciseId.CONSTRAINT_BOX,
                started = true,
            )
        show { DailyScreen(state = state, onBack = {}, onOpenExercise = {}, onReplace = { _, _ -> }) }
        audit("séance du jour")
    }

    @Test
    fun `end of daily session screen passes the audit`() {
        val state =
            DailyDoneUiState(
                isLoading = false,
                lines =
                    listOf(
                        DailyDoneLine(ExerciseId.CRAZY_EIGHTS, true, 480, rating = 2, isGem = true),
                        DailyDoneLine(ExerciseId.CONSTRAINT_BOX, true, 300),
                        DailyDoneLine(ExerciseId.DECISION_POINT, true, 30, rating = 3),
                    ),
                sessionCount = 48,
                streakDays = 6,
            )
        show { DailyDoneScreen(state, onHome = {}, onRedo = {}, onBack = {}) }
        audit("fin de séance")
    }

    // Une ligne de durée par exercice : à 2x la liste dépasse 5000 dp, la fenêtre est donc plus haute ici.
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h9000dp")
    fun `settings screen passes the audit`() {
        val actions = SettingsActions()
        val state =
            SettingsUiState(
                isLoading = false,
                durations =
                    ExerciseId.ALL.map {
                        DurationRow(it, 300, 150, 3000, untimed = it == ExerciseId.ONE_PAGE)
                    },
                vibration = true,
                sound = true,
                palette = ThemePalette.PARCHMENT,
                palettes = ThemePalette.entries,
            )
        show { SettingsScreen(state, actions) }
        audit("réglages")
    }

    @Test
    fun `about screen passes the audit`() {
        show { AboutScreen("1.0.0-rc1", onBack = {}) }
        audit("à propos")
    }
}
