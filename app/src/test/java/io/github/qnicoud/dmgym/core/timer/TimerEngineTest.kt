package io.github.qnicoud.dmgym.core.timer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TimerEngineTest {
    private val engine = TimerEngine()
    private val clock = FakeMonotonicClock(1_000)

    @Test
    fun `idle then start gives a running countdown from the clock`() {
        assertEquals(TimerState.Idle(60_000), engine.idle(60_000))

        val state = engine.start(60_000, clock.now())

        assertEquals(TimerState.Running(endAtElapsed = 61_000, durationMs = 60_000), state)
        assertEquals(60_000, engine.remainingMs(state, clock.now()))
    }

    @Test
    fun `remaining time is derived from the end instant, never decremented`() {
        val state = engine.start(60_000, clock.now())

        assertEquals(45_000, engine.remainingMs(state, 16_000))
        assertEquals(45_000, engine.remainingMs(state, 16_000))
        assertEquals(15_000, engine.elapsedMs(state, 16_000))
    }

    @Test
    fun `tick before the deadline keeps the state and emits nothing`() {
        val state = engine.start(60_000, clock.now())

        val result = engine.tick(state, 60_999)

        assertSame(state, result.state)
        assertNull(result.expired)
    }

    @Test
    fun `tick at the deadline expires exactly once with a fresh event`() {
        val state = engine.start(60_000, clock.now())

        val first = engine.tick(state, 61_000)
        val second = engine.tick(first.state, 61_250)

        assertEquals(TimerState.Overtime(endAtElapsed = 61_000, durationMs = 60_000), first.state)
        assertEquals(TimerEvent.Expired(endAtElapsed = 61_000, fresh = true), first.expired)
        assertNull(second.expired)
        assertSame(first.state, second.state)
    }

    @Test
    fun `an expiry observed more than two seconds late is stale`() {
        val state = engine.start(60_000, clock.now())

        assertEquals(true, engine.tick(state, 63_000).expired?.fresh)
        assertEquals(false, engine.tick(state, 63_001).expired?.fresh)
    }

    @Test
    fun `overtime exposes positive extra time and no remaining time`() {
        val overtime = engine.tick(engine.start(60_000, 1_000), 70_000).state

        assertEquals(0, engine.remainingMs(overtime, 70_000))
        assertEquals(9_000, engine.overtimeMs(overtime, 70_000))
        assertEquals(69_000, engine.elapsedMs(overtime, 70_000))
        assertEquals(0, engine.overtimeMs(engine.start(60_000, 0), 10))
    }

    @Test
    fun `untimed counts elapsed time upwards and never expires`() {
        val state = engine.startUntimed(1_000)

        val result = engine.tick(state, 10_000_000)

        assertEquals(TimerState.Untimed(startedAtElapsed = 1_000), state)
        assertSame(state, result.state)
        assertNull(result.expired)
        assertEquals(9_999_000, engine.elapsedMs(state, 10_000_000))
        assertEquals(0, engine.remainingMs(state, 5_000))
    }

    @Test
    fun `idle has full remaining time and no elapsed time`() {
        val idle = engine.idle(30_000)

        assertEquals(30_000, engine.remainingMs(idle, 99))
        assertEquals(0, engine.elapsedMs(idle, 99))
        assertSame(idle, engine.tick(idle, 99).state)
    }

    @Test
    fun `a non positive duration is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { engine.idle(0) }
        assertThrows(IllegalArgumentException::class.java) { engine.start(-1, 0) }
    }
}
