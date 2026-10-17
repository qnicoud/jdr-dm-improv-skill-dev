package io.github.qnicoud.dmgym.core.daily

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DailyReplacementsTest {
    private val plan =
        DailyPlan(
            epochDay = 100,
            exercises = listOf(ExerciseId.DECISION_POINT, ExerciseId.CRAZY_EIGHTS, ExerciseId.CORE_IMAGE),
        )

    @Test
    fun `replacements survive an encode and decode round trip`() {
        val map = mapOf(0 to ExerciseId.STATUS_NUMBERS, 2 to ExerciseId.CONNECT_STORY)
        assertEquals("0:status_numbers;2:connect_story", encodeReplacements(map))
        assertEquals(map, decodeReplacements(encodeReplacements(map)))
    }

    @Test
    fun `decoding ignores malformed entries and out of range positions`() {
        assertEquals(emptyMap<Int, ExerciseId>(), decodeReplacements(""))
        assertEquals(emptyMap<Int, ExerciseId>(), decodeReplacements(null))
        val decoded = decodeReplacements("x:crazy_eights;1:;;3:core_image;-1:core_image;1:one_page;nonsense;2")
        assertEquals(mapOf(1 to ExerciseId.ONE_PAGE), decoded)
    }

    @Test
    fun `replacements of another day are ignored`() {
        val settings = Settings(dailyEpochDay = 99, dailyReplacements = mapOf(0 to ExerciseId.CONNECT_STORY))
        assertEquals(emptyMap<Int, ExerciseId>(), settings.replacementsFor(100))
        assertEquals(mapOf(0 to ExerciseId.CONNECT_STORY), settings.replacementsFor(99))
    }

    @Test
    fun `a replacement stores the day`() {
        val updated = Settings().withReplacement(100, 1, ExerciseId.STATUS_NUMBERS)
        assertEquals(100L, updated.dailyEpochDay)
        assertEquals(mapOf(1 to ExerciseId.STATUS_NUMBERS), updated.replacementsFor(100))
    }

    @Test
    fun `a replacement on a new day clears the previous ones`() {
        val yesterday = Settings().withReplacement(99, 0, ExerciseId.CONNECT_STORY)
        val today = yesterday.withReplacement(100, 2, ExerciseId.STATUS_NUMBERS)
        assertEquals(mapOf(2 to ExerciseId.STATUS_NUMBERS), today.replacementsFor(100))
    }

    @Test
    fun `replacing again the same position overwrites it`() {
        val s =
            Settings()
                .withReplacement(
                    100,
                    1,
                    ExerciseId.STATUS_NUMBERS,
                ).withReplacement(100, 1, ExerciseId.CONNECT_STORY)
        assertEquals(mapOf(1 to ExerciseId.CONNECT_STORY), s.replacementsFor(100))
    }

    @Test
    fun `replacements are applied to the plan`() {
        val applied = plan.withReplacements(mapOf(0 to ExerciseId.STATUS_NUMBERS, 2 to ExerciseId.ONE_PAGE))
        assertEquals(listOf(ExerciseId.STATUS_NUMBERS, ExerciseId.CRAZY_EIGHTS, ExerciseId.ONE_PAGE), applied.exercises)
        assertEquals(100L, applied.epochDay)
    }

    @Test
    fun `a replacement that duplicates another slot or is out of range is ignored`() {
        val applied = plan.withReplacements(mapOf(0 to ExerciseId.CRAZY_EIGHTS, 5 to ExerciseId.ONE_PAGE))
        assertEquals(plan, applied)
    }

    @Test
    fun `swapping two slots through replacements keeps exercises distinct`() {
        val applied = plan.withReplacements(mapOf(0 to ExerciseId.CRAZY_EIGHTS, 1 to ExerciseId.DECISION_POINT))
        assertEquals(3, applied.exercises.toSet().size)
    }

    @Test
    fun `the order of the replacements does not matter`() {
        val abc = DailyPlan(1, listOf(ExerciseId.CRAZY_EIGHTS, ExerciseId.CORE_IMAGE, ExerciseId.CONNECT_STORY))
        val applied = abc.withReplacements(mapOf(1 to ExerciseId.STATUS_NUMBERS, 0 to ExerciseId.CORE_IMAGE))
        assertEquals(
            listOf(ExerciseId.CORE_IMAGE, ExerciseId.STATUS_NUMBERS, ExerciseId.CONNECT_STORY),
            applied.exercises,
        )
    }

    @Test
    fun `conflicting replacements are dropped so the plan stays distinct`() {
        val applied = plan.withReplacements(mapOf(0 to ExerciseId.STATUS_NUMBERS, 1 to ExerciseId.STATUS_NUMBERS))
        assertEquals(3, applied.exercises.toSet().size)
    }

    @Test
    fun `no replacement means no change`() {
        assertEquals(plan, plan.withReplacements(emptyMap()))
        assertNull(Settings().dailyEpochDay)
    }
}
