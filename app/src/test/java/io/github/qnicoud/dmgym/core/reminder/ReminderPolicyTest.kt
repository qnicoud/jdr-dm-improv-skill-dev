package io.github.qnicoud.dmgym.core.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReminderPolicyTest {
    private val today = 20_000L

    private fun decide(
        practiced: Set<Long> = setOf(today - 3),
        sessionCount: Int = 5,
        resumed: Long? = null,
        day: Long = today,
    ) = ReminderPolicy.decide(day, practiced, sessionCount, resumed)

    private fun notifyOn(offset: Long) =
        decide(day = today + offset, practiced = setOf(today + offset - 1)) as ReminderDecision.Notify

    @Test
    fun `already practiced today skips`() {
        assertEquals(ReminderDecision.Skip(SkipReason.AlreadyPracticed), decide(practiced = setOf(today)))
    }

    @Test
    fun `thirteen days without session still notifies`() {
        assertTrue(decide(practiced = setOf(today - 13)) is ReminderDecision.Notify)
    }

    @Test
    fun `fourteen days without session pauses`() {
        assertEquals(ReminderDecision.Skip(SkipReason.Paused14Days), decide(practiced = setOf(today - 14)))
    }

    @Test
    fun `resume restarts the fourteen days count`() {
        assertTrue(decide(practiced = setOf(today - 30), resumed = today - 2) is ReminderDecision.Notify)
        assertEquals(
            ReminderDecision.Skip(SkipReason.Paused14Days),
            decide(practiced = setOf(today - 30), resumed = today - 14),
        )
    }

    @Test
    fun `last practiced day is the most recent one`() {
        assertTrue(decide(practiced = setOf(today - 40, today - 2)) is ReminderDecision.Notify)
    }

    @Test
    fun `no session picks the first exercise text`() {
        val decision = decide(practiced = emptySet(), sessionCount = 0, resumed = today - 1)
        assertEquals(ReminderDecision.Notify(textIndex = 0, firstExercise = true), decision)
    }

    @Test
    fun `never practiced and never resumed does not pause`() {
        assertTrue(decide(practiced = emptySet(), sessionCount = 0) is ReminderDecision.Notify)
    }

    @Test
    fun `never practiced pauses fourteen days after activation`() {
        assertEquals(
            ReminderDecision.Skip(SkipReason.Paused14Days),
            decide(practiced = emptySet(), sessionCount = 0, resumed = today - 14),
        )
    }

    @Test
    fun `text is deterministic by day and within the pool`() {
        assertEquals(notifyOn(0), notifyOn(0))
        (0L..20L).forEach {
            val n = notifyOn(it)
            assertTrue(n.textIndex in 0 until ReminderPolicy.GENERAL_TEXT_COUNT)
            assertFalse(n.firstExercise)
        }
    }

    @Test
    fun `text varies across days`() {
        assertTrue((0L..5L).map { notifyOn(it).textIndex }.toSet().size > 1)
    }

    @Test
    fun `paused state is computed for the settings screen`() {
        assertTrue(ReminderPolicy.isPaused(today, setOf(today - 20), null))
        assertFalse(ReminderPolicy.isPaused(today, setOf(today - 20), today - 1))
    }
}
