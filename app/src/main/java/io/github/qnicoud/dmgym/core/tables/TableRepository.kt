package io.github.qnicoud.dmgym.core.tables

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Loads each table on demand, once, and keeps it in memory. Failures are not cached. */
class TableRepository(
    private val source: TableSource,
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<TableId, TableFile>()

    suspend fun table(id: TableId): TableFile =
        mutex.withLock {
            cache[id] ?: source.load(id).also { cache[id] = it }
        }
}
