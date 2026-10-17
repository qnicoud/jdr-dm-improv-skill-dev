package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.tables.TableRepository

/**
 * Complète à la lecture la catégorie des tirages enregistrés avant qu'elle soit stockée (schéma v1) : elle se
 * déduit de l'entrée de la table, grâce à `tableId` et `entryId`. Un tirage déjà catégorisé, sans table, dont
 * l'entrée a disparu ou dont la table est illisible reste tel quel.
 */
suspend fun List<Draw>.withCategories(tables: TableRepository): List<Draw> {
    val missing = filter { it.category == null && it.tableId != null }
    if (missing.isEmpty()) return this
    val byTable =
        missing
            .mapNotNull { draw -> TableId.entries.firstOrNull { it.fileName == draw.tableId } }
            .distinct()
            .associateWith { id -> categoriesOf(tables, id) }
    return map { draw ->
        if (draw.category != null || draw.tableId == null) {
            draw
        } else {
            val table = TableId.entries.firstOrNull { it.fileName == draw.tableId }
            draw.copy(category = table?.let { byTable[it] }?.get(draw.entryId))
        }
    }
}

private suspend fun categoriesOf(
    tables: TableRepository,
    id: TableId,
): Map<String, String?> =
    try {
        tables.table(id).entries.associate { it.id to it.category }
    } catch (_: TableLoadException) {
        emptyMap()
    }
