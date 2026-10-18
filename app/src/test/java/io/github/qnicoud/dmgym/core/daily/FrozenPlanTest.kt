package io.github.qnicoud.dmgym.core.daily

import app.cash.turbine.test
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.Settings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Séance du jour figée (ADR 0017 §4, D69). */
class FrozenPlanTest {
    private val day = 20_500L
    private val plan = DailyPlan(day, listOf(ExerciseId.DECISION_POINT, ExerciseId.CRAZY_EIGHTS, ExerciseId.CORE_IMAGE))

    @Test
    fun `the plan survives an encode and decode round trip`() {
        val encoded = encodePlan(plan)
        assertEquals("20500:decision_point,crazy_eights,core_image", encoded)
        assertEquals(plan, decodePlan(encoded))
    }

    @Test
    fun `a corrupted value decodes to nothing`() {
        listOf(null, "", "x", "abc:a,b,c", "1:a,b", "1:a,a,b", "1:a,,b", "1:a,b,c,d", ":a,b,c").forEach {
            assertNull(decodePlan(it), "value '$it'")
        }
    }

    @Test
    fun `a frozen plan is reused only for its day and a known catalog`() {
        val settings = Settings(frozenPlan = plan)
        assertEquals(plan, settings.frozenPlanFor(day, ExerciseId.ALL))
        assertNull(settings.frozenPlanFor(day + 1, ExerciseId.ALL))
        assertNull(settings.frozenPlanFor(day, ExerciseId.ALL - ExerciseId.CORE_IMAGE))
    }

    private fun resolver(
        settings: FakeSettingsRepository,
        catalog: List<ExerciseId>,
    ) = DailyPlanResolver(settings, DailyPlanSource(FakeHistoryRepository()), catalog = { catalog })

    private suspend fun resolve(
        resolver: DailyPlanResolver,
        on: Long,
    ) = resolver.observe(flowOf(on)).let { flow ->
        var result: DailyPlan? = null
        flow.test {
            result = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        result!!
    }

    @Test
    fun `the same day keeps the same plan when the pool changes`() =
        runTest {
            val settings = FakeSettingsRepository(Settings())
            val first = resolve(resolver(settings, ExerciseId.ALL), day)
            val grown = ExerciseId.ALL + ExerciseId("failing_forward") + ExerciseId("yes_and")
            repeat(20) { assertEquals(first, resolve(resolver(settings, grown), day)) }
        }

    @Test
    fun `the next day the plan is recomputed from the current pool`() =
        runTest {
            val settings = FakeSettingsRepository(Settings())
            resolve(resolver(settings, ExerciseId.ALL), day)
            val next = resolve(resolver(settings, ExerciseId.ALL), day + 1)
            assertEquals(day + 1, next.epochDay)
            assertEquals(next, settings.state.value.frozenPlan)
        }

    @Test
    fun `a corrupted stored plan is recomputed`() =
        runTest {
            val settings =
                FakeSettingsRepository(
                    Settings(
                        frozenPlan =
                            DailyPlan(
                                day,
                                listOf(ExerciseId("gone"), ExerciseId("gone2"), ExerciseId("gone3")),
                            ),
                    ),
                )
            val plan = resolve(resolver(settings, ExerciseId.ALL), day)
            assertEquals(3, plan.exercises.toSet().size)
            assertEquals(true, plan.exercises.all { it in ExerciseId.ALL })
        }
}
