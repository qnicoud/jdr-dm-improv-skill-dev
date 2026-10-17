package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.daily.DailyNext
import io.github.qnicoud.dmgym.core.daily.DailyStep
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
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

/** Enchaînement des exercices de la séance du jour depuis le bilan (parcours 4.2, mockup 08). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class DailyChainScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val history = RecordingHistoryRepository()
    private var left = false
    private var openedNext: ExerciseId? = null
    private var dailyDone = false

    private fun launch(step: DailyStep?) {
        val vm =
            ExerciseViewModel(
                definition = DemoExercise.definition,
                deps =
                    ExerciseDependencies(
                        DemoExercise.tables(),
                        history,
                        FakeSettingsRepository(Settings()),
                        FakeEndSignal(),
                        FakeMonotonicClock(1_000_000),
                        MutableClock(),
                        Dispatchers.Unconfined,
                    ),
                handle = SavedStateHandle(),
                launch = ExerciseLaunch(fromDaily = step != null, seed = 3),
            )
        compose.setContent {
            DmGymTheme {
                ExerciseRoute(
                    vm,
                    onBack = { left = true },
                    daily = step,
                    onNextExercise = { openedNext = it },
                    onDailyDone = { dailyDone = true },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun toReview() {
        tap(ExerciseTags.START)
        tap(ExerciseTags.FINISH)
    }

    @Test
    fun `the review offers the next exercise with its place in the session`() {
        launch(DailyStep(1, 3, DailyNext(ExerciseId.CONSTRAINT_BOX, 2)))
        toReview()
        compose.onNodeWithTag(ExerciseTags.NEXT_EXERCISE).assertTextContains("Exercice suivant (2 sur 3)")
        compose.onAllNodesWithTag(ExerciseTags.DONE).assertCountEquals(0)
        tap(ExerciseTags.NEXT_EXERCISE)
        assertEquals(ExerciseId.CONSTRAINT_BOX, openedNext)
        assertEquals(false, dailyDone)
    }

    @Test
    fun `the review of the last exercise ends the session`() {
        launch(DailyStep(3, 3, null))
        toReview()
        compose.onNodeWithTag(ExerciseTags.NEXT_EXERCISE).assertTextContains("Terminer la séance")
        tap(ExerciseTags.NEXT_EXERCISE)
        assertEquals(true, dailyDone)
        assertEquals(null, openedNext)
    }

    @Test
    fun `the top bar shows the place in the session while playing`() {
        launch(DailyStep(2, 3, DailyNext(ExerciseId.DECISION_POINT, 3)))
        compose.onNode(hasText("Séance du jour · 2 sur 3")).assertIsDisplayed()
    }

    @Test
    fun `the review keeps the plain done button outside the daily session`() {
        launch(null)
        toReview()
        compose.onNodeWithTag(ExerciseTags.DONE).assertIsDisplayed()
        compose.onAllNodesWithTag(ExerciseTags.NEXT_EXERCISE).assertCountEquals(0)
        compose.onAllNodes(hasText("Séance du jour", substring = true)).assertCountEquals(0)
        assertEquals(false, left)
    }

    @Test
    fun `skipping the review leaves the exercise like back does`() {
        launch(DailyStep(1, 3, DailyNext(ExerciseId.CONSTRAINT_BOX, 2)))
        toReview()
        tap(ExerciseTags.SKIP)
        assertEquals(true, left)
        assertEquals(null, openedNext)
    }
}
