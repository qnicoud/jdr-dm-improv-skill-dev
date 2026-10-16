package io.github.qnicoud.dmgym.core.detection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * Golden corpus measurement (ADR 0008). Format of `detection/corpus.json`:
 * `{ "cases": [ { "text", "targets": [ { "category", "keywords", "fuzzy"? } ], "expected": [category], "note"? } ] }`.
 * Categories are unique inside a case and serve as target ids. Every (case, target) pair is one decision:
 * expected and detected is a true positive, detected only a false positive, expected only a false negative.
 */
class GoldenCorpusTest {
    @Serializable
    data class Corpus(
        val cases: List<Case>,
    )

    @Serializable
    data class Case(
        val text: String,
        val targets: List<CorpusTarget>,
        val expected: List<String> = emptyList(),
        val note: String = "",
    )

    @Serializable
    data class CorpusTarget(
        val category: String,
        val keywords: List<String>,
        val fuzzy: Boolean = false,
    )

    data class Score(
        val truePositives: Int,
        val falsePositives: Int,
        val falseNegatives: Int,
        val misses: List<String>,
    ) {
        val recall get() = Score.ratio(truePositives, truePositives + falseNegatives)
        val precision get() = Score.ratio(truePositives, truePositives + falsePositives)

        operator fun plus(other: Score) =
            Score(
                truePositives + other.truePositives,
                falsePositives + other.falsePositives,
                falseNegatives + other.falseNegatives,
                misses + other.misses,
            )

        companion object {
            fun of(
                expected: Boolean,
                detected: Boolean,
                label: String,
            ) = when {
                expected && detected -> Score(1, 0, 0, emptyList())
                detected -> Score(0, 1, 0, listOf("FP $label"))
                expected -> Score(0, 0, 1, listOf("FN $label"))
                else -> Score(0, 0, 0, emptyList())
            }

            fun ratio(
                num: Int,
                den: Int,
            ) = if (den == 0) 1.0 else num.toDouble() / den
        }
    }

    private fun load(path: String): Corpus? =
        javaClass.classLoader.getResourceAsStream(path)?.use {
            Json.decodeFromString<Corpus>(it.readBytes().toString(Charsets.UTF_8))
        }

    private fun validate(
        corpus: Corpus,
        minCases: Int,
    ) {
        assertTrue(corpus.cases.size >= minCases, "corpus has ${corpus.cases.size} cases, expected at least $minCases")
        corpus.cases.forEach { case ->
            val categories = case.targets.map { it.category }
            assertEquals(categories.distinct(), categories, "duplicate category in: ${case.text}")
            assertTrue(categories.containsAll(case.expected), "expected category without target in: ${case.text}")
        }
        assertTrue(corpus.cases.any { it.expected.isNotEmpty() }, "corpus has no positive case")
        assertTrue(
            corpus.cases.any { case -> case.targets.any { it.category !in case.expected } },
            "corpus has no negative case",
        )
    }

    private fun measure(corpus: Corpus): Map<String?, Score> {
        val detector = KeywordDetector()
        val perCategory = mutableMapOf<String?, Score>()
        for (case in corpus.cases) {
            val targets = case.targets.map { DetectionTarget(it.category, it.keywords, it.fuzzy) }
            val result = detector.detect(case.text, targets)
            for (target in case.targets) {
                val expected = target.category in case.expected
                val detected = result.isDetected(target.category)
                val delta = Score.of(expected, detected, "${target.category}: ${case.text}")
                perCategory.merge(target.category, delta, Score::plus)
            }
        }
        val total = perCategory.values.fold(Score(0, 0, 0, emptyList()), Score::plus)
        return perCategory + (null to total)
    }

    private fun assertThresholds(
        path: String,
        minCases: Int,
    ) {
        val corpus = requireNotNull(load(path)) { "missing $path" }
        validate(corpus, minCases)
        val scores = measure(corpus)
        val total = requireNotNull(scores[null])
        val report =
            scores.entries.joinToString("\n") { (category, score) ->
                "${category ?: "TOTAL"}: recall=${score.recall} precision=${score.precision}"
            } + "\n" + total.misses.joinToString("\n")
        assertTrue(total.recall >= MIN_RECALL, report)
        assertTrue(total.precision >= MIN_PRECISION, report)
        println(report.lines().filter { it.contains("recall=") }.joinToString("\n"))
    }

    @Test
    fun `sample corpus meets the thresholds`() = assertThresholds("detection/corpus-sample.json", SAMPLE_MIN_CASES)

    @Test
    fun `corpus validation rejects tiny or one sided corpora`() {
        val case = Case("x", listOf(CorpusTarget("a", listOf("x"))), listOf("a"))

        assertThrows(AssertionError::class.java) { validate(Corpus(listOf(case)), minCases = 2) }
        assertThrows(AssertionError::class.java) { validate(Corpus(listOf(case, case)), minCases = 2) }
        val duplicated = case.copy(targets = case.targets + case.targets)
        assertThrows(AssertionError::class.java) { validate(Corpus(listOf(duplicated)), minCases = 1) }
    }

    @Test
    fun `golden corpus meets the thresholds`() {
        assumeTrue(REQUIRE_GOLDEN_CORPUS_PRESENT || load(CORPUS) != null, "$CORPUS not provided yet")
        assertThresholds(CORPUS, GOLDEN_MIN_CASES)
    }

    private companion object {
        /** Set to true once corpus.json is merged: its absence then fails the build instead of skipping the test. */
        const val REQUIRE_GOLDEN_CORPUS_PRESENT = false
        const val CORPUS = "detection/corpus.json"
        const val MIN_RECALL = 0.85
        const val MIN_PRECISION = 0.90
        const val SAMPLE_MIN_CASES = 10
        const val GOLDEN_MIN_CASES = 50
    }
}
