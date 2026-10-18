package io.github.qnicoud.dmgym.feature.exercise

import android.content.Context
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.DemoEngine
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseOption
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
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

/** Rendu des extensions génériques du moteur (ADR 0017) sur l'exercice de démonstration des tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class EngineExtensionsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch(
        definition: ExerciseDefinition,
        block: Boolean? = null,
    ): ExerciseViewModel {
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
                launch = ExerciseLaunch(seed = 5),
            )
        compose.setContent { DmGymTheme { ExerciseRoute(vm, onBack = {}) } }
        compose.waitForIdle()
        block?.let { vm.onOptionChanged(DemoEngine.BLOCK_OPTION, it) }
        compose.waitForIdle()
        return vm
    }

    private fun tap(tag: String) {
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun offers(vm: ExerciseViewModel) =
        vm.uiState.value.session
            .drawsOf(DemoEngine.OFFERS_SLOT)

    private fun textCount(text: String) = compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().size

    @Test
    fun `bound fields show the drawn type in their label`() {
        val vm = launch(DemoEngine.bound)
        tap(ExerciseTags.START)
        val types =
            vm.uiState.value.session
                .drawsOf(DemoEngine.TYPES_SLOT)
        types.forEachIndexed { i, type ->
            assertTrue(
                "field ${i + 1} names ${type.text}",
                textCount("${i + 1} · ${type.text}") >= 1,
            )
        }
    }

    @Test
    fun `offers are not revealed on the ready screen and cannot be rerolled`() {
        val vm = launch(DemoEngine.offers)
        compose.onNodeWithTag(QuestionTags.READY_HIDDEN).assertIsDisplayed()
        compose.onAllNodesWithTag(QuestionTags.READY_LIST).assertCountEquals(0)
        compose.onAllNodesWithTag(ExerciseTags.REROLL).assertCountEquals(0)
        offers(vm).forEach { assertEquals(0, textCount(it.text)) }
    }

    @Test
    fun `only the current offer is pinned and the list cannot be opened`() {
        val vm = launch(DemoEngine.offers)
        tap(ExerciseTags.START)
        val all = offers(vm)
        compose.onNodeWithTag(ExerciseTags.DRAW_CARD).assertIsDisplayed()
        assertEquals(1, textCount(all[0].text))
        assertEquals(0, textCount(all[1].text))
        compose.onAllNodesWithTag(QuestionTags.TOGGLE_LIST).assertCountEquals(0)
        compose.onAllNodesWithTag(ExerciseTags.REROLL).assertCountEquals(0)
    }

    @Test
    fun `next offer is manual and replaces the pinned offer`() {
        val vm = launch(DemoEngine.offers)
        tap(ExerciseTags.START)
        val all = offers(vm)
        tap(ExerciseTags.NEXT_QUESTION)
        assertEquals(1, vm.uiState.value.session.currentQuestion)
        assertEquals(0, textCount(all[0].text))
        assertEquals(1, textCount(all[1].text))
    }

    @Test
    fun `the prefix is shown but never typed into the answer`() {
        val vm = launch(DemoEngine.offers)
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.FIELD_PREFIX, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(DemoEngine.YES_FIELD, 0))).performTextInput("et il accepte")
        compose.waitForIdle()
        assertEquals(
            "et il accepte",
            vm.uiState.value.session
                .textOf(AnswerKey(DemoEngine.YES_FIELD, 0)),
        )
    }

    @Test
    fun `the prefix is read with the field by screen readers`() {
        launch(DemoEngine.offers)
        tap(ExerciseTags.START)
        val prefix = ApplicationProvider.getApplicationContext<Context>().getString(R.string.rubric_yes)
        compose
            .onNodeWithTag(ExerciseTags.field(AnswerKey(DemoEngine.YES_FIELD, 0)))
            .assert(hasText(prefix, substring = true))
    }

    @Test
    fun `an option true by default shows its field without being stored`() {
        val defaultTrue =
            DemoEngine.offers.copy(
                options = listOf(ExerciseOption(DemoEngine.BLOCK_OPTION, R.string.rubric_yes, default = true)),
            )
        launch(defaultTrue)
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(DemoEngine.BLOCK_FIELD, 0))).assertIsDisplayed()
    }

    @Test
    fun `the blocking field exists only with the option`() {
        launch(DemoEngine.offers, block = false)
        tap(ExerciseTags.START)
        compose.onAllNodesWithTag(ExerciseTags.field(AnswerKey(DemoEngine.BLOCK_FIELD, 0))).assertCountEquals(0)
    }

    @Test
    fun `with the option each offer has two fields and the grid gets a third question`() {
        val vm = launch(DemoEngine.offers, block = true)
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(DemoEngine.BLOCK_FIELD, 0))).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(DemoEngine.YES_FIELD, 0))).assertIsDisplayed()
        vm.onAnswerChanged(AnswerKey(DemoEngine.YES_FIELD, 0), "x")
        vm.onFinish()
        compose.waitForIdle()
        tap(ExerciseTags.RUBRIC_TOGGLE)
        compose.onNodeWithTag(ExerciseTags.rubric("q3", true)).assertExists()
    }

    @Test
    fun `without the option the grid keeps two questions`() {
        val vm = launch(DemoEngine.offers, block = false)
        tap(ExerciseTags.START)
        vm.onAnswerChanged(AnswerKey(DemoEngine.YES_FIELD, 0), "x")
        vm.onFinish()
        compose.waitForIdle()
        tap(ExerciseTags.RUBRIC_TOGGLE)
        compose.onNodeWithTag(ExerciseTags.rubric("q1", true)).assertExists()
        compose.onAllNodesWithTag(ExerciseTags.rubric("q3", true)).assertCountEquals(0)
    }

    @Test
    fun `the minute marker is shown while the exercise runs and moves nothing by itself`() {
        val vm = launch(DemoEngine.offers)
        compose.onAllNodesWithTag(ExerciseTags.RHYTHM_MARKER).assertCountEquals(0)
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.RHYTHM_MARKER).assertIsDisplayed()
        assertEquals(0, vm.uiState.value.session.currentQuestion)
    }

    @Test
    fun `an exercise without periodic event has no minute marker`() {
        launch(DemoEngine.bound)
        tap(ExerciseTags.START)
        compose.onAllNodesWithTag(ExerciseTags.RHYTHM_MARKER).assertCountEquals(0)
    }
}
