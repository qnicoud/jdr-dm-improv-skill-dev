package io.github.qnicoud.dmgym.core.tables

/** Port: where table files come from. */
interface TableSource {
    /** @throws TableLoadException if the table is missing or invalid. */
    suspend fun load(id: TableId): TableFile
}

/** Raised when a table cannot be read or decoded. */
class TableLoadException(
    val id: TableId,
    cause: Throwable,
) : Exception("Cannot load table ${id.fileName}", cause)
