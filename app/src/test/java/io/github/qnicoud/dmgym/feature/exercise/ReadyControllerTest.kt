package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadyControllerTest {
    private fun TestScope.rig() = ControllerRig(backgroundScope, UnconfinedTestDispatcher(testScheduler))

    private fun ControllerRig.duration() = (model.value.timer as TimerState.Idle).durationMs

    @Test
    fun `increase and decrease move the duration by a step`() =
        runTest {
            val rig = rig()
            val initial = rig.duration()
            rig.ready.increaseDuration()
            val longer = rig.duration()
            assertTrue(longer > initial)
            rig.ready.decreaseDuration()
            assertEquals(initial, rig.duration())
        }

    @Test
    fun `quick taps are written once after the debounce`() =
        runTest {
            val rig = rig()
            rig.ready.increaseDuration()
            rig.ready.increaseDuration()
            advanceTimeBy(300)
            runCurrent()
            assertEquals(0, rig.settings.updates)
            advanceTimeBy(200)
            runCurrent()
            assertEquals(1, rig.settings.updates)
            assertEquals(
                rig.duration(),
                rig.settings.state.value.durations
                    .getValue(DemoExercise.id)
                    .inWholeMilliseconds,
            )
        }

    @Test
    fun `flush writes at once`() =
        runTest {
            val rig = rig()
            rig.ready.increaseDuration()
            rig.ready.flush()
            runCurrent()
            assertEquals(1, rig.settings.updates)
        }

    @Test
    fun `flush without a change writes nothing`() =
        runTest {
            val rig = rig()
            rig.ready.flush()
            runCurrent()
            assertEquals(0, rig.settings.updates)
        }

    @Test
    fun `untimed is applied at once and written to the settings`() =
        runTest {
            val rig = rig()
            rig.ready.changeUntimed(true)
            assertTrue(rig.model.value.untimed)
            rig.ready.flush()
            runCurrent()
            assertTrue(DemoExercise.id in rig.settings.state.value.untimed)
            rig.ready.changeUntimed(false)
            rig.ready.flush()
            runCurrent()
            assertFalse(DemoExercise.id in rig.settings.state.value.untimed)
        }

    @Test
    fun `nothing changes once the session has started`() =
        runTest {
            val rig = rig()
            rig.dispatch(ExerciseAction.Started(1))
            val before = rig.duration()
            rig.ready.increaseDuration()
            rig.ready.changeUntimed(true)
            assertEquals(before, rig.duration())
            assertFalse(rig.model.value.untimed)
        }

    @Test
    fun `an option change is stored in the session`() =
        runTest {
            val rig = rig()
            rig.ready.changeOption("mode", true)
            runCurrent()
            assertEquals(true, rig.model.value.session.options["mode"])
        }

    @Test
    fun `flushOnClear writes the pending change`() =
        runTest {
            val rig = rig()
            rig.ready.increaseDuration()
            rig.ready.flushOnClear()
            runCurrent()
            assertEquals(1, rig.settings.updates)
        }
}
