package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.tables.TableSource
import io.github.qnicoud.dmgym.core.tables.TestTables
import kotlin.random.Random

/** Tables livrées dans les assets, lues depuis le disque (pas de fixture). */
object RealTables {
    private val source =
        object : TableSource {
            override suspend fun load(id: TableId): TableFile = TableFile.decode(TestTables.assetFile(id).readText())
        }

    val repository = TableRepository(source)

    fun drawer(seed: Int) = SlotDrawer(repository, Random(seed))

    suspend fun entry(
        table: TableId,
        entryId: String,
    ) = repository.table(table).entries.first { it.id == entryId }

    suspend fun drawAll(
        definition: ExerciseDefinition,
        seed: Int,
    ) = drawer(seed).drawAll(definition.slots)
}
