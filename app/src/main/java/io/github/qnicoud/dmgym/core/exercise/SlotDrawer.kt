package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.Drawer
import io.github.qnicoud.dmgym.core.tables.TableRepository
import kotlin.random.Random

/**
 * Tire les éléments d'un slot. Une seule instance par ViewModel : [Random] et [Drawer] ne sont pas thread-safe.
 *
 * @throws io.github.qnicoud.dmgym.core.tables.NotEnoughEntriesException table trop pauvre pour le slot.
 * @throws io.github.qnicoud.dmgym.core.tables.TableLoadException table illisible.
 */
class SlotDrawer(
    private val tables: TableRepository,
    private val random: Random,
) {
    private val drawer = Drawer(random)

    /** Tire tous les slots, dans l'ordre. */
    suspend fun drawAll(slots: List<DrawSlot>): List<Draw> = slots.flatMap { draw(it) }

    /** Tire [slot] ; les entrées de [exclude] ne reviennent que s'il n'y a pas d'autre choix. */
    suspend fun draw(
        slot: DrawSlot,
        exclude: Set<String> = emptySet(),
    ): List<Draw> =
        when (slot) {
            is TableSlot -> drawTable(slot, exclude)
            is CardSlot -> listOf(drawCard(slot, exclude))
            is ChoiceSlot -> listOf(Draw(slot.key, 0, null, slot.default, slot.default))
        }

    private suspend fun drawTable(
        slot: TableSlot,
        exclude: Set<String>,
    ): List<Draw> {
        val entries =
            drawer.draw(
                table = tables.table(slot.table),
                count = slot.count,
                category = slot.category,
                difficulty = slot.difficulty,
                distinctBy = slot.distinctBy,
                exclude = exclude,
            )
        return entries.mapIndexed { index, e -> Draw(slot.key, index, slot.table.fileName, e.id, e.text) }
    }

    private fun drawCard(
        slot: CardSlot,
        exclude: Set<String>,
    ): Draw {
        val values = slot.range.toList()
        val fresh = values.filter { it.toString() !in exclude }.ifEmpty { values }
        val value = fresh[random.nextInt(fresh.size)].toString()
        return Draw(slot.key, 0, null, value, value)
    }
}
