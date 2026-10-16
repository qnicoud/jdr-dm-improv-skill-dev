package io.github.qnicoud.dmgym.core.detection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenizerTest {
    private fun texts(input: String) = Tokenizer.tokenize(input).map { it.text }

    @Test
    fun `empty and symbol only text yield no token`() {
        assertTrue(Tokenizer.tokenize("").isEmpty())
        assertTrue(Tokenizer.tokenize("  12 -- !? ").isEmpty())
    }

    @Test
    fun `tokens keep their position in the original text`() {
        val tokens = Tokenizer.tokenize("Le  vieux pont")

        assertEquals(listOf("Le", "vieux", "pont"), tokens.map { it.text })
        assertEquals(listOf(0..1, 4..8, 10..13), tokens.map { it.range })
    }

    @Test
    fun `straight and curly apostrophes split elisions`() {
        assertEquals(listOf("l", "auberge", "qu", "il"), texts("l'auberge qu’il"))
    }

    @Test
    fun `hyphens split tokens`() {
        assertEquals(listOf("arc", "en", "ciel"), texts("arc-en-ciel"))
    }

    @Test
    fun `accented and ligature letters stay inside a token`() {
        assertEquals(listOf("cimetière", "œuf", "cæcum"), texts("cimetière, œuf; cæcum"))
    }

    @Test
    fun `decomposed accents stay attached to their letter`() {
        assertEquals(listOf("été"), texts("été"))
    }
}
