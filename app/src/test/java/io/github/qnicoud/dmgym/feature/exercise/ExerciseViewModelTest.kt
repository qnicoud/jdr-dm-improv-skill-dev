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
class ExerciseViewModelTest : ExerciseViewModelTestBase() {
    @Test
    fun `draws on opening and prepares the end signal`() =
        runTest {
            val vm = vm()
            collect(vm)
            val state = vm.uiState.value
            assertEquals(LoadStatus.Ready, state.load)
            assertEquals(1, state.session.draws.size)
            assertEquals(TimerKind.Ready, vm.timerState.value.kind)
            assertEquals(300_000, vm.timerState.value.displayMs)
            assertTrue(endSignal.prepared)
        }

    @Test
    fun `the end signal is released when the view model is cleared`() =
        runTest {
            val vm = vm()
            vm.invokeOnCleared()
            assertTrue(endSignal.released)
        }

    @Test
    fun `an unreadable table gives the degraded state, retry recovers`() =
        runTest {
            source.failing = true
            val vm = vm()
            collect(vm)
            assertEquals(LoadStatus.DrawFailed, vm.uiState.value.load)
            source.failing = false
            vm.onRetry()
            runCurrent()
            assertEquals(LoadStatus.Ready, vm.uiState.value.load)
        }

    @Test
    fun `a table too small for the slot also gives the degraded state`() =
        runTest {
            val big = DemoExercise.definition.copy(slots = listOf(TableSlot("p", TableId.Places, count = 50)))
            val vm = vm(big)
            collect(vm)
            assertEquals(LoadStatus.DrawFailed, vm.uiState.value.load)
        }

    @Test
    fun `start inserts the session and starts the timer`() =
        runTest {
            val vm = vm()
            collect(vm)
            vm.events.test {
                vm.onStart()
                runCurrent()
                assertEquals(ExerciseEvent.Announce(Milestone.Started), awaitItem())
            }
            val record = history.sessions.getValue(1).session
            assertEquals("demo", record.exerciseId)
            assertEquals(300, record.plannedDurationSec)
            assertEquals(SessionStatus.IN_PROGRESS, record.status)
            assertEquals(Phase.Active, vm.uiState.value.session.phase)
            assertEquals(TimerKind.Running, vm.timerState.value.kind)
            assertTrue(vm.uiState.value.keepScreenOn)
            assertEquals(1L, handle.get<Long>("session_id"))
        }

    @Test
    fun `a double tap on start creates a single session`() =
        runTest {
            val vm = vm()
            collect(vm)
            vm.onStart()
            vm.onStart()
            runCurrent()
            assertEquals(1, history.sessions.size)
        }

    @Test
    fun `typing is saved after the debounce, not before`() =
        runTest {
            val vm = started()
            val before = history.snapshots.size
            vm.onAnswerChanged(DemoExercise.keyA, "première idée")
            advanceTimeBy(900)
            assertEquals(before, history.snapshots.size)
            advanceTimeBy(200)
            runCurrent()
            assertEquals(
                "première idée",
                history.snapshots
                    .last()
                    .answers
                    .single()
                    .text,
            )
            assertEquals(SaveStatus.Saved, vm.uiState.value.save)
            assertEquals(
                1,
                vm.uiState.value.progress
                    ?.done,
            )
        }

    @Test
    fun `a reroll is saved at once and keeps the timer and the text`() =
        runTest {
            val vm = started()
            vm.onAnswerChanged(DemoExercise.keyA, "texte")
            pass(10_000)
            val before = vm.timerState.value
            val saved = history.snapshots.size
            vm.onReroll()
            runCurrent()
            val after = vm.uiState.value
            assertEquals(1, after.session.rerollCount)
            assertEquals("texte", after.session.textOf(DemoExercise.keyA))
            assertEquals(before.displayMs, vm.timerState.value.displayMs)
            assertTrue(history.snapshots.size > saved)
            vm.onReroll(DemoExercise.SLOT)
            runCurrent()
            assertEquals(2, vm.uiState.value.session.rerollCount)
        }

    @Test
    fun `rerolling before the start works`() =
        runTest {
            val vm = vm()
            collect(vm)
            vm.onReroll()
            runCurrent()
            assertEquals(1, vm.uiState.value.session.rerollCount)
        }

    @Test
    fun `leaving without content deletes the session`() =
        runTest {
            val vm = started()
            vm.onLeave()
            runCurrent()
            assertEquals(listOf(1L), history.deleted)
            assertTrue(history.sessions.isEmpty())
        }

    @Test
    fun `leaving with content keeps the draft in progress`() =
        runTest {
            val vm = started()
            vm.onAnswerChanged(DemoExercise.keyA, "brouillon")
            vm.onLeave()
            runCurrent()
            assertTrue(history.deleted.isEmpty())
            assertEquals(
                SessionStatus.IN_PROGRESS,
                history.sessions
                    .getValue(1)
                    .session.status,
            )
            assertEquals(
                "brouillon",
                history.sessions
                    .getValue(1)
                    .answers
                    .single()
                    .text,
            )
        }

    @Test
    fun `leaving before the start does nothing`() =
        runTest {
            val vm = vm()
            collect(vm)
            vm.onLeave()
            runCurrent()
            assertTrue(history.sessions.isEmpty() && history.deleted.isEmpty())
        }

    @Test
    fun `stopping the screen flushes without waiting for the debounce`() =
        runTest {
            val vm = started()
            vm.onAnswerChanged(DemoExercise.keyA, "vite")
            vm.onStop()
            runCurrent()
            assertEquals(
                "vite",
                history.sessions
                    .getValue(1)
                    .answers
                    .single()
                    .text,
            )
        }

    @Test
    fun `finish ends the session, saves it as completed and allows an assessment`() =
        runTest {
            val vm = started()
            vm.onAnswerChanged(DemoExercise.keyA, "fin")
            pass(30_000)
            vm.onFinish()
            runCurrent()
            val record = history.sessions.getValue(1).session
            assertEquals(SessionStatus.COMPLETED, record.status)
            assertNotNull(record.endedAtEpochMillis)
            assertEquals(30, record.activeDurationSec)
            vm.onAssess(2, true, mapOf("q1" to true))
            runCurrent()
            val saved = history.sessions.getValue(1)
            assertEquals(2, saved.session.selfRating)
            assertTrue(saved.session.isGem)
            assertEquals(listOf(true), saved.rubric.map { it.value })
        }

    @Test
    fun `the degraded mode lets the user write their own subject`() =
        runTest {
            source.failing = true
            val vm = vm()
            collect(vm)
            vm.onStart()
            runCurrent()
            assertEquals(Phase.Active, vm.uiState.value.session.phase)
            vm.onAnswerChanged(AnswerKey(io.github.qnicoud.dmgym.core.exercise.FREE_SUBJECT_KEY), "mon sujet")
            assertTrue(vm.uiState.value.session.hasContent)
        }

    @Test
    fun `the degraded mode is refused when the exercise has no free subject`() =
        runTest {
            source.failing = true
            val vm = vm(DemoExercise.definition.copy(acceptsFreeSubject = false))
            collect(vm)
            vm.onStart()
            runCurrent()
            assertEquals(Phase.Ready, vm.uiState.value.session.phase)
        }

    @Test
    fun `a failing write is reported and a vanished session too`() =
        runTest {
            val vm = started()
            history.failOnSave = true
            vm.onReroll()
            runCurrent()
            assertEquals(SaveStatus.Failed, vm.uiState.value.save)
            history.failOnSave = false
            history.sessions.clear()
            vm.onReroll()
            runCurrent()
            assertEquals(SaveStatus.Failed, vm.uiState.value.save)
        }

    @Test
    fun `text field states feed the answers`() =
        runTest {
            val vm = started()
            val field = vm.fieldState(DemoExercise.keyB)
            field.setTextAndPlaceCursorAtEnd("depuis le champ")
            Snapshot.sendApplyNotifications()
            runCurrent()
            assertEquals(
                "depuis le champ",
                vm.uiState.value.session
                    .textOf(DemoExercise.keyB),
            )
            assertTrue(vm.fieldState(DemoExercise.keyB) === field)
        }

    @Test
    fun `a field state starts with the restored text`() =
        runTest {
            val first = started()
            first.onAnswerChanged(DemoExercise.keyA, "gardé")
            first.onStop()
            runCurrent()
            val second = vm()
            collect(second)
            assertEquals("gardé", second.fieldState(DemoExercise.keyA).text.toString())
        }

    @Test
    fun `checks, flags and options reach the session`() =
        runTest {
            val vm = started()
            vm.onCheckToggled(DrawKey(DemoExercise.SLOT, 0))
            vm.onFlagToggled(DemoExercise.keyA)
            vm.onOptionChanged("video", true)
            val session = vm.uiState.value.session
            assertTrue(
                session.draws
                    .single()
                    .check!!
                    .checked,
            )
            assertTrue(session.answers.getValue(DemoExercise.keyA).flagged)
            assertEquals(mapOf("video" to true), session.options)
        }

    @Test
    fun `sequential questions advance up to the last one`() =
        runTest {
            val sequential =
                DemoExercise.definition.copy(
                    slots = listOf(TableSlot("q", TableId.Places, count = 3)),
                    layout = FieldLayout.Sequential("q", FieldSpec("answer")),
                )
            val vm = vm(sequential)
            collect(vm)
            repeat(5) { vm.onNextQuestion() }
            assertEquals(2, vm.uiState.value.session.currentQuestion)
            val fixed = vm()
            fixed.onNextQuestion()
            assertEquals(0, fixed.uiState.value.session.currentQuestion)
        }

    @Test
    fun `repeatable groups can be added up to the maximum`() =
        runTest {
            val groups =
                DemoExercise.definition.copy(layout = FieldLayout.RepeatableGroup(listOf(FieldSpec("npc")), max = 3))
            val vm = vm(groups)
            collect(vm)
            assertEquals(1, vm.uiState.value.groupCount)
            repeat(5) { vm.onAddGroup() }
            assertEquals(3, vm.uiState.value.groupCount)
            val fixed = vm()
            fixed.onAddGroup()
            assertEquals(1, fixed.uiState.value.groupCount)
        }

    @Test
    fun `the screen is not kept on while paused or before the start`() =
        runTest {
            val vm = vm()
            collect(vm)
            assertFalse(vm.uiState.value.keepScreenOn)
        }

    private fun ExerciseViewModel.invokeOnCleared() {
        val method = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("clear\$lifecycle_viewmodel")
        method.isAccessible = true
        method.invoke(this)
    }
}
