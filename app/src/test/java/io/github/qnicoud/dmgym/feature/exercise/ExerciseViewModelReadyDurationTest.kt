package io.github.qnicoud.dmgym.feature.exercise

import androidx.lifecycle.ViewModelStore
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Réglage de la durée depuis l'écran Prêt (recette 1.16) : appliqué tout de suite, enregistré dans les Réglages. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelReadyDurationTest : ExerciseViewModelTestBase() {
    private fun TestScope.ready(): ExerciseViewModel {
        val vm = vm()
        collect(vm)
        return vm
    }

    @Test
    fun `the ready state exposes the current duration and its bounds`() =
        runTest {
            val setting = ready().uiState.value.timerSetting
            assertNotNull(setting)
            assertEquals(300, setting!!.seconds)
            assertEquals(150, setting.minSeconds)
            assertEquals(3_000, setting.maxSeconds)
            assertFalse(setting.untimed)
        }

    @Test
    fun `increasing applies at once to the ready timer and is saved in the settings`() =
        runTest {
            val vm = ready()
            vm.onDurationIncreased()
            runCurrent()
            assertEquals(
                360,
                vm.uiState.value.timerSetting!!
                    .seconds,
            )
            assertEquals(360_000, vm.timerState.value.displayMs)
            advanceTimeBy(DEBOUNCE)
            assertEquals(6.minutes, settings.state.value.durations[DemoExercise.id])
        }

    @Test
    fun `decreasing stops at the lower bound`() =
        runTest {
            val vm = ready()
            repeat(20) { vm.onDurationDecreased() }
            advanceTimeBy(DEBOUNCE)
            runCurrent()
            assertEquals(
                150,
                vm.uiState.value.timerSetting!!
                    .seconds,
            )
            assertEquals(150.seconds, settings.state.value.durations[DemoExercise.id])
        }

    @Test
    fun `increasing stops at the upper bound`() =
        runTest {
            val vm = ready()
            repeat(80) { vm.onDurationIncreased() }
            runCurrent()
            assertEquals(
                3_000,
                vm.uiState.value.timerSetting!!
                    .seconds,
            )
        }

    @Test
    fun `the step follows the bounds step rule`() =
        runTest {
            settings.state.value = settings.state.value.copy(durations = mapOf(DemoExercise.id to 150.seconds))
            val vm = ready()
            vm.onDurationIncreased()
            runCurrent()
            assertEquals(
                210,
                vm.uiState.value.timerSetting!!
                    .seconds,
            )
        }

    @Test
    fun `the untimed option is applied at once and saved`() =
        runTest {
            val vm = ready()
            vm.onUntimedChanged(true)
            runCurrent()
            assertTrue(
                vm.uiState.value.timerSetting!!
                    .untimed,
            )
            assertEquals(TimerKind.Untimed, vm.timerState.value.kind)
            advanceTimeBy(DEBOUNCE)
            assertTrue(DemoExercise.id in settings.state.value.untimed)
            vm.onUntimedChanged(false)
            advanceTimeBy(DEBOUNCE)
            runCurrent()
            assertEquals(TimerKind.Ready, vm.timerState.value.kind)
            assertEquals(300_000, vm.timerState.value.displayMs)
            assertFalse(DemoExercise.id in settings.state.value.untimed)
        }

    @Test
    fun `the setting is gone once the exercise runs and later changes are ignored`() =
        runTest {
            val vm = started()
            assertNull(vm.uiState.value.timerSetting)
            vm.onDurationIncreased()
            vm.onUntimedChanged(true)
            advanceTimeBy(DEBOUNCE)
            runCurrent()
            assertTrue(
                settings.state.value.durations
                    .isEmpty(),
            )
            assertFalse(DemoExercise.id in settings.state.value.untimed)
        }

    @Test
    fun `five quick taps give a single write with the final value`() =
        runTest {
            val vm = ready()
            repeat(5) { vm.onDurationIncreased() }
            runCurrent()
            assertEquals(0, settings.updates)
            assertEquals(
                600,
                vm.uiState.value.timerSetting!!
                    .seconds,
            )
            advanceTimeBy(DEBOUNCE)
            runCurrent()
            assertEquals(1, settings.updates)
            assertEquals(600.seconds, settings.state.value.durations[DemoExercise.id])
        }

    @Test
    fun `closing the sheet writes at once`() =
        runTest {
            val vm = ready()
            vm.onDurationIncreased()
            vm.onDurationSheetClosed()
            runCurrent()
            assertEquals(1, settings.updates)
            assertEquals(6.minutes, settings.state.value.durations[DemoExercise.id])
            advanceTimeBy(DEBOUNCE)
            runCurrent()
            assertEquals(1, settings.updates)
        }

    @Test
    fun `a pending write is flushed when the view model is cleared`() =
        runTest {
            val vm = ready()
            vm.onDurationIncreased()
            runCurrent()
            assertEquals(0, settings.updates)
            ViewModelStore().apply { put("vm", vm) }.clear()
            runCurrent()
            assertEquals(6.minutes, settings.state.value.durations[DemoExercise.id])
        }

    @Test
    fun `nothing is written when nothing changed`() =
        runTest {
            val vm = ready()
            vm.onDurationSheetClosed()
            advanceTimeBy(DEBOUNCE)
            runCurrent()
            assertEquals(0, settings.updates)
        }

    private companion object {
        const val DEBOUNCE = 500L
    }
}
