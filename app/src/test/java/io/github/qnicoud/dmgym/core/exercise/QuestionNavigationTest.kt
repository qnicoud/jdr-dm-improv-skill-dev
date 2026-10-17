package io.github.qnicoud.dmgym.core.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Navigation entre les questions de Core Image et groupes répétables du réducteur. */
class QuestionNavigationTest {
    private val start = ExerciseSession(DemoExercise.id, phase = Phase.Active, sessionId = 1)

    private fun next(
        session: ExerciseSession,
        last: Int = 5,
    ) = reduce(session, ExerciseAction.NextQuestion(last))

    @Test
    fun `next moves forward and stops on the last question`() {
        val third = next(next(start))
        assertEquals(2, third.currentQuestion)
        assertEquals(5, (1..10).fold(start) { s, _ -> next(s) }.currentQuestion)
    }

    @Test
    fun `previous moves back and stops on the first question`() {
        val back = reduce(next(next(start)), ExerciseAction.PreviousQuestion)
        assertEquals(1, back.currentQuestion)
        assertEquals(0, reduce(start, ExerciseAction.PreviousQuestion).currentQuestion)
    }

    @Test
    fun `answers are kept per question`() {
        val first = AnswerKey("answer", 0)
        val second = AnswerKey("answer", 1)
        val s =
            listOf(
                ExerciseAction.AnswerChanged(first, "une"),
                ExerciseAction.NextQuestion(5),
                ExerciseAction.AnswerChanged(second, "deux"),
                ExerciseAction.PreviousQuestion,
            ).fold(start) { acc, a -> reduce(acc, a) }
        assertEquals("une", s.textOf(first))
        assertEquals("deux", s.textOf(second))
        assertEquals(0, s.currentQuestion)
    }

    @Test
    fun `groups are addressed by index and keep independent texts`() {
        val keys = (0..4).map { AnswerKey("npc", it) }
        val s = keys.foldIndexed(start) { i, acc, k -> reduce(acc, ExerciseAction.AnswerChanged(k, "pnj $i")) }
        assertEquals(keys.map { "pnj ${it.groupIndex}" }, keys.map { s.textOf(it) })
        assertEquals(true, s.hasContent)
    }
}
