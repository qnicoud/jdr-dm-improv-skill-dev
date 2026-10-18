package io.github.qnicoud.dmgym.core.tables

import io.github.qnicoud.dmgym.core.detection.TextNormalizer
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.text.Normalizer

/**
 * Content validation of the shipped tables (ADR 0009). Every table must exist and be valid.
 */
class TablesContentTest {
    private data class Rules(
        val categories: Set<String>? = null,
        val minPerCategory: Int = 0,
        val keywordsRequired: Boolean = false,
        val tags: Set<String> = emptySet(),
        /** Les entrées peuvent ne pas avoir de catégorie (mais, si elles en ont une, elle est dans [categories]). */
        val categoryOptional: Boolean = false,
        val maxTextLength: Int = 200,
        /** Table de vocabulaire (ADR 0017) : peu d'entrées, ids pris dans un vocabulaire fermé. */
        val vocabulary: Set<String>? = null,
        val entryCount: IntRange = 30..Int.MAX_VALUE,
    )

    private val senses = setOf("sight", "hearing", "smell", "taste", "touch")

    private val rules =
        mapOf(
            TableId.SensoryWords to Rules(senses, minPerCategory = 6, keywordsRequired = true),
            TableId.ThreadingQuestions to Rules(setOf("what", "origin", "who", "link")),
            TableId.NpcRoles to Rules(tags = setOf("rank:high", "rank:low")),
            TableId.StatusShifts to Rules(tags = setOf("shift:up", "shift:down")),
            TableId.FailureSituations to
                Rules(setOf("combat", "social", "exploration", "stealth", "knowledge"), categoryOptional = true),
            TableId.ComplicationTypes to
                Rules(
                    vocabulary =
                        setOf(
                            "cost",
                            "time",
                            "information",
                            "relation",
                            "position",
                            "danger",
                            "resource",
                            "ally",
                        ),
                    entryCount = 6..8,
                    maxTextLength = 20,
                ),
            TableId.ClueConclusions to
                Rules(setOf("identity", "location", "motive", "method", "secret"), categoryOptional = true),
            TableId.PlayerOffers to
                Rules(setOf("social", "action", "exploration", "unexpected"), minPerCategory = 10, maxTextLength = 120),
            TableId.StoryElements to
                Rules(
                    setOf("place", "object", "trait", "sensory", "line"),
                    minPerCategory = 30,
                    keywordsRequired = true,
                ),
        )

    @Test
    fun `the fourteen expected tables are shipped`() {
        val expected =
            setOf(
                "problems",
                "places",
                "sensory_words",
                "core_images",
                "threading_questions",
                "npc_roles",
                "status_shifts",
                "surprise_situations",
                "story_elements",
                "one_page_situations",
                "failure_situations",
                "complication_types",
                "clue_conclusions",
                "player_offers",
            )

        assertEquals(expected, TableId.entries.map { it.fileName }.toSet())
        expected.forEach { assertTrue(File(TestTables.assetsDir(), "$it.json").exists(), "$it.json missing") }
    }

    @Test
    fun `every content language ships a folder with all the tables`() {
        val root = File(TestTables.assetsDir(), "..").canonicalFile
        val folders =
            root
                .listFiles { f -> f.isDirectory }
                .orEmpty()
                .map { it.name }
                .toSet()
        assertEquals(ContentLanguage.entries.map { it.code }.toSet(), folders, "tables/<lang> folders")
        ContentLanguage.entries.forEach { lang ->
            val names =
                TestTables
                    .assetsDir(lang)
                    .list()
                    .orEmpty()
                    .toSet()
            val expected = TableId.entries.map { "${it.fileName}.json" }
            assertTrue(names.containsAll(expected), "files for ${lang.code}: missing ${expected - names}")
        }
    }

    @Test
    fun `tables are identical across languages except for text, keywords and fuzzy`() {
        val reference = ContentLanguage.DEFAULT
        ContentLanguage.entries.filter { it != reference }.forEach { lang ->
            TableId.entries.forEach { id ->
                val base = load(id, reference).entries.associateBy { it.id }
                val other = load(id, lang).entries.associateBy { it.id }
                assertEquals(base.keys, other.keys, "${id.fileName}: ids differ in ${lang.code}")
                base.forEach { (entryId, entry) ->
                    val twin = other.getValue(entryId)
                    assertEquals(entry.category, twin.category, "${id.fileName}/$entryId category")
                    assertEquals(entry.tags, twin.tags, "${id.fileName}/$entryId tags")
                    assertEquals(entry.difficulty, twin.difficulty, "${id.fileName}/$entryId difficulty")
                }
            }
        }
    }

    private val idPattern = Regex("[a-z0-9]+(-[a-z0-9]+)*")

    private fun load(
        id: TableId,
        language: ContentLanguage = ContentLanguage.DEFAULT,
    ): TableFile {
        val file = TestTables.assetFile(id, language)
        assertTrue(file.exists(), "table ${id.fileName} is missing from the ${language.code} assets")
        return TableFile.decode(file.readText())
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `envelope is valid`(id: TableId) {
        val table = load(id)

        assertEquals(id.fileName, table.table)
        assertEquals(1, table.schemaVersion)
        assertEquals(ContentLanguage.DEFAULT.code, table.locale)
        val expected = rules[id]?.entryCount ?: 30..Int.MAX_VALUE
        assertTrue(table.entries.size in expected, "expected $expected entries, found ${table.entries.size}")
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
        // A vocabulary table does not have to cover every level (ADR 0017).
        if (rules[id]?.vocabulary == null) assertEquals(setOf(1, 2, 3), entries.map { it.difficulty }.toSet())
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `texts are clean and not duplicated`(id: TableId) {
        val entries = load(id).entries

        entries.forEach {
            assertTrue(it.text.isNotBlank(), "${it.id}: blank text")
            assertEquals(it.text.trim(), it.text, "${it.id}: surrounding spaces")
            assertTrue(it.text.length <= (rules[id]?.maxTextLength ?: 200), "${it.id}: text too long")
            assertTrue(Normalizer.isNormalized(it.text, Normalizer.Form.NFC), "${it.id}: not NFC")
        }
        assertEquals(entries.size, entries.map { TextNormalizer.normalize(it.text) }.toSet().size, "duplicate texts")
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `categories follow the table rules`(id: TableId) {
        val entries = load(id).entries
        val allowed = rules[id]?.categories

        if (allowed == null) {
            entries.forEach { assertEquals(null, it.category, "${it.id}: unexpected category") }
        } else {
            val optional = rules.getValue(id).categoryOptional
            entries.forEach {
                assertTrue(
                    it.category in allowed || (optional && it.category == null),
                    "${it.id}: category ${it.category}",
                )
            }
            val counts = entries.groupingBy { it.category }.eachCount()
            allowed.forEach {
                val min = rules.getValue(id).minPerCategory
                assertTrue((counts[it] ?: 0) >= min, "category $it has ${counts[it] ?: 0} entries, expected >= $min")
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `vocabulary tables use a closed set of ids`(id: TableId) {
        val vocabulary = rules[id]?.vocabulary ?: return

        load(id).entries.forEach { assertTrue(it.id in vocabulary, "${it.id}: not in the closed vocabulary") }
    }

    @ParameterizedTest
    @EnumSource(TableId::class)
    fun `keywords follow the table rules`(id: TableId) {
        val entries = load(id).entries
        val required = rules[id]?.keywordsRequired ?: false

        entries.forEach { entry ->
            if (required) assertTrue(entry.keywords.size in 1..4, "${entry.id}: ${entry.keywords.size} keywords")
            entry.keywords.forEach { assertTrue(it.isNotBlank(), "${entry.id}: blank keyword") }
            val normalized = entry.keywords.map(TextNormalizer::normalize)
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
