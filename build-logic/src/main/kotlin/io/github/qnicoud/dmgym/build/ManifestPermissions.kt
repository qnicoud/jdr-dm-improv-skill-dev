package io.github.qnicoud.dmgym.build

import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/** Lecture pure des permissions d'un manifeste Android (testée en JVM). */
object ManifestPermissions {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private val TAGS = listOf("uses-permission", "uses-permission-sdk-23")

    /** Noms de toutes les permissions déclarées (`uses-permission` et `uses-permission-sdk-23`). */
    fun declared(manifestXml: String): List<String> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(manifestXml)))
        return TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NS, "name") }
        }
    }

    /** Permissions déclarées qui ne figurent pas dans la liste blanche. */
    fun violations(manifestXml: String, allowed: List<String>): List<String> =
        declared(manifestXml).filterNot { it in allowed }.distinct()
}
