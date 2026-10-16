package io.github.qnicoud.dmgym.core.detection

import io.github.qnicoud.dmgym.core.tables.TableEntry
import io.github.qnicoud.dmgym.core.tables.TableFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Golden corpus measurement (ADR 0008, `docs/qa/detection-corpus.md`). Each case of
 * `detection/corpus.json` tests one real entry of `story_elements` or `sensory_words`:
 * `{"cases":[{"id","entryId","table","text","expected":bool,"tag"}]}`. Keywords and `fuzzy` are read from the
 * shipped tables. Recall and precision are computed in total, per category and per tag; a detailed report
 * is written to `build/reports/detection-report.txt`.
 */
class GoldenCorpusTest {
    @Serializable
    data class Corpus(
        val cases: List<Case>,
    )

    @Serializable
    data class Case(
        val id: String,
        val entryId: String,
        val table: String,
        val text: String,
        val expected: Boolean,
        val tag: String,
    )

    /** Outcome counts of a group of cases; [misses] lists the wrong decisions as "FP/FN id (entry) [tag]". */
    data class Score(
        val truePositives: Int = 0,
        val falsePositives: Int = 0,
        val falseNegatives: Int = 0,
        val misses: List<String> = emptyList(),
    ) {
        val recall get() = ratio(truePositives, truePositives + falseNegatives)
        val precision get() = ratio(truePositives, truePositives + falsePositives)

        operator fun plus(other: Score) =
            Score(
                truePositives + other.truePositives,
                falsePositives + other.falsePositives,
                falseNegatives + other.falseNegatives,
                misses + other.misses,
            )

        private fun ratio(
            num: Int,
            den: Int,
        ) = if (den == 0) 1.0 else num.toDouble() / den

        companion object {
            fun of(
                expected: Boolean,
                detected: Boolean,
                label: String,
            ) = when {
                expected && detected -> Score(truePositives = 1)
                detected -> Score(falsePositives = 1, misses = listOf("FP $label"))
                expected -> Score(falseNegatives = 1, misses = listOf("FN $label"))
                else -> Score()
            }
        }
    }

    private class Measure(
        val total: Score,
        val byCategory: Map<String, Score>,
        val byTag: Map<String, Score>,
    )

    private fun load(): Corpus? =
        javaClass.classLoader.getResourceAsStream(CORPUS)?.use {
            Json.decodeFromString<Corpus>(it.readBytes().toString(Charsets.UTF_8))
        }

    private fun loadTable(name: String): Map<String, TableEntry> =
        TableFile
            .decode(File("src/main/assets/tables/$name.json").readText())
            .entries
            .associateBy { it.id }

    private fun validate(
        corpus: Corpus,
        tables: Map<String, Map<String, TableEntry>>,
        minCases: Int,
    ) {
        assertTrue(corpus.cases.size >= minCases, "corpus has ${corpus.cases.size} cases, expected at least $minCases")
        val ids = corpus.cases.map { it.id }
        assertEquals(ids.distinct(), ids, "duplicate case ids")
        corpus.cases.forEach { case ->
            val entry = tables[case.table]?.get(case.entryId)
            assertTrue(entry != null, "${case.id}: unknown entry ${case.table}/${case.entryId}")
        }
        assertTrue(corpus.cases.any { it.expected }, "corpus has no positive case")
        assertTrue(corpus.cases.any { !it.expected }, "corpus has no negative case")
    }

    private fun measure(
        corpus: Corpus,
        tables: Map<String, Map<String, TableEntry>>,
    ): Measure {
        val detector = KeywordDetector()
        val byCategory = mutableMapOf<String, Score>()
        val byTag = mutableMapOf<String, Score>()
        var total = Score()
        for (case in corpus.cases) {
            val entry = requireNotNull(tables[case.table]?.get(case.entryId))
            val target = DetectionTarget(entry.id, entry.keywords, entry.fuzzy)
            val detected = detector.detect(case.text, listOf(target)).isDetected(entry.id)
            val score = Score.of(case.expected, detected, "${case.id} (${case.entryId}) [${case.tag}]")
            total += score
            byCategory.merge("${case.table}/${entry.category}", score, Score::plus)
            byTag.merge(case.tag, score, Score::plus)
        }
        return Measure(total, byCategory, byTag)
    }

    private fun report(measure: Measure): String {
        fun line(
            label: String,
            score: Score,
        ) = "%-28s recall=%.3f precision=%.3f (tp=%d fp=%d fn=%d)".format(
            label,
            score.recall,
            score.precision,
            score.truePositives,
            score.falsePositives,
            score.falseNegatives,
        )
        return buildString {
            appendLine(line("TOTAL", measure.total))
            appendLine("-- by category")
            measure.byCategory.toSortedMap().forEach { (k, v) -> appendLine(line(k, v)) }
            appendLine("-- by tag")
            measure.byTag.toSortedMap().forEach { (k, v) -> appendLine(line(k, v)) }
            appendLine("-- misses")
            measure.total.misses.forEach(::appendLine)
        }
    }

    @Test
    fun `golden corpus meets the thresholds`() {
        val corpus = load()
        assumeTrue(REQUIRE_GOLDEN_CORPUS_PRESENT || corpus != null, "$CORPUS not provided yet")
        val tables = TABLES.associateWith(::loadTable)
        validate(requireNotNull(corpus) { "missing $CORPUS" }, tables, GOLDEN_MIN_CASES)

        val measure = measure(corpus, tables)
        val text = report(measure)
        File("build/reports").mkdirs()
        File("build/reports/detection-report.txt").writeText(text)

        val summary = text.lines().take(1).joinToString()
        assertTrue(measure.total.recall >= MIN_RECALL, "recall below $MIN_RECALL: $summary")
        assertTrue(measure.total.precision >= MIN_PRECISION, "precision below $MIN_PRECISION: $summary")
    }

    @Test
    fun `validation rejects tiny, unknown or one sided corpora`() {
        val entry = TableEntry(id = "pont", text = "un pont", difficulty = 1, keywords = listOf("pont"))
        val tables = mapOf("story_elements" to mapOf("pont" to entry))
        val positive = Case("c1", "pont", "story_elements", "le pont", true, "direct")
        val negative = positive.copy(id = "c2", expected = false)

        validate(Corpus(listOf(positive, negative)), tables, minCases = 2)
        assertThrows(AssertionError::class.java) { validate(Corpus(listOf(positive, negative)), tables, minCases = 3) }
        assertThrows(AssertionError::class.java) { validate(Corpus(listOf(positive, positive)), tables, minCases = 1) }
        assertThrows(AssertionError::class.java) { validate(Corpus(listOf(positive)), tables, minCases = 1) }
        val unknown = positive.copy(entryId = "nope")
        assertThrows(AssertionError::class.java) { validate(Corpus(listOf(unknown, negative)), tables, minCases = 1) }
    }

    @Test
    fun `score computes recall and precision`() {
        val score =
            Score.of(true, true, "a") + Score.of(true, false, "b") + Score.of(false, true, "c") +
                Score.of(false, false, "d")

        assertEquals(0.5, score.recall)
        assertEquals(0.5, score.precision)
        assertEquals(listOf("FN b", "FP c"), score.misses)
        assertEquals(1.0, Score().recall)
    }

    private companion object {
        /** True now that corpus.json is merged: its absence fails the build instead of skipping the test. */
        const val REQUIRE_GOLDEN_CORPUS_PRESENT = true
        const val CORPUS = "detection/corpus.json"
        val TABLES = listOf("story_elements", "sensory_words")
        const val MIN_RECALL = 0.85
        const val MIN_PRECISION = 0.90
        const val GOLDEN_MIN_CASES = 50
    }
}
