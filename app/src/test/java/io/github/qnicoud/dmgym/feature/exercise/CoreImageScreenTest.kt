package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
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
import io.github.qnicoud.dmgym.core.exercise.definitions.CoreImage
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Retours de test 1 : questions numérotées, question en cours seule, liste complète à la demande. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class CoreImageScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch(fontScale: Float = 1f): ExerciseViewModel {
        val vm =
            ExerciseViewModel(
                definition = CoreImage.definition,
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
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DmGymTheme { ExerciseRoute(vm, onBack = {}) }
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

    private fun questions(vm: ExerciseViewModel) =
        vm.uiState.value.session
            .drawsOf(CoreImage.QUESTIONS_SLOT)

    private fun assertNumberedList(vm: ExerciseViewModel) {
        questions(vm).forEachIndexed { i, q ->
            compose
                .onNodeWithTag(QuestionTags.item(i))
                .assertTextContains("${i + 1}/${CoreImage.QUESTION_COUNT} — ${q.text}")
        }
    }

    @Test
    fun `ready screen numbers every question`() {
        val vm = launch()
        compose.onNodeWithTag(QuestionTags.READY_LIST).assertIsDisplayed()
        assertNumberedList(vm)
    }

    @Test
    fun `active screen keeps questions out of the pinned card`() {
        val vm = launch()
        tap(ExerciseTags.START)
        val first = questions(vm).first().text
        compose.onNodeWithTag(ExerciseTags.DRAW_CARD).assertIsDisplayed()
        compose.onNodeWithTag(QuestionTags.CURRENT).assertTextContains("1/${CoreImage.QUESTION_COUNT} — $first")
        compose.onAllNodes(hasText(first, substring = true)).assertCountEquals(1)
        compose.onAllNodesWithTag(QuestionTags.LIST).assertCountEquals(0)
    }

    @Test
    fun `show all questions opens an inline list that closes when typing`() {
        val vm = launch()
        tap(ExerciseTags.START)
        tap(QuestionTags.TOGGLE_LIST)
        compose.onNodeWithTag(QuestionTags.LIST).assertIsDisplayed()
        assertNumberedList(vm)
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(CoreImage.ANSWER_FIELD, 0))).performTextInput("a")
        compose.waitForIdle()
        compose.onAllNodesWithTag(QuestionTags.LIST).assertCountEquals(0)
    }

    @Test
    fun `skip link moves to the next question without an answer, and is hidden on the last one`() {
        val vm = launch()
        tap(ExerciseTags.START)
        compose.onNodeWithTag(QuestionTags.SKIP).assertIsDisplayed()
        tap(QuestionTags.SKIP)
        assertEquals(1, vm.uiState.value.session.currentQuestion)
        assertEquals(
            "",
            vm.uiState.value.session
                .textOf(AnswerKey(CoreImage.ANSWER_FIELD, 0)),
        )
        repeat(CoreImage.QUESTION_COUNT - 2) { tap(QuestionTags.SKIP) }
        assertEquals(CoreImage.QUESTION_COUNT - 1, vm.uiState.value.session.currentQuestion)
        compose.onAllNodesWithTag(QuestionTags.SKIP).assertCountEquals(0)
    }

    @Test
    fun `skip link is offered only while the answer is empty`() {
        launch()
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(CoreImage.ANSWER_FIELD, 0))).performTextInput("une idée")
        compose.waitForIdle()
        compose.onAllNodesWithTag(QuestionTags.SKIP).assertCountEquals(0)
    }

    @Test
    fun `answer field stays displayed at 200 percent font scale`() {
        launch(fontScale = 2f)
        tap(ExerciseTags.START)
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(CoreImage.ANSWER_FIELD, 0))).assertIsDisplayed()
    }
}
