package io.github.qnicoud.dmgym.core.tables

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Loads each table on demand, once per language, and keeps it in memory. Failures are not cached, and a missing
 * table is never completed from another language (ADR 0016 §2).
 */
class TableRepository(
    private val source: TableSource,
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<Pair<ContentLanguage, TableId>, TableFile>()

    suspend fun table(
        id: TableId,
        language: ContentLanguage = ContentLanguage.DEFAULT,
    ): TableFile =
        mutex.withLock {
            cache.getOrPut(language to id) { source.load(id, language) }
        }
}
