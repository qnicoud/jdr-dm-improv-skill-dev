package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.identity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ImportPlannerTest {
    private val a = fullSession(START_2026_10_17)
    private val b = fullSession(START_2026_10_17 + 60_000)
    private val c = fullSession(START_2026_10_17 + 120_000, "constraint_box")

    @Test
    fun `new sessions are to insert and present ones are only counted`() {
        val plan = ImportPlanner.plan(listOf(a, b, c), unknownExercise = 2, existing = setOf(b.session.identity))

        assertEquals(listOf(a, c), plan.toInsert)
        assertEquals(1, plan.alreadyPresent)
        assertEquals(2, plan.unknownExercise)
        assertEquals(3, plan.found)
    }

    @Test
    fun `identity is the exercise and the start instant, not the local id`() {
        val sameStartOtherExercise = fullSession(START_2026_10_17, "constraint_box")

        val plan = ImportPlanner.plan(listOf(sameStartOtherExercise), 0, setOf(a.session.identity))

        assertEquals(1, plan.toInsert.size)
    }

    @Test
    fun `a duplicate inside the file is counted once`() {
        val plan = ImportPlanner.plan(listOf(a, a.withSession { it.copy(id = 9) }, b), 0, emptySet())

        assertEquals(2, plan.toInsert.size)
        assertEquals(2, plan.found)
    }

    @Test
    fun `the date range covers the sessions found, already present ones included`() {
        val later = fullSession(START_2026_10_17 + 1).withSession { it.copy(localEpochDay = EPOCH_DAY_2026_10_17 + 1) }

        val plan = ImportPlanner.plan(listOf(a, later), 0, setOf(a.session.identity))

        assertEquals(EPOCH_DAY_2026_10_17, plan.firstEpochDay)
        assertEquals(EPOCH_DAY_2026_10_17 + 1, plan.lastEpochDay)
    }

    @Test
    fun `invalid sessions are carried to the preview`() {
        val plan = ImportPlanner.plan(listOf(a), unknownExercise = 1, existing = emptySet(), invalid = 2)

        assertEquals(2, plan.invalid)
        assertEquals(3, plan.ignored)
    }

    @Test
    fun `an empty file has no date range and nothing to add`() {
        val plan = ImportPlanner.plan(emptyList(), 0, emptySet())

        assertNull(plan.firstEpochDay)
        assertEquals(0, plan.found)
        assertEquals(false, plan.hasSomethingToAdd)
    }
}
