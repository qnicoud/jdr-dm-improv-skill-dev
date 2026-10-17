package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SessionRecordsTest {
    private val record =
        SessionRecord(
            id = 3,
            exerciseId = "demo",
            status = SessionStatus.IN_PROGRESS,
            startedAtEpochMillis = 0,
            zoneId = "UTC",
            localEpochDay = 0,
        )

    private fun rich(): ExerciseSession {
        val base = ExerciseSession(DemoExercise.id, sessionId = 3, phase = Phase.Active)
        return listOf(
            ExerciseAction.Drawn(
                listOf(DemoExercise.draw(), DemoExercise.draw(position = 1).copy(check = ElementCheck(true, false))),
            ),
            ExerciseAction.AnswerChanged(DemoExercise.keyA, "idée"),
            ExerciseAction.FlagToggled(DemoExercise.keyA),
            ExerciseAction.OptionChanged("video", true),
            ExerciseAction.EventFired("shift", emptyList()),
            ExerciseAction.Assessed(1, true, mapOf("q1" to true)),
        ).fold(base, ::reduce)
    }

    private fun detail(
        s: ExerciseSession,
        status: SessionStatus = SessionStatus.IN_PROGRESS,
        options: String = s.optionsJson(),
    ) = SessionDetail(
        record.copy(status = status, optionsJson = options, selfRating = s.selfRating, isGem = s.isGem),
        s.drawRecords(),
        s.answerRecords(),
        s.rubricRecords(),
    )

    @Test
    fun `a session survives the round trip through the records`() {
        val original = rich()
        assertEquals(original, detail(original).toExerciseSession())
    }

    @Test
    fun `completed sessions come back ended`() {
        assertEquals(Phase.Ended, detail(rich(), SessionStatus.COMPLETED).toExerciseSession().phase)
    }

    @Test
    fun `options carry fired events under a reserved prefix`() {
        assertEquals("""{"video":true,"event:shift":true}""", rich().optionsJson())
    }

    @Test
    fun `a manual only check is restored and a corrupted options json gives no options`() {
        val s = rich().copy(draws = listOf(DemoExercise.draw().copy(check = ElementCheck(false, true))))
        assertEquals(
            ElementCheck(false, true),
            detail(s)
                .toExerciseSession()
                .draws
                .single()
                .check,
        )
        val broken = detail(s, options = "not json").toExerciseSession()
        assertEquals(emptyMap<String, Boolean>(), broken.options)
        assertEquals(emptySet<String>(), broken.firedEvents)
    }

    @Test
    fun `a draw without any check is restored without one`() {
        val s = rich().copy(draws = listOf(DemoExercise.draw()))
        assertEquals(
            null,
            detail(s)
                .toExerciseSession()
                .draws
                .single()
                .check,
        )
    }
}
