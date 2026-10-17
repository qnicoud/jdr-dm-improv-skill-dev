package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DailyDoneTest {
    private val plan = listOf(ExerciseId.CRAZY_EIGHTS, ExerciseId.CONSTRAINT_BOX, ExerciseId.DECISION_POINT)

    private fun session(
        id: ExerciseId,
        status: SessionStatus = SessionStatus.COMPLETED,
        day: Long = 100,
        rating: Int? = null,
        gem: Boolean = false,
        seconds: Int = 0,
    ) = SessionRecord(
        exerciseId = id.value,
        status = status,
        startedAtEpochMillis = 0,
        zoneId = "UTC",
        localEpochDay = day,
        selfRating = rating,
        isGem = gem,
        activeDurationSec = seconds,
    )

    @Test
    fun `each line carries the level, the gem and the time of its completed session`() {
        val lines =
            dailyDoneLines(
                plan,
                today = 100,
                sessions =
                    listOf(
                        session(ExerciseId.CRAZY_EIGHTS, rating = 2, gem = true, seconds = 480),
                        session(ExerciseId.CONSTRAINT_BOX, rating = null, seconds = 300),
                        session(ExerciseId.DECISION_POINT, rating = 1, seconds = 30),
                    ),
            )
        assertEquals(plan, lines.map { it.id })
        assertEquals(listOf(true, true, true), lines.map { it.done })
        assertEquals(listOf(2, null, 1), lines.map { it.rating })
        assertEquals(listOf(true, false, false), lines.map { it.isGem })
        assertEquals(listOf(480, 300, 30), lines.map { it.durationSec })
    }

    @Test
    fun `an exercise without a completed session of the day is not done`() {
        val lines =
            dailyDoneLines(
                plan,
                today = 100,
                sessions =
                    listOf(
                        session(ExerciseId.CRAZY_EIGHTS, status = SessionStatus.IN_PROGRESS),
                        session(ExerciseId.CONSTRAINT_BOX, day = 99),
                    ),
            )
        assertEquals(listOf(false, false, false), lines.map { it.done })
        assertTrue(lines.all { it.rating == null && !it.isGem })
    }

    @Test
    fun `the most recent completed session wins when an exercise was redone`() {
        val newest = session(ExerciseId.CRAZY_EIGHTS, rating = 3, seconds = 200)
        val older = session(ExerciseId.CRAZY_EIGHTS, rating = 1, seconds = 100)
        val line = dailyDoneLines(plan, today = 100, sessions = listOf(newest, older)).first()
        assertEquals(3, line.rating)
        assertEquals(200, line.durationSec)
    }

    @Test
    fun `the summary is complete only when every exercise is done`() {
        val all = plan.map { session(it) }
        assertTrue(dailyDoneLines(plan, 100, all).all { it.done })
        assertFalse(dailyDoneLines(plan, 100, all.drop(1)).all { it.done })
    }
}
