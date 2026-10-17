package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DailyStepTest {
    private val plan = listOf(ExerciseId.CRAZY_EIGHTS, ExerciseId.CONSTRAINT_BOX, ExerciseId.DECISION_POINT)
    private val todo = ExerciseProgress.TODO
    private val done = ExerciseProgress.DONE

    @Test
    fun `the first exercise is 1 of 3 and leads to the second`() {
        val step = checkNotNull(dailyStep(plan, listOf(todo, todo, todo), ExerciseId.CRAZY_EIGHTS))
        assertEquals(1, step.number)
        assertEquals(3, step.total)
        assertEquals(DailyNext(ExerciseId.CONSTRAINT_BOX, 2), step.next)
    }

    @Test
    fun `the next exercise skips the ones already done`() {
        val step = checkNotNull(dailyStep(plan, listOf(todo, todo, done), ExerciseId.CRAZY_EIGHTS))
        assertEquals(DailyNext(ExerciseId.CONSTRAINT_BOX, 2), step.next)
        assertNull(checkNotNull(dailyStep(plan, listOf(todo, todo, done), ExerciseId.CONSTRAINT_BOX)).next)
        val skipOver = checkNotNull(dailyStep(plan, listOf(todo, done, todo), ExerciseId.CRAZY_EIGHTS))
        assertEquals(DailyNext(ExerciseId.DECISION_POINT, 3), skipOver.next)
    }

    @Test
    fun `the third exercise has no next one, even if an earlier one was skipped`() {
        val step = checkNotNull(dailyStep(plan, listOf(todo, done, done), ExerciseId.DECISION_POINT))
        assertEquals(3, step.number)
        assertNull(step.next)
    }

    @Test
    fun `an exercise outside the plan has no step`() {
        assertNull(dailyStep(plan, listOf(todo, todo, todo), ExerciseId.CORE_IMAGE))
        assertNull(dailyStep(emptyList(), emptyList(), ExerciseId.CORE_IMAGE))
    }
}
