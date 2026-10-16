package io.github.qnicoud.dmgym.core.detection

/** Identifier of a detectable element (for example a table entry id or a category). */
typealias TargetId = String

/**
 * An element to look for in a text.
 *
 * @property id identifier used as key in [DetectionResult.matches]
 * @property keywords words or word sequences written by the content author ("lanterne", "vieille femme")
 * @property fuzzy when true, tokens of 8 letters or more tolerate one typo (Damerau-Levenshtein distance <= 1)
 */
data class DetectionTarget(
    val id: TargetId,
    val keywords: List<String>,
    val fuzzy: Boolean = false,
)

/** Detected elements. Targets without any match are absent from [matches]. */
data class DetectionResult(
    /** For each detected target, the sorted, distinct inclusive ranges in the original text. */
    val matches: Map<TargetId, List<IntRange>>,
) {
    /** True when [id] has at least one match. */
    fun isDetected(id: TargetId): Boolean = matches[id].orEmpty().isNotEmpty()
}

/**
 * Pure keyword detector (ADR 0008). Text tokens and keyword tokens go through the same pipeline
 * (normalization, then [stemmer]); a keyword matches a run of contiguous text tokens.
 *
 * Fuzzy matching only applies to targets flagged `fuzzy`, for keyword tokens of at least
 * [FUZZY_MIN_LENGTH] letters.
 */
class KeywordDetector(
    private val stemmer: Stemmer = IdentityStemmer,
) {
    private class Pattern(
        val target: TargetId,
        val keys: List<String>,
        val fuzzy: Boolean,
    )

    /**
     * Looks for [targets] in [text].
     *
     * @return the match ranges (inclusive, in [text] coordinates, ready for highlighting) per detected target
     */
    fun detect(
        text: String,
        targets: List<DetectionTarget>,
    ): DetectionResult {
        val tokens = Tokenizer.tokenize(text)
        val patterns = targets.flatMap(::patternsOf)
        if (tokens.isEmpty() || patterns.isEmpty()) return DetectionResult(emptyMap())
        val keys = tokens.map(::keyOf)
        val found = LinkedHashMap<TargetId, MutableSet<IntRange>>()
        for (pattern in patterns) {
            for (start in 0..keys.size - pattern.keys.size) {
                if (matchesAt(pattern, keys, start)) {
                    val range = tokens[start].range.first..tokens[start + pattern.keys.size - 1].range.last
                    found.getOrPut(pattern.target) { mutableSetOf() }.add(range)
                }
            }
        }
        return DetectionResult(found.mapValues { (_, ranges) -> ranges.sortedBy { it.first } })
    }

    private fun patternsOf(target: DetectionTarget): List<Pattern> =
        target.keywords.mapNotNull { keyword ->
            val keys = Tokenizer.tokenize(keyword).map(::keyOf)
            if (keys.isEmpty()) null else Pattern(target.id, keys, target.fuzzy)
        }

    private fun keyOf(token: Token): String = stemmer.stem(TextNormalizer.normalize(token.text))

    private fun matchesAt(
        pattern: Pattern,
        keys: List<String>,
        start: Int,
    ): Boolean =
        pattern.keys.indices.all { offset ->
            val expected = pattern.keys[offset]
            val actual = keys[start + offset]
            actual == expected ||
                (
                    pattern.fuzzy && expected.length >= FUZZY_MIN_LENGTH &&
                        DamerauLevenshtein.distance(actual, expected) <= 1
                )
        }

    companion object {
        /** Minimum letters of a keyword token for fuzzy matching to apply. */
        const val FUZZY_MIN_LENGTH = 8
    }
}
