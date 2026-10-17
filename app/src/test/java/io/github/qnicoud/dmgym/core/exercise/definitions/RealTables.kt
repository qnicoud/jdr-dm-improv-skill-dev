package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.tables.TableFile
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.tables.TableRepository
import io.github.qnicoud.dmgym.core.tables.TableSource
import io.github.qnicoud.dmgym.core.tables.TestTables

/** Les vraies tables livrées dans les assets, lues depuis le disque pour tester les définitions de bout en bout. */
object RealTables {
    private val source =
        object : TableSource {
            override suspend fun load(id: TableId): TableFile = TableFile.decode(TestTables.assetFile(id).readText())
        }

    fun repository() = TableRepository(source)

    fun file(id: TableId): TableFile = TableFile.decode(TestTables.assetFile(id).readText())
}
