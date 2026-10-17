package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.core.timer.TimerState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimerUiTest {
    private val engine = TimerEngine()
    private val five = 300_000L

    private fun ui(
        state: TimerState,
        now: Long = 0,
        untimed: Boolean = false,
    ) = timerUi(state, engine, now, untimed)

    @Test
    fun `idle is ready with the full duration`() {
        assertEquals(TimerUi(TimerKind.Ready, five, 1f), ui(TimerState.Idle(five)))
    }

    @Test
    fun `idle without timer is untimed`() {
        assertEquals(TimerKind.Untimed, ui(TimerState.Idle(five), untimed = true).kind)
    }

    @Test
    fun `running rounds the remaining time up to the second`() {
        val ui = ui(TimerState.Running(endAtElapsed = five, durationMs = five), now = 1_500)
        assertEquals(TimerKind.Running, ui.kind)
        assertEquals(299_000, ui.displayMs)
    }

    @Test
    fun `last minute needs a duration of at least three minutes`() {
        assertEquals(TimerKind.LastMinute, ui(TimerState.Running(five, five), now = five - 58_000).kind)
        assertEquals(TimerKind.Running, ui(TimerState.Running(30_000, 30_000), now = 1_000).kind)
    }

    @Test
    fun `overtime shows the time past the deadline`() {
        val ui = ui(TimerState.Overtime(endAtElapsed = five, durationMs = five), now = five + 42_000)
        assertEquals(TimerKind.Overtime, ui.kind)
        assertEquals(42_000, ui.displayMs)
        assertEquals(0f, ui.fraction)
    }

    @Test
    fun `paused states freeze the value`() {
        val countdown = ui(TimerState.Paused(130_000, five))
        assertEquals(TimerKind.Paused, countdown.kind)
        assertEquals(130_000, countdown.displayMs)
        val over = ui(TimerState.OvertimePaused(5_000, five))
        assertTrue(over.overtime)
        assertEquals(5_000, over.displayMs)
        assertEquals(9_000, ui(TimerState.UntimedPaused(9_000)).displayMs)
    }

    @Test
    fun `untimed shows the elapsed time`() {
        val ui = ui(TimerState.Untimed(startedAtElapsed = 1_000), now = 61_000)
        assertEquals(TimerKind.Untimed, ui.kind)
        assertEquals(60_000, ui.displayMs)
    }

    @Test
    fun `milestones are last minute and halfway for long durations`() {
        val eight = 480_000L
        val last = ui(TimerState.Running(eight, eight), now = eight - 30_000)
        assertEquals(setOf(Milestone.LastMinute, Milestone.Halfway), milestonesReached(last, eight))
        assertEquals(
            setOf(Milestone.LastMinute),
            milestonesReached(
                ui(
                    TimerState.Running(five, five),
                    now =
                        five - 30_000,
                ),
                five,
            ),
        )
        val early = ui(TimerState.Running(five, five), now = 10_000)
        assertEquals(emptySet<Milestone>(), milestonesReached(early, five))
        val shortMid = ui(TimerState.Running(240_000, 240_000), now = 130_000)
        assertEquals(emptySet<Milestone>(), milestonesReached(shortMid, 240_000))
        assertEquals(emptySet<Milestone>(), milestonesReached(ui(TimerState.Paused(1_000, five)), five))
    }
}
