package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.EventPlan
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.exercise.contrastBand
import io.github.qnicoud.dmgym.core.exercise.defaultDuration
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimerEngine
import io.github.qnicoud.dmgym.feature.exercise.timerConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Définitions de Core Image, Status Numbers, Decision Point et One-Page (L10), jouées sur les vraies tables. */
class GuidedDefinitionsTest {
    private val all =
        listOf(CoreImage.definition, StatusNumbers.definition, DecisionPoint.definition, OnePage.definition)

    private fun drawer(seed: Int = 1) = SlotDrawer(RealTables.repository, Random(seed))

    @Test
    fun `the four definitions are registered with distinct ids`() {
        val ids = all.map { it.id }
        assertEquals(
            listOf(ExerciseId.CORE_IMAGE, ExerciseId.STATUS_NUMBERS, ExerciseId.DECISION_POINT, ExerciseId.ONE_PAGE),
            ids,
        )
        ids.forEach { assertNotNull(ExerciseRegistry.Default[it]) }
    }

    @Test
    fun `every definition has a two or three question rubric and a default duration`() {
        all.forEach {
            assertTrue(it.rubric.size in 2..3, it.id.value)
            assertEquals(ExerciseId.DEFAULT_DURATIONS[it.id], it.timer.defaultDuration, it.id.value)
        }
    }

    @Test
    fun `core image takes 10 minutes and asks 6 questions one by one in category order`() =
        runTest {
            val def = CoreImage.definition
            assertEquals(TimerPolicy.Required(10.minutes), def.timer)
            val layout = def.layout as FieldLayout.Sequential
            assertEquals(CoreImage.QUESTIONS_SLOT, layout.questionsSlot)
            val draws = drawer().drawAll(def.slots)
            assertEquals(1, draws.count { it.slotKey == CoreImage.IMAGE_SLOT })
            val questions = draws.filter { it.slotKey == CoreImage.QUESTIONS_SLOT }
            assertEquals(CoreImage.QUESTION_COUNT, questions.size)
            val table = RealTables.file(TableId.ThreadingQuestions).entries.associateBy { it.id }
            assertEquals(
                List(CoreImage.QUESTION_COUNT) { CoreImage.CATEGORIES[it % CoreImage.CATEGORIES.size] },
                questions.map { table.getValue(it.entryId).category },
            )
            assertTrue(def.acceptsFreeSubject)
        }

    @Test
    fun `status numbers draws a card and a role, 3 minutes, no field during play`() =
        runTest {
            val def = StatusNumbers.definition
            assertEquals(TimerPolicy.Required(3.minutes), def.timer)
            assertTrue(def.layout is FieldLayout.None)
            assertEquals(emptyList<String>(), def.fieldKeys)
            assertNotNull(def.endNote)
            val draws = drawer().drawAll(def.slots, mapOf(StatusNumbers.OPTION_COUNTER to false))
            assertEquals(listOf(StatusNumbers.ROLE_SLOT, StatusNumbers.CARD_SLOT), draws.map { it.slotKey })
            assertTrue(draws.last().text.toInt() in 1..10)
            assertEquals(
                listOf(StatusNumbers.OPTION_COUNTER, StatusNumbers.OPTION_SHIFT),
                def.options.map { it.key },
            )
            assertTrue(def.options.none { it.default })
        }

    @Test
    fun `counter stereotype inverts the rank tag of every real role`() =
        runTest {
            val roles = RealTables.file(TableId.NpcRoles).entries.associateBy { it.id }
            repeat(200) { seed ->
                val draws =
                    drawer(seed).drawAll(
                        StatusNumbers.definition.slots,
                        mapOf(StatusNumbers.OPTION_COUNTER to true),
                    )
                val role = roles.getValue(draws.first().entryId)
                val band = checkNotNull(contrastBand(role.tags)) { "role ${role.id} has no rank tag" }
                assertTrue(draws.last().text.toInt() in band, "${role.tags} -> ${draws.last().text}")
            }
        }

    @Test
    fun `the status shift happens once between 40 and 60 percent of the time`() {
        val event = StatusNumbers.definition.events.single()
        assertEquals(StatusNumbers.OPTION_SHIFT, event.requiresOption)
        val total = 3.minutes.inWholeMilliseconds
        repeat(100) { seed ->
            val plan = EventPlan(TimerEngine(), listOf(event), total, Random(seed))
            val fireAt = (STEP_MS..total step STEP_MS).first { plan.due(it - STEP_MS, it).isNotEmpty() }
            assertTrue(fireAt in (total * 40 / 100)..(total * 60 / 100 + STEP_MS), "seed $seed fired at $fireAt")
        }
    }

    @Test
    fun `the shift draws a real situation`() =
        runTest {
            val draw =
                StatusNumbers.definition.events
                    .single()
                    .draw
            val drawn = drawer().draw(checkNotNull(draw)).single()
            assertEquals(StatusNumbers.SHIFT_SLOT, drawn.slotKey)
            assertTrue(drawn.text.isNotBlank())
        }

    @Test
    fun `only the decision point video mode has a pause-done start`() {
        val def = DecisionPoint.definition
        assertTrue(def.startVariantFor(emptyMap()) != null)
        assertEquals(null, def.startVariantFor(mapOf(DecisionPoint.OPTION_SITUATION to true)))
        assertTrue(def.endPrompt != null && def.endFlagHint != null)
        listOf(
            StatusNumbers.definition,
            CoreImage.definition,
        ).forEach { assertEquals(null, it.startVariantFor(emptyMap())) }
    }

    @Test
    fun `decision point is 30 seconds, 3 fields with a never flag, video mode by default`() =
        runTest {
            val def = DecisionPoint.definition
            assertEquals(TimerPolicy.Required(30.seconds), def.timer)
            val layout = def.layout as FieldLayout.Fixed
            assertEquals(3, layout.fields.size)
            assertTrue(layout.fields.all { it.flagLabel != null })
            assertEquals(emptyList<Any>(), drawer().drawAll(def.slots, mapOf(DecisionPoint.OPTION_SITUATION to false)))
            val table = drawer().drawAll(def.slots, mapOf(DecisionPoint.OPTION_SITUATION to true))
            assertEquals(listOf(DecisionPoint.SITUATION_SLOT), table.map { it.slotKey })
        }

    @Test
    fun `one page has no timer by default, 15 minutes suggested, at most 5 npcs`() =
        runTest {
            val def = OnePage.definition
            assertEquals(TimerPolicy.Optional(15.minutes), def.timer)
            assertTrue(ExerciseId.ONE_PAGE in ExerciseId.UNTIMED_BY_DEFAULT)
            val (durationMs, untimed) = timerConfig(def, Settings(untimed = ExerciseId.UNTIMED_BY_DEFAULT))
            assertEquals(15.minutes.inWholeMilliseconds, durationMs)
            assertTrue(untimed)
            assertFalse(timerConfig(CoreImage.definition, Settings(untimed = ExerciseId.UNTIMED_BY_DEFAULT)).second)
            val layout = def.layout as FieldLayout.RepeatableGroup
            assertEquals(OnePage.MAX_NPCS, layout.max)
            assertEquals(
                OnePage.MOTIVATION_SOFT_LIMIT,
                layout.fields.first { it.key == OnePage.MOTIVATION_FIELD }.softLimit,
            )
            assertEquals(listOf(OnePage.INACTION_FIELD), layout.after.map { it.key })
            assertFalse(def.warmUp)
        }

    @Test
    fun `one page without campaign draws a situation, with a campaign it asks for the context`() =
        runTest {
            val def = OnePage.definition
            assertEquals(emptyList<Any>(), drawer().drawAll(def.slots, mapOf(OnePage.OPTION_DRAWN to false)))
            assertEquals(1, drawer().drawAll(def.slots, mapOf(OnePage.OPTION_DRAWN to true)).size)
            val context = (def.layout as FieldLayout.RepeatableGroup).before.single()
            assertTrue(context.visibleWhen!!.holds(mapOf(OnePage.OPTION_DRAWN to false)))
            assertFalse(context.visibleWhen!!.holds(mapOf(OnePage.OPTION_DRAWN to true)))
        }

    @Test
    fun `every field key is unique within a definition`() {
        all.forEach { def: ExerciseDefinition ->
            assertEquals(def.fieldKeys.distinct(), def.fieldKeys, def.id.value)
        }
    }

    private companion object {
        const val STEP_MS = 500L
    }
}
