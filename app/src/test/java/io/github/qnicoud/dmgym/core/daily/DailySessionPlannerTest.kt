package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DailySessionPlannerTest {
    private val catalog = ExerciseId.ALL
    private val noStats = PracticeStats(emptyMap())
    private val firstDay = 20_000L
    private val days = (firstDay until firstDay + 200)

    private fun plan(
        day: Long,
        stats: PracticeStats = noStats,
        options: DailyOptions = DailyOptions(),
        previous: DailyPlan? = null,
        from: List<ExerciseId> = catalog,
    ) = DailySessionPlanner.plan(day, stats, from, options, previous)

    @Test
    fun `the plan is deterministic for a given date`() {
        days.forEach { assertEquals(plan(it), plan(it)) }
    }

    @Test
    fun `different dates give different plans`() {
        assertTrue(days.map { plan(it).exercises }.toSet().size > 10)
    }

    @Test
    fun `a plan holds three distinct exercises dated today`() {
        days.forEach {
            val p = plan(it)
            assertEquals(it, p.epochDay)
            assertEquals(DailyPlan.SIZE, p.exercises.size)
            assertEquals(3, p.exercises.toSet().size)
        }
    }

    @Test
    fun `one page is excluded by default and allowed when the option is set`() {
        assertTrue(days.none { ExerciseId.ONE_PAGE in plan(it).exercises })
        assertTrue(
            days.any { ExerciseId.ONE_PAGE in plan(it, options = DailyOptions(includeOnePage = true)).exercises },
        )
    }

    @Test
    fun `the first exercise is a short warm up`() {
        days.forEach { assertTrue(plan(it).exercises[0] in ExerciseId.DAILY_WARM_UPS) }
    }

    @Test
    fun `the second exercise is a core exercise rather than a warm up`() {
        days.forEach { assertTrue(plan(it).exercises[1] !in ExerciseId.DAILY_WARM_UPS) }
    }

    @Test
    fun `the weak zone is the least practiced exercise of the last days`() {
        val stats = PracticeStats(catalog.associateWith { 4 } + (ExerciseId.CORE_IMAGE to 0))
        days.forEach { assertEquals(ExerciseId.CORE_IMAGE, plan(it, stats).exercises[2]) }
    }

    @Test
    fun `missing counts are read as zero and ties are broken by the date`() {
        val weak = days.map { plan(it).exercises[2] }.toSet()
        assertTrue(weak.size > 2)
        val stats = PracticeStats(mapOf(ExerciseId.CRAZY_EIGHTS to 3))
        assertTrue(days.none { plan(it, stats).exercises[2] == ExerciseId.CRAZY_EIGHTS })
    }

    @Test
    fun `counts of excluded exercises do not matter`() {
        val stats = PracticeStats(catalog.associateWith { 2 } + (ExerciseId.ONE_PAGE to 0))
        assertTrue(days.none { ExerciseId.ONE_PAGE in plan(it, stats).exercises })
    }

    @Test
    fun `no exercise repeats the same position as the day before`() {
        days.forEach {
            val yesterday = plan(it - 1)
            val today = plan(it, previous = yesterday)
            today.exercises.indices.forEach { i -> assertNotEquals(yesterday.exercises[i], today.exercises[i]) }
        }
    }

    @Test
    fun `the repetition is accepted when it is the only choice`() {
        val stats = PracticeStats(catalog.associateWith { 9 } + (ExerciseId.CORE_IMAGE to 0))
        val yesterday = plan(firstDay - 1, stats)
        assertEquals(ExerciseId.CORE_IMAGE, yesterday.exercises[2])
        assertEquals(ExerciseId.CORE_IMAGE, plan(firstDay, stats, previous = yesterday).exercises[2])
    }

    @Test
    fun `a catalog without warm ups still yields three distinct exercises`() {
        val small = listOf(ExerciseId.CRAZY_EIGHTS, ExerciseId.CORE_IMAGE, ExerciseId.CONNECT_STORY)
        days.forEach { assertEquals(small.toSet(), plan(it, from = small).exercises.toSet()) }
    }

    @Test
    fun `a catalog of only warm ups still yields three distinct exercises`() {
        val warm = ExerciseId.DAILY_WARM_UPS.toList()
        days.forEach { assertEquals(warm.toSet(), plan(it, from = warm).exercises.toSet()) }
    }

    @Test
    fun `a catalog with fewer than three usable exercises is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            plan(firstDay, from = listOf(ExerciseId.CRAZY_EIGHTS, ExerciseId.ONE_PAGE, ExerciseId.CORE_IMAGE))
        }
    }

    @Test
    fun `duplicates in the catalog are ignored`() {
        val duplicated = catalog + catalog
        days.forEach { assertEquals(3, plan(it, from = duplicated).exercises.toSet().size) }
    }
}
