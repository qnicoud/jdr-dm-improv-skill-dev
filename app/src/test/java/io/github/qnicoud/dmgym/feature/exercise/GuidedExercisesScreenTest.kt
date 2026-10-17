package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.definitions.CoreImage
import io.github.qnicoud.dmgym.core.exercise.definitions.DecisionPoint
import io.github.qnicoud.dmgym.core.exercise.definitions.OnePage
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.exercise.definitions.StatusNumbers
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Un test d'interface par exercice de L10, sur les vraies définitions et les vraies tables. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class GuidedExercisesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val history = RecordingHistoryRepository()

    private fun launch(
        definition: ExerciseDefinition,
        dark: Boolean = false,
        fontScale: Float = 1f,
    ): ExerciseViewModel {
        val vm =
            ExerciseViewModel(
                definition = definition,
                deps =
                    ExerciseDependencies(
                        RealTables.repository(),
                        history,
                        FakeSettingsRepository(Settings()),
                        FakeEndSignal(),
                        FakeMonotonicClock(1_000_000),
                        MutableClock(),
                        Dispatchers.Unconfined,
                    ),
                handle = SavedStateHandle(),
                launch = ExerciseLaunch(seed = 11),
            )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DmGymTheme(darkTheme = dark) { ExerciseRoute(vm, onBack = {}) }
            }
        }
        compose.waitForIdle()
        return vm
    }

    private fun tap(tag: String) {
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    @Test
    fun `core image asks one question at a time and ends on a finish button`() {
        val vm = launch(CoreImage.definition)
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(CoreImage.ANSWER_FIELD, 0))).assertIsDisplayed()
        compose.onAllNodesWithTag(QuestionTags.PREVIOUS).assertCountEquals(0)
        repeat(CoreImage.QUESTION_COUNT - 1) { tap(ExerciseTags.NEXT_QUESTION) }
        assertEquals(CoreImage.QUESTION_COUNT - 1, vm.uiState.value.session.currentQuestion)
        compose.onAllNodesWithTag(ExerciseTags.NEXT_QUESTION).assertCountEquals(0)
        tap(QuestionTags.PREVIOUS)
        assertEquals(CoreImage.QUESTION_COUNT - 2, vm.uiState.value.session.currentQuestion)
        tap(ExerciseTags.NEXT_QUESTION)
        tap(QuestionTags.LAST_FINISH)
        assertEquals(Phase.Ended, vm.uiState.value.session.phase)
    }

    @Test
    fun `status numbers shows the card, the reminder and the options, then plays without keyboard`() {
        val vm = launch(StatusNumbers.definition, dark = true)
        compose.onNodeWithTag(ExerciseExtraTags.STATUS_CARD).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseExtraTags.REMINDER).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseExtraTags.option(StatusNumbers.OPTION_COUNTER)).assertIsDisplayed()
        tap(ExerciseExtraTags.option(StatusNumbers.OPTION_SHIFT))
        assertEquals(true, vm.uiState.value.session.options[StatusNumbers.OPTION_SHIFT])
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseExtraTags.SCENE_DONE).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseExtraTags.STATUS_CARD).assertIsDisplayed()
        tap(ExerciseExtraTags.SCENE_DONE)
        compose.onNodeWithTag(ExerciseTags.END_PANEL).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseExtraTags.END_NOTE).performScrollTo().performTextInput("regard fuyant")
        compose.waitForIdle()
        assertEquals(
            "regard fuyant",
            vm.uiState.value.session
                .textOf(AnswerKey(StatusNumbers.NOTES_FIELD)),
        )
    }

    @Test
    fun `decision point has three answers with a never box and a situation mode`() {
        val vm = launch(DecisionPoint.definition)
        compose.onAllNodesWithTag(ExerciseTags.DRAW_CARD).assertCountEquals(0)
        tap(ExerciseExtraTags.option(DecisionPoint.OPTION_SITUATION))
        compose.onNodeWithTag(ExerciseTags.DRAW_CARD).assertIsDisplayed()
        tap(ExerciseTags.START)
        DecisionPoint.FIELD_KEYS.forEach {
            compose.onNodeWithTag(ExerciseTags.field(AnswerKey(it))).assertIsDisplayed()
        }
        assertEquals(DecisionPoint.FIELD_KEYS.size, vm.uiState.value.definition.fieldKeys.size)
        assertTrue(
            vm.uiState.value.session.options
                .getValue(DecisionPoint.OPTION_SITUATION),
        )
    }

    @Test
    fun `one page shows the context field, 5 npc cap and the soft counter`() {
        val vm = launch(OnePage.definition)
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(OnePage.CONTEXT_FIELD))).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(OnePage.MOTIVATION_FIELD))).performScrollTo()
        compose.onNodeWithTag(QuestionTags.SOFT_COUNTER, useUnmergedTree = true).assertIsDisplayed()
        repeat(OnePage.MAX_NPCS) { if (vm.uiState.value.groupCount < OnePage.MAX_NPCS) tap(ExerciseTags.ADD_GROUP) }
        assertEquals(OnePage.MAX_NPCS, vm.uiState.value.groupCount)
        compose.onAllNodesWithTag(ExerciseTags.ADD_GROUP).assertCountEquals(0)
        compose
            .onNodeWithTag(
                ExerciseTags.field(AnswerKey(OnePage.INACTION_FIELD)),
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `one page hides the context field when a situation is drawn`() {
        launch(OnePage.definition)
        tap(ExerciseExtraTags.option(OnePage.OPTION_DRAWN))
        compose.onNodeWithTag(ExerciseTags.DRAW_CARD).assertIsDisplayed()
        tap(ExerciseTags.START)
        compose.onAllNodesWithTag(ExerciseTags.field(AnswerKey(OnePage.CONTEXT_FIELD))).assertCountEquals(0)
    }

    @Test
    fun `the status card is read as a single label that the draw card does not override`() {
        launch(StatusNumbers.definition)
        compose
            .onAllNodes(
                hasContentDescription("Statut ", substring = true) and
                    hasContentDescription(" sur 10 · ", substring = true),
            ).assertCountEquals(1)
    }

    @Test
    fun `the status card and the shift banner fit at 200 percent font`() {
        val vm = launch(StatusNumbers.definition, fontScale = 2f)
        compose.onNodeWithTag(ExerciseExtraTags.STATUS_CARD).assertIsDisplayed()
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseExtraTags.STATUS_CARD).assertIsDisplayed()
        assertEquals(Phase.Active, vm.uiState.value.session.phase)
        compose.onNodeWithTag(ExerciseExtraTags.SCENE_DONE).assertIsDisplayed()
    }
}
