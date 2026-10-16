package io.github.qnicoud.dmgym.core.timer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TimedEventsTest {
    private val engine = TimerEngine()
    private val periodic = TimedEvent.Periodic(intervalMs = 60_000, haptic = true)
    private val once = TimedEvent.Once(fromFraction = 0.4, toFraction = 0.6)

    @Test
    fun `periodic event is planned at each interval strictly inside the duration`() {
        val plan = engine.plan(listOf(periodic), durationMs = 8 * 60_000L, random = Random(1))

        assertEquals((1..7).map { it * 60_000L }, plan.map { it.offsetMs })
        assertTrue(plan.all { it.event == periodic })
    }

    @Test
    fun `once event lands between 40 and 60 percent for any seed`() {
        repeat(200) { seed ->
            val plan = engine.plan(listOf(once), durationMs = 180_000, random = Random(seed))

            assertEquals(1, plan.size)
            assertTrue(plan.single().offsetMs in 72_000..108_000, "offset ${plan.single().offsetMs}")
        }
    }

    @Test
    fun `once event is deterministic with a fixed random`() {
        val a = engine.plan(listOf(once), 180_000, Random(42))
        val b = engine.plan(listOf(once), 180_000, Random(42))

        assertEquals(a, b)
    }

    @Test
    fun `once with equal bounds is exact`() {
        val exact = TimedEvent.Once(0.5, 0.5)

        assertEquals(90_000L, engine.plan(listOf(exact), 180_000, Random(0)).single().offsetMs)
    }

    @Test
    fun `plan is sorted by offset`() {
        val plan = engine.plan(listOf(once, periodic), 180_000, Random(3))

        assertEquals(plan.map { it.offsetMs }.sorted(), plan.map { it.offsetMs })
    }

    @Test
    fun `no events planned without a duration`() {
        assertEquals(emptyList<PlannedEvent>(), engine.plan(listOf(periodic, once), 0, Random(0)))
    }

    @Test
    fun `due returns events crossed since the last evaluation, fresh when recent`() {
        val plan = listOf(PlannedEvent(periodic, 60_000), PlannedEvent(periodic, 120_000))

        val due = engine.due(plan, previousElapsedMs = 59_000, elapsedMs = 60_500)

        assertEquals(listOf(FiredEvent(periodic, 60_000, stale = false)), due)
    }

    @Test
    fun `due is empty when nothing is crossed and does not repeat`() {
        val plan = listOf(PlannedEvent(periodic, 60_000))

        assertEquals(emptyList<FiredEvent>(), engine.due(plan, 0, 59_999))
        assertEquals(emptyList<FiredEvent>(), engine.due(plan, 60_000, 61_000))
    }

    @Test
    fun `missed periodic reminders collapse to the latest one, stale, never caught up`() {
        val plan = engine.plan(listOf(periodic), 8 * 60_000L, Random(0))

        val due = engine.due(plan, previousElapsedMs = 30_000, elapsedMs = 250_000)

        assertEquals(listOf(FiredEvent(periodic, 240_000, stale = true)), due)
    }

    @Test
    fun `a missed once event is still reported but flagged stale`() {
        val plan = listOf(PlannedEvent(once, 90_000))

        val due = engine.due(plan, previousElapsedMs = 0, elapsedMs = 200_000)

        assertEquals(listOf(FiredEvent(once, 90_000, stale = true)), due)
    }

    @Test
    fun `staleness threshold is two seconds`() {
        val plan = listOf(PlannedEvent(once, 10_000))

        assertEquals(false, engine.due(plan, 0, 12_000).single().stale)
        assertEquals(true, engine.due(plan, 0, 12_001).single().stale)
    }

    @Test
    fun `an event at offset zero is included on the first evaluation only`() {
        val immediate = TimedEvent.Once(0.0, 0.0)
        val plan = engine.plan(listOf(immediate), 60_000, Random(0))

        assertEquals(
            listOf(FiredEvent(immediate, 0, stale = false)),
            engine.due(plan, previousElapsedMs = null, elapsedMs = 0),
        )
        assertEquals(emptyList<FiredEvent>(), engine.due(plan, previousElapsedMs = 0, elapsedMs = 250))
    }
}
