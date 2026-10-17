package io.github.qnicoud.dmgym.core.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProgressTest {
    private val session = ExerciseSession(DemoExercise.id)

    @Test
    fun `counts non blank fields`() {
        val s = reduce(session, ExerciseAction.AnswerChanged(DemoExercise.keyA, "x"))
        val blank = reduce(s, ExerciseAction.AnswerChanged(DemoExercise.keyB, "  "))
        assertEquals(Progress(1, 2), progressOf(DemoExercise.definition, blank))
    }

    @Test
    fun `no rule means no progress`() {
        assertNull(progressOf(DemoExercise.definition.copy(progress = ProgressRule.None), session))
    }
}
