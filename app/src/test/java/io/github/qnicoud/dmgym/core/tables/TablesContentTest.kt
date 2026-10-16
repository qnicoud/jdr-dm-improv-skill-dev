package io.github.qnicoud.dmgym.core.tables

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.text.Normalizer

/**
 * Content validation of the shipped tables (ADR 0009). A missing table is skipped until L12;
 * a present table that is invalid fails.
 */
class TablesContentTest {
    private data class Rules(
        val categories: Set<String>? = null,
        val minPerCategory: Int = 0,
        val keywordsRequired: Boolean = false,
        val tags: Set<String> = emptySet(),
    )

    private val senses = setOf("sight", "hearing", "smell", "taste", "touch")

    private val rules =
        mapOf(
            TableId.SensoryWords to Rules(senses, minPerCategory = 6, keywordsRequired = true),
            TableId.ThreadingQuestions to Rules(setOf("what", "origin", "who", "link")),
            TableId.NpcRoles to Rules(tags = setOf("rank:high", "rank:low")),
            TableId.StatusShifts to Rules(tags = setOf("shift:up", "shift:down")),
            TableId.StoryElements to
                Rules(
                    setOf("place", "object", "trait", "sensory", "line"),
                    minPerCategory = 30,
                    keywordsRequired = true,
                ),
        )

    private val idPattern = Regex("[a-z0-9]+(-[a-z0-9]+)*")

    /** Mirrors the detection normalizer (ADR 0008): lowercase, NFD, no diacritics, ligatures expanded. */
    private fun normalize(text: String): String =
        Normalizer
            .normalize(text.lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "")
            .replace("œ", "oe")
            .replace("æ", "ae")

    private fun load(id: TableId): TableFile {
        val file = TestTables.assetFile(id)
        assumeTrue(file.exists(), "table ${id.fileName} not written yet")
        return TableFile.decode(file.readText())
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `envelope is valid`(id: TableId) {
        val table = load(id)

        assertEquals(id.fileName, table.table)
        assertEquals(1, table.schemaVersion)
        assertEquals("fr", table.locale)
        assertTrue(table.entries.size >= 30, "at least 30 entries, found ${table.entries.size}")
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `ids are unique and well formed`(id: TableId) {
        val entries = load(id).entries

        assertEquals(entries.size, entries.map { it.id }.toSet().size, "duplicate ids")
        entries.forEach { assertTrue(idPattern.matches(it.id), "bad id '${it.id}'") }
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `difficulty is within 1 to 3 and every level is present`(id: TableId) {
        val entries = load(id).entries

        entries.forEach { assertTrue(it.difficulty in 1..3, "${it.id}: difficulty ${it.difficulty}") }
        assertEquals(setOf(1, 2, 3), entries.map { it.difficulty }.toSet())
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `texts are clean and not duplicated`(id: TableId) {
        val entries = load(id).entries

        entries.forEach {
            assertTrue(it.text.isNotBlank(), "${it.id}: blank text")
            assertEquals(it.text.trim(), it.text, "${it.id}: surrounding spaces")
            assertTrue(it.text.length <= 200, "${it.id}: text too long")
            assertTrue(Normalizer.isNormalized(it.text, Normalizer.Form.NFC), "${it.id}: not NFC")
        }
        assertEquals(entries.size, entries.map { normalize(it.text) }.toSet().size, "duplicate texts")
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `categories follow the table rules`(id: TableId) {
        val entries = load(id).entries
        val allowed = rules[id]?.categories

        if (allowed == null) {
            entries.forEach { assertEquals(null, it.category, "${it.id}: unexpected category") }
        } else {
            entries.forEach { assertTrue(it.category in allowed, "${it.id}: category ${it.category}") }
            val counts = entries.groupingBy { it.category }.eachCount()
            allowed.forEach {
                val min = rules.getValue(id).minPerCategory
                assertTrue((counts[it] ?: 0) >= min, "category $it has ${counts[it] ?: 0} entries, expected >= $min")
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `keywords follow the table rules`(id: TableId) {
        val entries = load(id).entries
        val required = rules[id]?.keywordsRequired ?: false

        entries.forEach { entry ->
            if (required) assertTrue(entry.keywords.size in 1..4, "${entry.id}: ${entry.keywords.size} keywords")
            entry.keywords.forEach { assertTrue(it.isNotBlank(), "${entry.id}: blank keyword") }
            val normalized = entry.keywords.map(::normalize)
            assertEquals(normalized.size, normalized.toSet().size, "${entry.id}: duplicate keywords")
            if (entry.fuzzy) {
                assertTrue(entry.keywords.any { it.length >= 8 }, "${entry.id}: fuzzy without a long keyword")
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `tags belong to the allowed vocabulary`(id: TableId) {
        val allowed = rules[id]?.tags ?: emptySet()

        load(id).entries.forEach { entry ->
            assertTrue(allowed.containsAll(entry.tags), "${entry.id}: tags ${entry.tags}")
        }
    }
}
