package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.AnswerDetector
import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.DetectionSpec
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelRobustnessTest : ExerciseViewModelTestBase() {
    private val containsMine =
        AnswerDetector { draws, text -> draws.associate { DrawKey(it.slotKey, it.position) to text.contains("texte") } }

    private val detecting =
        DemoExercise.definition.copy(detection = DetectionSpec(listOf(DemoExercise.SLOT), DemoExercise.FIELD_A))

    @Test
    fun `the last typed character is saved on leave even before the snapshot is applied`() =
        runTest {
            val vm = started()
            vm.fieldState(DemoExercise.keyA).setTextAndPlaceCursorAtEnd("dernier caractère !")
            vm.onLeave()
            runCurrent()
            assertEquals(
                "dernier caractère !",
                history.sessions
                    .getValue(1)
                    .answers
                    .single()
                    .text,
            )
            assertTrue(history.deleted.isEmpty())
        }

    @Test
    fun `stopping flushes the text still sitting in a field`() =
        runTest {
            val vm = started()
            vm.fieldState(DemoExercise.keyB).setTextAndPlaceCursorAtEnd("avant l'arrêt")
            vm.onStop()
            runCurrent()
            assertEquals(
                "avant l'arrêt",
                history.sessions
                    .getValue(1)
                    .answers
                    .single()
                    .text,
            )
        }

    @Test
    fun `a reroll that fails mid session keeps the draws and shows a message`() =
        runTest {
            val first = started()
            first.onAnswerChanged(DemoExercise.keyA, "x")
            first.onStop()
            runCurrent()
            val keptDraws = first.uiState.value.session.draws
            val broken = DemoExercise.FixtureSource(failing = true)
            val vm =
                ExerciseViewModel(
                    DemoExercise.definition,
                    ExerciseDependencies(
                        DemoExercise.tables(broken),
                        history,
                        settings,
                        endSignal,
                        monotonic,
                        wall,
                        dispatcher,
                    ),
                    handle,
                )
            collect(vm)
            vm.events.test {
                vm.onReroll()
                runCurrent()
                assertEquals(ExerciseEvent.Snackbar(R.string.draw_reroll_failed), awaitItem())
            }
            assertEquals(LoadStatus.Ready, vm.uiState.value.load)
            assertEquals(
                keptDraws.map { it.entryId },
                vm.uiState.value.session.draws
                    .map { it.entryId },
            )
        }

    @Test
    fun `a failing session creation shows a message and the user can try again`() =
        runTest {
            val vm = vm()
            collect(vm)
            history.failOnCreate = true
            vm.events.test {
                vm.onStart()
                runCurrent()
                assertEquals(ExerciseEvent.Snackbar(R.string.start_failed), awaitItem())
            }
            assertEquals(Phase.Ready, vm.uiState.value.session.phase)
            history.failOnCreate = false
            vm.onStart()
            runCurrent()
            assertEquals(Phase.Active, vm.uiState.value.session.phase)
        }

    @Test
    fun `the timer starts at the tap, before the session is created`() =
        runTest {
            val vm = vm()
            collect(vm)
            vm.onStart()
            runCurrent()
            assertEquals(Phase.Active, vm.uiState.value.session.phase)
            assertEquals(300_000, vm.timerState.value.displayMs)
        }

    @Test
    fun `finishing after the deadline does not play the end signal`() =
        runTest {
            val vm = started()
            monotonic.advance(400_000)
            vm.onFinish()
            runCurrent()
            assertTrue(endSignal.played.isEmpty())
            assertEquals(Phase.Ended, vm.uiState.value.session.phase)
        }

    @Test
    fun `detection checks the drawn words while typing and follows the typing debounce`() =
        runTest {
            val vm = startedWith(detecting)
            val before = history.snapshots.size
            vm.onAnswerChanged(DemoExercise.keyA, "un texte avec le mot")
            runCurrent()
            assertTrue(
                vm.uiState.value.session.draws
                    .single()
                    .check!!
                    .checked,
            )
            advanceTimeBy(900)
            assertEquals(before, history.snapshots.size)
            advanceTimeBy(200)
            runCurrent()
            assertEquals(
                true,
                history.snapshots
                    .last()
                    .draws
                    .single()
                    .autoDetected,
            )
        }

    @Test
    fun `detection ignores the other fields and redetects after a reroll`() =
        runTest {
            val vm = startedWith(detecting)
            vm.onAnswerChanged(DemoExercise.keyB, "texte")
            assertEquals(
                null,
                vm.uiState.value.session.draws
                    .single()
                    .check,
            )
            vm.onAnswerChanged(DemoExercise.keyA, "texte")
            vm.onReroll()
            runCurrent()
            assertTrue(
                vm.uiState.value.session.draws
                    .single()
                    .check!!
                    .checked,
            )
        }

    @Test
    fun `without a detector the draws are never checked`() =
        runTest {
            val vm = started(detecting)
            vm.onAnswerChanged(DemoExercise.keyA, "texte")
            assertFalse(
                vm.uiState.value.session.draws
                    .single()
                    .check
                    ?.checked == true,
            )
        }

    @Test
    fun `the current question survives the process death`() =
        runTest {
            val sequential =
                DemoExercise.definition.copy(
                    slots = listOf(TableSlot("q", TableId.Places, count = 3)),
                    layout = FieldLayout.Sequential("q", FieldSpec("answer")),
                )
            val first = started(sequential)
            first.onNextQuestion()
            first.onStop()
            runCurrent()
            val second = vm(sequential)
            collect(second)
            assertEquals(1, second.uiState.value.session.currentQuestion)
        }

    private fun kotlinx.coroutines.test.TestScope.startedWith(
        definition: io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition,
    ): ExerciseViewModel {
        detector = containsMine
        return started(definition)
    }
}
