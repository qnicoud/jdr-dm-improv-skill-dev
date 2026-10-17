package io.github.qnicoud.dmgym.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** D26: no cloud backup; device-to-device transfer only. */
class BackupRulesTest {
    private val domains = setOf("root", "file", "database", "sharedpref", "external")

    private fun parse(name: String): Element =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/xml/$name.xml"))
            .documentElement

    private fun section(
        name: String,
        tag: String,
    ): Element = parse(name).getElementsByTagName(tag).item(0) as Element

    private fun domainsOf(
        parent: Element,
        tag: String,
    ): Set<String> {
        val nodes = parent.getElementsByTagName(tag)
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("domain") }.toSet()
    }

    @Test
    fun `cloud backup excludes everything and includes nothing`() {
        val cloud = section("data_extraction_rules", "cloud-backup")

        assertEquals(domains, domainsOf(cloud, "exclude"))
        assertTrue(domainsOf(cloud, "include").isEmpty())
    }

    @Test
    fun `device transfer includes database and preferences`() {
        val transfer = section("data_extraction_rules", "device-transfer")

        assertTrue(domainsOf(transfer, "include").containsAll(setOf("database", "file", "root")))
        assertTrue(domainsOf(transfer, "exclude").isEmpty())
    }

    @Test
    fun `legacy full backup excludes everything`() {
        assertEquals(domains, domainsOf(parse("backup_rules"), "exclude"))
    }

    @Test
    fun `manifest references both rule files`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue("@xml/data_extraction_rules" in manifest)
        assertTrue("@xml/backup_rules" in manifest)
    }
}
