package io.github.qnicoud.dmgym.core.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReachedQuestionsTest {
    @Test
    fun `nothing answered still reaches the first question`() {
        assertEquals(1, reachedQuestions(emptyList(), total = 5))
        assertEquals(0, lastAnsweredQuestion(emptyList(), total = 5))
    }

    @Test
    fun `the highest answered question decides, gaps included`() {
        assertEquals(3, reachedQuestions(listOf(0, 2), total = 5))
        assertEquals(2, lastAnsweredQuestion(listOf(0, 2), total = 5))
    }

    @Test
    fun `it never exceeds the number of questions`() {
        assertEquals(5, reachedQuestions(listOf(9), total = 5))
        assertEquals(4, lastAnsweredQuestion(listOf(9), total = 5))
    }
}
