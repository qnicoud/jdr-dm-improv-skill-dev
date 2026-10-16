package io.github.qnicoud.dmgym.core.detection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
        val recall get() = ratio(truePositives, truePositives + falseNegatives)
        val precision get() = ratio(truePositives, truePositives + falsePositives)

        private fun ratio(
            num: Int,
            den: Int,
        ) = if (den == 0) 1.0 else num.toDouble() / den
    }

    private fun load(path: String): Corpus? =
        javaClass.classLoader.getResourceAsStream(path)?.use {
            Json.decodeFromString<Corpus>(it.readBytes().toString(Charsets.UTF_8))
        }

    private fun measure(corpus: Corpus): Score {
        val detector = KeywordDetector()
        var tp = 0
        var fp = 0
        var fn = 0
        val misses = mutableListOf<String>()
        for (case in corpus.cases) {
            val targets = case.targets.map { DetectionTarget(it.category, it.keywords, it.fuzzy) }
            val result = detector.detect(case.text, targets)
            for (target in case.targets) {
                val expected = target.category in case.expected
                val detected = result.isDetected(target.category)
                when {
                    expected && detected -> tp++
                    detected -> fp++.also { misses += "FP ${target.category}: ${case.text}" }
                    expected -> fn++.also { misses += "FN ${target.category}: ${case.text}" }
                }
            }
        }
        return Score(tp, fp, fn, misses)
    }

    private fun assertThresholds(path: String) {
        val score = measure(requireNotNull(load(path)) { "missing $path" })
        val report = "recall=${score.recall} precision=${score.precision}\n${score.misses.joinToString("\n")}"
        assertTrue(score.recall >= MIN_RECALL, report)
        assertTrue(score.precision >= MIN_PRECISION, report)
    }

    @Test
    fun `sample corpus meets the thresholds`() = assertThresholds("detection/corpus-sample.json")

    @Test
    fun `golden corpus meets the thresholds`() {
        assumeTrue(load("detection/corpus.json") != null, "detection/corpus.json not provided yet")
        assertThresholds("detection/corpus.json")
    }

    private companion object {
        const val MIN_RECALL = 0.85
        const val MIN_PRECISION = 0.90
    }
}
