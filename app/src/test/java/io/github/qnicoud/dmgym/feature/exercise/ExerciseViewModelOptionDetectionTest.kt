package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.DemoExercise
import io.github.qnicoud.dmgym.core.exercise.DetectionSpec
import io.github.qnicoud.dmgym.core.exercise.ExerciseOption
import io.github.qnicoud.dmgym.core.exercise.KeywordAnswerDetector
import io.github.qnicoud.dmgym.core.exercise.OptionCondition
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** La détection suit les tirages refaits quand une option change (L8 + L10). */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelOptionDetectionTest : ExerciseViewModelTestBase() {
    private val slot = "words"
    private val definition =
        DemoExercise.definition.copy(
            slots = listOf(TableSlot(slot, TableId.Places, category = "c", onlyIf = OptionCondition("drawn"))),
            options = listOf(ExerciseOption("drawn", 1)),
            detection = DetectionSpec(listOf(slot), DemoExercise.FIELD_A),
        )

    @Test
    fun `the detection targets follow the draw made by an option`() =
        runTest {
            detector = KeywordAnswerDetector(DemoExercise.tables(source))
            val vm = vm(definition)
            collect(vm)
            assertEquals(emptyList<IntRange>(), vm.highlights("Une mine"))
            vm.onOptionChanged("drawn", true)
            runCurrent()
            assertEquals(listOf(4..7), vm.highlights("Une mine"))
        }

    @Test
    fun `the detection targets follow a reroll`() =
        runTest {
            detector = KeywordAnswerDetector(DemoExercise.tables(source))
            val vm = vm(definition)
            collect(vm)
            vm.onOptionChanged("drawn", true)
            runCurrent()
            vm.onReroll(null)
            runCurrent()
            assertEquals(listOf(4..7), vm.highlights("Une mine"))
        }
}
