package io.github.qnicoud.dmgym.feature.exercise

import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.TimerKind
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class ExerciseScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val history = RecordingHistoryRepository()
    private val monotonic = FakeMonotonicClock(1_000_000)
    private val wall = MutableClock()
    private var left = false

    private fun viewModel() =
        ExerciseViewModel(
            definition = DemoExercise.definition,
            deps =
                ExerciseDependencies(
                    DemoExercise.tables(),
                    history,
                    FakeSettingsRepository(Settings()),
                    FakeEndSignal(),
                    monotonic,
                    wall,
                    Dispatchers.Unconfined,
                ),
            handle = SavedStateHandle(),
            launch = ExerciseLaunch(seed = 3),
        )

    private fun launch(dark: Boolean = false): ExerciseViewModel {
        val vm = viewModel()
        compose.setContent { DmGymTheme(darkTheme = dark) { ExerciseRoute(vm, onBack = { left = true }) } }
        compose.waitForIdle()
        return vm
    }

    private fun passTime(ms: Long) {
        monotonic.advance(ms)
        wall.millis += ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        compose.waitForIdle()
    }

    private fun typeInFirstField(text: String) {
        compose.onNodeWithTag(ExerciseTags.field(DemoExercise.keyA)).performTextInput(text)
        compose.waitForIdle()
    }

    @Test
    fun `shows the draw, the timer and the start button before the session`() {
        launch()
        compose.onNodeWithTag(ExerciseTags.DRAW_CARD).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseTags.TIMER).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseTags.START).assertIsDisplayed()
    }

    @Test
    fun `starting shows the fields and the finish button`() {
        val vm = launch()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.field(DemoExercise.keyA)).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseTags.FINISH).assertIsDisplayed()
        assertEquals(TimerKind.Running, vm.timerState.value.kind)
        assertEquals(1, history.sessions.size)
    }

    @Test
    fun `typing stays possible in overtime and no dialog ever appears`() {
        val vm = launch()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        typeInFirstField("avant ")
        passTime(301_000)
        assertEquals(TimerKind.Overtime, vm.timerState.value.kind)
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        typeInFirstField("après")
        compose.onNodeWithTag(ExerciseTags.field(DemoExercise.keyA)).assertIsDisplayed()
        assertEquals(
            "avant après",
            vm.uiState.value.session
                .textOf(DemoExercise.keyA),
        )
        compose.onAllNodes(isDialog()).assertCountEquals(0)
    }

    @Test
    fun `pause works in overtime too`() {
        val vm = launch()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        passTime(301_000)
        compose.onNodeWithTag(ExerciseTags.PAUSE).performClick()
        compose.waitForIdle()
        assertEquals(TimerKind.Paused, vm.timerState.value.kind)
        assertTrue(vm.timerState.value.overtime)
    }

    @Test
    fun `finishing opens the assessment, which can be filled or skipped`() {
        val vm = launch(dark = true)
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        typeInFirstField("une idée")
        compose.onNodeWithTag(ExerciseTags.FINISH).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.END_PANEL).assertIsDisplayed()
        compose.onNodeWithTag(ExerciseTags.level(2)).performClick()
        compose.onNodeWithTag(ExerciseTags.GEM).performClick()
        compose.onNodeWithTag(ExerciseTags.RUBRIC_TOGGLE).performClick()
        compose.onNodeWithTag(ExerciseTags.rubric("q1", true)).performClick()
        compose.waitForIdle()
        val session = vm.uiState.value.session
        assertEquals(2, session.selfRating)
        assertTrue(session.isGem)
        assertEquals(mapOf("q1" to true), session.rubric)
        compose.onNodeWithTag(ExerciseTags.level(2)).performClick()
        compose.waitForIdle()
        assertEquals(null, vm.uiState.value.session.selfRating)
        compose.onNodeWithTag(ExerciseTags.SKIP).performScrollTo().performClick()
        assertTrue(left)
    }

    @Test
    fun `reroll shows the counter`() {
        launch()
        compose.onNodeWithTag(ExerciseTags.REROLL).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.REROLL_COUNT).assertIsDisplayed()
    }
}
