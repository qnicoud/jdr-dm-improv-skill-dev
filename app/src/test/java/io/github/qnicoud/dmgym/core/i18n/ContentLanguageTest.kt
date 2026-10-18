package io.github.qnicoud.dmgym.core.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ContentLanguageTest {
    @Test
    fun `french is the default and the only shipped language`() {
        assertEquals(ContentLanguage.FR, ContentLanguage.DEFAULT)
        assertEquals(listOf(ContentLanguage.FR), ContentLanguage.entries)
        assertEquals("fr", ContentLanguage.FR.code)
    }

    @Test
    fun `resolve takes the first supported language of the list`() {
        assertEquals(ContentLanguage.FR, ContentLanguage.resolve(listOf("de-DE", "fr-CA", "en")))
    }

    @Test
    fun `resolve falls back silently to french`() {
        assertEquals(ContentLanguage.FR, ContentLanguage.resolve(listOf("de", "ja-JP")))
        assertEquals(ContentLanguage.FR, ContentLanguage.resolve(emptyList()))
    }

    @Test
    fun `resolve ignores case and region`() {
        assertEquals(ContentLanguage.FR, ContentLanguage.resolve(listOf("FR_be")))
    }

    @Test
    fun `fromCode reads stored codes`() {
        assertEquals(ContentLanguage.FR, ContentLanguage.fromCode("fr"))
        assertNull(ContentLanguage.fromCode("xx"))
        assertNull(ContentLanguage.fromCode(null))
    }

    @Test
    fun `locale is built from the code`() {
        assertEquals("fr", ContentLanguage.FR.locale.language)
    }

    @Test
    fun `fixed provider exposes its language`() {
        assertEquals(ContentLanguage.FR, LanguageProvider.Fixed(ContentLanguage.FR).current.value)
    }
}
