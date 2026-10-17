package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.Drawer
import io.github.qnicoud.dmgym.core.tables.NotEnoughEntriesException
import io.github.qnicoud.dmgym.core.tables.TableEntry
import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableRepository
import kotlin.random.Random

/** Ce qui est déjà tiré et les options en vigueur : la carte contrastée en dépend. */
data class DrawContext(
    val draws: List<Draw> = emptyList(),
    val options: Map<String, Boolean> = emptyMap(),
)

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

    /** Tire les slots actifs pour [options], dans l'ordre ; un slot voit les tirages qui le précèdent. */
    suspend fun drawAll(
        slots: List<DrawSlot>,
        options: Map<String, Boolean> = emptyMap(),
    ): List<Draw> =
        slots
            .activeFor(
                options,
            ).fold(emptyList()) { acc, slot -> acc + draw(slot, context = DrawContext(acc, options)) }

    /**
     * Tire [slots] à neuf pour une session dont les tirages sont [current] : chaque slot voit les tirages courants,
     * remplacés par ceux déjà refaits. [exclude] donne, par slot, les entrées à éviter.
     */
    suspend fun redraw(
        slots: List<DrawSlot>,
        current: List<Draw>,
        options: Map<String, Boolean>,
        exclude: Map<String, Set<String>> = emptyMap(),
    ): List<Draw> =
        slots.fold(emptyList()) { fresh, slot ->
            val context = current.filter { d -> fresh.none { it.slotKey == d.slotKey } } + fresh
            fresh + draw(slot, exclude[slot.key].orEmpty(), DrawContext(context, options))
        }

    /** Tire [slot] ; les entrées de [exclude] ne reviennent que s'il n'y a pas d'autre choix. */
    suspend fun draw(
        slot: DrawSlot,
        exclude: Set<String> = emptySet(),
        context: DrawContext = DrawContext(),
    ): List<Draw> =
        when (slot) {
            is TableSlot -> drawTable(slot, exclude, context)
            is CardSlot -> listOf(drawCard(slot, exclude, context))
            is ChoiceSlot -> listOf(Draw(slot.key, 0, null, slot.default, slot.default))
        }

    private suspend fun drawTable(
        slot: TableSlot,
        exclude: Set<String>,
        context: DrawContext,
    ): List<Draw> {
        val table = tables.table(slot.table)
        val entries =
            if (slot.categoryCycle.isEmpty()) {
                drawPreferringTag(slot, table, exclude, context)
            } else {
                (0 until slot.count).fold(emptyList()) { picked, i ->
                    val category = slot.categoryCycle[i % slot.categoryCycle.size]
                    val taken = exclude + picked.map { it.id }
                    picked + drawer.draw(table, 1, category, slot.difficulty, slot.distinctBy, taken)
                }
            }
        return entries.mapIndexed { index, entry ->
            Draw(slot.key, index, slot.table.fileName, entry.id, entry.text, category = entry.category)
        }
    }

    /** Tire en privilégiant l'étiquette du statut de la carte ; sans assez d'entrées étiquetées, tire sans filtre. */
    private fun drawPreferringTag(
        slot: TableSlot,
        table: TableFile,
        exclude: Set<String>,
        context: DrawContext,
    ): List<TableEntry> {
        val tag = slot.tagByStatus?.let { preferredTag(it, context) }
        if (tag != null) {
            try {
                return drawer.draw(
                    table,
                    slot.count,
                    slot.category,
                    slot.difficulty,
                    slot.distinctBy,
                    exclude,
                    tagFilter = { tag in it },
                )
            } catch (_: NotEnoughEntriesException) {
                // Pas assez d'entrées dans ce sens : repli sur la table entière.
            }
        }
        return drawer.draw(table, slot.count, slot.category, slot.difficulty, slot.distinctBy, exclude)
    }

    private fun preferredTag(
        rule: StatusTagRule,
        context: DrawContext,
    ): String? {
        val cardDraw = context.draws.firstOrNull { it.slotKey == rule.cardSlot }
        val card = cardDraw?.text?.toIntOrNull() ?: return null
        return when (statusLevel(card)) {
            StatusLevel.High -> rule.high
            StatusLevel.Low -> rule.low
            StatusLevel.Medium -> null
        }
    }

    private suspend fun drawCard(
        slot: CardSlot,
        exclude: Set<String>,
        context: DrawContext,
    ): Draw {
        val band = contrastBandOf(slot, context)
        val values = slot.range.filter { band == null || it in band }.ifEmpty { slot.range.toList() }
        val fresh = values.filter { it.toString() !in exclude }.ifEmpty { values }
        val value = fresh[random.nextInt(fresh.size)].toString()
        return Draw(slot.key, 0, null, value, value)
    }

    /** Bande de cartes imposée par le contre-stéréotype, ou `null` si l'option est inactive ou le rôle sans rang. */
    private suspend fun contrastBandOf(
        slot: CardSlot,
        context: DrawContext,
    ): IntRange? {
        val contrast = slot.contrast?.takeIf { context.options[it.option] == true } ?: return null
        val role = context.draws.firstOrNull { it.slotKey == contrast.roleSlot } ?: return null
        val table = TableId.entries.firstOrNull { it.fileName == role.tableId } ?: return null
        val tags =
            tables
                .table(table)
                .entries
                .firstOrNull { it.id == role.entryId }
                ?.tags
                .orEmpty()
        return contrastBand(tags)
    }
}
