package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableLoadException
import io.github.qnicoud.dmgym.core.tables.TableRepository

/**
 * Complète à la lecture la catégorie des tirages enregistrés avant qu'elle soit stockée (schéma v1) : elle se
 * déduit de l'entrée de la table, grâce à `tableId` et `entryId`. Un tirage déjà catégorisé, sans table, dont
 * l'entrée a disparu ou dont la table est illisible reste tel quel.
 */
suspend fun List<Draw>.withCategories(tables: TableRepository): List<Draw> =
    fillCategories(tables, { it.tableId }, { it.entryId }, { it.category }) { draw, category ->
        draw.copy(category = category)
    }

/** Même complétion pour les tirages d'une session archivée (détail de session). */
suspend fun List<DrawRecord>.withRecordCategories(tables: TableRepository): List<DrawRecord> =
    fillCategories(tables, { it.tableId }, { it.entryId }, { it.category }) { draw, category ->
        draw.copy(category = category)
    }

private suspend fun <T> List<T>.fillCategories(
    tables: TableRepository,
    tableOf: (T) -> String?,
    entryOf: (T) -> String,
    categoryOf: (T) -> String?,
    withCategory: (T, String?) -> T,
): List<T> {
    val missing = filter { categoryOf(it) == null && tableOf(it) != null }
    if (missing.isEmpty()) return this
    val byTable =
        missing
            .mapNotNull { item -> TableId.entries.firstOrNull { it.fileName == tableOf(item) } }
            .distinct()
            .associateWith { id -> categoriesOf(tables, id) }
    return map { item ->
        val fileName = tableOf(item)
        if (categoryOf(item) != null || fileName == null) {
            item
        } else {
            val table = TableId.entries.firstOrNull { it.fileName == fileName }
            withCategory(item, table?.let { byTable[it] }?.get(entryOf(item)))
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
