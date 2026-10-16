package io.github.qnicoud.dmgym.core.tables

import kotlin.random.Random

/** Whether drawn entries must differ by id only, or also by category. */
enum class DistinctBy { Id, Category }

/**
 * Raised when the table cannot provide [requested] entries even after relaxing `exclude` and
 * `difficulty` ([available] is the best count reached).
 *
 * For the exercise engine (L5): this signals a content or configuration error (for example a
 * slot asking for more distinct categories than exist), not a user error. The ViewModel must
 * catch it and show an error state for the slot instead of crashing.
 */
class NotEnoughEntriesException(
    val requested: Int,
    val available: Int,
) : IllegalStateException("Requested $requested entries but only $available are drawable")

/**
 * Pure, seedable draw without replacement (architecture §6).
 *
 * Not thread-safe: [Random] is stateful. Use one `Random` instance (hence one `Drawer`) per
 * ViewModel.
 */
class Drawer(
    private val random: Random,
) {
    /**
     * Draws [count] entries. Category and tag filters are hard constraints; [exclude] is a
     * preference (non-excluded entries first), lifted before [difficulty] is relaxed.
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
        val tiers = listOf(base.filter { it.difficulty in difficulty }, base)
        var available = 0
        for (tier in tiers) {
            val picked = pick(tier, count, distinctBy, exclude)
            if (picked.size == count) return picked
            available = maxOf(available, picked.size)
        }
        throw NotEnoughEntriesException(count, available)
    }

    /** Non-excluded entries first, completed with excluded ones only if too few remain. */
    private fun pick(
        candidates: List<TableEntry>,
        count: Int,
        distinctBy: DistinctBy,
        exclude: Set<String>,
    ): List<TableEntry> {
        val (excluded, preferred) = candidates.partition { it.id in exclude }
        val ordered = preferred.shuffled(random) + excluded.shuffled(random)
        val unique =
            when (distinctBy) {
                DistinctBy.Id -> ordered

                // An entry without category is never merged with another one.
                DistinctBy.Category -> ordered.distinctBy { it.category ?: "\u0000${it.id}" }
            }
        return unique.take(count)
    }
}
