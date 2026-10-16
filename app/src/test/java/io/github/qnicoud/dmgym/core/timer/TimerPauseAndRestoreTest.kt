package io.github.qnicoud.dmgym.core.timer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TimerPauseAndRestoreTest {
    private val engine = TimerEngine()

    @Test
    fun `pause freezes the remaining time and resume restarts from it`() {
        val result = engine.pause(engine.start(60_000, 0), 20_000)
        val paused = result.state

        assertEquals(TimerState.Paused(remainingMs = 40_000, durationMs = 60_000), paused)
        assertNull(result.expired)
        assertEquals(40_000, engine.remainingMs(paused, 500_000))
        assertEquals(20_000, engine.elapsedMs(paused, 500_000))
        assertSame(paused, engine.tick(paused, 500_000).state)
        assertNull(engine.tick(paused, 500_000).expired)
        assertEquals(TimerState.Running(540_000, 60_000), engine.resume(paused, 500_000))
    }

    @Test
    fun `pausing a countdown past its deadline still reports the expiry and pauses the overtime`() {
        val result = engine.pause(engine.start(1_000, 0), 5_000)

        assertEquals(TimerEvent.Expired(endAtElapsed = 1_000, fresh = false), result.expired)
        assertEquals(TimerState.OvertimePaused(overtimeMs = 4_000, durationMs = 1_000), result.state)
    }

    @Test
    fun `pausing exactly on a fresh deadline reports a fresh expiry`() {
        val result = engine.pause(engine.start(1_000, 0), 1_500)

        assertEquals(TimerEvent.Expired(1_000, fresh = true), result.expired)
    }

    @Test
    fun `overtime can be paused and resumed`() {
        val overtime = engine.tick(engine.start(1_000, 0), 1_000).state

        val paused = engine.pause(overtime, 4_000)

        assertEquals(TimerState.OvertimePaused(overtimeMs = 3_000, durationMs = 1_000), paused.state)
        assertNull(paused.expired)
        assertEquals(3_000, engine.overtimeMs(paused.state, 99_000))
        assertEquals(0, engine.remainingMs(paused.state, 99_000))
        assertEquals(4_000, engine.elapsedMs(paused.state, 99_000))
        assertSame(paused.state, engine.tick(paused.state, 99_000).state)
        assertEquals(
            TimerState.Overtime(endAtElapsed = 97_000, durationMs = 1_000),
            engine.resume(paused.state, 100_000),
        )
    }

    @Test
    fun `pause and resume also work without a timer`() {
        val paused = engine.pause(engine.startUntimed(0), 12_000).state

        assertEquals(TimerState.UntimedPaused(elapsedMs = 12_000), paused)
        assertEquals(12_000, engine.elapsedMs(paused, 99_000))
        assertEquals(TimerState.Untimed(startedAtElapsed = 88_000), engine.resume(paused, 100_000))
    }

    @Test
    fun `pause is ignored when there is nothing to pause`() {
        val idle = engine.idle(1_000)
        val paused = engine.pause(idle, 10).state
        val running = engine.start(1_000, 0)

        assertSame(idle, paused)
        assertSame(idle, engine.resume(idle, 10))
        assertSame(running, engine.resume(running, 10))
    }

    @Test
    fun `restore rebuilds a running countdown from the wall clock`() {
        val snapshot = TimerSnapshot.Running(endAtWallMillis = 1_000_000, durationMs = 60_000)

        val state = engine.restore(snapshot, wallNow = 970_000, elapsedNow = 5_000)

        assertEquals(TimerState.Running(endAtElapsed = 35_000, durationMs = 60_000), state)
    }

    @Test
    fun `restore after the deadline goes straight to overtime without an event`() {
        val snapshot = TimerSnapshot.Running(1_000_000, 60_000)

        val state = engine.restore(snapshot, wallNow = 1_030_000, elapsedNow = 5_000)

        assertEquals(TimerState.Overtime(endAtElapsed = -25_000, durationMs = 60_000), state)
        assertEquals(30_000, engine.overtimeMs(state, 5_000))
    }

    @Test
    fun `restore keeps an untimed stopwatch running across the downtime`() {
        val state =
            engine.restore(
                TimerSnapshot.Untimed(startedAtWallMillis = 1_000),
                wallNow = 11_000,
                elapsedNow = 50_000,
            )

        assertEquals(TimerState.Untimed(startedAtElapsed = 40_000), state)
    }

    @Test
    fun `restore of paused states ignores the clocks`() {
        assertEquals(
            TimerState.Paused(40_000, 60_000),
            engine.restore(TimerSnapshot.Paused(40_000, 60_000), wallNow = 1, elapsedNow = 2),
        )
        assertEquals(
            TimerState.OvertimePaused(3_000, 60_000),
            engine.restore(TimerSnapshot.OvertimePaused(3_000, 60_000), 1, 2),
        )
        assertEquals(TimerState.UntimedPaused(7_000), engine.restore(TimerSnapshot.UntimedPaused(7_000), 1, 2))
        assertEquals(TimerState.Idle(5_000), engine.restore(TimerSnapshot.Idle(5_000), 1, 2))
    }

    @Test
    fun `snapshot then restore round trips every state`() {
        val wallNow = 1_700_000_000_000
        val elapsedNow = 100_000L
        val states =
            listOf(
                engine.idle(60_000),
                engine.start(60_000, 90_000),
                engine.pause(engine.start(60_000, 90_000), 95_000).state,
                engine.tick(engine.start(60_000, 0), 70_000).state,
                engine.pause(engine.tick(engine.start(60_000, 0), 70_000).state, 80_000).state,
                engine.startUntimed(10_000),
                engine.pause(engine.startUntimed(10_000), 30_000).state,
            )

        states.forEach { state ->
            val restored = engine.restore(engine.snapshot(state, wallNow, elapsedNow), wallNow, elapsedNow)

            assertEquals(state, restored)
        }
    }

    @Test
    fun `restore after a process death moves elapsed clock but keeps the wall deadline`() {
        val running = engine.start(60_000, 40_000)
        val snapshot = engine.snapshot(running, wallNow = 1_000_000, elapsedNow = 50_000)

        val restored = engine.restore(snapshot, wallNow = 1_020_000, elapsedNow = 1_000)

        assertEquals(TimerState.Running(endAtElapsed = 31_000, durationMs = 60_000), restored)
    }
}
