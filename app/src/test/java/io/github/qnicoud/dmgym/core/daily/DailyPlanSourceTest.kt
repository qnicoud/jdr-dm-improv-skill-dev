package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class DailyPlanSourceTest {
    private val today = 20_500L
    private val options = DailyOptions()

    @Test
    fun `reads only the fourteen days before the plan day`() =
        runTest {
            val history = FakeHistoryRepository()
            DailyPlanSource(history).planFor(today, ExerciseId.ALL, options)
            assertEquals(
                listOf((today - 15) to (today - 2), (today - 14) to (today - 1)),
                history.countCalls.sortedBy { it.first },
            )
        }

    @Test
    fun `the plan does not change after a session is done today`() =
        runTest {
            val first = DailyPlanSource(FakeHistoryRepository()).planFor(today, ExerciseId.ALL, options)
            // Une session d'aujourd'hui ne figure que dans des fenêtres qui incluent today : elles ne sont jamais lues.
            val withToday =
                FakeHistoryRepository(counts = { _, to -> if (to >= today) todayCounts(first) else emptyList() })
            assertEquals(first, DailyPlanSource(withToday).planFor(today, ExerciseId.ALL, options))
        }

    private fun todayCounts(plan: DailyPlan) = listOf(ExerciseCountRecord(plan.exercises[0].value, 9))

    @Test
    fun `the weak zone follows the counts of the window`() =
        runTest {
            val counts =
                ExerciseId.ALL.filter { it != ExerciseId.STATUS_NUMBERS }.map { ExerciseCountRecord(it.value, 3) }
            val history = FakeHistoryRepository(counts = { _, _ -> counts })
            val plan = DailyPlanSource(history).planFor(today, ExerciseId.ALL, options)
            assertEquals(ExerciseId.STATUS_NUMBERS, plan.exercises[2])
        }

    @Test
    fun `the plan avoids the positions of the previous day`() =
        runTest {
            val source = DailyPlanSource(FakeHistoryRepository())
            (0L until 60L).forEach { offset ->
                val day = today + offset
                val yesterday = DailySessionPlanner.plan(day - 1, PracticeStats(emptyMap()), ExerciseId.ALL, options)
                val plan = source.planFor(day, ExerciseId.ALL, options)
                plan.exercises.indices.forEach { assertNotEquals(yesterday.exercises[it], plan.exercises[it]) }
            }
        }
}
