package io.github.qnicoud.dmgym.platform

import android.content.res.Configuration
import android.os.LocaleList
import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigurationLanguageTest {
    private fun config(tags: String) = Configuration().apply { setLocales(LocaleList.forLanguageTags(tags)) }

    @Test
    fun `first shipped language of the phone wins`() {
        assertEquals(ContentLanguage.FR, config("de-DE,fr-CA,en").contentLanguage())
    }

    @Test
    fun `unsupported or empty list falls back to french`() {
        assertEquals(ContentLanguage.FR, config("ja-JP,de").contentLanguage())
        assertEquals(ContentLanguage.FR, Configuration().contentLanguage())
    }
}
