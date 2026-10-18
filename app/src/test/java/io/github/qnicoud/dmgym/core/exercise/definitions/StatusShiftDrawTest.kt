package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawContext
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.tables.TableSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/** La bascule de Status Numbers va dans le sens inverse du statut de la carte, pour que le changement ait un sens. */
class StatusShiftDrawTest {
    private val shiftSlot =
        checkNotNull(
            StatusNumbers.definition.events
                .single()
                .draw,
        )

    private fun context(card: Int?) =
        DrawContext(
            draws = card?.let { listOf(Draw(StatusNumbers.CARD_SLOT, 0, null, "$it", "$it")) }.orEmpty(),
            options = mapOf(StatusNumbers.OPTION_SHIFT to true),
        )

    private suspend fun tagsOfShift(
        card: Int?,
        seed: Int,
    ): List<String> {
        val drawn = RealTables.drawer(seed).draw(shiftSlot, context = context(card)).single()
        return RealTables.entry(TableId.StatusShifts, drawn.entryId).tags
    }

    @Test
    fun `a high status card draws a downward shift`() =
        runTest {
            listOf(8, 9, 10).forEach { card ->
                repeat(30) { seed -> assertTrue("shift:down" in tagsOfShift(card, seed), "card $card seed $seed") }
            }
        }

    @Test
    fun `a low status card draws an upward shift`() =
        runTest {
            listOf(1, 2, 3).forEach { card ->
                repeat(30) { seed -> assertTrue("shift:up" in tagsOfShift(card, seed), "card $card seed $seed") }
            }
        }

    @Test
    fun `a medium card or no card draws from both directions`() =
        runTest {
            listOf(5, null).forEach { card ->
                val tags = (0 until 60).flatMap { tagsOfShift(card, it) }.toSet()
                assertEquals(setOf("shift:up", "shift:down"), tags, "card $card")
            }
        }

    @Test
    fun `falls back to any shift when the table has none in the wanted direction`() =
        runTest {
            val onlyUp =
                """{"schemaVersion":1,"table":"status_shifts","locale":"fr","entries":[
                {"id":"a","text":"A","difficulty":1,"tags":["shift:up"]},
                {"id":"b","text":"B","difficulty":1,"tags":["shift:up"]}]}"""
            val source =
                object : TableSource {
                    override suspend fun load(
                        id: TableId,
                        language: ContentLanguage,
                    ): TableFile = TableFile.decode(onlyUp)
                }
            val drawer = SlotDrawer(TableRepository(source), Random(1))
            val drawn = drawer.draw(shiftSlot, context = context(card = 9)).single()
            assertTrue(drawn.entryId in setOf("a", "b"))
        }

    @Test
    fun `a slot without a shift rule ignores the card`() =
        runTest {
            val plain = TableSlot("shift", TableId.StatusShifts)
            val tags =
                (0 until 60)
                    .flatMap { seed ->
                        val id =
                            RealTables
                                .drawer(seed)
                                .draw(plain, context = context(9))
                                .single()
                                .entryId
                        RealTables.entry(TableId.StatusShifts, id).tags
                    }.toSet()
            assertEquals(setOf("shift:up", "shift:down"), tags)
        }
}
