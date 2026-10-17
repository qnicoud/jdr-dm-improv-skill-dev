package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class EventPlanTest {
    private val engine = TimerEngine()
    private val events = DemoExercise.withEvents.events

    @Test
    fun `no duration means no events`() {
        assertTrue(EventPlan(engine, events, null, Random(1)).due(0, 600_000).isEmpty())
    }

    @Test
    fun `the periodic reminder is due each minute and the shift once`() {
        val plan = EventPlan(engine, events, 300_000, Random(1))
        assertEquals(listOf("reminder"), plan.due(0, 61_000).map { it.event.id })
        assertEquals(listOf("reminder", "shift"), plan.due(61_000, 180_000).map { it.event.id })
        assertTrue(plan.due(180_000, 181_000).none { it.event.id == "shift" })
    }

    @Test
    fun `a missed occurrence is reported stale`() {
        val plan = EventPlan(engine, events.take(1), 300_000, Random(1))
        assertEquals(listOf(true), plan.due(0, 70_000).map { it.stale })
    }

    @Test
    fun `a timer rebuilt without snapshot is paused and picks up planned time`() {
        assertEquals(TimerState.UntimedPaused(90_000), pausedTimerFrom(null, 90))
        assertEquals(TimerState.Paused(210_000, 300_000), pausedTimerFrom(300, 90))
        assertEquals(TimerState.OvertimePaused(30_000, 300_000), pausedTimerFrom(300, 330))
    }
}
