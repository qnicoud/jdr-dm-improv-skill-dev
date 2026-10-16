package io.github.qnicoud.dmgym.core.detection

import java.text.Normalizer
import java.util.Locale

/** A word of the original text: [text] is the raw form, [range] its inclusive position in that text. */
data class Token(
    val text: String,
    val range: IntRange,
)

/** Splits a text into letter tokens while keeping their positions (ADR 0008). */
object Tokenizer {
    private val tokenRegex = Regex("""\p{L}[\p{L}\p{M}]*""")

    /**
     * Returns the tokens of [text] in order. Apostrophes (' and ’), hyphens, digits and punctuation
     * are separators, which handles French elisions (l', d', qu').
     */
    fun tokenize(text: String): List<Token> = tokenRegex.findAll(text).map { Token(it.value, it.range) }.toList()
}

/** Normalizes a word so that spelling variants compare equal. */
object TextNormalizer {
    private val combiningMarks = Regex("""\p{Mn}""")

    /** Lowercases (locale independent), removes accents, and expands œ and æ. */
    fun normalize(word: String): String =
        Normalizer
            .normalize(word.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace("œ", "oe")
            .replace("æ", "ae")
}

/** Reduces a normalized word to a key; applied on both the text and the keywords. */
fun interface Stemmer {
    fun stem(word: String): String
}

/** v1 stemmer: exact match on normalized forms. A Snowball implementation can replace it (ADR 0008). */
object IdentityStemmer : Stemmer {
    override fun stem(word: String): String = word
}

/** Damerau-Levenshtein distance (optimal string alignment variant). */
object DamerauLevenshtein {
    /** Minimal number of insertions, deletions, substitutions and adjacent transpositions turning [a] into [b]. */
    fun distance(
        a: String,
        b: String,
    ): Int {
        var beforePrevious = IntArray(b.length + 1)
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var best = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = minOf(best, beforePrevious[j - 2] + 1)
                }
                current[j] = best
            }
            val recycled = beforePrevious
            beforePrevious = previous
            previous = current
            current = recycled
        }
        return previous[b.length]
    }
}
