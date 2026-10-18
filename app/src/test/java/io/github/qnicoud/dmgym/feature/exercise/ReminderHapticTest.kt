package io.github.qnicoud.dmgym.feature.exercise

import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.definitions.CrazyEights
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.VibrationStrength
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Le rappel périodique de Crazy Eights fait vibrer le vibreur (`EndSignal.tick`) quand le réglage est actif. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class ReminderHapticTest {
    @get:Rule
    val compose = createComposeRule()

    private val monotonic = FakeMonotonicClock(1_000_000)
    private val wall = MutableClock()

    private val endSignal = FakeEndSignal()

    private fun launch(settings: Settings) {
        val vm =
            ExerciseViewModel(
                definition = CrazyEights.definition,
                deps =
                    ExerciseDependencies(
                        RealTables.repository,
                        RecordingHistoryRepository(),
                        FakeSettingsRepository(settings),
                        endSignal,
                        monotonic,
                        wall,
                        Dispatchers.Unconfined,
                    ),
                handle = SavedStateHandle(),
                launch = ExerciseLaunch(seed = 5),
            )
        compose.setContent { DmGymTheme { ExerciseRoute(vm, onBack = {}) } }
        compose.waitForIdle()
        compose.onNodeWithTag(ExerciseTags.START).performClick()
        compose.waitForIdle()
    }

    private fun passTime(ms: Long) {
        monotonic.advance(ms)
        wall.millis += ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        compose.waitForIdle()
    }

    @Test
    fun `the minute reminder ticks at the chosen strength when haptics are on`() {
        launch(Settings(crazyEightsMinuteHaptic = true, vibrationEnabled = true))
        passTime(60_500)
        assertEquals(listOf(VibrationStrength.NORMAL), endSignal.ticks)
    }

    @Test
    fun `the minute reminder follows the vibration strength setting`() {
        launch(Settings(crazyEightsMinuteHaptic = true, vibrationStrength = VibrationStrength.SOFT))
        passTime(60_500)
        assertEquals(listOf(VibrationStrength.SOFT), endSignal.ticks)
    }

    @Test
    fun `the minute reminder stays silent when haptics are off`() {
        launch(Settings(crazyEightsMinuteHaptic = false))
        passTime(60_500)
        assertEquals(emptyList<VibrationStrength>(), endSignal.ticks)
    }

    @Test
    fun `the reminder follows the duration, 8 ideas in 4 minutes pulse every 30 seconds`() {
        launch(Settings(crazyEightsMinuteHaptic = true, durations = mapOf(ExerciseId.CRAZY_EIGHTS to 4.minutes)))
        passTime(30_500)
        assertEquals(1, endSignal.ticks.size)
        passTime(30_000)
        assertEquals(2, endSignal.ticks.size)
    }

    @Test
    fun `without a timer there is no pulse and no marker`() {
        launch(Settings(crazyEightsMinuteHaptic = true, untimed = setOf(ExerciseId.CRAZY_EIGHTS)))
        passTime(90_000)
        assertEquals(emptyList<VibrationStrength>(), endSignal.ticks)
        compose.onAllNodesWithTag(ExerciseTags.RHYTHM_MARKER).assertCountEquals(0)
    }
}
