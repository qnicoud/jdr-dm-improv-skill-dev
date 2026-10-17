package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.FakeMonotonicClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelTimerTest : ExerciseViewModelTestBase() {
    @Test
    fun `the end signal is played once when the timer runs out`() =
        runTest {
            val vm = started()
            vm.events.test {
                assertEquals(ExerciseEvent.Announce(Milestone.Started), awaitItem())
                pass(299_000)
                pass(1_100)
                pass(5_000)
                pass(5_000)
                val announces =
                    cancelAndConsumeRemainingEvents()
                        .filterIsInstance<app.cash.turbine.Event.Item<ExerciseEvent>>()
                assertEquals(1, announces.count { it.value == ExerciseEvent.Announce(Milestone.TimeUp) })
            }
            assertEquals(1, endSignal.played.size)
            assertTrue(endSignal.played.single().soundEnabled && endSignal.played.single().vibrationEnabled)
            val ui = vm.uiState.value
            assertEquals(TimerKind.Overtime, vm.timerState.value.kind)
            assertTrue(ui.keepScreenOn)
            assertEquals(Phase.Active, ui.session.phase)
        }

    @Test
    fun `typing stays possible in overtime`() =
        runTest {
            val vm = started()
            pass(301_000)
            vm.onAnswerChanged(DemoExercise.keyA, "encore")
            assertEquals(
                "encore",
                vm.uiState.value.session
                    .textOf(DemoExercise.keyA),
            )
        }

    @Test
    fun `a stale expiry after a long absence plays nothing`() =
        runTest {
            val vm = started()
            monotonic.advance(400_000)
            wall.millis += 400_000
            advanceTimeBy(TICK)
            runCurrent()
            assertEquals(TimerKind.Overtime, vm.timerState.value.kind)
            assertTrue(endSignal.played.isEmpty())
        }

    @Test
    fun `milestones are announced once`() =
        runTest {
            val long =
                DemoExercise.definition.copy(
                    timer =
                        io.github.qnicoud.dmgym.core.exercise.TimerPolicy
                            .Required(kotlin.time.Duration.parse("8m")),
                )
            val vm = started(long)
            vm.events.test {
                assertEquals(ExerciseEvent.Announce(Milestone.Started), awaitItem())
                pass(241_000)
                assertEquals(ExerciseEvent.Announce(Milestone.Halfway), awaitItem())
                pass(180_000)
                assertEquals(ExerciseEvent.Announce(Milestone.LastMinute), awaitItem())
                pass(5_000)
                expectNoEvents()
            }
        }

    @Test
    fun `pause freezes the chrono and resume restarts it, also in overtime`() =
        runTest {
            val vm = started()
            pass(60_000)
            vm.onPause()
            val frozen = vm.timerState.value.displayMs
            pass(120_000)
            assertEquals(TimerKind.Paused, vm.timerState.value.kind)
            assertEquals(frozen, vm.timerState.value.displayMs)
            assertFalse(vm.uiState.value.keepScreenOn)
            vm.onResume()
            pass(1_000)
            assertEquals(TimerKind.Running, vm.timerState.value.kind)
            pass(300_000)
            assertEquals(TimerKind.Overtime, vm.timerState.value.kind)
            vm.onPause()
            pass(60_000)
            assertTrue(vm.timerState.value.overtime)
            vm.onResume()
            assertEquals(TimerKind.Overtime, vm.timerState.value.kind)
        }

    @Test
    fun `pausing right after the deadline still plays the signal`() =
        runTest {
            val vm = started()
            monotonic.advance(300_500)
            vm.onPause()
            assertEquals(1, endSignal.played.size)
        }

    @Test
    fun `pause and finish are ignored before the start`() =
        runTest {
            val vm = vm()
            collect(vm)
            vm.onPause()
            vm.onResume()
            vm.onFinish()
            assertEquals(TimerKind.Ready, vm.timerState.value.kind)
            assertEquals(Phase.Ready, vm.uiState.value.session.phase)
        }

    @Test
    fun `overtime is recorded`() =
        runTest {
            val vm = started()
            pass(330_000)
            vm.onFinish()
            runCurrent()
            val record = history.sessions.getValue(1).session
            assertEquals(330, record.activeDurationSec)
            assertEquals(30, record.overtimeSec)
        }

    @Test
    fun `no timer policy and the untimed setting both give a chronometer`() =
        runTest {
            settings.state.value = Settings(untimed = setOf(DemoExercise.id))
            val vm = started()
            assertEquals(TimerKind.Untimed, vm.timerState.value.kind)
            assertNull(
                history.sessions
                    .getValue(1)
                    .session.plannedDurationSec,
            )
            pass(20_000)
            assertEquals(20_000, vm.timerState.value.displayMs)
        }

    @Test
    fun `the duration comes from the settings`() =
        runTest {
            settings.state.value = Settings(durations = mapOf(DemoExercise.id to kotlin.time.Duration.parse("2m")))
            val vm = vm()
            collect(vm)
            assertEquals(120_000, vm.timerState.value.displayMs)
        }

    @Test
    fun `without a timer policy the exercise is untimed`() =
        runTest {
            val none = DemoExercise.definition.copy(timer = io.github.qnicoud.dmgym.core.exercise.TimerPolicy.None)
            val vm = vm(none)
            collect(vm)
            assertEquals(TimerKind.Untimed, vm.timerState.value.kind)
        }

    @Test
    fun `the minute reminder and the shift event fire during the session`() =
        runTest {
            val vm = started(DemoExercise.withEvents)
            vm.events.test {
                assertEquals(ExerciseEvent.Announce(Milestone.Started), awaitItem())
                pass(61_000)
                assertEquals(ExerciseEvent.Reminder("reminder", haptic = true), awaitItem())
                pass(120_000)
                cancelAndConsumeRemainingEvents()
            }
            val session = vm.uiState.value.session
            assertEquals(setOf("shift"), session.firedEvents)
            assertEquals(listOf(DemoExercise.SLOT, "shift"), session.draws.map { it.slotKey })
        }

    @Test
    fun `a stale reminder is dropped`() =
        runTest {
            val vm = started(DemoExercise.withEvents)
            vm.events.test {
                assertEquals(ExerciseEvent.Announce(Milestone.Started), awaitItem())
                pass(90_000)
                expectNoEvents()
            }
        }

    @Test
    fun `the process death restores the session, the text and the running timer`() =
        runTest {
            val first = started()
            first.onAnswerChanged(DemoExercise.keyA, "avant la mort")
            pass(60_000)
            first.onStop()
            runCurrent()
            val saved = handle
            // Nouveau processus : horloge monotone repartie de zéro, heure murale en avance de 30 s.
            monotonic.nowMillis = 500
            wall.millis += 30_000
            handle = saved
            val second = vm()
            collect(second)
            val state = second.uiState.value
            assertEquals(Phase.Active, state.session.phase)
            assertEquals("avant la mort", state.session.textOf(DemoExercise.keyA))
            assertEquals(TimerKind.Running, second.timerState.value.kind)
            assertEquals(210_000, second.timerState.value.displayMs)
            assertTrue(endSignal.played.isEmpty())
        }

    @Test
    fun `restoring after the deadline gives overtime and no signal`() =
        runTest {
            val first = started()
            first.onAnswerChanged(DemoExercise.keyA, "x")
            first.onStop()
            runCurrent()
            monotonic.nowMillis = 100
            wall.millis += 400_000
            val second = vm()
            collect(second)
            assertEquals(TimerKind.Overtime, second.timerState.value.kind)
            advanceTimeBy(1_000)
            assertTrue(endSignal.played.isEmpty())
        }

    @Test
    fun `a signal already delivered is not replayed after the restart`() =
        runTest {
            val first = started()
            pass(301_000)
            assertEquals(1, endSignal.played.size)
            val second = vm()
            collect(second)
            pass(1_000)
            assertEquals(1, endSignal.played.size)
        }

    @Test
    fun `resuming from home without a snapshot gives a paused timer`() =
        runTest {
            val first = started()
            first.onAnswerChanged(DemoExercise.keyA, "reste")
            pass(90_000)
            first.onStop()
            runCurrent()
            handle = SavedStateHandle()
            val resumed = vm(launch = ExerciseLaunch(resumeSessionId = 1, seed = 9))
            collect(resumed)
            val state = resumed.uiState.value
            assertEquals(TimerKind.Paused, resumed.timerState.value.kind)
            assertEquals(210_000, resumed.timerState.value.displayMs)
            assertEquals("reste", state.session.textOf(DemoExercise.keyA))
        }

    @Test
    fun `resuming an unknown session starts afresh`() =
        runTest {
            val vm = vm(launch = ExerciseLaunch(resumeSessionId = 99, seed = 1))
            collect(vm)
            assertEquals(Phase.Ready, vm.uiState.value.session.phase)
            assertEquals(LoadStatus.Ready, vm.uiState.value.load)
        }

    @Test
    fun `a session of another exercise is not resumed`() =
        runTest {
            val first = started()
            first.onAnswerChanged(DemoExercise.keyA, "x")
            first.onStop()
            runCurrent()
            val other =
                vm(
                    DemoExercise.definition.copy(
                        id =
                            io.github.qnicoud.dmgym.core.exercise
                                .ExerciseId("other"),
                    ),
                    ExerciseLaunch(resumeSessionId = 1),
                )
            collect(other)
            assertEquals(Phase.Ready, other.uiState.value.session.phase)
        }
}
