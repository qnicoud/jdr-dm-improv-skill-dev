package io.github.qnicoud.dmgym.feature.exercise

import android.content.Context
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.exercise.definitions.YesAnd
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

/** Touche d'action du clavier de « Oui, et… » : Suivant passe à l'offre suivante, Terminé ferme le clavier. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class YesAndScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch(block: Boolean = false): ExerciseViewModel {
        val vm =
            ExerciseViewModel(
                definition = YesAnd.definition,
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
        if (block) vm.onOptionChanged(YesAnd.BLOCK_FIRST, true)
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        return vm
    }

    private fun ime(
        field: String,
        offer: Int,
    ) {
        compose.onNodeWithTag(ExerciseTags.field(AnswerKey(field, offer))).performScrollTo().performImeAction()
        compose.waitForIdle()
    }

    @Test
    fun `next on the answer moves to the next offer`() {
        val vm = launch()
        ime(YesAnd.YES_AND_FIELD, 0)
        assertEquals(1, vm.uiState.value.session.currentQuestion)
    }

    @Test
    fun `next on the reflex goes to the answer, not to the next offer`() {
        val vm = launch(block = true)
        ime(YesAnd.BLOCK_FIELD, 0)
        assertEquals(0, vm.uiState.value.session.currentQuestion)
        ime(YesAnd.YES_AND_FIELD, 0)
        assertEquals(1, vm.uiState.value.session.currentQuestion)
    }

    @Test
    fun `done on the fifth offer neither advances nor finishes`() {
        val vm = launch()
        repeat(YesAnd.OFFER_COUNT - 1) { ime(YesAnd.YES_AND_FIELD, it) }
        assertEquals(YesAnd.OFFER_COUNT - 1, vm.uiState.value.session.currentQuestion)
        ime(YesAnd.YES_AND_FIELD, YesAnd.OFFER_COUNT - 1)
        assertEquals(YesAnd.OFFER_COUNT - 1, vm.uiState.value.session.currentQuestion)
        assertEquals(Phase.Active, vm.uiState.value.session.phase)
    }

    private fun finishAndOpenGrid(vm: ExerciseViewModel) {
        vm.onAnswerChanged(AnswerKey(YesAnd.YES_AND_FIELD, 0), "x")
        vm.onFinish()
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.RUBRIC_TOGGLE).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun questionCount(): Int =
        listOf("adds_something", "builds_on_offer", "spotted_reflex").sumOf { key ->
            compose.onAllNodesWithTag(ExerciseTags.rubric(key, true)).fetchSemanticsNodes().size
        }

    @Test
    fun `the end grid shows two questions without blocking first`() {
        val vm = launch()
        vm.onFinish()
        compose.waitForIdle()
        compose
            .onNodeWithTag(
                ExerciseTags.RUBRIC_TOGGLE,
            ).assert(hasText(context.getString(R.string.end_rubric_toggle, 2)))
        finishAndOpenGrid(vm)
        assertEquals(2, questionCount())
        compose.onAllNodesWithTag(ExerciseTags.rubric("spotted_reflex", true)).assertCountEquals(0)
    }

    @Test
    fun `the end grid shows three questions with blocking first, including the reflex one`() {
        finishAndOpenGrid(launch(block = true))
        assertEquals(3, questionCount())
        compose.onNodeWithTag(ExerciseTags.rubric("spotted_reflex", true)).assertExists()
        compose.onNodeWithText(context.getString(R.string.yes_and_rubric_reflex)).assertExists()
    }

    @Test
    fun `the answer field is titled Oui, et with the add-something placeholder`() {
        launch()
        val field = compose.onNodeWithTag(ExerciseTags.field(AnswerKey(YesAnd.YES_AND_FIELD, 0)))
        field.assert(hasText(context.getString(R.string.yes_and_title), substring = true))
        compose.onNodeWithText(context.getString(R.string.yes_and_answer_hint)).assertExists()
    }

    @Test
    fun `the rhythm marker shows during the exercise`() {
        launch()
        compose.onNodeWithTag(ExerciseTags.RHYTHM_MARKER).assertExists()
    }

    private val context get() = ApplicationProvider.getApplicationContext<Context>()
}
