package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.timer.TimerSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TimerSnapshotCodecTest {
    @Test
    fun `every snapshot survives the round trip`() {
        listOf(
            TimerSnapshot.Idle(10),
            TimerSnapshot.Running(1_000, 10),
            TimerSnapshot.Paused(5, 10),
            TimerSnapshot.Overtime(900, 10),
            TimerSnapshot.OvertimePaused(7, 10),
            TimerSnapshot.Untimed(123),
            TimerSnapshot.UntimedPaused(456),
        ).forEach { assertEquals(it, timerSnapshotOf(it.toLongs())) }
    }

    @Test
    fun `absent or malformed input gives null`() {
        assertNull(timerSnapshotOf(null))
        assertNull(timerSnapshotOf(longArrayOf(1, 2)))
        assertNull(timerSnapshotOf(longArrayOf(99, 0, 0)))
    }
}
