package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.TableEntry
import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.tables.TableSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/** Tirages conditionnés par les options : contre-stéréotype (D45), modes, questions par catégorie. */
class SlotConditionsTest {
    private val roleHigh = TableEntry("roi", "un roi", 1, tags = listOf("rank:high"))
    private val roleLow = TableEntry("valet", "un valet", 1, tags = listOf("rank:low"))
    private val roleNone = TableEntry("passant", "un passant", 1)

    private fun drawer(vararg roles: TableEntry): SlotDrawer {
        val source =
            object : TableSource {
                override suspend fun load(id: TableId): TableFile =
                    TableFile(
                        1,
                        id.fileName,
                        "fr",
                        if (id == TableId.NpcRoles) roles.toList() else questions(),
                    )
            }
        return SlotDrawer(TableRepository(source), Random(5))
    }

    private fun questions() =
        listOf("what", "origin", "who", "link").flatMap { c ->
            (1..3).map { TableEntry("$c-$it", "q $c $it", 1, category = c) }
        }

    private val slots =
        listOf(
            TableSlot("role", TableId.NpcRoles),
            CardSlot("card", contrast = RankContrast("role", "counter")),
        )

    @Test
    fun `band of a rank is the opposite one`() {
        assertEquals(1..3, contrastBand(listOf("rank:high")))
        assertEquals(8..10, contrastBand(listOf("rank:low")))
        assertEquals(null, contrastBand(listOf("shift:up")))
        assertEquals(StatusLevel.Low, statusLevel(3))
        assertEquals(StatusLevel.Medium, statusLevel(5))
        assertEquals(StatusLevel.High, statusLevel(8))
    }

    @Test
    fun `counter stereotype gives a low card to a high rank role`() =
        runTest {
            val drawer = drawer(roleHigh)
            repeat(30) {
                val draws = drawer.drawAll(slots, mapOf("counter" to true))
                assertTrue(draws.first { it.slotKey == "card" }.entryId.toInt() in 1..3)
            }
        }

    @Test
    fun `counter stereotype gives a high card to a low rank role`() =
        runTest {
            val drawer = drawer(roleLow)
            repeat(30) {
                val draws = drawer.drawAll(slots, mapOf("counter" to true))
                assertTrue(draws.first { it.slotKey == "card" }.entryId.toInt() in 8..10)
            }
        }

    @Test
    fun `counter stereotype leaves the card free without rank tag or when off`() =
        runTest {
            val seen = mutableSetOf<Int>()
            val noRank = drawer(roleNone)
            val off = drawer(roleHigh)
            repeat(60) {
                seen +=
                    noRank
                        .drawAll(slots, mapOf("counter" to true))
                        .last()
                        .entryId
                        .toInt()
                seen +=
                    off
                        .drawAll(slots, mapOf("counter" to false))
                        .last()
                        .entryId
                        .toInt()
            }
            assertTrue(seen.any { it in 4..7 })
            assertTrue(seen.any { it >= 8 })
        }

    @Test
    fun `a rerolled card keeps following the current role`() =
        runTest {
            val role = Draw("role", 0, TableId.NpcRoles.fileName, "roi", "un roi")
            val card =
                drawer(roleHigh)
                    .draw(slots[1], exclude = setOf("2"), context = DrawContext(listOf(role), mapOf("counter" to true)))
            assertTrue(card.single().entryId.toInt() in setOf(1, 3))
        }

    @Test
    fun `slots with an unmet condition are not drawn`() =
        runTest {
            val conditional =
                listOf(
                    TableSlot("situation", TableId.NpcRoles, onlyIf = OptionCondition("table")),
                    CardSlot("c"),
                )
            assertEquals(listOf("c"), drawer(roleHigh).drawAll(conditional, mapOf("table" to false)).map { it.slotKey })
            assertEquals(
                listOf("situation", "c"),
                drawer(roleHigh).drawAll(conditional, mapOf("table" to true)).map { it.slotKey },
            )
            assertEquals(listOf("c"), conditional.activeFor(emptyMap()).map { it.key })
        }

    @Test
    fun `questions follow the category cycle without repeats`() =
        runTest {
            val cycle = listOf("what", "origin", "who", "link")
            val draws = drawer().draw(TableSlot("q", TableId.ThreadingQuestions, count = 6, categoryCycle = cycle))
            assertEquals(listOf(0, 1, 2, 3, 4, 5), draws.map { it.position })
            assertEquals(
                listOf("what", "origin", "who", "link", "what", "origin"),
                draws.map { it.text.split(" ")[1] },
            )
            assertEquals(6, draws.map { it.entryId }.distinct().size)
        }

    @Test
    fun `a missing option falls back to the default of the definition`() {
        val condition = OptionCondition("on")
        assertEquals(false, condition.holds(emptyMap()))
        assertEquals(true, condition.holds(emptyMap(), mapOf("on" to true)))
        assertEquals(false, condition.holds(mapOf("on" to false), mapOf("on" to true)))
        val def =
            DemoExercise.definition.copy(
                options = listOf(ExerciseOption("on", 1, default = true)),
                slots = listOf(TableSlot("s", TableId.Places, onlyIf = condition)),
            )
        assertEquals(mapOf("on" to true), def.resolveOptions(emptyMap()))
        assertEquals(mapOf("on" to false), def.resolveOptions(mapOf("on" to false)))
        assertEquals(listOf("s"), def.slots.activeFor(def.resolveOptions(emptyMap())).map { it.key })
    }
}
