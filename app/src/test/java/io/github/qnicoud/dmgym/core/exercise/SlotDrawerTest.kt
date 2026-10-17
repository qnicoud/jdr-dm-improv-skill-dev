package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.DistinctBy
import io.github.qnicoud.dmgym.core.tables.NotEnoughEntriesException
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SlotDrawerTest {
    private val drawer = SlotDrawer(DemoExercise.tables(), Random(42))

    @Test
    fun `draws a table slot with numbered positions`() =
        runTest {
            val draws = drawer.draw(TableSlot("w", TableId.Places, count = 3, distinctBy = DistinctBy.Category))
            assertEquals(listOf(0, 1, 2), draws.map { it.position })
            assertEquals(3, draws.map { it.entryId }.distinct().size)
            assertTrue(draws.all { it.slotKey == "w" && it.tableId == "places" })
        }

    @Test
    fun `reroll avoids the excluded entries when possible`() =
        runTest {
            repeat(20) {
                val draw =
                    drawer
                        .draw(
                            TableSlot("w", TableId.Places),
                            exclude = setOf("cave", "pont", "tour", "gue", "mine"),
                        ).single()
                assertEquals("puits", draw.entryId)
            }
        }

    @Test
    fun `a card is drawn in range and avoids the excluded value`() =
        runTest {
            repeat(20) {
                val card = drawer.draw(CardSlot("card", 1..3), exclude = setOf("1", "2")).single()
                assertEquals("3", card.entryId)
                assertEquals(null, card.tableId)
            }
            assertEquals("1", drawer.draw(CardSlot("one", 1..1), exclude = setOf("1")).single().entryId)
        }

    @Test
    fun `a choice slot resolves to its default`() =
        runTest {
            val draw = drawer.draw(ChoiceSlot("domain", listOf("lieu", "cour"))).single()
            assertEquals("lieu", draw.text)
        }

    @Test
    fun `draw all keeps the slot order`() =
        runTest {
            val draws = drawer.drawAll(listOf(CardSlot("card"), TableSlot("w", TableId.Places)))
            assertEquals(listOf("card", "w"), draws.map { it.slotKey })
        }

    @Test
    fun `errors reach the caller`() =
        runTest {
            assertThrows(NotEnoughEntriesException::class.java) {
                kotlinx.coroutines.runBlocking { drawer.draw(TableSlot("w", TableId.Places, count = 50)) }
            }
            val broken = SlotDrawer(DemoExercise.tables(DemoExercise.FixtureSource(failing = true)), Random(1))
            assertThrows(TableLoadException::class.java) {
                kotlinx.coroutines.runBlocking { broken.draw(TableSlot("w", TableId.Places)) }
            }
        }
}
