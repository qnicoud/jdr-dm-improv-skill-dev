package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.ExerciseAction
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.timer.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerControllerTest {
    private fun kotlinx.coroutines.test.TestScope.rig() =
        ControllerRig(backgroundScope, StandardTestDispatcher(testScheduler))

    private fun ControllerRig.started(plannedMs: Long? = 60_000) {
        timer.start(plannedMs, monotonic.now())
        dispatch(ExerciseAction.Started(1))
    }

    private fun ControllerRig.drainedAnnouncements(): List<ExerciseEvent> =
        generateSequence { channel.tryReceive().getOrNull() }.toList()

    @Test
    fun `start runs the countdown and saves a snapshot in the handle`() =
        runTest {
            val rig = rig()
            rig.started()
            assertTrue(rig.model.value.timer is TimerState.Running)
            assertNotNull(rig.handle.get<LongArray>(HandleKeys.TIMER))
        }

    @Test
    fun `start without a planned duration runs untimed`() =
        runTest {
            val rig = rig()
            rig.started(plannedMs = null)
            assertTrue(rig.model.value.timer is TimerState.Untimed)
        }

    @Test
    fun `pause then resume goes through paused and running`() =
        runTest {
            val rig = rig()
            rig.started()
            rig.timer.pause()
            assertTrue(rig.model.value.timer is TimerState.Paused)
            rig.timer.resume()
            assertTrue(rig.model.value.timer is TimerState.Running)
        }

    @Test
    fun `pause and resume do nothing before the start`() =
        runTest {
            val rig = rig()
            rig.timer.pause()
            rig.timer.resume()
            assertTrue(rig.model.value.timer is TimerState.Idle)
        }

    @Test
    fun `a tick before the start is ignored`() =
        runTest {
            val rig = rig()
            rig.timer.onTick(rig.monotonic.now() + 1_000)
            assertTrue(rig.model.value.timer is TimerState.Idle)
        }

    @Test
    fun `the expiry is published once however many ticks follow`() =
        runTest {
            val rig = rig()
            rig.started()
            rig.drainedAnnouncements()
            val end = rig.monotonic.now() + 60_000
            rig.timer.onTick(end + 100)
            rig.timer.onTick(end + 600)
            rig.timer.onTick(end + 1_100)
            runCurrent()
            assertTrue(rig.model.value.timer is TimerState.Overtime)
            assertEquals(1, rig.endSignal.played.size)
            assertEquals(1, rig.drainedAnnouncements().count { it == ExerciseEvent.Announce(Milestone.TimeUp) })
        }

    @Test
    fun `freeze stops the clock without playing the end signal`() =
        runTest {
            val rig = rig()
            rig.started()
            rig.monotonic.advance(70_000)
            rig.timer.freeze()
            assertTrue(rig.model.value.timer !is TimerState.Running)
            assertEquals(0, rig.endSignal.played.size)
        }

    @Test
    fun `times report the active and overtime durations`() =
        runTest {
            val rig = rig()
            rig.started()
            rig.monotonic.advance(5_000)
            assertEquals(5_000, rig.timer.times().activeMs)
            assertEquals(0, rig.timer.times().overtimeMs)
        }

    @Test
    fun `restore from a snapshot keeps the clock and does not replay the end signal`() =
        runTest {
            val rig = rig()
            rig.dispatch(ExerciseAction.Started(1))
            val past = rig.engine.start(10_000, rig.monotonic.now() - 20_000)
            val overdue = rig.engine.tick(past, rig.monotonic.now()).state
            rig.timer.restore(overdue)
            assertEquals(overdue, rig.model.value.timer)
            rig.timer.onTick(rig.monotonic.now() + 1_000)
            assertEquals(0, rig.endSignal.played.size)
            assertNotNull(rig.handle.get<LongArray>(HandleKeys.TIMER))
        }
}
