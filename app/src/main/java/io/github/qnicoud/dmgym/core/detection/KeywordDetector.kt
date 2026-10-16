package io.github.qnicoud.dmgym.core.detection

import kotlin.math.abs

/** Identifier of a detectable element (for example a table entry id or a category). */
typealias TargetId = String

/**
 * An element to look for in a text. The keyword list is copied at construction.
 *
 * @property id identifier used as key in [DetectionResult.matches]
 * @param keywords words or word sequences written by the content author ("lanterne", "vieille femme")
 * @property fuzzy when true, tokens of 8 letters or more tolerate one typo (Damerau-Levenshtein distance <= 1)
 */
class DetectionTarget(
    val id: TargetId,
    keywords: List<String>,
    val fuzzy: Boolean = false,
) {
    /** Immutable copy of the keywords given at construction. */
    val keywords: List<String> = keywords.toList()

    override fun equals(other: Any?): Boolean =
        other is DetectionTarget && id == other.id && keywords == other.keywords && fuzzy == other.fuzzy

    override fun hashCode(): Int = (id.hashCode() * 31 + keywords.hashCode()) * 31 + fuzzy.hashCode()

    override fun toString(): String = "DetectionTarget(id=$id, keywords=$keywords, fuzzy=$fuzzy)"
}

/** Detected elements. Targets without any match are absent from [matches]. */
data class DetectionResult(
    /** For each detected target, the sorted inclusive ranges in the original text; overlaps are merged. */
    val matches: Map<TargetId, List<IntRange>>,
) {
    /** True when [id] has at least one match. */
    fun isDetected(id: TargetId): Boolean = matches[id].orEmpty().isNotEmpty()
}

internal class KeywordPattern(
    val target: TargetId,
    val keys: List<String>,
    val fuzzy: Boolean,
) {
    fun isFuzzyAt(index: Int): Boolean = fuzzy && keys[index].length >= KeywordDetector.FUZZY_MIN_LENGTH
}

/**
 * Targets prepared once by [KeywordDetector.compile]: keywords are tokenized, normalized and stemmed,
 * and indexed by their first key. Reuse it across calls to [KeywordDetector.detect].
 */
class CompiledTargets internal constructor(
    internal val exact: Map<String, List<KeywordPattern>>,
    internal val fuzzy: List<KeywordPattern>,
) {
    internal val isEmpty: Boolean get() = exact.isEmpty() && fuzzy.isEmpty()
}

/**
 * Pure keyword detector (ADR 0008). Text tokens and keyword tokens go through the same pipeline
 * (normalization, then [stemmer]); a keyword matches a run of consecutive text tokens separated only by
 * whitespace, hyphens or apostrophes.
 *
 * Fuzzy matching only applies to targets flagged `fuzzy`, for keyword tokens of at least
 * [FUZZY_MIN_LENGTH] letters.
 */
class KeywordDetector(
    private val stemmer: Stemmer = IdentityStemmer,
) {
    /** Prepares [targets] for repeated detection. Call it once per draw, not once per keystroke. */
    fun compile(targets: List<DetectionTarget>): CompiledTargets {
        val patterns = targets.flatMap(::patternsOf)
        val (fuzzyFirst, exactFirst) = patterns.partition { it.isFuzzyAt(0) }
        return CompiledTargets(exactFirst.groupBy { it.keys.first() }, fuzzyFirst)
    }

    /** Convenience overload that compiles [targets] on every call; prefer [compile] for repeated use. */
    fun detect(
        text: String,
        targets: List<DetectionTarget>,
    ): DetectionResult = detect(text, compile(targets))

    /**
     * Looks for the [targets] in [text].
     *
     * @return the match ranges (inclusive, in [text] coordinates, ready for highlighting) per detected
     * target, sorted, with overlapping ranges merged. Undetected targets are absent.
     */
    fun detect(
        text: String,
        targets: CompiledTargets,
    ): DetectionResult {
        val tokens = Tokenizer.tokenize(text)
        if (tokens.isEmpty() || targets.isEmpty) return DetectionResult(emptyMap())
        val keys = tokens.map(::keyOf)
        val found = LinkedHashMap<TargetId, MutableList<IntRange>>()
        for (start in tokens.indices) {
            val exact = targets.exact[keys[start]].orEmpty()
            for (pattern in exact + targets.fuzzy) {
                val last = matchEnd(pattern, text, tokens, keys, start)
                if (last >= 0) {
                    found.getOrPut(pattern.target) { mutableListOf() }.add(tokens[start].range.first..last)
                }
            }
        }
        return DetectionResult(found.mapValues { (_, ranges) -> mergeOverlaps(ranges) })
    }

    private fun patternsOf(target: DetectionTarget): List<KeywordPattern> =
        target.keywords.mapNotNull { keyword ->
            val keys = Tokenizer.tokenize(keyword).map(::keyOf)
            if (keys.isEmpty()) null else KeywordPattern(target.id, keys, target.fuzzy)
        }

    private fun keyOf(token: Token): String = stemmer.stem(TextNormalizer.normalize(token.text))

    /** Returns the last index of the match in the text, or -1 when [pattern] does not match at [start]. */
    private fun matchEnd(
        pattern: KeywordPattern,
        text: String,
        tokens: List<Token>,
        keys: List<String>,
        start: Int,
    ): Int {
        if (start + pattern.keys.size > tokens.size) return -1
        for (offset in pattern.keys.indices) {
            if (!keyMatches(pattern, offset, keys[start + offset])) return -1
            if (offset > 0 && !isSeparator(text, tokens[start + offset - 1], tokens[start + offset])) return -1
        }
        return tokens[start + pattern.keys.size - 1].range.last
    }

    private fun keyMatches(
        pattern: KeywordPattern,
        offset: Int,
        actual: String,
    ): Boolean {
        val expected = pattern.keys[offset]
        if (actual == expected) return true
        return pattern.isFuzzyAt(offset) &&
            abs(actual.length - expected.length) <= 1 &&
            DamerauLevenshtein.distance(actual, expected) <= 1
    }

    private fun isSeparator(
        text: String,
        previous: Token,
        next: Token,
    ): Boolean =
        (previous.range.last + 1 until next.range.first).all { i ->
            val c = text[i]
            c.isWhitespace() || c == '\'' || c == '’' || c == '-'
        }

    private fun mergeOverlaps(ranges: List<IntRange>): List<IntRange> {
        val merged = mutableListOf<IntRange>()
        for (range in ranges.sortedWith(compareBy({ it.first }, { it.last }))) {
            val previous = merged.lastOrNull()
            if (previous != null && range.first <= previous.last) {
                merged[merged.lastIndex] = previous.first..maxOf(previous.last, range.last)
            } else {
                merged += range
            }
        }
        return merged
    }

    companion object {
        /** Minimum letters of a keyword token for fuzzy matching to apply. */
        const val FUZZY_MIN_LENGTH = 8
    }
}
