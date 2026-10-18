package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.i18n.ContentLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** ADR 0016 §6 / D55 : le manifeste déclare les langues de l'app, qui sont celles du contenu livré. */
class LocaleConfigTest {
    private fun parse(path: String): Element =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(File(path))
            .documentElement

    @Test
    fun `the manifest points to the locale config`() {
        val application = parse("src/main/AndroidManifest.xml").getElementsByTagName("application").item(0) as Element

        assertEquals("@xml/locales_config", application.getAttribute("android:localeConfig"))
    }

    @Test
    fun `the locale config lists exactly the shipped content languages`() {
        val nodes = parse("src/main/res/xml/locales_config.xml").getElementsByTagName("locale")
        val tags = (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("android:name") }

        assertEquals(ContentLanguage.entries.map { it.code }, tags)
    }

    @Test
    fun `the default resources stay french`() {
        assertTrue(File("src/main/res/values/strings.xml").exists())
        assertEquals(ContentLanguage.FR, ContentLanguage.DEFAULT)
    }
}
