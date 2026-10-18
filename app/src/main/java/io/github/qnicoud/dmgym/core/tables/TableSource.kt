package io.github.qnicoud.dmgym.core.tables

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage

/** Port: where table files come from. */
interface TableSource {
    /** @throws TableLoadException if the table is missing or invalid in [language]. */
    suspend fun load(
        id: TableId,
        language: ContentLanguage,
    ): TableFile
}

/** Raised when a table cannot be read or decoded. */
class TableLoadException(
    val id: TableId,
    cause: Throwable,
) : Exception("Cannot load table ${id.fileName}", cause)
