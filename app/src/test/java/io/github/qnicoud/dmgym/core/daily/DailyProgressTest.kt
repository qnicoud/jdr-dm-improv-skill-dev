package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DailyProgressTest {
    private val today = 500L
    private val ids = listOf(ExerciseId.CRAZY_EIGHTS, ExerciseId.CONSTRAINT_BOX, ExerciseId.DECISION_POINT)

    private fun session(
        id: ExerciseId,
        status: SessionStatus,
        day: Long = today,
        fromDaily: Boolean = false,
    ) = SessionRecord(
        exerciseId = id.value,
        status = status,
        startedAtEpochMillis = 0,
        zoneId = "UTC",
        localEpochDay = day,
        fromDaily = fromDaily,
    )

    @Test
    fun `without sessions everything is to do`() {
        val p = dailyProgress(ids, today, emptyList())
        assertEquals(List(3) { ExerciseProgress.TODO }, p.states)
        assertFalse(p.allDone)
        assertFalse(p.started)
        assertEquals(ids[0], p.nextExercise)
    }

    @Test
    fun `a completed session today marks the exercise done whatever its origin`() {
        val p =
            dailyProgress(
                ids,
                today,
                listOf(
                    session(ids[0], SessionStatus.COMPLETED),
                    session(ids[1], SessionStatus.COMPLETED, fromDaily = true),
                ),
            )
        assertEquals(listOf(ExerciseProgress.DONE, ExerciseProgress.DONE, ExerciseProgress.TODO), p.states)
        assertTrue(p.started)
        assertEquals(ids[2], p.nextExercise)
    }

    @Test
    fun `an unfinished session today is in progress and is proposed first`() {
        val p = dailyProgress(ids, today, listOf(session(ids[1], SessionStatus.IN_PROGRESS)))
        assertEquals(listOf(ExerciseProgress.TODO, ExerciseProgress.IN_PROGRESS, ExerciseProgress.TODO), p.states)
        assertEquals(ids[1], p.nextExercise)
    }

    @Test
    fun `done wins over in progress for the same exercise`() {
        val p =
            dailyProgress(
                ids,
                today,
                listOf(session(ids[0], SessionStatus.IN_PROGRESS), session(ids[0], SessionStatus.COMPLETED)),
            )
        assertEquals(ExerciseProgress.DONE, p.states[0])
    }

    @Test
    fun `sessions of other days and other exercises are ignored`() {
        val p =
            dailyProgress(
                ids,
                today,
                listOf(
                    session(ids[0], SessionStatus.COMPLETED, day = today - 1),
                    session(ExerciseId.CORE_IMAGE, SessionStatus.COMPLETED),
                ),
            )
        assertEquals(List(3) { ExerciseProgress.TODO }, p.states)
    }

    @Test
    fun `the session is finished when the three exercises are done`() {
        val p = dailyProgress(ids, today, ids.map { session(it, SessionStatus.COMPLETED) })
        assertTrue(p.allDone)
        assertNull(p.nextExercise)
    }
}
