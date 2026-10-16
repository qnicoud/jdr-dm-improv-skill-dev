package io.github.qnicoud.dmgym.core.detection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

class KeywordDetectorTest {
    private val detector = KeywordDetector()

    private fun target(
        id: String,
        vararg keywords: String,
        fuzzy: Boolean = false,
    ) = DetectionTarget(id, keywords.toList(), fuzzy)

    private fun detect(
        text: String,
        vararg targets: DetectionTarget,
    ) = detector.detect(text, targets.toList())

    @Test
    fun `no target or empty text gives an empty result`() {
        assertTrue(detector.detect("du texte", emptyList()).matches.isEmpty())
        assertTrue(detect("", target("a", "pont")).matches.isEmpty())
    }

    @Test
    fun `exact keyword is found with its range in the original text`() {
        val result = detect("Nous passons le vieux pont ce soir", target("bridge", "pont"))

        assertEquals(mapOf("bridge" to listOf(22..25)), result.matches)
        assertTrue(result.isDetected("bridge"))
        assertFalse(result.isDetected("other"))
    }

    @Test
    fun `undetected targets are absent from the result`() {
        val result = detect("rien ici", target("bridge", "pont"))

        assertFalse(result.matches.containsKey("bridge"))
    }

    @Test
    fun `accents and case are ignored on both sides`() {
        assertTrue(detect("LE CIMETIERE est calme", target("c", "cimetière")).isDetected("c"))
        assertTrue(detect("Le cimetière est calme", target("c", "CIMETIERE")).isDetected("c"))
    }

    @Test
    fun `ligatures match their expanded spelling`() {
        assertTrue(detect("un cœur brisé", target("h", "coeur")).isDetected("h"))
        assertTrue(detect("un coeur brisé", target("h", "cœur")).isDetected("h"))
    }

    @Test
    fun `a keyword does not match inside a longer word`() {
        assertFalse(detect("un pontife et un pontage", target("b", "pont")).isDetected("b"))
        assertFalse(detect("le sapin", target("a", "pin", "api")).isDetected("a"))
    }

    @Test
    fun `elisions are split so the keyword is found`() {
        val text = "Dans l’auberge et d'auberge en auberge"
        val result = detect(text, target("inn", "auberge"))

        assertEquals(listOf(7..13, 20..26, 31..37), result.matches["inn"])
    }

    @Test
    fun `multi token keyword needs contiguous tokens`() {
        val old = target("old", "vieille femme")

        assertEquals(listOf(4..16), detect("Une vieille femme sourit", old).matches["old"])
        assertFalse(detect("Une vieille et sage femme", old).isDetected("old"))
        assertFalse(detect("Une vieille", old).isDetected("old"))
    }

    @Test
    fun `hyphenated keyword matches hyphen or space`() {
        val rainbow = target("r", "arc-en-ciel")

        assertTrue(detect("un arc-en-ciel", rainbow).isDetected("r"))
        assertTrue(detect("un arc en ciel", rainbow).isDetected("r"))
    }

    @Test
    fun `any keyword of a target is enough and ranges are sorted`() {
        val result = detect("des lanternes puis une lanterne", target("l", "lanterne", "lanternes"))

        assertEquals(listOf(4..12, 23..30), result.matches["l"])
    }

    @Test
    fun `identical ranges from several keywords are reported once`() {
        val result = detect("une lanterne", target("l", "lanterne", "Lanterne"))

        assertEquals(listOf(4..11), result.matches["l"])
    }

    @Test
    fun `several targets are detected independently`() {
        val result = detect("la lanterne du pont", target("l", "lanterne"), target("p", "pont"), target("x", "cave"))

        assertEquals(setOf("l", "p"), result.matches.keys)
    }

    @Test
    fun `blank and symbol only keywords are ignored`() {
        val result = detect("du texte", target("a", "", "  ", "--"))

        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun `fuzzy tolerates one typo on a long keyword`() {
        val chandelier = target("c", "chandelier", fuzzy = true)

        assertTrue(detect("un chandelie brille", chandelier).isDetected("c"))
        assertTrue(detect("un chadnelier brille", chandelier).isDetected("c"))
        assertTrue(detect("un chandelier brille", chandelier).isDetected("c"))
        assertFalse(detect("un chandlie brille", chandelier).isDetected("c"))
    }

    @Test
    fun `fuzzy is off by default`() {
        assertFalse(detect("un chandelie brille", target("c", "chandelier")).isDetected("c"))
    }

    @Test
    fun `fuzzy never applies to words shorter than eight letters`() {
        assertFalse(detect("une lanter", target("l", "lanterne", "bougie", fuzzy = true)).isDetected("l"))
        assertFalse(detect("un pone", target("p", "pont", fuzzy = true)).isDetected("p"))
        assertFalse(detect("des bougi", target("b", "bougie", fuzzy = true)).isDetected("b"))
    }

    @Test
    fun `fuzzy applies only to the long token of a multi token keyword`() {
        val target = target("t", "vieille cheminees", fuzzy = true)

        assertTrue(detect("vieille cheminee", target).isDetected("t"))
        assertFalse(detect("vieile cheminees", target).isDetected("t"))
    }

    @Test
    fun `stemmer is applied on text and keywords`() {
        val stemmed = KeywordDetector { it.removeSuffix("s") }

        val result = stemmed.detect("des ponts", listOf(target("b", "pont")))

        assertEquals(listOf(4..8), result.matches["b"])
    }

    @Test
    fun `identity stemmer returns the word unchanged`() {
        assertEquals("pont", IdentityStemmer.stem("pont"))
    }

    @Test
    fun `multi token keyword rejects punctuation between tokens`() {
        val old = target("old", "vieille femme")

        assertFalse(detect("Une vieille. Femme sourit", old).isDetected("old"))
        assertFalse(detect("Une vieille, femme sourit", old).isDetected("old"))
        assertTrue(detect("Une vieille-femme sourit", old).isDetected("old"))
        assertTrue(detect("Une vieille\n femme sourit", old).isDetected("old"))
        assertTrue(detect("Une vieille ’ femme sourit", old).isDetected("old"))
    }

    @Test
    fun `overlapping ranges of one target are merged`() {
        val result = detect("un pont levis", target("p", "pont", "pont levis"))

        assertEquals(listOf(3..12), result.matches["p"])
    }

    @Test
    fun `touching but separate ranges stay distinct`() {
        val result = detect("pont pont", target("p", "pont"))

        assertEquals(listOf(0..3, 5..8), result.matches["p"])
    }

    @Test
    fun `targets copy their lists defensively`() {
        val keywords = mutableListOf("pont")
        val built = DetectionTarget("p", keywords)
        keywords.clear()

        assertEquals(listOf("pont"), built.keywords)
    }

    @Test
    fun `compiled targets can be reused`() {
        val compiled = detector.compile(listOf(target("p", "pont"), target("c", "chandelier", fuzzy = true)))

        assertEquals(setOf("p"), detector.detect("un pont", compiled).matches.keys)
        assertEquals(setOf("c"), detector.detect("un chandelie", compiled).matches.keys)
    }

    @Test
    fun `fuzzy skips words whose length differs by more than one`() {
        val chandelier = target("c", "chandelier", fuzzy = true)

        assertFalse(detect("chandel", chandelier).isDetected("c"))
        assertFalse(detect("chandeliersss", chandelier).isDetected("c"))
    }

    @Test
    fun `detection of a realistic text against five targets stays fast`() {
        val text = "Le voyageur traverse la forêt sombre près d’une auberge, puis il croise un vieux pont. ".repeat(23)
        val compiled =
            detector.compile(
                listOf(
                    target("a", "lanterne", "lanternes", fuzzy = true),
                    target("b", "vieille femme", "sorcière"),
                    target("c", "chandelier", "chandeliers", fuzzy = true),
                    target("d", "pont", "ponts"),
                    target("e", "arc-en-ciel", "auberge"),
                ),
            )
        repeat(200) { detector.detect(text, compiled) }

        val nanos = measureNanoTime { repeat(100) { detector.detect(text, compiled) } } / 100

        assertTrue(nanos < 5_000_000, "took ${nanos / 1_000_000.0} ms")
    }
}
