package io.github.qnicoud.dmgym.core.detection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class TextNormalizerTest {
    @Test
    fun `accents are removed`() {
        assertEquals("cimetiere", TextNormalizer.normalize("cimetière"))
        assertEquals("ecole", TextNormalizer.normalize("École"))
        assertEquals("francais", TextNormalizer.normalize("français"))
    }

    @Test
    fun `decomposed and composed forms are equal`() {
        assertEquals(TextNormalizer.normalize("é"), TextNormalizer.normalize("é"))
    }

    @Test
    fun `ligatures are expanded`() {
        assertEquals("coeur", TextNormalizer.normalize("cœur"))
        assertEquals("oeuf", TextNormalizer.normalize("ŒUF"))
        assertEquals("caecum", TextNormalizer.normalize("cæcum"))
        assertEquals("aerien", TextNormalizer.normalize("Ærien"))
    }

    @Test
    fun `case is ignored`() {
        assertEquals("lanterne", TextNormalizer.normalize("LaNtErNe"))
    }

    @Test
    fun `lowercasing ignores the default locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            assertEquals("incendie", TextNormalizer.normalize("INCENDIE"))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
