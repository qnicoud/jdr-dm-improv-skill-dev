package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.DetectionSpec
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ElementCheck
import io.github.qnicoud.dmgym.core.exercise.KeywordAnswerDetector
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Détection réelle (mots-clés des tables de fixture) : l'entrée « mine » porte les mots-clés mine et mines. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelDetectionTest : ExerciseViewModelTestBase() {
    private val slot = "words"
    private val key = DrawKey(slot, 0)
    private val detecting =
        DemoExercise.definition.copy(
            slots = listOf(TableSlot(slot, TableId.Places, category = "c")),
            detection = DetectionSpec(listOf(slot), DemoExercise.FIELD_A),
        )

    private fun TestScope.startedDetecting() =
        run {
            detector = KeywordAnswerDetector(DemoExercise.tables(source))
            started(detecting)
        }

    private fun ExerciseViewModel.check(): ElementCheck? =
        uiState.value.session
            .drawsOf(slot)
            .single()
            .check

    private fun ExerciseViewModel.type(text: String) {
        onAnswerChanged(DemoExercise.keyA, text)
    }

    @Test
    fun `typing a keyword pre-checks the draw`() =
        runTest {
            val vm = startedDetecting()
            vm.type("Une mine abandonnée")
            runCurrent()
            assertEquals(ElementCheck(auto = true, occurrences = 1), vm.check())
            assertTrue(vm.check()!!.checked)
        }

    @Test
    fun `deleting the keyword unchecks an automatic check`() =
        runTest {
            val vm = startedDetecting()
            vm.type("Une mine")
            vm.type("Une cave")
            assertFalse(vm.check()!!.checked)
        }

    @Test
    fun `a manual uncheck wins over a later recomputation`() =
        runTest {
            val vm = startedDetecting()
            vm.type("Une mine")
            vm.onCheckToggled(key)
            assertEquals(false, vm.check()!!.manual)
            vm.type("Une mine, encore")
            assertFalse(vm.check()!!.checked)
            assertTrue(vm.check()!!.auto)
        }

    @Test
    fun `a new occurrence of an unchecked word checks it again`() =
        runTest {
            val vm = startedDetecting()
            vm.type("Une mine")
            vm.onCheckToggled(key)
            vm.type("Une mine, puis une autre mine")
            assertTrue(vm.check()!!.checked)
        }

    @Test
    fun `a manual check wins when the word is not detected`() =
        runTest {
            val vm = startedDetecting()
            vm.type("Rien du tout")
            vm.onCheckToggled(key)
            vm.type("Toujours rien")
            assertEquals(true, vm.check()!!.manual)
            assertFalse(vm.check()!!.auto)
            assertTrue(vm.check()!!.checked)
        }

    @Test
    fun `the manual state is saved with the automatic one`() =
        runTest {
            val vm = startedDetecting()
            vm.type("Une mine")
            vm.onCheckToggled(key)
            vm.onStop()
            runCurrent()
            val saved =
                history.sessions
                    .getValue(1)
                    .draws
                    .single()
            assertEquals(true, saved.autoDetected)
            assertEquals(false, saved.manualChecked)
        }

    @Test
    fun `highlights follow the typed text from the first keystroke`() =
        runTest {
            val vm = startedDetecting()
            assertEquals(listOf(4..7), vm.highlights("Une mine"))
            assertEquals(emptyList<IntRange>(), vm.highlights("Une cave"))
        }

    @Test
    fun `highlights are empty for an exercise without detection`() =
        runTest {
            detector = KeywordAnswerDetector(DemoExercise.tables(source))
            val vm = started()
            assertEquals(emptyList<IntRange>(), vm.highlights("Une mine"))
        }

    @Test
    fun `a resumed session detects right away`() =
        runTest {
            val first = startedDetecting()
            first.type("Une mine")
            first.onStop()
            runCurrent()
            val second = vm(detecting)
            collect(second)
            runCurrent()
            assertEquals(listOf(4..7), second.highlights("Une mine"))
        }

    @Test
    fun `haptic reminders follow the settings`() =
        runTest {
            val vm = started()
            assertFalse(vm.hapticReminders.value)
            settings.state.value = settings.state.value.copy(crazyEightsMinuteHaptic = true)
            runCurrent()
            assertTrue(vm.hapticReminders.value)
            settings.state.value = settings.state.value.copy(vibrationEnabled = false)
            runCurrent()
            assertFalse(vm.hapticReminders.value)
        }
}
