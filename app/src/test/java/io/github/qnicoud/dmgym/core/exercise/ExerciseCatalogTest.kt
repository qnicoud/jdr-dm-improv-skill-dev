package io.github.qnicoud.dmgym.core.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Le catalogue de [ExerciseId] est dérivé du registre livré : ajouter un exercice ne touche qu'un seul endroit. */
class ExerciseCatalogTest {
    private val registry = ExerciseRegistry.Default

    @Test
    fun `ALL lists the registry in order, without duplicates`() {
        assertEquals(registry.all.map { it.id }, ExerciseId.ALL)
        assertEquals(ExerciseId.ALL.distinct(), ExerciseId.ALL)
    }

    @Test
    fun `the seven v1 exercises come first, in home order`() {
        assertEquals(
            listOf(
                ExerciseId.CRAZY_EIGHTS,
                ExerciseId.CONSTRAINT_BOX,
                ExerciseId.CORE_IMAGE,
                ExerciseId.STATUS_NUMBERS,
                ExerciseId.DECISION_POINT,
                ExerciseId.CONNECT_STORY,
                ExerciseId.ONE_PAGE,
            ),
            ExerciseId.ALL.take(7),
        )
    }

    @Test
    fun `default durations come from the definitions and keep the v1 values`() {
        assertEquals(ExerciseId.ALL.toSet(), ExerciseId.DEFAULT_DURATIONS.keys)
        registry.all.forEach {
            assertEquals(
                it.timer.defaultDuration,
                ExerciseId.DEFAULT_DURATIONS[it.id],
                it.id.value,
            )
        }
        val v1 =
            mapOf(
                ExerciseId.CRAZY_EIGHTS to 8.minutes,
                ExerciseId.CONSTRAINT_BOX to 5.minutes,
                ExerciseId.CORE_IMAGE to 10.minutes,
                ExerciseId.STATUS_NUMBERS to 3.minutes,
                ExerciseId.DECISION_POINT to 30.seconds,
                ExerciseId.CONNECT_STORY to 10.minutes,
                ExerciseId.ONE_PAGE to 15.minutes,
            )
        v1.forEach { (id, duration) -> assertEquals(duration, ExerciseId.DEFAULT_DURATIONS[id], id.value) }
    }

    @Test
    fun `warm-ups are the definitions flagged as warm-up`() {
        assertEquals(
            registry.all
                .filter { it.warmUp }
                .map { it.id }
                .toSet(),
            ExerciseId.DAILY_WARM_UPS,
        )
        assertTrue(
            ExerciseId.DAILY_WARM_UPS.containsAll(
                setOf(ExerciseId.DECISION_POINT, ExerciseId.CONSTRAINT_BOX, ExerciseId.STATUS_NUMBERS),
            ),
        )
        assertTrue(
            ExerciseId.DAILY_WARM_UPS
                .intersect(
                    setOf(
                        ExerciseId.CRAZY_EIGHTS,
                        ExerciseId.CORE_IMAGE,
                        ExerciseId.CONNECT_STORY,
                        ExerciseId.ONE_PAGE,
                    ),
                ).isEmpty(),
        )
    }
}
