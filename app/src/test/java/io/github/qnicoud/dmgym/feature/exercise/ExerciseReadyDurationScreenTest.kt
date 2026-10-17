package io.github.qnicoud.dmgym.feature.exercise

import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
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
import kotlin.time.Duration.Companion.minutes

/** Recette 1.16 : la durée se règle depuis l'écran Prêt, jamais pendant l'exercice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class ExerciseReadyDurationScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = FakeSettingsRepository(Settings())

    private fun launch() {
        val vm =
            ExerciseViewModel(
                definition = DemoExercise.definition,
                deps =
                    ExerciseDependencies(
                        DemoExercise.tables(),
                        RecordingHistoryRepository(),
                        settings,
                        FakeEndSignal(),
                        FakeMonotonicClock(1_000_000),
                        MutableClock(),
                        Dispatchers.Unconfined,
                    ),
                handle = SavedStateHandle(),
                launch = ExerciseLaunch(seed = 3),
            )
        compose.setContent { DmGymTheme { ExerciseRoute(vm, onBack = {}) } }
        compose.waitForIdle()
    }

    private fun passTime(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.waitForIdle()
    }

    @Test
    fun `tapping the duration opens the sheet, and the buttons change and save it`() {
        launch()
        compose.onAllNodesWithTag(DurationSheetTags.SHEET).assertCountEquals(0)
        compose.onNodeWithTag(ExerciseTags.TIMER).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DurationSheetTags.VALUE).assertIsDisplayed()
        compose.onNodeWithTag(DurationSheetTags.INCREASE).performClick()
        compose.waitForIdle()
        assertTrue(
            settings.state.value.durations
                .isEmpty(),
        )
        passTime(500)
        assertEquals(6.minutes, settings.state.value.durations[DemoExercise.id])
        compose.onNodeWithTag(DurationSheetTags.UNTIMED).performClick()
        passTime(500)
        assertTrue(DemoExercise.id in settings.state.value.untimed)
    }

    @Test
    fun `the new duration shows at once on the ready screen`() {
        launch()
        compose.onNodeWithTag(ExerciseTags.TIMER).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DurationSheetTags.INCREASE).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.TIMER).assertIsDisplayed()
        compose.onNode(hasText("6:00"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `no sheet opens while the exercise runs`() {
        launch()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.TIMER).performClick()
        compose.waitForIdle()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.onAllNodesWithTag(DurationSheetTags.SHEET).assertCountEquals(0)
    }
}
