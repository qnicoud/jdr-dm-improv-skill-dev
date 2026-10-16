package io.github.qnicoud.dmgym.core.tables

import kotlinx.serialization.Serializable

/** One drawable entry of a table (ADR 0009). [id] is stable and never renamed. */
@Serializable
data class TableEntry(
    val id: String,
    val text: String,
    val difficulty: Int,
    val category: String? = null,
    val keywords: List<String> = emptyList(),
    val fuzzy: Boolean = false,
    val tags: List<String> = emptyList(),
)
