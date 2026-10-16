package io.github.qnicoud.dmgym.core.tables

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Common envelope of every table file (ADR 0009). */
@Serializable
data class TableFile(
    val schemaVersion: Int,
    val table: String,
    val locale: String,
    val entries: List<TableEntry>,
) {
    companion object {
        /** Strict decoding: an unknown key or a missing required field is an error. */
        private val strictJson =
            Json {
                ignoreUnknownKeys = false
                isLenient = false
            }

        fun decode(text: String): TableFile = strictJson.decodeFromString(serializer(), text)
    }
}
