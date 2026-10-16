package io.github.qnicoud.dmgym.core.tables

import kotlin.random.Random

/** Whether drawn entries must differ by id only, or also by category. */
enum class DistinctBy { Id, Category }

/** Raised when the table cannot provide the requested number of entries. */
class NotEnoughEntriesException(
    val requested: Int,
    val available: Int,
) : IllegalStateException("Requested $requested entries but only $available are drawable")

/** Pure, seedable draw without replacement (architecture §6). */
class Drawer(
    private val random: Random,
) {
    /**
     * Draws [count] entries. Category and tag filters are hard constraints; [exclude] then
     * [difficulty] are relaxed, in that order, when too few entries remain.
     *
     * @throws NotEnoughEntriesException if even the relaxed filters leave fewer than [count].
     */
    @Suppress("LongParameterList")
    fun draw(
        table: TableFile,
        count: Int,
        category: String? = null,
        difficulty: IntRange = 1..3,
        distinctBy: DistinctBy = DistinctBy.Id,
        exclude: Set<String> = emptySet(),
        tagFilter: (List<String>) -> Boolean = { true },
    ): List<TableEntry> {
        require(count >= 0) { "count must be >= 0" }
        val base = table.entries.filter { (category == null || it.category == category) && tagFilter(it.tags) }
        val stages =
            listOf(
                base.filter { it.difficulty in difficulty && it.id !in exclude },
                base.filter { it.difficulty in difficulty },
                base,
            )
        var available = 0
        for (stage in stages) {
            val picked = pick(stage, count, distinctBy)
            if (picked.size == count) return picked
            available = maxOf(available, picked.size)
        }
        throw NotEnoughEntriesException(count, available)
    }

    private fun pick(
        candidates: List<TableEntry>,
        count: Int,
        distinctBy: DistinctBy,
    ): List<TableEntry> {
        val shuffled = candidates.shuffled(random)
        val unique =
            when (distinctBy) {
                DistinctBy.Id -> shuffled
                DistinctBy.Category -> shuffled.distinctBy { it.category }
            }
        return unique.take(count)
    }
}
